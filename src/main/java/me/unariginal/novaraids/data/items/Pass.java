package me.unariginal.novaraids.data.items;

import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;

import java.util.List;

public record Pass(Item pass_item, String pass_name, List<String> pass_lore, ComponentChanges pass_data) {}
