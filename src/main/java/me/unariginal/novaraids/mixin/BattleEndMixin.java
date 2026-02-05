package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.ai.StrongBattleAIFix;
import me.unariginal.novaraids.cache.PlayerRaidCache;
import me.unariginal.novaraids.managers.BattleManager;
import me.unariginal.novaraids.managers.Raid;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PokemonBattle.class, remap = false)
public class BattleEndMixin {
    @Inject(method = "end", at = @At("HEAD"), remap = false)
    private void battleEnd(CallbackInfo ci) {
        PokemonBattle battle = (PokemonBattle) (Object) this;

        ServerPlayerEntity player = null;

        for (BattleActor actor : battle.getActors()) {
            switch (actor) {
                case PokemonBattleActor pokemonBattleActor -> {
                    Pokemon pokemon = pokemonBattleActor.getPokemon().getEffectedPokemon();
                    if (pokemon.getPersistentData().getBoolean("boss_clone")
                            && pokemon.getPersistentData().getBoolean("battle_clone")) {
                        if (pokemonBattleActor.getBattleAI() instanceof StrongBattleAIFix) {
                            ((StrongBattleAIFix) pokemonBattleActor.getBattleAI()).cleanUp();
                        }
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

        if (player == null)
            return;

        Raid raid = PlayerRaidCache.currentRaid(player);
        if (raid == null)
            return;

        // Automatic battle logic (NO damage calculation here anymore!)
        if (NovaRaids.INSTANCE.config().automaticBattles) {
            if (raid.currentHealth() > 0 && !raid.isPlayerFleeing(player)) {
                ServerPlayerEntity finalPlayer = player;
                raid.addTask(player.getServerWorld(), NovaRaids.INSTANCE.config().automaticBattleDelay * 20L,
                        () -> BattleManager.invokeBattle(raid, finalPlayer));
            }
        }
    }
}