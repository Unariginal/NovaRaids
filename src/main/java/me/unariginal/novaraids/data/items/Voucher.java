package me.unariginal.novaraids.data.items;

import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;

import java.util.List;

public record Voucher(Item voucherItem, String voucherName, List<String> voucherLore, ComponentChanges voucherData) {}
