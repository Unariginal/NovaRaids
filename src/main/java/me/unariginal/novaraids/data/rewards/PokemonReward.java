package me.unariginal.novaraids.data.rewards;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.*;
import com.google.gson.JsonObject;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

public class PokemonReward extends Reward {
    private final String species;
    private final int level;
    private final String ability;
    private final String nature;
    private final String features;
    private final String gender;
    private final boolean shiny;
    private final float scale;
    private final String heldItem;
    private final ComponentChanges heldItemData;
    private final List<String> moveSet;
    private final IVs ivs;
    private final EVs evs;

    public PokemonReward(JsonObject rewardObject, String name, String species, int level, String ability, String nature, String features, String gender, boolean shiny, float scale, String heldItem, ComponentChanges heldItemData, List<String> moveSet, IVs ivs, EVs evs) {
        super(rewardObject, name, "pokemon");
        this.species = species;
        this.level = level;
        this.ability = ability;
        this.nature = nature;
        this.features = features;
        this.gender = gender;
        this.shiny = shiny;
        this.scale = scale;
        this.heldItem = heldItem;
        this.heldItemData = heldItemData;
        this.moveSet = moveSet;
        this.ivs = ivs;
        this.evs = evs;
    }

    public Species species() {
        return PokemonSpecies.getByName(species);
    }

    public int level() {
        return level;
    }

    public Ability ability() {
        AbilityTemplate abilityTemplate = Abilities.get(ability);
        assert abilityTemplate != null;
        return abilityTemplate.create(false, Priority.LOWEST);
    }

    public Nature nature() {
        return Natures.getNature(nature);
    }

    public PokemonProperties features() {
        return PokemonProperties.Companion.parse(features);
    }

    public Gender gender() {
        return Gender.valueOf(gender.toUpperCase());
    }

    public boolean shiny() {
        return shiny;
    }

    public float scale() {
        return scale;
    }

    public Item heldItem() {
        if (!heldItem.isEmpty()) {
            return Registries.ITEM.get(Identifier.of(heldItem));
        }
        return null;
    }

    public ComponentChanges heldItemData() {
        return heldItemData;
    }

    public ItemStack heldItemStack() {
        ItemStack stack = new ItemStack(heldItem());
        if (heldItemData() != null) {
            stack.applyChanges(heldItemData());
        }
        return stack;
    }

    public MoveSet moves() {
        int index = 0;
        MoveSet moves = new MoveSet();
        for (String move : moveSet) {
            MoveTemplate moveTemplate = Moves.getByName(move);
            if (moveTemplate != null) {
                moves.setMove(index, moveTemplate.create());
                index++;
            }
        }
        return moves;
    }

    public IVs ivs() {
        return ivs;
    }

    public EVs evs() {
        return evs;
    }

    @Override
    public void applyReward(ServerPlayerEntity player) {
        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species());
        pokemon.setLevel(level());
        pokemon.updateAbility(ability());
        pokemon.setNature(nature());
        features().apply(pokemon);
        pokemon.setGender(gender());
        pokemon.setShiny(shiny());
        pokemon.setScaleModifier(scale());
        if (heldItem() != null) {
            pokemon.setHeldItem$common(heldItemStack());
        }
        int move_slot = 0;
        for (Move move : moves()) {
            pokemon.getMoveSet().setMove(move_slot, move);
        }
        pokemon.setIvs$common(ivs());
        pokemon.setEvs$common(evs());

        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        party.add(pokemon);
    }
}
