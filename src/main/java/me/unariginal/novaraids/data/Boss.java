package me.unariginal.novaraids.data;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.pokemon.*;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import me.unariginal.novaraids.data.rewards.DistributionSection;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Random;

public record Boss(String name,
                   Species species,
                   int level,
                   FormData form,
                   String features,
                   Map<Ability, Double> possible_abilities,
                   Map<Nature, Double> possible_natures,
                   Map<Gender, Double> possible_gender,
                   boolean shiny,
                   float scale,
                   Item held_item,
                   JsonElement held_item_data,
                   MoveSet moves,
                   IVs ivs,
                   EVs evs,
                   String display_form,
                   int base_health,
                   int health_increase_per_player,
                   String category,
                   double random_weight,
                   Float facing,
                   boolean do_catch_phase,
                   Map<String, Double> spawn_locations,
                   List<DistributionSection> rewards,
                   CatchSettings catch_settings
) {
    public Map.Entry<?, Double> getRandomEntry(Map<?, Double> map) {
        double total_weight = 0.0;

        for (Double value : map.values()) {
            total_weight += value;
        }

        if (total_weight > 0.0) {
            double random_weight = new Random().nextDouble(total_weight);
            total_weight = 0.0;

            for (Map.Entry<?, Double> entry : map.entrySet()) {
                total_weight += entry.getValue();
                if (random_weight < total_weight) {
                    return entry;
                }
            }
        }
        return map.entrySet().iterator().next();

    }

    public ComponentChanges get_held_item_data() {
        if (held_item_data != null) {
            return ComponentChanges.CODEC.decode(JsonOps.INSTANCE, held_item_data).getOrThrow().getFirst();
        }
        return null;
    }

    public ItemStack held_item_stack() {
        ItemStack stack = new ItemStack(held_item());
        if (get_held_item_data() != null) {
            stack.applyChanges(get_held_item_data());
        }
        return stack;
    }

    public Pokemon createPokemon() {
        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        if (level <= 100) {
            pokemon.setLevel(level);
        } else {
            try {
                Field level_field = pokemon.getClass().getDeclaredField("level");
                level_field.setAccessible(true);
                level_field.set(pokemon, level);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        pokemon.heal();
        pokemon.setForm(form);
        PokemonProperties.Companion.parse(features).apply(pokemon);
        pokemon.updateAbility((Ability) getRandomEntry(possible_abilities).getKey());
        pokemon.setNature((Nature) getRandomEntry(possible_natures).getKey());
        pokemon.setGender((Gender) getRandomEntry(possible_gender).getKey());
        pokemon.setShiny(shiny);
        pokemon.setScaleModifier(scale);

        if (held_item != null) {
            pokemon.setHeldItem$common(held_item_stack());
        }

        pokemon.getMoveSet().setMove(0, moves.get(0));
        pokemon.getMoveSet().setMove(1, moves.get(1));
        pokemon.getMoveSet().setMove(2, moves.get(2));
        pokemon.getMoveSet().setMove(3, moves.get(3));

        for (Map.Entry<? extends Stat, ? extends Integer> iv : ivs) {
            pokemon.setIV(iv.getKey(), iv.getValue());
        }

        for (Map.Entry<? extends Stat, ? extends Integer> ev : evs) {
            pokemon.setEV(ev.getKey(), ev.getValue());
        }

        NbtCompound nbt = pokemon.getPersistentData();
        nbt.putBoolean("raid_entity", true);
        pokemon.setPersistentData$common(nbt);

        return pokemon;
    }
}
