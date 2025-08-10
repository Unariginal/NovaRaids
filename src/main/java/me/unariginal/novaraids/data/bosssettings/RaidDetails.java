package me.unariginal.novaraids.data.bosssettings;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.pokemon.Species;
import me.unariginal.novaraids.data.rewards.DistributionSection;
import net.minecraft.item.Item;

import java.util.List;

public record RaidDetails(int minimumLevel,
                          int maximumLevel,
                          int setupPhaseTime,
                          int fightPhaseTime,
                          boolean doCatchPhase,
                          int preCatchPhaseTime,
                          int catchPhaseTime,
                          boolean healPartyOnChallenge,
                          List<Species> bannedPokemon,
                          List<Move> bannedMoves,
                          List<Ability> bannedAbilities,
                          List<Item> bannedHeldItems,
                          List<Item> bannedBagItems,
                          String setupBossbar,
                          String fightBossbar,
                          String preCatchBossbar,
                          String catchBossbar,
                          List<DistributionSection> rewards) {
}
