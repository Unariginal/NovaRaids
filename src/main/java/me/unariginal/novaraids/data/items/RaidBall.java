package me.unariginal.novaraids.data.items;

import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Objects;

public class RaidBall extends RaidItem {
    String key;
    List<String> categories;
    List<String> bosses;
    public RaidBall(String key, Item item, Text name, List<Text> lore, ComponentChanges data, List<String> categories, List<String> bosses) {
        super(item, name, lore, data);
        this.key = key;
        this.categories = categories;
        this.bosses = bosses;
    }
    @Override
    public ItemStack getItem(String raid_item, ServerPlayerEntity player) {
        ItemStack ball = new ItemStack(item);
        NbtCompound data = new NbtCompound();
        data.putString("raid_item", raid_item);
        data.putUuid("owner_uuid", player.getUuid());

        if (!categories.isEmpty()) {
            NbtList nbtCategories = new NbtList();
            for (String category : categories) {
                nbtCategories.add(NbtString.of(category));
            }
            data.put("raid_categories", nbtCategories);
        }

        if (!bosses.isEmpty()) {
            NbtList nbtBosses = new NbtList();
            for (String boss : bosses) {
                nbtBosses.add(NbtString.of(boss));
            }
            data.put("raid_bosses", nbtBosses);
        }

        ball.applyComponentsFrom(ComponentMap.builder()
                .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data))
                .add(DataComponentTypes.CUSTOM_NAME, name)
                .add(DataComponentTypes.LORE, new LoreComponent(lore))
                .build());
        ball.applyChanges(this.data);
        return ball;
    }
}
