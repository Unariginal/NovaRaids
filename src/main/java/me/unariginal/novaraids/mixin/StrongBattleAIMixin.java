package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.battles.*;
import com.cobblemon.mod.common.battles.ai.StrongBattleAI;
import com.cobblemon.mod.common.battles.ai.strongBattleAI.TrackerPokemon;
import com.cobblemon.mod.common.battles.interpreter.instructions.TerastallizeInstruction;
import com.llamalad7.mixinextras.sugar.Local;
import kotlin.Pair;
import me.unariginal.novaraids.NovaRaids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(value = StrongBattleAI.class, remap = false)
public abstract class StrongBattleAIMixin {
    @Shadow
    public abstract double estimateMatchup(@NotNull ActiveBattlePokemon activeBattlePokemon, @NotNull BattleSide aiSide, @NotNull PokemonBattle battle, @Nullable TrackerPokemon nonActiveMon);

    @Unique
    public List<UUID> isTerastallized = new ArrayList<>();

    @Inject(method = "findAndUseMostDamagingMove", at = @At(value = "TAIL"), cancellable = true)
    private void allowGimmickSelection(ActiveBattlePokemon activeBattlePokemon, TrackerPokemon activeTrackerPokemon, List<TrackerPokemon> opponents, List<? extends Pair<InBattleMove, ? extends MoveTemplate>> availableMoves, ShowdownMoveset moveset, PokemonBattle battle, BattleSide aiSide, CallbackInfoReturnable<ShowdownActionResponse> cir, @Local Map.Entry<Pair<InBattleMove, MoveTemplate>, Pair<ActiveBattlePokemon, Double>> bestMove, @Local(ordinal = 1) ActiveBattlePokemon target) {
        if (shouldDynamax(activeBattlePokemon, activeTrackerPokemon, opponents, battle, aiSide)) {
            NovaRaids.LOGGER.info("[NovaRaids] Should Dynamax!");
            cir.setReturnValue(new MoveActionResponse(bestMove.getKey().getFirst().getId(), target.getPNX(), ShowdownMoveset.Gimmick.DYNAMAX.getId()));
        } else if (shouldTera(activeBattlePokemon, activeTrackerPokemon, opponents, battle, aiSide)) {
            NovaRaids.LOGGER.info("[NovaRaids] Should Tera!");
            assert activeBattlePokemon.getBattlePokemon() != null;
            battle.dispatch((pokemonBattle) -> {
                new TerastallizeInstruction(new BattleMessage("|-terastallize|p2a: " + activeBattlePokemon.getBattlePokemon().getUuid() + "|" + activeBattlePokemon.getBattlePokemon().getEffectedPokemon().getTeraType().showdownId())).invoke(battle);
                return pokemonBattle.getDispatchResult();
            });
            isTerastallized.add(activeBattlePokemon.getBattlePokemon().getUuid());
            cir.setReturnValue(new MoveActionResponse(bestMove.getKey().getFirst().getId(), target.getPNX(), ShowdownMoveset.Gimmick.TERASTALLIZATION.getId()));
        }
    }

    @Unique
    private boolean shouldDynamax(ActiveBattlePokemon activeBattlePokemon, TrackerPokemon activeTrackerPokemon, List<TrackerPokemon> opponents, PokemonBattle battle, BattleSide aiSide) {
        if (activeBattlePokemon.getActor().getCanDynamax()) {
            if (aiSide.getActivePokemon().stream()
                    .filter(pokemon -> {
                        assert pokemon.getBattlePokemon() != null;
                        return pokemon.getBattlePokemon().getHealth() == pokemon.getBattlePokemon().getMaxHealth();
                    })
                    .count() == 1) {
                if (activeTrackerPokemon.getCurrentHpPercent() == 1) {
                    return true;
                }
            }

            if (estimateMatchup(activeBattlePokemon, aiSide, battle, null) > 0 && activeTrackerPokemon.getCurrentHpPercent() == 1 && opponents.getFirst().getCurrentHpPercent() == 1) {
                return true;
            }

            if (aiSide.getActivePokemon().stream()
                    .filter(pokemon -> {
                        assert pokemon.getBattlePokemon() != null;
                        return pokemon.getBattlePokemon().getHealth() != 0;
                    }).count() == 1 && activeTrackerPokemon.getCurrentHpPercent() == 1) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean shouldTera(ActiveBattlePokemon activeBattlePokemon, TrackerPokemon activeTrackerPokemon, List<TrackerPokemon> opponents, PokemonBattle battle, BattleSide aiSide) {
//        return estimateMatchup(activeBattlePokemon, aiSide, battle, null) < 0 && activeTrackerPokemon.getCurrentHpPercent() == 1 && opponents.getFirst().getCurrentHpPercent() >= 0.75;
        assert activeBattlePokemon.getBattlePokemon() != null;
        return !isTerastallized.contains(activeBattlePokemon.getBattlePokemon().getUuid());
    }
}
