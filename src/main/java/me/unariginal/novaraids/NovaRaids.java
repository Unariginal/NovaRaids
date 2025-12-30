package me.unariginal.novaraids;

import me.unariginal.novaraids.commands.RaidCommands;
import me.unariginal.novaraids.config.*;
import me.unariginal.novaraids.data.QueueItem;
import me.unariginal.novaraids.managers.BossBarHandler;
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
    public static final String MOD_ID = "novaraids";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static NovaRaids INSTANCE;
    public static boolean LOADED = true;

    private BossBarHandler bossBarHandler;

    private Config config;
    private LocationsConfig locationsConfig;
    private BossbarsConfig bossbarsConfig;
    private MessagesConfig messagesConfig;
    private SchedulesConfig schedulesConfig;
    private RewardPresetsConfig rewardPresetsConfig;
    private RewardPoolsConfig rewardPoolsConfig;
    private BossesConfig bossesConfig;
    private GuisConfig guisConfig;

    public boolean debug = false;
    private MinecraftServer server;
    private FabricServerAudiences audience;
    private RaidCommands raidCommands;

    private final Map<Integer, Raid> activeRaids = new HashMap<>();
    private final Queue<QueueItem> queuedRaids = new LinkedList<>();
    public List<UUID> ignorePlayerVisibility = new ArrayList<>();
    public List<UUID> ignorePokemonVisibility = new ArrayList<>();

    @Override
    public void onInitialize() {
        INSTANCE = this;

        raidCommands = new RaidCommands();

        // Set up event handlers and configuration at server load
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            this.audience = FabricServerAudiences.of(server);

            reloadConfig();
            if (LOADED) {
                EventManager.initialiseEvents();
                bossBarHandler = new BossBarHandler();
            } else {
                LOGGER.error("Config did not load properly!");
            }
        });

        // Server tick loop
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (LOADED) {
                try {
                    TickManager.updateWebhooks();
                    TickManager.fixBossPositions();
                    TickManager.handleDefeatedBosses();
                    TickManager.executeTasks();
                    TickManager.fixPlayerPositions();
                    TickManager.fixPlayerPokemon();
                    TickManager.scheduledRaids();
                } catch (ConcurrentModificationException e) {
                    logInfo("Suppressing concurrent modification exception!");
                }
                for (Raid raid : activeRaids.values()) {
                    raid.removePlayers();
                }
            }
        });

        // Clean up at server stop
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (LOADED) {
                for (QueueItem queue : queuedRaids) {
                    queue.cancelItem();
                }
                queuedRaids.clear();

                for (Raid raid : activeRaids.values()) {
                    raid.stop();
                }
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
        if (WebhookHandler.webhookToggle) {
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
            logger().info("[NovaRaids] {}", message);
        }
    }

    public void logError(String message) {
        logger().error("[NovaRaids] {}", message);
    }

    public Map<Integer, Raid> activeRaids() {
        return activeRaids;
    }

    public Queue<QueueItem> queuedRaids() {
        return queuedRaids;
    }

    public void addQueueItem(QueueItem item) {
        if (!queuedRaids.contains(item)) {
            queuedRaids.add(item);
        } else {
            logInfo("Queue item already exists!");
        }
    }

    public void initNextRaid() {
        if (config.useQueueSystem) {
            if (!queuedRaids.isEmpty()) {
                queuedRaids.remove().startRaid();
            }
        }
    }

    public RaidCommands raidCommands() {
        return raidCommands;
    }

    public int getRaidId(Raid raid) {
        for (int key : activeRaids.keySet()) {
            if (activeRaids.get(key).uuid().equals(raid.uuid())) {
                return key;
            }
        }
        return -1;
    }

    public void addRaid(Raid raid) {
        if (getRaidId(raid) == -1) {
            int next_id = fixRaidIds();
            activeRaids.put(next_id, raid);
        }
    }

    public void removeRaid(Raid raid) {
        int id = getRaidId(raid);
        if (id != -1) {
            activeRaids.remove(id);
            fixRaidIds();
        }
    }

    public int fixRaidIds() {
        Map<Integer, Raid> newRaids = new HashMap<>();
        int count = 1;
        for (int key : activeRaids.keySet()) {
            Raid raid = activeRaids.get(key);
            newRaids.put(count, raid);
            count++;
        }
        activeRaids.clear();
        activeRaids.putAll(newRaids);
        return count;
    }
}