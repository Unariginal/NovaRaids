package me.unariginal.novaraids.cache;

import com.google.common.collect.Maps;
import me.unariginal.novaraids.managers.Raid;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerRaidCache {

    private static final Map<UUID, Raid> playersInRaid = Maps.newConcurrentMap();

    @Nullable
    public static Raid currentRaid(UUID playerUUID) {
        return playersInRaid.get(playerUUID);
    }

    @Nullable
    public static Raid currentRaid(ServerPlayerEntity player) {
        return playersInRaid.get(player.getUuid());
    }

    public static boolean isInRaid(ServerPlayerEntity player) {
        return playersInRaid.containsKey(player.getUuid());
    }

    public static boolean isInRaid(UUID playerUUID) {
        return playersInRaid.containsKey(playerUUID);
    }

    public static void add(UUID uuid, Raid raid) {
        playersInRaid.put(uuid, raid);
    }

    public static void add(ServerPlayerEntity player, Raid raid) {
        playersInRaid.put(player.getUuid(), raid);
    }

    public static void remove(UUID uuid) {
        playersInRaid.remove(uuid);
    }

    public static void remove(ServerPlayerEntity player) {
        playersInRaid.remove(player.getUuid());
    }

    public static void clearFromRaid(UUID uuid) {
        List<UUID> playersToRemove = new ArrayList<>();
        for (Map.Entry<UUID, Raid> entry : playersInRaid.entrySet()) {
            if (entry.getValue().uuid().equals(uuid)) {
                playersToRemove.add(entry.getKey());
            }
        }

        playersToRemove.forEach(PlayerRaidCache::remove);
    }
}
