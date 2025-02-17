package me.unariginal.novaraids.data.rewards;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Random;

public class ItemReward extends Reward {
    private final String item;
    private final JsonElement data;
    private final int minCount;
    private final int maxCount;

    public ItemReward(String name, String item, JsonElement data, int minCount, int maxCount) {
        super(name, "item");
        this.item = item;
        this.data = data;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    public Item item() {
        return Registries.ITEM.get(Identifier.of(item));
    }

    public ComponentChanges data() {
        if (data != null) {
            return ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
        }
        return null;
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
        Random rand = new Random();
        reward.setCount(rand.nextInt(minCount(), maxCount() + 1));
        if (data() != null) {
            reward.applyChanges(data());
        }
        player.giveItemStack(reward);
    }
}
