package me.unariginal.novaraids.data;

import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.raid.RaidManager;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class QueueItem {
    public final UUID uuid = UUID.randomUUID();
    public Boss boss;
    public UUID startingPlayerUuid;
    public ItemStack startingItem;
    public Boolean requirePass;

    public QueueItem(Boss boss, @Nullable ServerPlayerEntity startingPlayer, @Nullable ItemStack startingItem, @Nullable Boolean requirePass) {
        this.boss = boss;
        this.startingPlayerUuid = startingPlayer == null ? null : startingPlayer.getUuid();
        this.startingItem = startingItem;
        this.requirePass = requirePass;
    }

    public boolean startRaid() {
        ServerPlayerEntity player = null;
        if (startingPlayerUuid != null) player = NovaRaids.INSTANCE.server.getPlayerManager().getPlayer(startingPlayerUuid);
        if (!RaidManager.startRaid(boss, player, startingItem, requirePass)) {
            cancelItem();
            return false;
        }
        return true;
    }

    public void cancelItem() {
//        if (Category.getCategory(boss.categoryId).raidDetails.requirePass) {
//            if (startingItem != null) {
//                ServerPlayerEntity player = NovaRaids.INSTANCE.server.getPlayerManager().getPlayer(startingPlayerUuid);
//                if (player != null) {
//                    player.giveItemStack(startingItem);
//                }
//            }
//        }
        // TODO: Give item if player is online, otherwise queue the item to be given when the player logs in next
    }
}
