package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.managers.BattleManager;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.utils.TextUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PokemonBattle.class, remap = false)
public class BattleEndMixin {
    @Inject(method = "end", at = @At("HEAD"), remap = false)
    private void battleEnd(CallbackInfo ci) {
        PokemonBattle battle = (PokemonBattle)(Object)this;

        ServerPlayerEntity player = null;
        int damage = 0;

        for (BattleActor actor : battle.getActors()) {
            if (actor instanceof PokemonBattleActor pokemonBattleActor) {
                Pokemon pokemon = pokemonBattleActor.getPokemon().getEffectedPokemon();
                if (pokemon.getPersistentData().contains("boss_clone") && pokemon.getPersistentData().contains("battle_clone")) {
                    if (pokemon.getPersistentData().getBoolean("boss_clone") && pokemon.getPersistentData().getBoolean("battle_clone")) {
                        damage = Math.abs(pokemon.getCurrentHealth() - pokemon.getMaxHealth());
                    } else {
                        return;
                    }
                }
            } else if (actor instanceof PlayerBattleActor playerBattleActor) {
                player = playerBattleActor.getEntity();
                if (!playerBattleActor.getActivePokemon().isEmpty()) {
                    for (ActiveBattlePokemon activeBattlePokemon : playerBattleActor.getActivePokemon()) {
                        BattlePokemon battlePokemon = activeBattlePokemon.getBattlePokemon();
                        if (battlePokemon != null) {
                            battlePokemon.getOriginalPokemon().recall();
                        }
                    }
                }
            }
        }

        if (player != null) {
            for (Raid raid : NovaRaids.INSTANCE.activeRaids().values()) {
                if (raid.participatingPlayers().contains(player.getUuid())) {
                    if (!raid.isPlayerFleeing(player.getUuid())) {
                        if (damage > raid.currentHealth()) {
                            damage = raid.currentHealth();
                        }

                        if (damage > 0) {
                            raid.applyDamage(damage);
                            raid.updatePlayerDamage(player.getUuid(), damage);
                            raid.participatingBroadcast(TextUtils.deserialize(TextUtils.parse(NovaRaids.INSTANCE.messagesConfig().getMessage("player_damage_report"), raid, player, damage, -1)));
                        }
                    }

                    if (NovaRaids.INSTANCE.config().automaticBattles) {
                        if (raid.currentHealth() > 0) {
                            ServerPlayerEntity finalPlayer = player;
                            raid.addTask(player.getServerWorld(), NovaRaids.INSTANCE.config().automaticBattleDelay * 20L, () -> BattleManager.invokeBattle(raid, finalPlayer));
                        }
                    }
                    break;
                }
            }
        }
    }
}
