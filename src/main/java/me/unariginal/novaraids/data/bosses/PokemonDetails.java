package me.unariginal.novaraids.data.bosses;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.IVs;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.unariginal.novaraids.NovaRaids;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

import static me.unariginal.novaraids.config.ConfigManager.CONFIG;

public class PokemonDetails {
    public String species;
    public int level;
    public List<WeightedFeature> features;
    public List<WeightedAbility> abilities;
    public List<WeightedNature> natures;
    public List<WeightedGender> genders;
    public List<WeightedGimmick> gimmicks;
    public boolean shiny;
    public String teraType;
    public boolean gmaxFactor;
    public int dynamaxLevel;
    public float scale;
    public ItemStack heldItem;
    public List<String> moves;
    public int friendship;
    public IVs ivs;
    public EVs evs;

    public static class WeightedFeature {
        public String feature;
        public double weight;
    }
    public static class WeightedAbility {
        public String ability;
        public double weight;
    }
    public static class WeightedNature {
        public String nature;
        public double weight;
    }
    public static class WeightedGender {
        public String gender;
        public double weight;
    }
    public static class WeightedGimmick {
        public String gimmick;
        public double weight;
    }

    public String getRandomFeature() {
        double totalWeight = 0.0;
        for (WeightedFeature weightedObject : features) {
            totalWeight += weightedObject.weight;
        }
        if (totalWeight <= 0.0) return "";
        double randomWeight = new Random().nextDouble(totalWeight);
        totalWeight = 0.0;
        for (WeightedFeature weightedObject : features) {
            totalWeight += weightedObject.weight;
            if (randomWeight < totalWeight) return weightedObject.feature;
        }
        return "";
    }

    public String getRandomAbility() {
        double totalWeight = 0.0;
        for (WeightedAbility weightedObject : abilities) {
            totalWeight += weightedObject.weight;
        }
        if (totalWeight <= 0.0) return "";
        double randomWeight = new Random().nextDouble(totalWeight);
        totalWeight = 0.0;
        for (WeightedAbility weightedObject : abilities) {
            totalWeight += weightedObject.weight;
            if (randomWeight < totalWeight) return weightedObject.ability;
        }
        return "";
    }

    public String getRandomNature() {
        double totalWeight = 0.0;
        for (WeightedNature weightedObject : natures) {
            totalWeight += weightedObject.weight;
        }
        if (totalWeight <= 0.0) return "";
        double randomWeight = new Random().nextDouble(totalWeight);
        totalWeight = 0.0;
        for (WeightedNature weightedObject : natures) {
            totalWeight += weightedObject.weight;
            if (randomWeight < totalWeight) return weightedObject.nature;
        }
        return "";
    }

    public String getRandomGender() {
        double totalWeight = 0.0;
        for (WeightedGender weightedObject : genders) {
            totalWeight += weightedObject.weight;
        }
        if (totalWeight <= 0.0) return "";
        double randomWeight = new Random().nextDouble(totalWeight);
        totalWeight = 0.0;
        for (WeightedGender weightedObject : genders) {
            totalWeight += weightedObject.weight;
            if (randomWeight < totalWeight) return weightedObject.gender;
        }
        return "";
    }

    public String getRandomGimmick() {
        double totalWeight = 0.0;
        for (WeightedGimmick weightedObject : gimmicks) {
            totalWeight += weightedObject.weight;
        }
        if (totalWeight <= 0.0) return "";
        double randomWeight = new Random().nextDouble(totalWeight);
        totalWeight = 0.0;
        for (WeightedGimmick weightedObject : gimmicks) {
            totalWeight += weightedObject.weight;
            if (randomWeight < totalWeight) return weightedObject.gimmick;
        }
        return "";
    }

    // TODO: Check usage locations of this method and determine performance impact
    public Pokemon createPokemon() {
        PokemonProperties pokemonProperties = PokemonProperties.Companion.parse(getRandomFeature());
        pokemonProperties.setSpecies(species);
        pokemonProperties.setGender(Gender.valueOf(getRandomGender().toUpperCase()));
        pokemonProperties.setShiny(shiny);
        pokemonProperties.setTeraType(teraType);
        pokemonProperties.setGmaxFactor(gmaxFactor);
        pokemonProperties.setDmaxLevel(dynamaxLevel);
        pokemonProperties.setScaleModifier(scale);
        pokemonProperties.setFriendship(friendship);
        pokemonProperties.setIvs(ivs);
        pokemonProperties.setEvs(evs);
        pokemonProperties.setAbility(getRandomAbility());
        pokemonProperties.setNature(getRandomNature());

        Pokemon pokemon = pokemonProperties.create();
        if (level <= 100) pokemon.setLevel(level);
        else {
            try {
                Field levelField = pokemon.getClass().getDeclaredField("level");
                levelField.setAccessible(true);
                levelField.set(pokemon, level);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                NovaRaids.LOGGER.error("[NovaRaids] Failed to set pokemon level above 100.", e);
            }
        }
        for (int i = 0; i < moves.size() || i < 4; i++) {
            MoveTemplate moveTemplate = Moves.getByName(moves.get(i));
            if (moveTemplate == null) {
                i--;
                continue;
            }

            if (CONFIG.raidSettings.bossesHaveInfinitePP) pokemon.getMoveSet().setMove(i, moveTemplate.create(10000));
            else pokemon.getMoveSet().setMove(i, moveTemplate.create(moveTemplate.getMaxPp()));
        }
        pokemon.swapHeldItem(heldItem, true, false);
        pokemon.getPersistentData().putBoolean("raid_entity", true);
        pokemon.heal();

        return pokemon;
    }
}
