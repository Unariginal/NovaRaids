package me.unariginal.novaraids.data;

import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.bosses.Boss;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.managers.Raid;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public record QueueItem(UUID uuid, Boss bossInfo, String raidBossLocation, UUID startedBy, ItemStack startingItem) {
    private static final NovaRaids nr = NovaRaids.INSTANCE;

    public void startRaid() {
        nr.addRaid(new Raid(bossInfo, raidBossLocation, startedBy, startingItem));
    }

    public void cancelItem() {
        if (Category.getCategory(bossInfo.categoryId).raidDetails.requirePass) {
            if (startingItem != null) {
                ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(startedBy);
                if (player != null) {
                    player.giveItemStack(startingItem);
                }
            }
        }
    }
}
