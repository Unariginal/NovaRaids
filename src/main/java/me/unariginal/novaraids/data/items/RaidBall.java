package me.unariginal.novaraids.data.items;

import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;

import java.util.List;

public record RaidBall(String id, Item ball_item, String ball_name, List<String> ball_lore, ComponentChanges ball_data) {}
