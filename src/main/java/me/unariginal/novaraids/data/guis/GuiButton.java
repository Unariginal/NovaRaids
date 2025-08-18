package me.unariginal.novaraids.data.guis;

import com.google.gson.JsonObject;
import net.minecraft.component.ComponentChanges;

import java.util.List;

public record GuiButton(JsonObject buttonObject, String symbol, String item, String itemName, List<String> itemLore, ComponentChanges itemData) {}