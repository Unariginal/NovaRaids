package me.unariginal.novaraids.data.items;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class Pass {
    public ItemStack passItem = Items.PAPER.getDefaultStack();
    public String passName = "<light_purple>Raid Pass";
    public List<String> passLore = List.of("<gray>Use to join a raid!");
}
