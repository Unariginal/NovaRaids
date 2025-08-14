package me.unariginal.novaraids.utils;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.MiscUtilsKt;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.config.MessagesConfig;
import me.unariginal.novaraids.data.bosssettings.Boss;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class BanHandler {
    private static final NovaRaids nr = NovaRaids.INSTANCE;
    private static final MessagesConfig messages = nr.messagesConfig();

    public static boolean hasContraband(ServerPlayerEntity player, Boss boss) {
        // Party Check!
        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);

        List<Species> bannedPokemon = new ArrayList<>(nr.config().globalContraband.bannedPokemon());
        bannedPokemon.addAll(boss.raidDetails().contraband().bannedPokemon());
        bannedPokemon.addAll(nr.bossesConfig().getCategory(boss.categoryId()).contraband().bannedPokemon());
        List<Ability> bannedAbilities = new ArrayList<>(nr.config().globalContraband.bannedAbilities());
        bannedAbilities.addAll(boss.raidDetails().contraband().bannedAbilities());
        bannedAbilities.addAll(nr.bossesConfig().getCategory(boss.categoryId()).contraband().bannedAbilities());
        List<Move> bannedMoves = new ArrayList<>(nr.config().globalContraband.bannedMoves());
        bannedMoves.addAll(boss.raidDetails().contraband().bannedMoves());
        bannedMoves.addAll(nr.bossesConfig().getCategory(boss.categoryId()).contraband().bannedMoves());
        List<Item> bannedHeldItems = new ArrayList<>(nr.config().globalContraband.bannedHeldItems());
        bannedHeldItems.addAll(boss.raidDetails().contraband().bannedHeldItems());
        bannedHeldItems.addAll(nr.bossesConfig().getCategory(boss.categoryId()).contraband().bannedHeldItems());

        for (Pokemon pokemon : party) {
            for (Species species : bannedPokemon) {
                if (pokemon.getSpecies().getName().equals(species.getName())) {
                    nr.logInfo("Not allowed pokemon");
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_banned_pokemon").replaceAll("%banned.pokemon%", species.getName()))));
                    return true;
                }
            }

            for (Ability ability : bannedAbilities) {
                if (pokemon.getAbility().getName().equals(ability.getName())) {
                    nr.logInfo("Not allowed ability");
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_banned_ability").replaceAll("%banned.ability%", MiscUtilsKt.asTranslated(ability.getDisplayName()).getString()))));
                    return true;
                }
            }

            for (Move move : bannedMoves) {
                for (Move set : pokemon.getMoveSet()) {
                    if (set.getName().equals(move.getName())) {
                        nr.logInfo("Not allowed move");
                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_banned_move").replaceAll("%banned.move%", move.getDisplayName().getString()))));
                        return true;
                    }
                }
            }

            for (Item item : bannedHeldItems) {
                if (pokemon.getHeldItem$common().getItem().equals(item)) {
                    nr.logInfo("Not allowed held item");
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_banned_held_item").replaceAll("%banned.held_item%", item.getName().getString()))));
                    return true;
                }
            }
        }

        // Item Check!
        PlayerInventory inventory = player.getInventory();
        List<Item> bannedBagItems = new ArrayList<>(nr.config().globalContraband.bannedBagItems());
        bannedBagItems.addAll(boss.raidDetails().contraband().bannedBagItems());
        bannedBagItems.addAll(nr.bossesConfig().getCategory(boss.categoryId()).contraband().bannedBagItems());
        for (Item item : bannedBagItems) {
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
