package me.unariginal.novaraids.config;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;

import java.util.List;

public record Settings(int raid_radius,
                       int raid_pushback_radius,
                       boolean bosses_glow,
                       boolean do_health_scaling,
                       int health_increase,
                       boolean do_catch_phase,
                       int setup_phase_time,
                       int fight_phase_time,
                       int pre_catch_phase_time,
                       int catch_phase_time,
                       List<Species> banned_pokemon,
                       List<Move> banned_moves,
                       List<Ability> banned_abilities,
                       List<Item> banned_held_items,
                       List<Item> banned_bag_items,
                       Item voucher_item,
                       Item pass_item,
                       boolean use_raid_pokeballs,
                       Item raid_pokeball,
                       ComponentChanges component_changes
) {}
