package me.unariginal.novaraids;

import me.unariginal.novaraids.commands.RaidCommands;
import me.unariginal.novaraids.config.Config;
import me.unariginal.novaraids.data.QueueItem;
import me.unariginal.novaraids.managers.EventManager;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.managers.TickManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class NovaRaids implements ModInitializer {
    private static final String MOD_ID = "novaraids";
    private final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static NovaRaids INSTANCE;

    private final MiniMessage mm = MiniMessage.miniMessage();
    private Config config;
    private MinecraftServer server;
    private RaidCommands raidCommands;

    private final Map<Integer, Raid> active_raids = new HashMap<>();
    private final Queue<QueueItem> queued_raids = new LinkedList<>();

    @Override
    public void onInitialize() {
        LOGGER.info("[RAIDS] Loading..");
        INSTANCE = this;

        raidCommands = new RaidCommands();

        // Set up event handlers and configuration at server load
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            reloadConfig();

            EventManager.battle_events();
            EventManager.catch_events();
            EventManager.right_click_events();
            EventManager.player_events();
        });

        // Server tick loop
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TickManager.fix_boss_positions();
            TickManager.handle_defeated_bosses();
            TickManager.execute_tasks();
            TickManager.update_bossbars();
            TickManager.fix_player_positions();
        });

        // Clean up at server stop
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Raid raid : active_raids.values()) {
                raid.stop();
            }
        });
    }

    public MiniMessage mm() {
        return mm;
    }

    public Config config() {
        return config;
    }

    public void reloadConfig() {
        config = new Config();
    }

    public MinecraftServer server() {
        return server;
    }

    public Logger logger() {
        return LOGGER;
    }

    public void logInfo(String message) {
        if (config.debug) {
            logger().info(message);
        }
    }

    public void logWarning(String message) {
        if (config.debug) {
            logger().warn(message);
        }
    }

    public void logError(String message) {
        logger().error(message);
    }

    public Map<Integer, Raid> active_raids() {
        return active_raids;
    }

    public Queue<QueueItem> queued_raids() {
        return queued_raids;
    }

    public void add_queue_item(QueueItem item) {
        if (!queued_raids.contains(item)) {
            queued_raids.add(item);
        }
    }

    public void remove_queue_item(QueueItem item) {
        queued_raids.remove(item);
    }

    public void init_next_raid() {
        if (config.getSettings().use_queue_system()) {
            if (active_raids.isEmpty()) {
                if (!queued_raids.isEmpty()) {
                    queued_raids.remove().start_raid();
                }
            }
        }
    }

    public RaidCommands raidCommands() {
        return raidCommands;
    }

    public int get_raid_id(Raid raid) {
        for (int key : active_raids.keySet()) {
            if (active_raids.get(key).uuid().equals(raid.uuid())) {
                return key;
            }
        }
        return -1;
    }

    public void add_raid(Raid raid) {
        if (get_raid_id(raid) == -1) {
            int next_id = fix_raid_ids();
            active_raids.put(next_id, raid);
        }
    }

    public void remove_raid(Raid raid) {
        int id = get_raid_id(raid);
        if (id != -1) {
            active_raids.remove(id);
            fix_raid_ids();
        }
    }

    public int fix_raid_ids() {
        Map<Integer, Raid> new_raids = new HashMap<>();
        int count = 1;
        for (int key : active_raids.keySet()) {
            Raid raid = active_raids.get(key);
            new_raids.put(count, raid);
            count++;
        }
        active_raids.clear();
        active_raids.putAll(new_raids);
        return count;
    }
}
