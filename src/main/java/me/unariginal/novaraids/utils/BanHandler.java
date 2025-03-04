package me.unariginal.novaraids.utils;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.managers.Messages;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class BanHandler {
    private static final NovaRaids nr = NovaRaids.INSTANCE;
    private static final Messages messages = nr.config().getMessages();

    public static boolean hasContraband(ServerPlayerEntity player) {
        // Party check!
        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);

        List<Species> banned_pokemon = nr.config().getSettings().banned_pokemon();
        List<Ability> banned_abilities = nr.config().getSettings().banned_abilities();
        List<Move> banned_moves = nr.config().getSettings().banned_moves();
        List<Item> banned_held_items = nr.config().getSettings().banned_held_items();
        for (Pokemon pokemon : party) {
            for (Species species : banned_pokemon) {
                if (pokemon.getSpecies().equals(species)) {
                    player.sendMessage(TextUtil.format(messages.parse(messages.message("warning_banned_ability").replaceAll("%banned.pokemon%", species.getName()))));
                    return true;
                }
            }

            for (Ability ability : banned_abilities) {
                if (pokemon.getAbility().equals(ability)) {
                    player.sendMessage(TextUtil.format(messages.parse(messages.message("warning_banned_ability").replaceAll("%banned.ability%", ability.getName()))));
                    return true;
                }
            }

            for (Move move : banned_moves) {
                if (pokemon.getMoveSet().getMoves().contains(move)) {
                    player.sendMessage(TextUtil.format(messages.parse(messages.message("warning_banned_move").replaceAll("%banned.move%", move.getName()))));
                    return true;
                }
            }

            for (Item item : banned_held_items) {
                if (pokemon.getHeldItem$common().getItem().equals(item)) {
                    player.sendMessage(TextUtil.format(messages.parse(messages.message("warning_banned_held_item").replaceAll("%banned.held_item%", item.getName().getString()))));
                    return true;
                }
            }
        }

        // Item Check!
        PlayerInventory inventory = player.getInventory();
        List<Item> banned_bag_items = nr.config().getSettings().banned_bag_items();
        for (Item item : banned_bag_items) {
            for (int i = 0; i < inventory.size(); i++) {
                if (inventory.getStack(i).getItem().equals(item)) {
                    player.sendMessage(TextUtil.format(messages.parse(messages.message("warning_banned_bag_item").replaceAll("%banned.bag_item%", item.getName().getString()))));
                    return true;
                }
            }
        }

        return false;
    }
}
