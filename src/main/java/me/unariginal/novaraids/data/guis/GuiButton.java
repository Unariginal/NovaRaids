package me.unariginal.novaraids.data.guis;

import net.minecraft.component.ComponentChanges;

import java.util.List;

public record GuiButton(String symbol, String item, String itemName, List<String> itemLore, ComponentChanges itemData) {}