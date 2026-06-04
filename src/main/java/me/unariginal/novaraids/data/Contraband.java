package me.unariginal.novaraids.data;

import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class Contraband {
    public List<String> bannedPokemon;
    public List<String> bannedMoves;
    public List<String> bannedAbilities;
    public List<String> bannedHeldItems;
    public List<String> bannedBagItems;

    public Contraband(
            List<String> bannedPokemon,
            List<String> bannedMoves,
            List<String> bannedAbilities,
            List<String> bannedHeldItems,
            List<String> bannedBagItems
    ) {
        this.bannedPokemon = bannedPokemon;
        this.bannedMoves = bannedMoves;
        this.bannedAbilities = bannedAbilities;
        this.bannedHeldItems = bannedHeldItems;
        this.bannedBagItems = bannedBagItems;
    }

    public List<Species> getBannedPokemonSpecies() {
        List<Species> speciesList = new ArrayList<>();
        bannedPokemon.forEach(speciesString -> {
            Species species = PokemonSpecies.getByName(speciesString);
            if (species != null) speciesList.add(species);
        });
        return speciesList;
    }

    public List<MoveTemplate> getBannedMoves() {
        List<MoveTemplate> moveList = new ArrayList<>();
        bannedMoves.forEach(moveString -> {
            MoveTemplate moveTemplate = Moves.getByName(moveString);
            if (moveTemplate != null) moveList.add(moveTemplate);
        });
        return moveList;
    }

    public List<AbilityTemplate> getBannedAbilities() {
        List<AbilityTemplate> abilityList = new ArrayList<>();
        bannedAbilities.forEach(abilityString -> {
            AbilityTemplate abilityTemplate = Abilities.get(abilityString);
            if (abilityTemplate != null) abilityList.add(abilityTemplate);
        });
        return abilityList;
    }

    public List<Item> getBannedHeldItems() {
        List<Item> itemList = new ArrayList<>();
        bannedHeldItems.forEach(itemString -> {
            if (Registries.ITEM.containsId(Identifier.of(itemString))) {
                itemList.add(Registries.ITEM.get(Identifier.of(itemString)));
            }
        });
        return itemList;
    }

    public List<Item> getBannedBagItems() {
        List<Item> itemList = new ArrayList<>();
        bannedBagItems.forEach(itemString -> {
            if (Registries.ITEM.containsId(Identifier.of(itemString))) {
                itemList.add(Registries.ITEM.get(Identifier.of(itemString)));
            }
        });
        return itemList;
    }
}
