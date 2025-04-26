package me.unariginal.novaraids.data.bosssettings;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.pokemon.*;
import me.unariginal.novaraids.utils.RandomUtils;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public record PokemonDetails(
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
        ComponentChanges held_item_data,
        List<MoveTemplate> moves,
        IVs ivs,
        EVs evs) {

    public ItemStack held_item_stack() {
        ItemStack stack = new ItemStack(held_item());
        if (held_item_data != null) {
            stack.applyChanges(held_item_data);
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

        Map.Entry<?, Double> entry = RandomUtils.getRandomEntry(possible_abilities);
        if (entry != null) {
            pokemon.updateAbility((Ability) entry.getKey());
        }

        entry = RandomUtils.getRandomEntry(possible_natures);
        if (entry != null) {
            pokemon.setNature((Nature) entry.getKey());
        }

        entry = RandomUtils.getRandomEntry(possible_gender);
        if (entry != null) {
            pokemon.setGender((Gender) entry.getKey());
        }

        pokemon.setShiny(shiny);
        pokemon.setScaleModifier(scale);

        if (held_item != null) {
            pokemon.setHeldItem$common(held_item_stack());
        }

        for (int i = 0; i < moves.size(); i++) {
            pokemon.getMoveSet().setMove(i, moves.get(i).create());
        }

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
