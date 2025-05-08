package me.unariginal.novaraids.utils;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.config.MessagesConfig;
import me.unariginal.novaraids.data.bosssettings.Boss;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class BanHandler {
    private static final NovaRaids nr = NovaRaids.INSTANCE;
    private static final MessagesConfig messages = nr.messagesConfig();

    // TODO: <!>TEST<!> Include other contraband sections
    public static boolean hasContraband(ServerPlayerEntity player, Boss boss) {
        // Party Check!
        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);

        List<Species> banned_pokemon = nr.config().global_banned_pokemon;
        banned_pokemon.addAll(boss.raid_details().banned_pokemon());
        banned_pokemon.addAll(nr.bossesConfig().getCategory(boss.category_id()).banned_pokemon());
        List<Ability> banned_abilities = nr.config().global_banned_abilities;
        banned_abilities.addAll(boss.raid_details().banned_abilities());
        banned_abilities.addAll(nr.bossesConfig().getCategory(boss.category_id()).banned_abilities());
        List<Move> banned_moves = nr.config().global_banned_moves;
        banned_moves.addAll(boss.raid_details().banned_moves());
        banned_moves.addAll(nr.bossesConfig().getCategory(boss.category_id()).banned_moves());
        List<Item> banned_held_items = nr.config().global_banned_held_items;
        banned_held_items.addAll(boss.raid_details().banned_held_items());
        banned_held_items.addAll(nr.bossesConfig().getCategory(boss.category_id()).banned_held_items());

        for (Pokemon pokemon : party) {
            for (Species species : banned_pokemon) {
                if (pokemon.getSpecies().getName().equals(species.getName())) {
                    nr.logInfo("Not allowed pokemon");
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_banned_pokemon").replaceAll("%banned.pokemon%", species.getName()))));
                    return true;
                }
            }

            for (Ability ability : banned_abilities) {
                if (pokemon.getAbility().getName().equals(ability.getName())) {
                    nr.logInfo("Not allowed ability");
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_banned_ability").replaceAll("%banned.ability%", ability.getName()))));
                    return true;
                }
            }

            for (Move move : banned_moves) {
                for (Move set : pokemon.getMoveSet()) {
                    if (set.getName().equals(move.getName())) {
                        nr.logInfo("Not allowed move");
                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_banned_move").replaceAll("%banned.move%", move.getName()))));
                        return true;
                    }
                }
            }

            for (Item item : banned_held_items) {
                if (pokemon.getHeldItem$common().getItem().equals(item)) {
                    nr.logInfo("Not allowed held item");
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_banned_held_item").replaceAll("%banned.held_item%", item.getName().getString()))));
                    return true;
                }
            }
        }

        // Item Check!
        PlayerInventory inventory = player.getInventory();
        List<Item> banned_bag_items = nr.config().global_banned_bag_items;
        banned_bag_items.addAll(boss.raid_details().banned_bag_items());
        banned_bag_items.addAll(nr.bossesConfig().getCategory(boss.category_id()).banned_bag_items());
        for (Item item : banned_bag_items) {
            for (int i = 0; i < inventory.size(); i++) {
                if (inventory.getStack(i).getItem().equals(item)) {
                    nr.logInfo("Not allowed bag item");
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_banned_bag_item").replaceAll("%banned.bag_item%", item.getName().getString()))));
                    return true;
                }
            }
        }

        return false;
    }
}
