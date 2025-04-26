package me.unariginal.novaraids.data.items;

import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;

import java.util.List;

public record Voucher(Item voucher_item, String voucher_name, List<String> voucher_lore, ComponentChanges voucher_data) {}
