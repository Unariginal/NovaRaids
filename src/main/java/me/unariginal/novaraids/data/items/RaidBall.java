package me.unariginal.novaraids.data.items;

import com.cobblemon.mod.common.CobblemonItems;
import net.minecraft.item.ItemStack;

import java.util.List;

public class RaidBall {
    public ItemStack pokeballItem = CobblemonItems.POKE_BALL.getDefaultStack();
    public String pokeballName = "<red>Raid Pokeball";
    public List<String> pokeballLore = List.of("<gray>Use this to try and capture raid bosses!");
}
