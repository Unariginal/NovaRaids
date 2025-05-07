package me.unariginal.novaraids.data.guis;

import net.minecraft.component.ComponentChanges;

import java.util.List;

public record GuiButton(String symbol, String item, String item_name, List<String> item_lore, ComponentChanges item_data) {}