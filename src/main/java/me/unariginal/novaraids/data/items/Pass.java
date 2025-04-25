package me.unariginal.novaraids.data.items;

import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.text.Text;

import java.util.List;

public record Pass(Item pass_item, Text pass_name, List<Text> pass_lore, ComponentChanges pass_data) {}
