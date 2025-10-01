package me.unariginal.novaraids.config;

import com.google.gson.*;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.schedule.*;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.core.util.CronExpression;

import java.io.*;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class SchedulesConfig {
    private final NovaRaids nr = NovaRaids.INSTANCE;

    public ZoneId zone = ZoneId.systemDefault();
    public List<Schedule> schedules = new ArrayList<>(
            List.of(
                    new RandomSchedule(
                            "random",
                            List.of(
                                    new ScheduleBoss(
                                            "category",
                                            "common",
                                            5.0
                                    ),
                                    new ScheduleBoss(
                                            "boss",
                                            "example_eevee",
                                            5.0
                                    )
                            ),
                            1200,
                            3600
                    ),
                    new CronSchedule(
                            "cron",
                            List.of(
                                    new ScheduleBoss(
                                            "category",
                                            "common",
                                            5.0
                                    ),
                                    new ScheduleBoss(
                                            "boss",
                                            "example_eevee",
                                            5.0
                                    )
                            ),
                            "0 30 * ? * *"
                    ),
                    new CronSchedule(
                            "cron",
                            List.of(
                                    new ScheduleBoss(
                                            "category",
                                            "common",
                                            5.0
                                    ),
                                    new ScheduleBoss(
                                            "boss",
                                            "example_eevee",
                                            5.0
                                    )
                            ),
                            "@hourly"
                    ),
                    new SpecificSchedule(
                            "specific",
                            List.of(
                                    new ScheduleBoss(
                                            "category",
                                            "common",
                                            5.0
                                    ),
                                    new ScheduleBoss(
                                            "boss",
                                            "example_eevee",
                                            5.0
                                    )
                            ),
                            List.of(
                                    LocalTime.parse("00:00:00"),
                                    LocalTime.parse("00:15:00"),
                                    LocalTime.parse("00:30:00"),
                                    LocalTime.parse("00:45:00"),
                                    LocalTime.parse("01:00:00"),
                                    LocalTime.parse("01:43:01"),
                                    LocalTime.parse("01:54:32"),
                                    LocalTime.parse("01:59:59")
                            )
                    )
            )
    );

    public SchedulesConfig() {
        try {
            loadSchedules();
        } catch (IOException | NullPointerException | UnsupportedOperationException | DateTimeException e) {
            NovaRaids.LOADED = false;
            NovaRaids.LOGGER.error("[NovaRaids] Failed to load schedules file.", e);
        }
    }

    public void loadSchedules() throws IOException, NullPointerException, UnsupportedOperationException, DateTimeException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        File file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/schedules.json").toFile();
        JsonObject config = new JsonObject();
        if (file.exists()) config = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

        String zoneIDRaw = "EST";
        if (config.has("timezone"))
            zoneIDRaw = config.get("timezone").getAsString();

        if (ZoneId.getAvailableZoneIds().contains(zoneIDRaw)) {
            zone = ZoneId.of(zoneIDRaw);
        } else if (ZoneId.SHORT_IDS.containsKey(zoneIDRaw.toUpperCase())) {
            String shortID = ZoneId.SHORT_IDS.get(zoneIDRaw.toUpperCase());
            if (shortID.startsWith("+") || shortID.startsWith("-")) {
                zone = ZoneId.ofOffset("UTC", ZoneOffset.of(ZoneId.SHORT_IDS.get(zoneIDRaw.toUpperCase())));
            } else {
                zone = ZoneId.of(shortID);
            }
        } else {
            try {
                zone = ZoneId.of(zoneIDRaw);
            } catch (DateTimeException e) {
                zone = ZoneId.systemDefault();
                NovaRaids.LOGGER.error("[NovaRaids] Failed to parse timezone id: {}. Using system default.", zoneIDRaw);
            }
        }

        config.remove("timezone");
        config.addProperty("timezone", zone.getId());

        JsonArray schedulesArray = new JsonArray();
        if (config.has("schedules"))
            schedulesArray = config.getAsJsonArray("schedules");

        if (!schedulesArray.isEmpty()) schedules.clear();

        for (JsonElement scheduleElement : schedulesArray) {
            JsonObject scheduleObject = scheduleElement.getAsJsonObject();

            if (!scheduleObject.has("type")) continue;
            String type = scheduleObject.get("type").getAsString();

            List<ScheduleBoss> scheduleBosses = new ArrayList<>();
            JsonArray bossesArray = new JsonArray();
            if (scheduleObject.has("bosses"))
                bossesArray = scheduleObject.getAsJsonArray("bosses");
            for (JsonElement bossElement : bossesArray) {
                JsonObject bossObject = bossElement.getAsJsonObject();

                if (!bossObject.has("type")) continue;
                String bossType = bossObject.get("type").getAsString();

                double bossWeight = 1;
                if (bossObject.has("weight"))
                    bossWeight = bossObject.get("weight").getAsDouble();
                bossObject.remove("weight");
                bossObject.addProperty("weight", bossWeight);

                if (bossType.equalsIgnoreCase("category")) {
                    String category = bossObject.get("category").getAsString();
                    scheduleBosses.add(new ScheduleBoss(bossType, category, bossWeight));
                } else if (bossType.equalsIgnoreCase("boss")) {
                    String bossID = bossObject.get("boss_id").getAsString();
                    scheduleBosses.add(new ScheduleBoss(bossType, bossID, bossWeight));
                }
            }

            if (type.equalsIgnoreCase("random")) {
                int min = 3600;
                int max = 7200;

                JsonObject boundsObject = new JsonObject();
                if (scheduleObject.has("bounds"))
                    boundsObject = scheduleObject.getAsJsonObject("bounds");

                if (boundsObject.has("min"))
                    min = boundsObject.get("min").getAsInt();
                boundsObject.remove("min");
                boundsObject.addProperty("min", min);

                if (boundsObject.has("max"))
                    max = boundsObject.get("max").getAsInt();
                boundsObject.remove("max");
                boundsObject.addProperty("max", max);

                schedules.add(new RandomSchedule(type, scheduleBosses, min, max));
            } else if (type.equalsIgnoreCase("cron")) {
                if (!scheduleObject.has("expression")) continue;
                String expression = scheduleObject.get("expression").getAsString();
                if (CronExpression.isValidExpression(expression)
                        || expression.equals("@hourly")
                        || expression.equals("@weekly")
                        || expression.equals("@monthly")
                        || expression.equals("@yearly")
                        || expression.equals("@daily")
                        || expression.equals("@midnight")
                        || expression.equals("@annually"))
                    this.schedules.add(new CronSchedule(type, scheduleBosses, expression));
                else nr.logError("Invalid cron expression " + expression + " in schedules.json");
            } else if (type.equalsIgnoreCase("specific")) {
                if (!scheduleObject.has("times")) continue;
                JsonArray timesArray = scheduleObject.getAsJsonArray("times");
                List<LocalTime> localTimes = new ArrayList<>();
                for (JsonElement timeElement : timesArray) {
                    String time = timeElement.getAsString();
                    try {
                        localTimes.add(LocalTime.parse(time));
                    } catch (DateTimeParseException e) {
                        nr.logError("Invalid specific time format " + time + " in schedules.json");
                    }
                }
                schedules.add(new SpecificSchedule(type, scheduleBosses, localTimes));
            } else
                nr.logError("Invalid schedule type " + type + " in schedules.json");
        }

        schedulesArray = new JsonArray();
        for (Schedule schedule : schedules) {
            JsonObject scheduleObject = new JsonObject();
            if (schedule instanceof RandomSchedule randomSchedule) {
                scheduleObject.addProperty("type", "random");
                JsonObject boundsObject = new JsonObject();
                boundsObject.addProperty("min", randomSchedule.minBound);
                boundsObject.addProperty("max", randomSchedule.maxBound);
                scheduleObject.add("bounds", boundsObject);
            } else if (schedule instanceof CronSchedule cronSchedule) {
                scheduleObject.addProperty("type", "cron");
                scheduleObject.addProperty("expression", cronSchedule.expression);
            } else if (schedule instanceof SpecificSchedule specificSchedule) {
                scheduleObject.addProperty("type", "specific");
                JsonArray timesArray = new JsonArray();
                for (LocalTime time : specificSchedule.setTimes) {
                    timesArray.add(time.toString());
                }
                scheduleObject.add("times", timesArray);
            } else continue;

            JsonArray bossesArray = new JsonArray();
            for (ScheduleBoss scheduleBoss : schedule.bosses) {
                JsonObject bossObject = new JsonObject();
                String bossType = scheduleBoss.type();
                bossObject.addProperty("type", bossType);
                if (bossType.equalsIgnoreCase("category")) {
                    bossObject.addProperty("category", scheduleBoss.id());
                } else if (bossType.equalsIgnoreCase("boss")) {
                    bossObject.addProperty("boss_id", scheduleBoss.id());
                } else continue;
                bossObject.addProperty("weight", scheduleBoss.weight());
                bossesArray.add(bossObject);
            }

            scheduleObject.add("bosses", bossesArray);

            schedulesArray.add(scheduleObject);
        }

        config.remove("schedules");
        config.add("schedules", schedulesArray);

        file.delete();
        file.createNewFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(file);
        gson.toJson(config, writer);
        writer.close();
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
