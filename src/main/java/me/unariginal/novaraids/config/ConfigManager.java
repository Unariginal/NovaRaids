package me.unariginal.novaraids.config;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonParser;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.config.guis.*;
import me.unariginal.novaraids.data.bosses.Boss;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.events.Event;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.*;
import java.lang.reflect.Field;
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
    public static Map<String, Category> CATEGORIES;
    public static Map<String, Boss> BOSSES;
    public static Map<Identifier, Event> EVENTS;

    public static ContrabandGUIConfig GLOBAL_CONTRABAND_GUI;
    public static ContrabandGUIConfig CATEGORY_CONTRABAND_GUI;
    public static ContrabandGUIConfig BOSS_CONTRABAND_GUI;
    public static LeaderboardGUIConfig LEADERBOARD_GUI;
    public static RaidListGUIConfig RAID_LIST_GUI;
    public static RaidQueueGUIConfig RAID_QUEUE_GUI;
    public static RaidItemGUIConfig RAID_PASS_GUI;
    public static RaidItemGUIConfig RAID_VOUCHER_GUI;

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

        CONFIG = loadFile("config.json", Config.class);
        SCHEDULES = loadFile("schedules.json", SchedulesConfig.class);
        MESSAGES = loadFile("messages.json", MessagesConfig.class);

        LOCATIONS = castMap(loadMapFile("locations.json", LocationsConfig.class), LocationsConfig.class);
        BOSSBARS = castMap(loadMapFile("bossbars.json", BossbarsConfig.class), BossbarsConfig.class);
        REWARD_PRESETS = castMap(loadMapFile("reward_presets.json", RewardPresetsConfig.Reward.class), RewardPresetsConfig.Reward.class);
        REWARD_POOLS = castMap(loadMapFile("reward_pools.json", RewardPoolsConfig.RewardPool.class), RewardPoolsConfig.RewardPool.class);

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

        fillMissingWithDefaults("config.json", CONFIG, Config.class);
        fillMissingWithDefaults("schedules.json", SCHEDULES, SchedulesConfig.class);
        fillMissingWithDefaults("messages.json", MESSAGES, MessagesConfig.class);
        fillMissingWithDefaults("guis/global_contraband.json", GLOBAL_CONTRABAND_GUI, ContrabandGUIConfig.class);
        fillMissingWithDefaults("guis/category_contraband.json", CATEGORY_CONTRABAND_GUI, ContrabandGUIConfig.class);
        fillMissingWithDefaults("guis/boss_contraband.json", BOSS_CONTRABAND_GUI, ContrabandGUIConfig.class);
        fillMissingWithDefaults("guis/leaderboard.json", LEADERBOARD_GUI, LeaderboardGUIConfig.class);
        fillMissingWithDefaults("guis/raid_list.json", RAID_LIST_GUI, RaidListGUIConfig.class);
        fillMissingWithDefaults("guis/raid_queue.json", RAID_QUEUE_GUI, RaidQueueGUIConfig.class);
        fillMissingWithDefaults("guis/raid_pass.json", RAID_PASS_GUI, RaidItemGUIConfig.class);
        fillMissingWithDefaults("guis/raid_voucher.json", RAID_VOUCHER_GUI, RaidItemGUIConfig.class);
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

        File categoriesFolder = new File(configDir, "categories");
        File bossesFolder = new File(configDir, "bosses");
        if (bossesFolder.exists()) {
            bossesFolder.renameTo(categoriesFolder);
        }
        if (!categoriesFolder.exists()) {
            generateDefaultFile("categories/common/settings.json");
            generateDefaultFile("categories/common/bosses/example_eevee.json");
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
                    Category category = loadFile(categoryFileName, Category.class);
                    if (category != null) {
                        fillMissingWithDefaults(categoryFileName, category, Category.class);
                        CATEGORIES.put(category.categoryId, category);
                    }
                    else continue;

                    File bossesFolder = new File(categoryFolder, "bosses");
                    File[] bossFiles = bossesFolder.listFiles();
                    if (bossFiles != null) {
                        for (File bossFile : bossFiles) {
                            if (bossFile.getName().endsWith(".json")) {
                                String bossFileName = "categories/" + categoryFolder.getName() + "/bosses/" + bossFile.getName();
                                Boss boss = loadFile(bossFileName, Boss.class);
                                if (boss != null)  {
                                    boss.categoryId = category.categoryId;
                                    fillMissingWithDefaults(bossFileName, boss, Boss.class);
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
//            File ongoingFolder = new File(eventFolder, "ongoing");
            File postFolder = new File(eventFolder, "post");

            Event preEvent = null;
            Event postEvent = null;

            File[] preEventFiles = preFolder.listFiles();
            if (preEventFiles != null) {
                for (File preEventFile : preEventFiles) {
                    if (preEventFile.getName().endsWith(".json")) {
                        String preEventFileName = "events/" + eventName + "/pre/" + preEventFile.getName();
                        preEvent = loadFile(preEventFileName, Event.class);
                        fillMissingWithDefaults(preEventFileName, preEvent, Event.class);
                    }
                }
            }

            File[] postEventFiles = postFolder.listFiles();
            if (postEventFiles != null) {
                for (File postEventFile : postEventFiles) {
                    if (postEventFile.getName().endsWith(".json")) {
                        String postEventFileName = "events/" + eventName + "/post/" + postEventFile.getName();
                        postEvent = loadFile(postEventFileName, Event.class);
                        fillMissingWithDefaults(postEventFileName, postEvent, Event.class);
                    }
                }
            }

            if (preEvent != null) EVENTS.put(Identifier.of(eventName + "_pre", preEvent.eventId), preEvent);
            if (postEvent != null) EVENTS.put(Identifier.of(eventName + "_post", postEvent.eventId), postEvent);
        }
    }

    public static <T> Map<String, T> castMap(Map<String, ?> inputMap, Class<T> castClass) {
        Map<String, T> result = new HashMap<>();
        for (Map.Entry<String, ?> inputEntry : inputMap.entrySet()) {
            if (castClass.isInstance(inputEntry.getValue())) {
                result.put(inputEntry.getKey(), castClass.cast(inputEntry.getValue()));
            }
        }
        return result;
    }

    public static <T> Map<String, ?> loadMapFile(String fileName, Class<T> ignored) {
        File file = new File(configDir, fileName);
        Type mapType = new TypeToken<Map<String, T>>() {}.getType();
        try {
            Files.createDirectories(configDir.toPath());
            if (file.exists()) {
                String json = JsonParser.parseReader(new FileReader(file)).toString();
                return gson.fromJson(json, mapType);
            }
        } catch (IOException e) {
            LOGGER.error("[NovaRaids] Failed to load config {}", file, e);
        }
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

    public static String getJsonString(String fileName) {
        InputStream in = NovaRaids.class.getResourceAsStream("/raid_config_files/" + fileName);
        assert in != null;
        return JsonParser.parseReader(new InputStreamReader(in)).toString();
    }

    public static <T> T loadDefaults(String fileName, Class<T> clazz) {
        return gson.fromJson(getJsonString(fileName), clazz);
    }

    public static void generateDefaultFile(String fileName) {
        File file = new File(configDir, fileName);
        if (file.exists()) return;
        try {
            Files.createDirectories(file.toPath());
            writeFile(file, getJsonString(fileName));
        } catch (IOException e) {
            LOGGER.error("Failed to create directories for file {}", file.getName(), e);
        }
    }

    public static <T> void fillMissingWithDefaults(String fileName, T loaded, Class<T> clazz) {
        File file = new File(configDir, fileName);
        T defaults = loadDefaults(fileName, clazz);
        try {
            applyDefaults(loaded, defaults);
            writeFile(file, gson.toJson(loaded));
        } catch (IllegalAccessException e) {
            LOGGER.error("[NovaRaids] Failed to load defaults for file {}", fileName, e);
        }
    }

    public static <T> boolean applyDefaults(T target, T defaults) throws IllegalAccessException {
        boolean changed = false;

        for (Field field : target.getClass().getDeclaredFields()) {
            field.setAccessible(true);

            Object value = field.get(target);
            Object defaultValue = field.get(defaults);

            if (value == null) {
                field.set(target, defaultValue);
                changed = true;
            } else if (!field.getType().isPrimitive()) {
                changed |= applyDefaults(value, defaultValue);
            }
        }

        return changed;
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
