package me.unariginal.novaraids;

import me.unariginal.novaraids.commands.RaidCommands;
import me.unariginal.novaraids.config.*;
import me.unariginal.novaraids.data.QueueItem;
import me.unariginal.novaraids.managers.BossBarHandler;
import me.unariginal.novaraids.managers.EventManager;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.managers.TickManager;
import me.unariginal.novaraids.managers.WebhookHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static me.unariginal.novaraids.config.ConfigManager.CONFIG;
import static me.unariginal.novaraids.config.ConfigManager.load;

public class NovaRaids implements ModInitializer {
    public static final String MOD_ID = "novaraids";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static NovaRaids INSTANCE;

    private BossBarHandler bossBarHandler;

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

            load();
            reloadConfig();
            EventManager.initialiseEvents();
            bossBarHandler = new BossBarHandler();
        });

        // Server tick loop
        ServerTickEvents.END_SERVER_TICK.register(server -> {
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
        });

        // Clean up at server stop
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (QueueItem queue : queuedRaids) {
                queue.cancelItem();
            }
            queuedRaids.clear();

            for (Raid raid : activeRaids.values()) {
                raid.stop();
            }
            // TODO: Save current raid, write queue to file
        });
    }

    public void reloadConfig() {
        load();
        if (CONFIG.discordWebhook.enabled) {
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
        if (CONFIG.debug) {
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
        if (CONFIG.raidSettings.useQueueSystem) {
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
            if (activeRaids.get(key).uuid.equals(raid.uuid)) {
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