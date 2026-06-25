package me.unariginal.novaraids;

import me.unariginal.novaraids.commands.RaidCommands;
import me.unariginal.novaraids.config.PersistentQueue;
import me.unariginal.novaraids.data.QueueItem;
import me.unariginal.novaraids.handlers.BossBarHandler;
import me.unariginal.novaraids.handlers.CobblemonEventHandler;
import me.unariginal.novaraids.placeholders.types.categoryModifier.*;
import me.unariginal.novaraids.placeholders.types.history.*;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.handlers.TickEventHandler;
import me.unariginal.novaraids.handlers.WebhookHandler;
import me.unariginal.novaraids.placeholders.types.boss.*;
import me.unariginal.novaraids.placeholders.types.raid.*;
import me.unariginal.novaraids.raid.RaidManager;
import me.unariginal.novaraids.utils.Threading;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static me.unariginal.novaraids.config.ConfigManager.*;
import static me.unariginal.novaraids.placeholders.PlaceholderManager.registerPlaceholders;
import static me.unariginal.novaraids.raid.RaidManager.activeRaids;

public class NovaRaids implements ModInitializer {
    public static final String MOD_ID = "novaraids";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static NovaRaids INSTANCE;

    public MinecraftServer server;
    public FabricServerAudiences audience;

    public List<UUID> ignorePlayerVisibility = new ArrayList<>();
    public List<UUID> ignorePokemonVisibility = new ArrayList<>();

    public BossBarHandler bossBarHandler;

    @Override
    public void onInitialize() {
        INSTANCE = this;

        CommandRegistrationCallback.EVENT.register(RaidCommands::register);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            this.audience = FabricServerAudiences.of(server);
            this.server = server;

            reloadConfig();
            loadQueue();

            registerPlaceholders();
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            while (PERSISTENT_QUEUE.queue.peek() != null) {
                PersistentQueue.QueueItemData queueItemData = PERSISTENT_QUEUE.queue.remove();
                if (!RaidManager.queueRaid(queueItemData)) {
                    logInfo("Failed to queue raid from file, boss " + queueItemData.boss + " is null!");
                }
            }
        });

        // Set up event handlers and configuration at server load
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            CobblemonEventHandler.initialiseEvents();
            bossBarHandler = new BossBarHandler();
        });

        // Server tick loop
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TickEventHandler.updateWebhooks();
            TickEventHandler.fixBossPositions();
            TickEventHandler.handleDefeatedBosses();
            TickEventHandler.executeTasks();
            TickEventHandler.fixPlayerPositions();
            TickEventHandler.fixPlayerPokemon();
            TickEventHandler.scheduledRaids();
            TickEventHandler.attemptNextRaid();

            for (Raid raid : activeRaids.values()) {
                raid.removePlayers();
            }
        });

        // Clean up at server stop
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Raid raid : activeRaids.values()) {
                PersistentQueue.QueueItemData queueItemData = new PersistentQueue.QueueItemData();
                queueItemData.boss = raid.boss.bossId;
                queueItemData.startingPlayerUuid = raid.startingPlayer == null ? null : raid.startingPlayer.toString();
                queueItemData.startingItem = raid.startingItem;
                queueItemData.requirePass = raid.requiresPass;
                PERSISTENT_QUEUE.queue.add(queueItemData);
            }

            for (QueueItem queue : RaidManager.queuedRaids) {
                PersistentQueue.QueueItemData queueItemData = new PersistentQueue.QueueItemData();
                queueItemData.boss = queue.boss.bossId;
                queueItemData.startingPlayerUuid = queue.startingPlayerUuid == null ? null : queue.startingPlayerUuid.toString();
                queueItemData.startingItem = queue.startingItem;
                queueItemData.requirePass = queue.requirePass;
                PERSISTENT_QUEUE.queue.add(queueItemData);
                queue.cancelItem();
            }
            RaidManager.queuedRaids.clear();

            Collection<UUID> raidIds = new ArrayList<>(RaidManager.raidIds);
            for (UUID uuid : raidIds) {
                RaidManager.stopRaid(uuid);
            }

            saveQueue();

            Threading.shutdown();
            if (WebhookHandler.webhook != null) WebhookHandler.webhook.close();
        });
    }

    public static void reloadConfig() {
        load();
        if (INSTANCE.server != null && CONFIG.discordWebhook.enabled) {
            WebhookHandler.connectWebhook();
        }
    }

    public static void logInfo(String message) {
        if (CONFIG.debug) LOGGER.info("[NovaRaids] {}", message);
    }

    public static void logError(String message) {
        LOGGER.error("[NovaRaids] {}", message);
    }
}