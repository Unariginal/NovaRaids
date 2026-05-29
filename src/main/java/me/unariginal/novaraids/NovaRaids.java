package me.unariginal.novaraids;

import me.unariginal.novaraids.commands.RaidCommands;
import me.unariginal.novaraids.config.PersistentQueue;
import me.unariginal.novaraids.data.QueueItem;
import me.unariginal.novaraids.handlers.BossBarHandler;
import me.unariginal.novaraids.handlers.CobblemonEventHandler;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.handlers.TickEventHandler;
import me.unariginal.novaraids.handlers.WebhookHandler;
import me.unariginal.novaraids.placeholders.ServerPlaceholder;
import me.unariginal.novaraids.placeholders.services.MiniPlaceholdersService;
import me.unariginal.novaraids.placeholders.services.PlaceholderAPIService;
import me.unariginal.novaraids.placeholders.types.NovaRaidsPrefix;
import me.unariginal.novaraids.placeholders.types.boss.*;
import me.unariginal.novaraids.placeholders.types.raid.*;
import me.unariginal.novaraids.raid.RaidManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static me.unariginal.novaraids.config.ConfigManager.*;
import static me.unariginal.novaraids.raid.RaidManager.activeRaids;

public class NovaRaids implements ModInitializer {
    public static final String MOD_ID = "novaraids";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static NovaRaids INSTANCE;

    public MinecraftServer server;
    public FabricServerAudiences audience;
    public boolean usingMiniPlaceholders = false;
    public MiniPlaceholdersService miniPlaceholdersService;
    public boolean usingPlaceholderAPI = false;
    public PlaceholderAPIService placeholderAPIService = null;

    public List<UUID> ignorePlayerVisibility = new ArrayList<>();
    public List<UUID> ignorePokemonVisibility = new ArrayList<>();

    public BossBarHandler bossBarHandler;

    @Override
    public void onInitialize() {
        INSTANCE = this;

        CommandRegistrationCallback.EVENT.register(RaidCommands::register);
        reloadConfig();
        loadQueue();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            this.audience = FabricServerAudiences.of(server);
            this.server = server;

            usingMiniPlaceholders = FabricLoader.getInstance().isModLoaded("miniplaceholders");
            if (usingMiniPlaceholders) miniPlaceholdersService = new MiniPlaceholdersService();
            usingPlaceholderAPI = FabricLoader.getInstance().isModLoaded("placeholder-api");
            if (usingPlaceholderAPI) placeholderAPIService = new PlaceholderAPIService();

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
                PERSISTENT_QUEUE.queue.add(queueItemData);
            }

            for (QueueItem queue : RaidManager.queuedRaids) {
                PersistentQueue.QueueItemData queueItemData = new PersistentQueue.QueueItemData();
                queueItemData.boss = queue.boss.bossId;
                queueItemData.startingPlayerUuid = queue.startingPlayerUuid == null ? null : queue.startingPlayerUuid.toString();
                queueItemData.startingItem = queue.startingItem;
                PERSISTENT_QUEUE.queue.add(queueItemData);
                queue.cancelItem();
            }
            RaidManager.queuedRaids.clear();

            Collection<UUID> raidIds = new ArrayList<>(RaidManager.raidIds);
            for (UUID uuid : raidIds) {
                RaidManager.stopRaid(uuid);
            }

            saveQueue();

            bossBarHandler.schedule.cancel(true);
            if (WebhookHandler.webhook != null) WebhookHandler.webhook.close();
        });
    }

    public static void reloadConfig() {
        load();
        if (CONFIG.discordWebhook.enabled) {
            WebhookHandler.connectWebhook();
        }
    }

    public static void logInfo(String message) {
        if (CONFIG.debug) {
            LOGGER.info("[NovaRaids] {}", message);
        }
    }

    public static void logError(String message) {
        LOGGER.error("[NovaRaids] {}", message);
    }

    public void registerPlaceholders() {
        List<ServerPlaceholder> serverPlaceholders = List.of(
                new NovaRaidsPrefix(),
                new RaidBossAbility(),
                new RaidBossDynamaxLevel(),
                new RaidBossEvs(),
                new RaidBossForm(),
                new RaidBossFriendship(),
                new RaidBossGender(),
                new RaidBossGmaxFactor(),
                new RaidBossHeldItem(),
                new RaidBossIvs(),
                new RaidBossLevel(),
                new RaidBossMoves(),
                new RaidBossName(),
                new RaidBossNature(),
                new RaidBossScale(),
                new RaidBossShiny(),
                new RaidBossSpecies(),
                new RaidBossTeraType(),
                new RaidCategory(),
                new RaidCategoryId(),
                new RaidCompletionTime(),
                new RaidDefeatedTime(),
                new RaidHealth(),
                new RaidJoinMethod(),
                new RaidLocation(),
                new RaidMaximumHealth(),
                new RaidMaximumLevel(),
                new RaidMaximumPartySize(),
                new RaidMaximumPlayers(),
                new RaidMinimumLevel(),
                new RaidMinimumPartySize(),
                new RaidMinimumPlayers(),
                new RaidParticipatingPlayers(),
                new RaidPhase(),
                new RaidPhaseTimer(),
                new RaidTimer(),
                new RaidTotalDamage(),
                new RaidUUID()
        );

//        List<PlayerPlaceholder> playerPlaceholders = List.of();

        serverPlaceholders.forEach(placeholder -> {
            if (usingMiniPlaceholders) miniPlaceholdersService.registerServer(placeholder);
            if (usingPlaceholderAPI) placeholderAPIService.registerServer(placeholder);
        });

//        playerPlaceholders.forEach(placeholder -> {
//            if (usingMiniPlaceholders) miniPlaceholdersService.registerPlayer(placeholder);
//            if (usingPlaceholderAPI) placeholderAPIService.registerPlayer(placeholder);
//        });

        if (usingMiniPlaceholders) miniPlaceholdersService.registerBuilder();
    }
}