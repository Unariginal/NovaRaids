package me.unariginal.novaraids.data;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.*;
import net.minecraft.item.Item;

import java.util.Map;

public record Boss(String name,
                   Species species,
                   int level,
                   Ability ability,
                   Nature nature,
                   PokemonProperties form,
                   Gender gender,
                   boolean shiny,
                   float scale,
                   Item held_item,
                   MoveSet moves,
                   IVs ivs,
                   EVs evs,
                   int base_health,
                   String category,
                   Map<String, Double> spawn_locations) {
    public Pokemon createPokemon() {
        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        pokemon.setLevel(level);
        pokemon.updateAbility(ability);
        pokemon.setNature(nature);
        form.apply(pokemon);
        pokemon.setGender(gender);
        pokemon.setShiny(shiny);
        pokemon.setScaleModifier(scale);
        pokemon.setHeldItem$common(held_item.getDefaultStack());
        pokemon.getMoveSet().setMove(0, moves.get(0));
        pokemon.getMoveSet().setMove(1, moves.get(1));
        pokemon.getMoveSet().setMove(2, moves.get(2));
        pokemon.getMoveSet().setMove(3, moves.get(3));
        pokemon.setIvs$common(ivs);
        pokemon.setEvs$common(evs);

        return pokemon;
    }
}
