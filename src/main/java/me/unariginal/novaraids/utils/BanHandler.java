package me.unariginal.novaraids.utils;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.MiscUtilsKt;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.bosses.Boss;
import me.unariginal.novaraids.data.categories.Category;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

import static me.unariginal.novaraids.config.ConfigManager.CONFIG;
import static me.unariginal.novaraids.config.ConfigManager.MESSAGES;

public class BanHandler {
    private static final NovaRaids nr = NovaRaids.INSTANCE;

    public static boolean hasContraband(ServerPlayerEntity player, Boss boss) {
        // Party Check!
        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);

        List<String> bannedPokemon = new ArrayList<>(CONFIG.raidSettings.globalContraband.bannedPokemon);
        bannedPokemon.addAll(boss.raidDetails.contraband.bannedPokemon);
        bannedPokemon.addAll(Category.getCategory(boss.categoryId).raidDetails.contraband.bannedPokemon);
        List<String> bannedAbilities = new ArrayList<>(CONFIG.raidSettings.globalContraband.bannedAbilities);
        bannedAbilities.addAll(boss.raidDetails.contraband.bannedAbilities);
        bannedAbilities.addAll(Category.getCategory(boss.categoryId).raidDetails.contraband.bannedAbilities);
        List<String> bannedMoves = new ArrayList<>(CONFIG.raidSettings.globalContraband.bannedMoves);
        bannedMoves.addAll(boss.raidDetails.contraband.bannedMoves);
        bannedMoves.addAll(Category.getCategory(boss.categoryId).raidDetails.contraband.bannedMoves);
        List<String> bannedHeldItems = new ArrayList<>(CONFIG.raidSettings.globalContraband.bannedHeldItems);
        bannedHeldItems.addAll(boss.raidDetails.contraband.bannedHeldItems);
        bannedHeldItems.addAll(Category.getCategory(boss.categoryId).raidDetails.contraband.bannedHeldItems);

        for (Pokemon pokemon : party) {
            for (String speciesId : bannedPokemon) {
                Species species = PokemonSpecies.getByName(speciesId);
                if (species == null) continue;
                if (pokemon.getSpecies().getName().equals(species.getName())) {
                    nr.logInfo("Not allowed pokemon");
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.bannedPokemon.replaceAll("%banned%", species.getName()))));
                    return true;
                }
            }

            for (String abilityId : bannedAbilities) {
                AbilityTemplate ability = Abilities.get(abilityId);
                if (ability == null) continue;
                if (pokemon.getAbility().getName().equals(ability.getName())) {
                    nr.logInfo("Not allowed ability");
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.bannedAbility.replaceAll("%banned%", MiscUtilsKt.asTranslated(ability.getDisplayName()).getString()))));
                    return true;
                }
            }

            for (String moveId : bannedMoves) {
                MoveTemplate move = Moves.getByName(moveId);
                if (move == null) continue;
                for (Move set : pokemon.getMoveSet()) {
                    if (set.getName().equals(move.getName())) {
                        nr.logInfo("Not allowed move");
                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.bannedMove.replaceAll("%banned%", move.getDisplayName().getString()))));
                        return true;
                    }
                }
            }

            for (String itemId : bannedHeldItems) {
                if (!Registries.ITEM.containsId(Identifier.of(itemId))) continue;
                Item item = Registries.ITEM.get(Identifier.of(itemId));
                if (pokemon.getHeldItem$common().getItem().equals(item)) {
                    nr.logInfo("Not allowed held item");
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.bannedHeldItem.replaceAll("%banned%", item.getName().getString()))));
                    return true;
                }
            }
        }

        // Item Check!
        PlayerInventory inventory = player.getInventory();
        List<String> bannedBagItems = new ArrayList<>(CONFIG.raidSettings.globalContraband.bannedBagItems);
        bannedBagItems.addAll(boss.raidDetails.contraband.bannedBagItems);
        bannedBagItems.addAll(Category.getCategory(boss.categoryId).raidDetails.contraband.bannedBagItems);
        for (String itemId : bannedBagItems) {
            if (!Registries.ITEM.containsId(Identifier.of(itemId))) continue;
            Item item = Registries.ITEM.get(Identifier.of(itemId));
            for (int i = 0; i < inventory.size(); i++) {
                if (inventory.getStack(i).getItem().equals(item)) {
                    nr.logInfo("Not allowed bag item");
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.bannedBagItem.replaceAll("%banned%", item.getName().getString()))));
                    return true;
                }
            }
        }

        return false;
    }
}
