package me.unariginal.novaraids.data.rewards;

import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Random;

public class ItemReward extends Reward {
    private final Item item;
    private final ComponentChanges data;
    private final int minCount;
    private final int maxCount;

    public ItemReward(String name, Item item, ComponentChanges data, int minCount, int maxCount) {
        super(name, "item");
        this.item = item;
        this.data = data;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    public Item item() {
        return item;
    }

    public ComponentChanges data() {
        return data;
    }

    public int minCount() {
        return minCount;
    }

    public int maxCount() {
        return maxCount;
    }

    @Override
    public void apply_reward(ServerPlayerEntity player) {
        ItemStack reward = new ItemStack(item());
        reward.setCount(new Random().nextInt(minCount(), maxCount() + 1));
        if (data() != null) {
            reward.applyChanges(data());
        }
        player.giveItemStack(reward);
    }
}
