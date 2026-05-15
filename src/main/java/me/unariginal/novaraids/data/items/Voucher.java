package me.unariginal.novaraids.data.items;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class Voucher {
    public ItemStack voucherItem = Items.FEATHER.getDefaultStack();
    public String voucherName = "<aqua>Raid Voucher";
    public List<String> voucherLore = List.of("<gray>Use to start a raid!");
}
