package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.battles.runner.ShowdownService;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.managers.Raid;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(value = PokemonBattle.class, remap = false)
public abstract class GimmickActionMixin {
    @Shadow
    public abstract int getTurn();

    @Shadow
    public abstract BattleSide getSide2();

    @Shadow
    public abstract void log(@NotNull String message);

    @Shadow
    public abstract UUID getBattleId();

    /*  Replicate initial implementation:
     *      fun writeShowdownAction(vararg messages: String) {
     *          log(messages.joinToString("\n"))
     *          ShowdownService.service.send(battleId, messages.toList().toTypedArray())
     *      }
     *  But we need to make the message look like this example of when a player uses a gimmick:
     *      >p1 move 2 +1 terastal
     *  But p2 because it's the wild pokemon, not the player
     *  Then cancel because we don't want 2 instructions sending
     *
     * Mega:
     * >p2 move # mega
     * Tera:
     * >p2 move # terastal
     * Dynamax:
     * >p2 move # max
     * Z Power:
     * >p2 move # zmove
     */
    @Inject(method = "writeShowdownAction", at = @At("HEAD"), cancellable = true)
    private void injectGimmick(String[] messages, CallbackInfo ci) {
        if (this.getTurn() != 1 || !messages[0].startsWith(">p2")) return;

        List<ActiveBattlePokemon> activeBattlePokemon = this.getSide2().getActivePokemon();
        BattlePokemon battlePokemon = activeBattlePokemon.getFirst().getBattlePokemon();
        if (battlePokemon == null) return;
        PokemonEntity pokemonEntity = battlePokemon.getEntity();
        if (pokemonEntity == null) return;
        for (Raid raid : NovaRaids.INSTANCE.activeRaids().values()) {
            if (raid.getClones().containsKey(pokemonEntity)) {
                String gimmick = raid.baseGimmick();
                if (raid.bossInfo().rerollGimmickEachBattle()) {
                    gimmick = raid.bossInfo().pokemonDetails().getGimmick();
                }

                if (gimmick != null && !gimmick.isEmpty()) {
                    String instruction = switch (gimmick) {
                        case "tera", "terastal", "terastallize", "terastallization" -> "terastal";
                        case "mega" -> "mega";
                        case "dmax", "dynamax", "gmax", "gigantamax", "max" -> "max";
                        //case "zpower" -> "zmove";
                        default -> "";
                    };

                    if (instruction.isEmpty()) return;
                    if (instruction.equals("mega") && pokemonEntity.getPokemon().getLevel() > 100) return;

                    this.log(String.join("\n", messages));
                    messages[0] = messages[0] + " " + instruction;
                    ShowdownService.Companion.getService().send(this.getBattleId(), messages);
                    ci.cancel();
                }
            }
        }
    }
}
