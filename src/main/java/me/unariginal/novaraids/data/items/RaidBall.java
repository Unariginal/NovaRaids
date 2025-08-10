package me.unariginal.novaraids.data.items;

import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;

import java.util.List;

public record RaidBall(String id, Item ballItem, String ballName, List<String> ballLore, ComponentChanges ballData) {}
