package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.unariginal.novaraids.ai.StrongBattleAIFix;
import me.unariginal.novaraids.cache.PlayerRaidCache;
import me.unariginal.novaraids.data.players.BattleAttempt;
import me.unariginal.novaraids.data.players.CatchDetails;
import me.unariginal.novaraids.data.players.PlayerRaidData;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.handlers.BattleHandler;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.raid.RaidPhase;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.unariginal.novaraids.config.ConfigManager.CONFIG;

@Mixin(value = PokemonBattle.class, remap = false)
public class BattleEndMixin {
    @Inject(method = "end", at = @At("HEAD"), remap = false)
    private void battleEnd(CallbackInfo ci) {
        PokemonBattle battle = (PokemonBattle) (Object) this;
        if (!battle.isPvW()) return;

        ServerPlayerEntity player = null;
        int damage = 0;

        boolean catchEncounter = false;
        Pokemon pokemon = null;

        for (BattleActor actor : battle.getActors()) {
            switch (actor) {
                case PokemonBattleActor pokemonBattleActor -> {
                    pokemon = pokemonBattleActor.getPokemon().getEffectedPokemon();
                    if (pokemon.getPersistentData().contains("raid_entity")) {
                        if (pokemon.getPersistentData().contains("catch_encounter")) {
                            catchEncounter = true;
                        } else if (pokemon.getPersistentData().contains("battle_clone")) {
                            damage = Math.abs(pokemon.getCurrentHealth() - pokemon.getMaxHealth());
                            if (pokemonBattleActor.getBattleAI() instanceof StrongBattleAIFix) {
                                ((StrongBattleAIFix) pokemonBattleActor.getBattleAI()).cleanUp();
                            }
                        } else {
                            return;
                        }
                    } else {
                        return;
                    }
                }
                case PlayerBattleActor playerBattleActor -> {
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
                default -> {}
            }
        }

        if (player == null) return;
        if (pokemon == null) return;

        Raid raid = PlayerRaidCache.currentRaid(player);
        if (raid == null) return;

        if (raid.phase == RaidPhase.FIGHT) {
            if (!raid.isPlayerFleeing(player)) {
                if (damage > raid.currentHealth) {
                    damage = raid.currentHealth;
                }

                PlayerRaidData playerRaidData = raid.playerRaidData.get(player.getUuidAsString());
                if (playerRaidData != null) {
                    playerRaidData.battleAttempts.add(new BattleAttempt(damage, battle.getTurn(), battle.getTime()));
                }

                if (damage > 0) {
                    RaidEvents.BOSS_DAMAGED_EVENT_PRE.invoker().onBossDamagedPre(raid, damage);
                    raid.applyDamage(damage);
                    raid.updatePlayerDamage(player.getUuid(), damage);
                    RaidEvents.BOSS_DAMAGED_EVENT_POST.invoker().onBossDamagedPost(raid, damage);
                }
            }

            if (CONFIG.raidSettings.automaticBattles) {
                if (raid.currentHealth > 0) {
                    ServerPlayerEntity finalPlayer = player;
                    raid.addTask(player.getServerWorld(), CONFIG.raidSettings.automaticBattleDelay * 20L, () -> BattleHandler.invokeBattle(raid, finalPlayer));
                }
            }
        } else if (raid.phase == RaidPhase.CATCH && catchEncounter) {
            PlayerRaidData playerRaidData = raid.playerRaidData.get(player.getUuidAsString());
            if (playerRaidData != null) {
                // I *hope* the captured event fires before the battle ends... Let's find out!
                if (playerRaidData.catchResult == null) {
                    playerRaidData.catchResult = new CatchDetails(
                            false,
                            pokemon.getSpecies().getName(),
                            pokemon.getForm().formOnlyShowdownId(),
                            pokemon.getIvs(),
                            pokemon.getEvs(),
                            pokemon.getShiny()
                    );
                }
            }
        }
    }
}
