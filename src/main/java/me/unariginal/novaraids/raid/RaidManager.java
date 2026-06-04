package me.unariginal.novaraids.raid;

import com.google.common.collect.Maps;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.config.LocationsConfig;
import me.unariginal.novaraids.config.PersistentQueue;
import me.unariginal.novaraids.config.RaidHistory;
import me.unariginal.novaraids.data.QueueItem;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.categories.bosses.BossDetails;
import me.unariginal.novaraids.data.categories.bosses.BossDetails.WeightedLocation;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.utils.TextUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

import static me.unariginal.novaraids.NovaRaids.logError;
import static me.unariginal.novaraids.NovaRaids.logInfo;
import static me.unariginal.novaraids.config.ConfigManager.*;

public class RaidManager {
    public static final Queue<QueueItem> queuedRaids = new LinkedList<>();
    public static final List<UUID> raidIds = new ArrayList<>();
    public static final Map<UUID, Raid> activeRaids = Maps.newConcurrentMap();
    public static final List<String> busyLocations = new ArrayList<>();

    public static boolean queueRaid(@Nullable Boss boss, @Nullable ServerPlayerEntity startingPlayer, @Nullable ItemStack startingItem, @Nullable Boolean requirePass) {
        if (boss == null) return false;
        QueueItem queueItem = new QueueItem(boss, startingPlayer, startingItem, requirePass);
        queuedRaids.add(queueItem);
        if (CONFIG.raidSettings.useQueueSystem && startingPlayer != null)
            startingPlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.addedToQueue, boss)));
        return true;
    }

    public static boolean queueRaid(PersistentQueue.QueueItemData queueItemData) {
        Boss boss = Boss.getBoss(queueItemData.boss);
        ServerPlayerEntity player = null;
        if (queueItemData.startingPlayerUuid != null) {
            player = NovaRaids.INSTANCE.server.getPlayerManager().getPlayer(UUID.fromString(queueItemData.startingPlayerUuid));
        }
        return queueRaid(boss, player, queueItemData.startingItem, queueItemData.requirePass);
    }

    public static void startNextQueuedRaid() {
        QueueItem queueItem = queuedRaids.poll();
        if (queueItem != null) {
            if (!queueItem.startRaid()) {
                logInfo("Failed to start next queued raid!");
            }
        }
    }

    public static boolean startRaid(@Nullable Boss boss, @Nullable ServerPlayerEntity startingPlayer, @Nullable ItemStack startingItem, @Nullable Boolean requiresPass) {
        if (boss == null) return false;
        if (CONFIG.raidSettings.runRaidsWithNoPlayers && NovaRaids.INSTANCE.server.getPlayerManager().getCurrentPlayerCount() == 0) return false;

        List<WeightedLocation> possibleLocations = boss.bossDetails.locations;
        List<WeightedLocation> availableLocations = new ArrayList<>();

        for (BossDetails.WeightedLocation location : possibleLocations) {
            if (!busyLocations.contains(location.location)) availableLocations.add(location);
        }

        if (availableLocations.isEmpty()) {
            if (startingPlayer != null) startingPlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.noAvailableLocations, boss)));
            logInfo("Failed to start raid. All raid locations are busy.");
            return false;
        }

        String locationId = boss.bossDetails.getRandomLocation(availableLocations);
        if (locationId == null) {
            logError("Location was null");
            return false;
        }

        LocationsConfig spawnLocation = LocationsConfig.getLocation(locationId);
        if (spawnLocation == null) {
            logError("Location was null");
            return false;
        }

        Raid raid = new Raid(boss, locationId, startingPlayer, startingItem, requiresPass);
        RaidEvents.RAID_START_EVENT_PRE.invoker().onRaidStartPre(raid);
        activeRaids.put(raid.uuid, raid);
        raidIds.add(raid.uuid);
        RaidEvents.RAID_START_EVENT_POST.invoker().onRaidStartPost(raid);
        return true;
    }

    public static void stopRaid(UUID uuid) {
        Raid raid = activeRaids.get(uuid);
        if (raid == null) return;
        raid.stop();
        removeRaid(uuid);
    }

    public static RaidHistory writeHistory(UUID uuid) {
        Raid raid = activeRaids.get(uuid);
        if (raid == null) return null;
        List<String> moves = new ArrayList<>();
        raid.bossPokemon.getMoveSet().getMoves().forEach(move -> moves.add(move.getName()));
        RaidHistory.BossInformation bossInformation = new RaidHistory.BossInformation(
                raid.boss.bossId,
                raid.boss.bossDetails.displayName,
                raid.boss.pokemonDetails.species,
                raid.boss.pokemonDetails.level,
                raid.bossPokemon.getForm().formOnlyShowdownId(),
                raid.bossPokemon.getAbility().getName(),
                raid.bossPokemon.getNature().getName().toString(),
                raid.bossPokemon.getGender(),
                raid.boss.pokemonDetails.shiny,
                raid.baseGimmick,
                raid.boss.pokemonDetails.teraType,
                raid.boss.pokemonDetails.gmaxFactor,
                raid.boss.pokemonDetails.dynamaxLevel,
                raid.boss.pokemonDetails.scale,
                raid.boss.pokemonDetails.heldItem,
                moves,
                raid.boss.pokemonDetails.friendship,
                raid.boss.pokemonDetails.ivs,
                raid.boss.pokemonDetails.evs
        );

        LinkedHashMap<String, Integer> convertedLeaderboard = new LinkedHashMap<>();
        for (Map.Entry<UUID, Integer> entry : raid.getDamageLeaderboard().entrySet()) {
            convertedLeaderboard.put(entry.getKey().toString(), entry.getValue());
        }
        return new RaidHistory(
                uuid.toString(),
                raid.raidStatus,
                raid.realStartTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withZone(SCHEDULES.getTimezone())),
                raid.realEndTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withZone(SCHEDULES.getTimezone())),
                bossInformation,
                raid.category.categoryId,
                raid.category.categoryName,
                raid.location.locationId,
                raid.location.name,
                raid.boss.bossDetails.aiSkillLevel,
                raid.minPlayers,
                raid.maxPlayers,
                raid.maxHealth,
                raid.startTime,
                raid.endTime,
                raid.fightStartTime,
                raid.fightEndTime,
                convertedLeaderboard,
                raid.catchPhaseResults
        );
    }

    public static int getRaidId(UUID uuid) {
        return raidIds.indexOf(uuid);
    }

    public static void removeRaid(UUID uuid) {
        raidIds.remove(uuid);
        activeRaids.remove(uuid);
    }

    public static @Nullable Raid getRaid(int id) {
        if (id >= raidIds.size() || id < 0) return null;
        UUID uuid = raidIds.get(id);
        return activeRaids.get(uuid);
    }
}
