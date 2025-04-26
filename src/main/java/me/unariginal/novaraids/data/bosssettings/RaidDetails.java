package me.unariginal.novaraids.data.bosssettings;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.pokemon.Species;
import me.unariginal.novaraids.data.rewards.DistributionSection;
import net.minecraft.item.Item;

import java.util.List;

public record RaidDetails(int minimum_level,
                          int setup_phase_time,
                          int fight_phase_time,
                          boolean do_catch_phase,
                          int pre_catch_phase_time,
                          int catch_phase_time,
                          boolean heal_party_on_challenge,
                          List<Species> banned_pokemon,
                          List<Move> banned_moves,
                          List<Ability> banned_abilities,
                          List<Item> banned_held_items,
                          List<Item> banned_bag_items,
                          String setup_bossbar,
                          String fight_bossbar,
                          String pre_catch_bossbar,
                          String catch_bossbar,
                          List<DistributionSection> rewards) {
}
