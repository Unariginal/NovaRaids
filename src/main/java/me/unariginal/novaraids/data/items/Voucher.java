package me.unariginal.novaraids.data.items;

import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.text.Text;

import java.util.List;

public record Voucher(Item voucher_item, Text voucher_name, List<Text> voucher_lore, ComponentChanges voucher_data) {}
