package me.unariginal.novaraids.data.bosssettings;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.types.tera.TeraType;
import com.cobblemon.mod.common.pokemon.*;
import me.unariginal.novaraids.NovaRaids;
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
        Map<String, Double> possibleFeatures,
        Map<Ability, Double> possibleAbilities,
        Map<Nature, Double> possibleNatures,
        Map<Gender, Double> possibleGenders,
        Map<String, Double> possibleGimmicks,
        TeraType teraType,
        boolean gmaxFactor,
        int dynamaxLevel,
        boolean shiny,
        float scale,
        Item heldItem,
        ComponentChanges heldItemData,
        List<MoveTemplate> moves,
        int friendship,
        IVs ivs,
        EVs evs) {

    public ItemStack heldItemStack() {
        ItemStack stack = new ItemStack(heldItem());
        if (heldItemData != null) {
            stack.applyChanges(heldItemData);
        }
        return stack;
    }

    public String getGimmick() {
        Map.Entry<?, Double> entry = RandomUtils.getRandomEntry(possibleGimmicks);
        if (entry != null) {
            return (String) entry.getKey();
        }
        return "";
    }

    public Pokemon createPokemon(boolean catchEncounter) {
        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        if (level <= 100) {
            pokemon.setLevel(level);
        } else {
            try {
                Field levelField = pokemon.getClass().getDeclaredField("level");
                levelField.setAccessible(true);
                levelField.set(pokemon, level);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                NovaRaids.LOGGER.error("[NovaRaids] Failed to set pokemon level above 100.", e);
            }
        }
        pokemon.heal();
        Map.Entry<?, Double> entry = RandomUtils.getRandomEntry(possibleFeatures);
        if (entry != null) {
            PokemonProperties.Companion.parse((String) entry.getKey()).apply(pokemon);
        }

        entry = RandomUtils.getRandomEntry(possibleAbilities);
        if (entry != null) {
            pokemon.updateAbility((Ability) entry.getKey());
        }

        entry = RandomUtils.getRandomEntry(possibleNatures);
        if (entry != null) {
            pokemon.setNature((Nature) entry.getKey());
        }

        entry = RandomUtils.getRandomEntry(possibleGenders);
        if (entry != null) {
            pokemon.setGender((Gender) entry.getKey());
        }

        pokemon.setShiny(shiny);
        if (teraType != null) pokemon.setTeraType(teraType);
        pokemon.setGmaxFactor(gmaxFactor);
        pokemon.setDmaxLevel(dynamaxLevel);
        pokemon.setScaleModifier(scale);

        if (heldItem != null) {
            pokemon.setHeldItem$common(heldItemStack());
        }

        for (int i = 0; i < moves.size(); i++) {
            if (NovaRaids.INSTANCE.config().bossesHaveInfinitePP && !catchEncounter)
                pokemon.getMoveSet().setMove(i, moves.get(i).create(10000));
            else
                pokemon.getMoveSet().setMove(i, moves.get(i).create());
        }

        pokemon.setFriendship(friendship, true);

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
