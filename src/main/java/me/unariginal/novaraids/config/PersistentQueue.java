package me.unariginal.novaraids.config;

import net.minecraft.item.ItemStack;

import java.util.LinkedList;
import java.util.Queue;

public class PersistentQueue {
    public Queue<QueueItemData> queue = new LinkedList<>();
    public static class QueueItemData {
        public String boss;
        public String startingPlayerUuid;
        public ItemStack startingItem;
        public Boolean requirePass;
    }
}
