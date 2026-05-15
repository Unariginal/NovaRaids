package me.unariginal.novaraids.config;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.*;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.*;
import java.util.*;

public class RewardPresetsConfig {
    public static class Reward {
        public String type;
        private UUID uuid;

        public Reward(String type) {
            this.type = type;
            this.uuid = UUID.randomUUID();
        }

        public UUID getUuid() {
            if (uuid == null) uuid = UUID.randomUUID();
            return uuid;
        }

        public void grantReward(ServerPlayerEntity player) {}
    }

    public static class ItemReward extends Reward {
        public ItemStack item;
        public int minCount;
        public int maxCount;

        public ItemReward(String type, ItemStack item, int minCount, int maxCount) {
            super(type);
            this.item = item;
            this.minCount = minCount;
            this.maxCount = maxCount;
        }

        @Override
        public void grantReward(ServerPlayerEntity player) {
            player.giveItemStack(item.copyWithCount(new Random().nextInt(minCount, maxCount + 1)));
        }
    }

    public static class CommandReward extends Reward {
        public List<String> commands;

        public CommandReward(String type, List<String> commands) {
            super(type);
            this.commands = commands;
        }

        @Override
        public void grantReward(ServerPlayerEntity player) {
            CommandManager cmdManager = Objects.requireNonNull(player.getServer()).getCommandManager();
            ServerCommandSource source = player.getServer().getCommandSource();
            for (String command : commands) {
                cmdManager.executeWithPrefix(source, command.replaceAll("%player%", player.getNameForScoreboard()));
            }
        }
    }

    public static class PokemonReward extends Reward {
        public PokemonData pokemon;

        public static class PokemonData {
            public String species;
            public int level;
            public String ability;
            public String nature;
            public String features;
            public String gender;
            public boolean shiny;
            public float scale;
            public ItemStack heldItem;
            public List<String> moves;
            public IVs ivs;
            public EVs evs;
        }

        public PokemonReward(String type, PokemonData pokemon) {
            super(type);
            this.pokemon = pokemon;
        }

        @Override
        public void grantReward(ServerPlayerEntity player) {
            Pokemon pokemon = new Pokemon();
            Species species = PokemonSpecies.getByName(this.pokemon.species);
            if (species == null) return;
            pokemon.setSpecies(species);

            pokemon.setLevel(this.pokemon.level);

            AbilityTemplate abilityTemplate = Abilities.get(this.pokemon.ability);
            if (abilityTemplate == null) return;
            pokemon.updateAbility(abilityTemplate.create(false, Priority.LOWEST));

            Nature nature = Natures.getNature(this.pokemon.nature);
            if (nature == null) return;
            pokemon.setNature(nature);

            PokemonProperties.Companion.parse(this.pokemon.features).apply(pokemon);
            pokemon.setGender(Gender.valueOf(this.pokemon.gender.toUpperCase()));
            pokemon.setShiny(this.pokemon.shiny);
            pokemon.setScaleModifier(this.pokemon.scale);
            pokemon.setHeldItem$common(this.pokemon.heldItem);
            int moveSlot = 0;
            for (String moveName : this.pokemon.moves) {
                MoveTemplate moveTemplate = Moves.getByName(moveName);
                if (moveTemplate == null) continue;
                pokemon.getMoveSet().setMove(moveSlot++, moveTemplate.create());
                if (moveSlot >= 4) break;
            }
            pokemon.setIvs$common(this.pokemon.ivs);
            pokemon.setEvs$common(this.pokemon.evs);

            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
            party.add(pokemon);
        }
    }

    public static Reward getReward(String id) {
        return ConfigManager.REWARD_PRESETS.get(id);
    }
}
