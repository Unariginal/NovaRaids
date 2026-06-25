package me.unariginal.novaraids.data.categories.bosses;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.IVs;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static me.unariginal.novaraids.NovaRaids.logError;
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

    public Pokemon createPokemon(@Nullable CategoryModifier modifier) {
        PokemonProperties pokemonProperties = PokemonProperties.Companion.parse(getRandomFeature());
        pokemonProperties.setSpecies(species);
        pokemonProperties.setGender(Gender.valueOf(getRandomGender().toUpperCase()));
        pokemonProperties.setShiny(modifier != null && modifier.bossPokemonModifiers.shinyOverride || shiny);
        pokemonProperties.setTeraType(teraType);
        pokemonProperties.setGmaxFactor(gmaxFactor);

        int finalDynamaxLevel = dynamaxLevel;
        if (modifier != null) finalDynamaxLevel += modifier.bossPokemonModifiers.dynamaxLevelOffset;
        finalDynamaxLevel = Math.clamp(finalDynamaxLevel, 0, 10);
        pokemonProperties.setDmaxLevel(finalDynamaxLevel);

        float finalScale = scale;
        if (modifier != null) finalScale += modifier.bossPokemonModifiers.scaleOffset;
        pokemonProperties.setScaleModifier(finalScale);

        pokemonProperties.setFriendship(friendship);

        IVs finalIvs = new IVs();
        if (modifier != null && modifier.bossPokemonModifiers.ivsOverrideToggle) {
            finalIvs = modifier.bossPokemonModifiers.ivsModifier;
        } else if (modifier != null) {
            finalIvs.set(Stats.HP, Objects.requireNonNullElse(ivs.get(Stats.HP), 0)
                    + Objects.requireNonNullElse(modifier.bossPokemonModifiers.ivsModifier.get(Stats.HP), 0));
            finalIvs.set(Stats.ATTACK, Objects.requireNonNullElse(ivs.get(Stats.ATTACK), 0)
                    + Objects.requireNonNullElse(modifier.bossPokemonModifiers.ivsModifier.get(Stats.ATTACK), 0));
            finalIvs.set(Stats.DEFENCE, Objects.requireNonNullElse(ivs.get(Stats.DEFENCE), 0)
                    + Objects.requireNonNullElse(modifier.bossPokemonModifiers.ivsModifier.get(Stats.DEFENCE), 0));
            finalIvs.set(Stats.SPECIAL_ATTACK, Objects.requireNonNullElse(ivs.get(Stats.SPECIAL_ATTACK), 0)
                    + Objects.requireNonNullElse(modifier.bossPokemonModifiers.ivsModifier.get(Stats.SPECIAL_ATTACK), 0));
            finalIvs.set(Stats.DEFENCE, Objects.requireNonNullElse(ivs.get(Stats.SPECIAL_DEFENCE), 0)
                    + Objects.requireNonNullElse(modifier.bossPokemonModifiers.ivsModifier.get(Stats.SPECIAL_DEFENCE), 0));
            finalIvs.set(Stats.SPEED, Objects.requireNonNullElse(ivs.get(Stats.SPEED), 0)
                    + Objects.requireNonNullElse(modifier.bossPokemonModifiers.ivsModifier.get(Stats.SPEED), 0));
        } else {
            finalIvs = ivs;
        }
        pokemonProperties.setIvs(finalIvs);

        EVs finalEvs = new EVs();
        if (modifier != null && modifier.bossPokemonModifiers.evsOverrideToggle) {
            finalEvs = modifier.bossPokemonModifiers.evsModifier;
        } else if (modifier != null) {
            finalEvs.set(Stats.HP, Objects.requireNonNullElse(evs.get(Stats.HP), 0)
                    + Objects.requireNonNullElse(modifier.bossPokemonModifiers.evsModifier.get(Stats.HP), 0));
            finalEvs.set(Stats.ATTACK, Objects.requireNonNullElse(evs.get(Stats.ATTACK), 0)
                    + Objects.requireNonNullElse(modifier.bossPokemonModifiers.evsModifier.get(Stats.ATTACK), 0));
            finalEvs.set(Stats.DEFENCE, Objects.requireNonNullElse(evs.get(Stats.DEFENCE), 0)
                    + Objects.requireNonNullElse(modifier.bossPokemonModifiers.evsModifier.get(Stats.DEFENCE), 0));
            finalEvs.set(Stats.SPECIAL_ATTACK, Objects.requireNonNullElse(evs.get(Stats.SPECIAL_ATTACK), 0)
                    + Objects.requireNonNullElse(modifier.bossPokemonModifiers.evsModifier.get(Stats.SPECIAL_ATTACK), 0));
            finalEvs.set(Stats.DEFENCE, Objects.requireNonNullElse(evs.get(Stats.SPECIAL_DEFENCE), 0)
                    + Objects.requireNonNullElse(modifier.bossPokemonModifiers.evsModifier.get(Stats.SPECIAL_DEFENCE), 0));
            finalEvs.set(Stats.SPEED, Objects.requireNonNullElse(evs.get(Stats.SPEED), 0)
                    + Objects.requireNonNullElse(modifier.bossPokemonModifiers.evsModifier.get(Stats.SPEED), 0));
        } else {
            finalEvs = evs;
        }
        pokemonProperties.setEvs(finalEvs);

        pokemonProperties.setAbility(getRandomAbility());
        pokemonProperties.setNature(getRandomNature());

        Pokemon pokemon = pokemonProperties.create();
        int finalLevel = level;
        if (modifier != null) finalLevel += modifier.bossPokemonModifiers.levelOffset;
        if (finalLevel <= 100) pokemon.setLevel(finalLevel);
        else {
            try {
                Field levelField = pokemon.getClass().getDeclaredField("level");
                levelField.setAccessible(true);
                levelField.set(pokemon, finalLevel);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                NovaRaids.LOGGER.error("[NovaRaids] Failed to set pokemon level above 100.", e);
            }
        }
        pokemon.getMoveSet().clear();
        for (int i = 0; i < moves.size() && i < 4; i++) {
            MoveTemplate moveTemplate = Moves.getByName(moves.get(i));
            if (moveTemplate == null) {
                logError("Move \"" + moves.get(i) + "\" does not exist! Removing from move list.");
                moves.remove(i);
                i--;
                continue;
            }

            if (CONFIG.raidSettings.bossesHaveInfinitePP) pokemon.getMoveSet().setMove(i, moveTemplate.create(10000));
            else pokemon.getMoveSet().setMove(i, moveTemplate.create(moveTemplate.getMaxPp()));
        }
        pokemon.swapHeldItem(heldItem, true, false);
        NbtCompound data = new NbtCompound();
        data.putBoolean("raid_entity", true);
        pokemon.getPersistentData().put("raid_data", data);
        pokemon.heal();

        return pokemon;
    }

    public Pokemon createDisplayPokemon() {
        PokemonProperties pokemonProperties = PokemonProperties.Companion.parse(getRandomFeature());
        pokemonProperties.setSpecies(species);
        pokemonProperties.setGender(Gender.valueOf(getRandomGender().toUpperCase()));
        pokemonProperties.setShiny(shiny);
        return pokemonProperties.create();
    }
}
