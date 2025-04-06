package me.unariginal.novaraids.data.items;

import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public class RaidItem {
    public Item item;
    public Text name;
    public List<Text> lore;
    public ComponentChanges data;
    public String raid_boss;
    public String raid_category;

    public RaidItem(Item item, Text name, List<Text> lore, ComponentChanges data) {
        this.item = item;
        this.name = name;
        this.lore = lore;
        this.data = data;
    }

    public RaidItem(Item item, Text name, List<Text> lore, ComponentChanges data, String raid_boss, String raid_category) {
        this.item = item;
        this.name = name;
        this.lore = lore;
        this.data = data;
        this.raid_boss = raid_boss;
        this.raid_category = raid_category;
    }

    public ItemStack getItem(String raid_item, ServerPlayerEntity player) {
        ItemStack ball = new ItemStack(item);
        NbtCompound data = new NbtCompound();
        data.putString("raid_item", raid_item);
        data.putString("raid_boss", raid_boss);
        data.putString("raid_category", raid_category);

        ball.applyComponentsFrom(ComponentMap.builder()
                .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data))
                .add(DataComponentTypes.CUSTOM_NAME, name)
                .add(DataComponentTypes.LORE, new LoreComponent(lore))
                .add(DataComponentTypes.MAX_STACK_SIZE, 1)
                .build());
        ball.applyChanges(this.data);
        return ball;
    }
}
