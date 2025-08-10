package me.unariginal.novaraids.data.items;

import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;

import java.util.List;

public record Pass(Item passItem, String passName, List<String> passLore, ComponentChanges passData) {}
