package me.unariginal.novaraids.config;

import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.schedule.*;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.core.util.CronExpression;

import java.io.*;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.List;

public class SchedulesConfig {
    private final NovaRaids nr = NovaRaids.INSTANCE;

    ZoneId zone = ZoneId.systemDefault();
    List<Schedule> schedules = new ArrayList<>();

    public SchedulesConfig() {
        try {
            loadSchedules();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            nr.loaded_properly = false;
            nr.logError("[RAIDS] Failed to load schedules file. " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                nr.logError("  " + element.toString());
            }
        }
    }

    public void loadSchedules() throws IOException, NullPointerException, UnsupportedOperationException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        File file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/schedules.json").toFile();
        if (file.createNewFile()) {
            InputStream stream = NovaRaids.class.getResourceAsStream("/raid_config_files/schedules.json");
            assert stream != null;
            OutputStream out = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = stream.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            stream.close();
            out.close();
        }

        JsonElement root = JsonParser.parseReader(new FileReader(file));
        assert root != null;
        JsonObject config = root.getAsJsonObject();

        if (checkProperty(config, "timezone")) {
            String timezone = config.get("timezone").getAsString();
            try {
                zone = ZoneId.of(ZoneId.SHORT_IDS.getOrDefault(timezone, timezone));
            } catch (ZoneRulesException e) {
                nr.logError("[RAIDS] Failed to parse timezone " + timezone + ". Using default timezone.");
            }
        }
        if (checkProperty(config, "schedules")) {
            JsonArray schedules = config.getAsJsonArray("schedules");
            for (JsonElement s : schedules) {
                JsonObject schedule = s.getAsJsonObject();
                if (checkProperty(schedule, "type")) {
                    String type = schedule.get("type").getAsString();
                    List<ScheduleBoss> bosses = new ArrayList<>();
                    if (checkProperty(schedule, "bosses")) {
                        JsonArray boss_array = schedule.getAsJsonArray("bosses");
                        for (JsonElement b : boss_array) {
                            JsonObject boss = b.getAsJsonObject();
                            if (checkProperty(boss, "type")) {
                                String boss_type = boss.get("type").getAsString();
                                if (checkProperty(boss, "weight")) {
                                    double weight = boss.get("weight").getAsDouble();
                                    if (boss_type.equalsIgnoreCase("category")) {
                                        if (checkProperty(boss, "category")) {
                                            String category = boss.get("category").getAsString();
                                            bosses.add(new ScheduleBoss(boss_type, category, weight));
                                        }
                                    } else if (boss_type.equalsIgnoreCase("boss")) {
                                        if (checkProperty(boss, "boss_id")) {
                                            String boss_id = boss.get("boss_id").getAsString();
                                            bosses.add(new ScheduleBoss(boss_type, boss_id, weight));
                                        }
                                    } else {
                                        nr.logError("[RAIDS] Invalid boss type " + boss_type + " in schedules.json");
                                    }
                                }
                            }
                        }
                    }

                    if (type.equalsIgnoreCase("random")) {
                        if (checkProperty(schedule, "bounds")) {
                            JsonObject bounds = schedule.getAsJsonObject("bounds");
                            if (checkProperty(bounds, "min") && checkProperty(bounds, "max")) {
                                int min = bounds.get("min").getAsInt();
                                int max = bounds.get("max").getAsInt();
                                this.schedules.add(new RandomSchedule(type, bosses, min, max));
                            }
                        }
                    } else if (type.equalsIgnoreCase("cron")) {
                        if (checkProperty(schedule, "expression")) {
                            String expression = schedule.get("expression").getAsString();
                            if (CronExpression.isValidExpression(expression)
                                    || expression.equals("@hourly")
                                    || expression.equals("@weekly")
                                    || expression.equals("@monthly")
                                    || expression.equals("@yearly")
                                    || expression.equals("@daily")
                                    || expression.equals("@midnight")
                                    || expression.equals("@annually")) {
                                this.schedules.add(new CronSchedule(type, bosses, expression));
                            } else {
                                nr.logError("[RAIDS] Invalid expression " + expression + " in schedules.json");
                            }
                        }
                    } else if (type.equalsIgnoreCase("specific")) {
                        if (checkProperty(schedule, "times")) {
                            JsonArray times = schedule.getAsJsonArray("times");
                            List<LocalTime> timesList = new ArrayList<>();
                            for (JsonElement t : times) {
                                String time = t.getAsString();
                                timesList.add(LocalTime.parse(time));
                            }
                            this.schedules.add(new SpecificSchedule(type, bosses, timesList));
                        }
                    } else {
                        nr.logError("[RAIDS] Invalid schedule type: " + type);
                    }
                }
            }
        }
    }

    public boolean checkProperty(JsonObject section, String property) {
        if (section.has(property)) {
            return true;
        }
        nr.logError("[RAIDS] Missing " + property + " property in schedules.json. Using default value(s) or skipping.");
        return false;
    }

    public Schedule getSchedule(String boss) {
        for (Schedule schedule : this.schedules) {
            for (ScheduleBoss scheduleBoss : schedule.bosses) {
                if (scheduleBoss.id().equalsIgnoreCase(boss)) {
                    return schedule;
                }
            }
        }
        return null;
    }
}
