package me.unariginal.novaraids.data.items;

import com.google.gson.JsonObject;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;

import java.util.List;

public record Pass(JsonObject passObject, Item passItem, String passName, List<String> passLore, ComponentChanges passData) {}
