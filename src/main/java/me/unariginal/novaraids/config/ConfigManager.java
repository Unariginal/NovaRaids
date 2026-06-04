package me.unariginal.novaraids.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.config.guis.*;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
import me.unariginal.novaraids.data.events.Event;
import me.unariginal.novaraids.data.schedule.SpecificSchedule;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static me.unariginal.novaraids.NovaRaids.LOGGER;
import static me.unariginal.novaraids.utils.GsonUtils.gson;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ConfigManager {
    public static File configDir;

    public static Config CONFIG;
    public static SchedulesConfig SCHEDULES;
    public static MessagesConfig MESSAGES;

    public static Map<String, LocationsConfig> LOCATIONS;
    public static Map<String, BossbarsConfig> BOSSBARS;
    public static Map<String, RewardPresetsConfig.Reward> REWARD_PRESETS;
    public static Map<String, RewardPoolsConfig.RewardPool> REWARD_POOLS;
    public static Map<String, Category> CATEGORIES = new HashMap<>();
    public static Map<String, CategoryModifier> CATEGORY_MODIFIERS = new HashMap<>();
    public static Map<String, Boss> BOSSES = new HashMap<>();
    public static Map<Identifier, Event> EVENTS = new HashMap<>();

    public static ContrabandGUIConfig GLOBAL_CONTRABAND_GUI;
    public static ContrabandGUIConfig CATEGORY_CONTRABAND_GUI;
    public static ContrabandGUIConfig BOSS_CONTRABAND_GUI;
    public static LeaderboardGUIConfig LEADERBOARD_GUI;
    public static RaidListGUIConfig RAID_LIST_GUI;
    public static RaidQueueGUIConfig RAID_QUEUE_GUI;
    public static RaidItemGUIConfig RAID_PASS_GUI;
    public static RaidItemGUIConfig RAID_VOUCHER_GUI;

    public static PersistentQueue PERSISTENT_QUEUE = new PersistentQueue();

    public static String[] eventNames = {
            "boss_damaged",
            "boss_defeated",
            "catch_phase",
            "catch_warning_phase",
            "fight_phase",
            "raid_end",
            "raid_lost",
            "raid_start",
            "setup_phase"
    };

    public static void load() {
        configDir = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();

        generateDefaultFiles();

        fillMissingWithDefaults("config.json", null, false);
        fillMissingWithDefaults("schedules.json", null, false);
        fillMissingWithDefaults("messages.json", null, false);
        fillMissingWithDefaults("guis/global_contraband.json", null, false);
        fillMissingWithDefaults("guis/category_contraband.json", null, false);
        fillMissingWithDefaults("guis/boss_contraband.json", null, false);
        fillMissingWithDefaults("guis/leaderboard.json", null, false);
        fillMissingWithDefaults("guis/raid_list.json", null, false);
        fillMissingWithDefaults("guis/raid_queue.json", null, false);
        fillMissingWithDefaults("guis/raid_pass.json", null, false);
        fillMissingWithDefaults("guis/raid_voucher.json", null, false);

        CONFIG = loadFile("config.json", Config.class);
        SCHEDULES = loadFile("schedules.json", SchedulesConfig.class);
        if (SCHEDULES != null) {
            SCHEDULES.schedules.forEach(schedule -> {
                if (schedule instanceof SpecificSchedule specificSchedule) {
                    specificSchedule.fillLocalTimes();
                }
            });
        }
        MESSAGES = loadFile("messages.json", MessagesConfig.class);

        fillMissingWithDefaults("locations.json", null, true);
        LOCATIONS = loadMapFile("locations.json", LocationsConfig.class);
        for (Map.Entry<String, LocationsConfig> entry : LOCATIONS.entrySet()) {
            entry.getValue().locationId = entry.getKey();
        }

        fillMissingWithDefaults("bossbars.json", null, true);
        BOSSBARS = loadMapFile("bossbars.json", BossbarsConfig.class);
        for (Map.Entry<String, BossbarsConfig> entry : BOSSBARS.entrySet()) {
            entry.getValue().bossbarId = entry.getKey();
        }

        fillMissingWithDefaults("reward_presets.json", null, true);
        REWARD_PRESETS = loadMapFile("reward_presets.json", RewardPresetsConfig.Reward.class);
        for (Map.Entry<String, RewardPresetsConfig.Reward> entry : REWARD_PRESETS.entrySet()) {
            entry.getValue().rewardId = entry.getKey();
        }

        fillMissingWithDefaults("reward_pools.json", null, true);
        REWARD_POOLS = loadMapFile("reward_pools.json", RewardPoolsConfig.RewardPool.class);
        for (Map.Entry<String, RewardPoolsConfig.RewardPool> entry : REWARD_POOLS.entrySet()) {
            entry.getValue().rewardPoolId = entry.getKey();
        }

        GLOBAL_CONTRABAND_GUI = loadFile("guis/global_contraband.json", ContrabandGUIConfig.class);
        CATEGORY_CONTRABAND_GUI = loadFile("guis/category_contraband.json", ContrabandGUIConfig.class);
        BOSS_CONTRABAND_GUI = loadFile("guis/boss_contraband.json", ContrabandGUIConfig.class);
        LEADERBOARD_GUI = loadFile("guis/leaderboard.json", LeaderboardGUIConfig.class);
        RAID_LIST_GUI = loadFile("guis/raid_list.json", RaidListGUIConfig.class);
        RAID_QUEUE_GUI = loadFile("guis/raid_queue.json", RaidQueueGUIConfig.class);
        RAID_PASS_GUI = loadFile("guis/raid_pass.json", RaidItemGUIConfig.class);
        RAID_VOUCHER_GUI = loadFile("guis/raid_voucher.json", RaidItemGUIConfig.class);

        loadCategories();
        loadEvents();
    }

    public static void saveQueue() {
        File persistentFolder = new File(configDir, "persistent");
        persistentFolder.mkdirs();
        File queueFile = new File(persistentFolder, "queue.json");

        writeFile(queueFile, gson.toJson(PERSISTENT_QUEUE));
    }

    public static void loadQueue() {
        PERSISTENT_QUEUE = loadFile("persistent/queue.json", PersistentQueue.class);
    }

    public static void saveRaid(RaidHistory raidHistory) {
        File historyFolder = new File(configDir, "persistent/history");
        historyFolder.mkdirs();
        File historyFile = new File(historyFolder, raidHistory.uuid + ".json");
        writeFile(historyFile, gson.toJson(raidHistory));
    }

    public static void generateDefaultFiles() {
        generateDefaultFile("config.json");
        generateDefaultFile("schedules.json");
        generateDefaultFile("messages.json");
        generateDefaultFile("locations.json");
        generateDefaultFile("bossbars.json");
        generateDefaultFile("reward_presets.json");
        generateDefaultFile("reward_pools.json");
        generateDefaultFile("guis/global_contraband.json");
        generateDefaultFile("guis/category_contraband.json");
        generateDefaultFile("guis/boss_contraband.json");
        generateDefaultFile("guis/leaderboard.json");
        generateDefaultFile("guis/raid_list.json");
        generateDefaultFile("guis/raid_queue.json");
        generateDefaultFile("guis/raid_pass.json");
        generateDefaultFile("guis/raid_voucher.json");
        generateDefaultFile("persistent/queue.json");

        File categoriesFolder = new File(configDir, "categories");
        File bossesFolder = new File(configDir, "bosses");
        if (bossesFolder.exists()) {
            bossesFolder.renameTo(categoriesFolder);
        }
        if (!categoriesFolder.exists()) {
            generateDefaultFile("categories/common/settings.json");
            generateDefaultFile("categories/common/bosses/example_eevee.json");
            generateDefaultFile("categories/common/modifiers/example_modifier.json");
        }

        for (String eventName : eventNames) {
            File fileCheck = new File(configDir, "events/" + eventName);
            if (fileCheck.exists()) continue;

            generateDefaultFile("events/" + eventName + "/pre/default.json");
            generateDefaultFile("events/" + eventName + "/post/default.json");
        }
    }

    public static void loadCategories() {
        File categoriesFolder = new File(configDir, "categories");
        File oldBossesFolder = new File(configDir, "bosses");
        if (oldBossesFolder.exists()) {
            oldBossesFolder.renameTo(categoriesFolder);
        }

        File[] categoryFolders = categoriesFolder.listFiles();
        if (categoryFolders != null) {
            for (File categoryFolder : categoryFolders) {
                if (categoryFolder.isDirectory()) {
                    String categoryFileName = "categories/" + categoryFolder.getName() + "/settings.json";
                    String defaultCategoryFileName = "categories/common/settings.json";
                    fillMissingWithDefaults(categoryFileName, defaultCategoryFileName, false);
                    Category category = loadFile(categoryFileName, Category.class);
                    if (category != null) {
                        CATEGORIES.put(category.categoryId, category);
                    }
                    else continue;

                    File modifiersFolder = new File(categoryFolder, "modifiers");
                    File[] modifierFiles = modifiersFolder.listFiles();
                    if (modifierFiles != null) {
                        for (File modifierFile : modifierFiles) {
                            if (modifierFile.getName().endsWith(".json")) {
                                String modifierFileName = "categories/" + categoryFolder.getName() + "/modifiers/" + modifierFile.getName();
                                String defaultModifierFileName = "categories/common/modifiers/example_modifier.json";
                                fillMissingWithDefaults(modifierFileName, defaultModifierFileName, false);
                                CategoryModifier categoryModifier = loadFile(modifierFileName, CategoryModifier.class);
                                if (categoryModifier != null) {
                                    categoryModifier.categoryId = category.categoryId;
                                    CATEGORY_MODIFIERS.put(categoryModifier.modifierId, categoryModifier);
                                }
                            }
                        }
                    }

                    category.fillModifiers();

                    File bossesFolder = new File(categoryFolder, "bosses");
                    File[] bossFiles = bossesFolder.listFiles();
                    if (bossFiles != null) {
                        for (File bossFile : bossFiles) {
                            if (bossFile.getName().endsWith(".json")) {
                                String bossFileName = "categories/" + categoryFolder.getName() + "/bosses/" + bossFile.getName();
                                String defaultBossFileName = "categories/common/bosses/example_eevee.json";
                                fillMissingWithDefaults(bossFileName, defaultBossFileName, false);
                                Boss boss = loadFile(bossFileName, Boss.class);
                                if (boss != null)  {
                                    boss.categoryId = category.categoryId;
                                    BOSSES.put(boss.bossId, boss);
                                }
                            }
                        }
                    }

                    category.fillBosses();
                }
            }
        }
    }

    public static void loadEvents() {
        File eventsFolder = new File(configDir, "events");

        for (String eventName : eventNames) {
            File eventFolder = new File(eventsFolder, eventName);
            File preFolder = new File(eventFolder, "pre");
            File postFolder = new File(eventFolder, "post");

            Event preEvent = null;
            Event postEvent = null;

            File[] preEventFiles = preFolder.listFiles();
            if (preEventFiles != null) {
                for (File preEventFile : preEventFiles) {
                    if (preEventFile.getName().endsWith(".json")) {
                        String preEventFileName = "events/" + eventName + "/pre/" + preEventFile.getName();
                        String defaultEventFileName = "events/" + eventName + "/pre/default.json";
                        fillMissingWithDefaults(preEventFileName, defaultEventFileName, false);
                        preEvent = loadFile(preEventFileName, Event.class);
                        if (preEvent != null) preEvent.eventId = preEventFile.getName().replace(".json", "");
                    }
                }
            }

            File[] postEventFiles = postFolder.listFiles();
            if (postEventFiles != null) {
                for (File postEventFile : postEventFiles) {
                    if (postEventFile.getName().endsWith(".json")) {
                        String postEventFileName = "events/" + eventName + "/post/" + postEventFile.getName();
                        String defaultEventFileName = "events/" + eventName + "/post/default.json";
                        fillMissingWithDefaults(postEventFileName, defaultEventFileName, false);
                        postEvent = loadFile(postEventFileName, Event.class);
                        if (postEvent != null) postEvent.eventId = postEventFile.getName().replace(".json", "");
                    }
                }
            }

            if (preEvent != null) EVENTS.put(Identifier.of(eventName + "_pre", preEvent.eventId), preEvent);
            if (postEvent != null) EVENTS.put(Identifier.of(eventName + "_post", postEvent.eventId), postEvent);
        }
    }

    public static <T> Map<String, T> loadMapFile(String fileName, Class<T> clazz) {
        File file = new File(configDir, fileName);
        Type mapType = TypeToken.getParameterized(Map.class, String.class, clazz).getType();

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                return gson.fromJson(reader, mapType);
            } catch (IOException e) {
                LOGGER.error("[NovaRaids] Failed to load config file {}", fileName, e);
            }
        }
        LOGGER.error("[NovaRaids] Error loading config file {}. File does not exist!", fileName);
        return Map.of();
    }

    public static <T> T loadFile(String fileName, Class<T> clazz) {
        File file = new File(configDir, fileName);
        if (file.exists()) {
            try {
                String jsonString = JsonParser.parseReader(new FileReader(file)).toString();
                return gson.fromJson(jsonString, clazz);
            } catch (IOException e) {
                LOGGER.error("[NovaRaids] Failed to load config file {}", fileName, e);
            }
        }
        LOGGER.error("[NovaRaids] Error loading config file {}. File does not exist!", fileName);
        return null;
    }

    public static String getDefaultJsonString(String fileName) {
        InputStream in = NovaRaids.class.getResourceAsStream("/raid_config_files/" + fileName);
        assert in != null;
        return gson.toJson(JsonParser.parseReader(new InputStreamReader(in)));
    }

    public static void generateDefaultFile(String fileName) {
        File file = new File(configDir, fileName);
        if (file.exists()) return;
        try {
            Files.createDirectories(file.getParentFile().toPath());
            Files.createFile(file.toPath());
            writeFile(file, getDefaultJsonString(fileName));
        } catch (IOException e) {
            LOGGER.error("Failed to create directories for file {}", file.getName(), e);
        }
    }

    public static void fillMissingWithDefaults(String fileName, String defaultFileName, boolean isMapFile) {
        try {
            File file = new File(configDir, fileName);
            if (defaultFileName == null) defaultFileName = fileName;
            InputStream in = NovaRaids.class.getResourceAsStream("/raid_config_files/" + defaultFileName);
            assert in != null;
            JsonObject defaultJson = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
            JsonObject targetJson = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
            mergeJsonObjects(targetJson, defaultJson, isMapFile);
            writeFile(file, gson.toJson(targetJson));
        } catch (IOException e) {
            LOGGER.error("[NovaRaids] Failed to parse json for filling defaults.", e);
        }
    }

    private static void mergeJsonObjects(JsonObject target, JsonObject defaults, boolean isMapFile) {
        if (!isMapFile) {
            for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
                String key = entry.getKey();
                JsonElement defaultValue = entry.getValue();

                // Special handling for maps
                if ("raid_balls".equals(key) && target.has(key) && target.get(key).isJsonObject() && defaultValue.isJsonObject()) {
                    JsonObject targetMap = target.getAsJsonObject(key);
                    JsonObject defaultMap = defaultValue.getAsJsonObject();
                    Map.Entry<String, JsonElement> defaultMapEntry = defaultMap.entrySet().stream().findFirst().isPresent() ? defaultMap.entrySet().stream().findFirst().get() : null;

                    // Only merge existing map entries
                    if (defaultMapEntry != null && defaultMapEntry.getValue().isJsonObject()) {
                        for (Map.Entry<String, JsonElement> mapEntry : targetMap.entrySet()) {
                            if (mapEntry.getValue().isJsonObject()) {
                                mergeJsonObjects(mapEntry.getValue().getAsJsonObject(), defaultMapEntry.getValue().getAsJsonObject(), false);
                            }
                        }
                    }

                    continue;
                }

                if (!target.has(key)) {
                    target.add(key, defaultValue.deepCopy());
                } else {
                    JsonElement targetValue = target.get(key);
                    if (!targetValue.isJsonArray() && targetValue.isJsonObject() && defaultValue.isJsonObject()) {
                        mergeJsonObjects(targetValue.getAsJsonObject(), defaultValue.getAsJsonObject(), false);
                    }
                }
            }
        } else {
            Map.Entry<String, JsonElement> defaultMapEntry = defaults.entrySet().stream().findFirst().isPresent() ? defaults.entrySet().stream().findFirst().get() : null;
            if (defaultMapEntry != null && defaultMapEntry.getValue().isJsonObject()) {
                for (Map.Entry<String, JsonElement> mapEntry : target.entrySet()) {
                    if (mapEntry.getValue().isJsonObject()) {
                        mergeJsonObjects(mapEntry.getValue().getAsJsonObject(), defaultMapEntry.getValue().getAsJsonObject(), false);
                    }
                }
            }
        }
    }

    public static void writeFile(File file, String content) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.flush();
        } catch (IOException e) {
            LOGGER.error("[NovaRaids] Failed to write to file {}", file.getName(), e);
        }
    }
}
