package me.unariginal.novaraids.config;

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

    public ZoneId zone = ZoneId.systemDefault();
    public List<Schedule> schedules = new ArrayList<>();

    public SchedulesConfig() {
        try {
            loadSchedules();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            nr.loadedProperly = false;
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

        String location = "schedules";

        if (ConfigHelper.checkProperty(config, "timezone", location)) {
            String timezone = config.get("timezone").getAsString();
            try {
                zone = ZoneId.of(ZoneId.SHORT_IDS.getOrDefault(timezone.toUpperCase(), ZoneId.systemDefault().getId()));
            } catch (ZoneRulesException e) {
                zone = ZoneId.systemDefault();
                nr.logError("[RAIDS] Failed to parse timezone " + timezone + ". Using default timezone.");
            }
        }
        if (ConfigHelper.checkProperty(config, "schedules", location)) {
            JsonArray schedules = config.getAsJsonArray("schedules");
            for (JsonElement s : schedules) {
                JsonObject schedule = s.getAsJsonObject();
                if (ConfigHelper.checkProperty(schedule, "type", location)) {
                    String type = schedule.get("type").getAsString();
                    List<ScheduleBoss> bosses = new ArrayList<>();
                    if (ConfigHelper.checkProperty(schedule, "bosses", location)) {
                        JsonArray boss_array = schedule.getAsJsonArray("bosses");
                        for (JsonElement b : boss_array) {
                            JsonObject boss = b.getAsJsonObject();
                            if (ConfigHelper.checkProperty(boss, "type", location)) {
                                String boss_type = boss.get("type").getAsString();
                                if (ConfigHelper.checkProperty(boss, "weight", location)) {
                                    double weight = boss.get("weight").getAsDouble();
                                    if (boss_type.equalsIgnoreCase("category")) {
                                        if (ConfigHelper.checkProperty(boss, "category", location)) {
                                            String category = boss.get("category").getAsString();
                                            bosses.add(new ScheduleBoss(boss_type, category, weight));
                                        }
                                    } else if (boss_type.equalsIgnoreCase("boss")) {
                                        if (ConfigHelper.checkProperty(boss, "boss_id", location)) {
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
                        if (ConfigHelper.checkProperty(schedule, "bounds", location)) {
                            JsonObject bounds = schedule.getAsJsonObject("bounds");
                            if (ConfigHelper.checkProperty(bounds, "min", location) && ConfigHelper.checkProperty(bounds, "max", location)) {
                                int min = bounds.get("min").getAsInt();
                                int max = bounds.get("max").getAsInt();
                                this.schedules.add(new RandomSchedule(type, bosses, min, max));
                            }
                        }
                    } else if (type.equalsIgnoreCase("cron")) {
                        if (ConfigHelper.checkProperty(schedule, "expression", location)) {
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
                        if (ConfigHelper.checkProperty(schedule, "times", location)) {
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
