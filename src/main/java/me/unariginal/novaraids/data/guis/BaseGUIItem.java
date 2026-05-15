package me.unariginal.novaraids.data.guis;

import net.minecraft.item.ItemStack;

import java.util.List;

public class BaseGUIItem {
    public String symbol;
    public ItemStack item;
    public String itemName;
    public List<String> itemLore;

    public BaseGUIItem(
            String symbol,
            ItemStack item,
            String itemName,
            List<String> itemLore
    ) {
        this.symbol = symbol;
        this.item = item;
        this.itemName = itemName;
        this.itemLore = itemLore;
    }
}
