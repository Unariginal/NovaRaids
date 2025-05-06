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
    private final String form;
    private final String features;
    private final String gender;
    private final boolean shiny;
    private final float scale;
    private final String held_item;
    private final ComponentChanges held_item_data;
    private final List<String> move_set;
    private final IVs ivs;
    private final EVs evs;

    public PokemonReward(String name, String species, int level, String ability, String nature, String form, String features, String gender, boolean shiny, float scale, String held_item, ComponentChanges held_item_data, List<String> move_set, IVs ivs, EVs evs) {
        super(name, "pokemon");
        this.species = species;
        this.level = level;
        this.ability = ability;
        this.nature = nature;
        this.form = form;
        this.features = features;
        this.gender = gender;
        this.shiny = shiny;
        this.scale = scale;
        this.held_item = held_item;
        this.held_item_data = held_item_data;
        this.move_set = move_set;
        this.ivs = ivs;
        this.evs = evs;
    }

    public Species species() {
        return PokemonSpecies.INSTANCE.getByName(species);
    }

    public int level() {
        return level;
    }

    public Ability ability() {
        AbilityTemplate abilityTemplate = Abilities.INSTANCE.get(ability);
        assert abilityTemplate != null;
        return abilityTemplate.create(false, Priority.LOWEST);
    }

    public Nature nature() {
        return Natures.INSTANCE.getNature(nature);
    }

    public FormData form() {
        return species().getFormByName(form);
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

    public Item held_item() {
        if (!held_item.isEmpty()) {
            return Registries.ITEM.get(Identifier.of(held_item));
        }
        return null;
    }

    public ComponentChanges held_item_data() {
        return held_item_data;
    }

    public ItemStack held_item_stack() {
        ItemStack stack = new ItemStack(held_item());
        if (held_item_data() != null) {
            stack.applyChanges(held_item_data());
        }
        return stack;
    }

    public MoveSet moves() {
        int index = 0;
        MoveSet moves = new MoveSet();
        for (String move : move_set) {
            MoveTemplate moveTemplate = Moves.INSTANCE.getByName(move);
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
    public void apply_reward(ServerPlayerEntity player) {
        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species());
        pokemon.setLevel(level());
        pokemon.updateAbility(ability());
        pokemon.setNature(nature());
        pokemon.setForm(form());
        features().apply(pokemon);
        pokemon.setGender(gender());
        pokemon.setShiny(shiny());
        pokemon.setScaleModifier(scale());
        if (held_item() != null) {
            pokemon.setHeldItem$common(held_item_stack());
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
