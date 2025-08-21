package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.ShowdownInterpreter;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.managers.Raid;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShowdownInterpreter.class)
public class ShowdownInterpreterMixin {
    @Inject(method = "interpret", at = @At("TAIL"))
    private void getDamageFromMessage(PokemonBattle battle, String rawMessage, CallbackInfo ci) {
        if (rawMessage.contains("-damage")) {
            boolean isRaidBattle = false;
            PlayerBattleActor playerActor = null;
            ServerPlayerEntity player = null;
            Raid raid = null;

            for (BattleActor actor : battle.getActors()) {
                if (actor instanceof PlayerBattleActor playerBattleActor) {
                    playerActor = playerBattleActor;
                    player = playerBattleActor.getEntity();
                    if (player != null) {
                        for (Raid possibleRaid : NovaRaids.INSTANCE.activeRaids().values()) {
                            if (possibleRaid.participatingPlayers().contains(player.getUuid())) {
                                isRaidBattle = true;
                                raid = possibleRaid;
                            }
                        }
                    }
                }
            }

            if (isRaidBattle) {
                boolean damageToRaidBoss = true;
                for (ActiveBattlePokemon activeBattlePokemon : playerActor.getActivePokemon()) {
                    BattlePokemon battlePokemon = activeBattlePokemon.getBattlePokemon();
                    if (battlePokemon != null) {
                        if (rawMessage.contains(activeBattlePokemon.getBattlePokemon().getEffectedPokemon().getUuid().toString())) {
                            damageToRaidBoss = false;
                        }
                    }
                }

                if (damageToRaidBoss) {
                    int raidMaxHealth = raid.raidBossPokemon().getMaxHealth();
                    boolean notThePercentageInstruction = rawMessage.contains(Integer.toString(raidMaxHealth));
                    if (notThePercentageInstruction) {
                        NovaRaids.LOGGER.info("------- Damage Registered In Raid Battle -------");
                        NovaRaids.LOGGER.info(" - Damage Instruction: {}", rawMessage);
                    }
                }
            }
        }
    }
}
