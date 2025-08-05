package me.unariginal.novaraids;

import me.unariginal.novaraids.commands.RaidCommands;
import me.unariginal.novaraids.config.*;
import me.unariginal.novaraids.data.QueueItem;
import me.unariginal.novaraids.managers.EventManager;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.managers.TickManager;
import me.unariginal.novaraids.utils.WebhookHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class NovaRaids implements ModInitializer {
    private static final String MOD_ID = "novaraids";
    private final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static NovaRaids INSTANCE;

    private Config config;
    private LocationsConfig locationsConfig;
    private BossbarsConfig bossbarsConfig;
    private MessagesConfig messagesConfig;
    private SchedulesConfig schedulesConfig;
    private RewardPresetsConfig rewardPresetsConfig;
    private RewardPoolsConfig rewardPoolsConfig;
    private BossesConfig bossesConfig;
    private GuisConfig guisConfig;
    public boolean loaded_properly = true;

    public boolean debug = true;
    private MinecraftServer server;
    private FabricServerAudiences audience;
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
            this.audience = FabricServerAudiences.of(server);
            // Ignore my bullshit method of error handling for these configs, I'll ask someone
            // what the best practice for configs is eventually...
            // But for now, I settle for what works.
            // Amo, you're the someone
            reloadConfig();
            if (loaded_properly) {
                EventManager.battle_events();
                EventManager.right_click_events();
                EventManager.player_events();
                EventManager.cobblemon_events();
                EventManager.capture_event();
//                if (!config.opt_out) {
//                    try {
//                        CollectingDataToSellToTheChineseGovernment.sendStartWebhook();
//                    } catch (ExecutionException | InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
            } else {
                LOGGER.error("[RAIDS] Config did not load properly! Mod will not be loaded.");
            }
        });

        // Server tick loop
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (loaded_properly) {
                try {
                    TickManager.update_webhooks();
                    TickManager.fix_boss_positions();
                    TickManager.handle_defeated_bosses();
                    TickManager.execute_tasks();
                    TickManager.update_bossbars();
                    TickManager.fix_player_positions();
                    TickManager.fix_player_pokemon();
                    TickManager.scheduled_raids();
                } catch (ConcurrentModificationException e) {
                    logInfo("[RAIDS] Concurrent modification error!");
                }
                for (Raid raid : active_raids.values()) {
                    raid.removePlayers();
                }
            }
        });

        // Clean up at server stop
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (loaded_properly) {
                for (QueueItem queue : queued_raids) {
                    queue.cancel_item();
                }
                queued_raids.clear();

                for (Raid raid : active_raids.values()) {
                    raid.stop();
                }

//                try {
//                    CollectingDataToSellToTheChineseGovernment.deleteWebhook();
//                } catch (ExecutionException | InterruptedException e) {
//                    e.printStackTrace();
//                }
                // TODO: Save current raid, write queue to file
            }
        });
    }

    public Config config() {
        return config;
    }
    public LocationsConfig locationsConfig() {
        return locationsConfig;
    }
    public BossbarsConfig bossbarsConfig() {
        return bossbarsConfig;
    }
    public MessagesConfig messagesConfig() {
        return messagesConfig;
    }
    public SchedulesConfig schedulesConfig() {
        return schedulesConfig;
    }
    public RewardPresetsConfig rewardPresetsConfig() {
        return rewardPresetsConfig;
    }
    public RewardPoolsConfig rewardPoolsConfig() {
        return rewardPoolsConfig;
    }
    public BossesConfig bossesConfig() {
        return bossesConfig;
    }
    public GuisConfig guisConfig() {
        return guisConfig;
    }

    public void reloadConfig() {
        config = new Config();
        locationsConfig = new LocationsConfig();
        bossbarsConfig = new BossbarsConfig();
        messagesConfig = new MessagesConfig();
        schedulesConfig = new SchedulesConfig();
        rewardPresetsConfig = new RewardPresetsConfig();
        rewardPoolsConfig = new RewardPoolsConfig();
        bossesConfig = new BossesConfig();
        guisConfig = new GuisConfig();
        if (WebhookHandler.webhook_toggle) {
            WebhookHandler.connectWebhook();
        }
    }

    public MinecraftServer server() {
        return server;
    }

    public FabricServerAudiences audience() {
        return audience;
    }

    public Logger logger() {
        return LOGGER;
    }

    public void logInfo(String message) {
        if (debug) {
            logger().info(message);
        }
    }

    public void logWarning(String message) {
        if (debug) {
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
        } else {
            logInfo("[RAIDS] Queue item already exists!");
        }
    }

    public void remove_queue_item(QueueItem item) {
        queued_raids.remove(item);
    }

    public void init_next_raid() {
        if (config.use_queue_system) {
            if (!queued_raids.isEmpty()) {
                queued_raids.remove().start_raid();
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