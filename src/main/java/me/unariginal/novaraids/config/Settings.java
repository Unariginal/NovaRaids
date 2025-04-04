package me.unariginal.novaraids.config;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;

import java.util.List;
import java.util.Map;

public record Settings(int raid_radius,
                       int raid_pushback_radius,
                       boolean bosses_glow,
                       boolean heal_party_on_challenge,
                       boolean use_queue_system,
                       boolean run_raids_with_no_players,
                       boolean only_reward_if_damage,
                       boolean only_catch_encounter_if_damage,
                       int setup_phase_time,
                       int fight_phase_time,
                       int pre_catch_phase_time,
                       int catch_phase_time,
                       List<Species> banned_pokemon,
                       List<Move> banned_moves,
                       List<Ability> banned_abilities,
                       List<Item> banned_held_items,
                       List<Item> banned_bag_items,
                       boolean use_raid_voucher,
                       Item voucher_item,
                       ComponentChanges voucher_item_data,
                       Item pass_item,
                       ComponentChanges pass_item_data,
                       boolean use_raid_pokeballs,
                       Map<String, Item> raid_pokeballs,
                       Map<String, List<String>> pokeball_categories,
                       Map<String, List<String>> pokeball_bosses,
                       Map<String, ComponentChanges> raid_pokeball_data
) {}
