package me.unariginal.novaraids.data.items;

import com.google.gson.JsonObject;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;

import java.util.List;

public record Voucher(JsonObject voucherObject, Item voucherItem, String voucherName, List<String> voucherLore, ComponentChanges voucherData) {}
