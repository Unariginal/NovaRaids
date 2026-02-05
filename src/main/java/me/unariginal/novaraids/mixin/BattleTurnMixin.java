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
import me.unariginal.novaraids.utils.TextUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(value = PokemonBattle.class, remap = false)
public class BattleTurnMixin {
    @Unique
    private final Map<UUID, Integer> novaraids$lastKnownBossHP = new HashMap<>();
    
    @Unique
    private final Map<UUID, Integer> novaraids$damageAtBattleStart = new HashMap<>();

    @Unique
    private boolean novaraids$initialized = false;

    @Inject(method = "turn", at = @At("HEAD"), remap = false)
    private void beforeTurn(int turnNumber, CallbackInfo ci) {
        if (!novaraids$initialized) {
            novaraids$initializeBossHP();
            novaraids$initialized = true;
        }
    }

    @Unique
    private void novaraids$initializeBossHP() {
        PokemonBattle battle = (PokemonBattle) (Object) this;

        ServerPlayerEntity player = null;
        Pokemon bossPokemon = null;

        for (BattleActor actor : battle.getActors()) {
            if (actor instanceof PokemonBattleActor pokemonBattleActor) {
                Pokemon pokemon = pokemonBattleActor.getPokemon().getEffectedPokemon();
                if (pokemon.getPersistentData().getBoolean("boss_clone")
                        && pokemon.getPersistentData().getBoolean("battle_clone")) {
                    bossPokemon = pokemon;
                    novaraids$lastKnownBossHP.put(pokemon.getUuid(), pokemon.getCurrentHealth());
                }
            }
            if (actor instanceof PlayerBattleActor playerBattleActor) {
                player = playerBattleActor.getEntity();
            }
        }
        
        // Store damage at battle start for this player
        if (player != null && bossPokemon != null) {
            Raid raid = PlayerRaidCache.currentRaid(player);
            if (raid != null) {
                novaraids$damageAtBattleStart.put(player.getUuid(), raid.getPlayerDamage(player.getUuid()));
            }
        }
    }

    @Unique
    private void novaraids$updateBossDamage() {
        PokemonBattle battle = (PokemonBattle) (Object) this;

        ServerPlayerEntity player = null;
        Pokemon bossPokemon = null;

        for (BattleActor actor : battle.getActors()) {
            switch (actor) {
                case PokemonBattleActor pokemonBattleActor -> {
                    Pokemon pokemon = pokemonBattleActor.getPokemon().getEffectedPokemon();
                    if (pokemon.getPersistentData().getBoolean("boss_clone")
                            && pokemon.getPersistentData().getBoolean("battle_clone")) {
                        bossPokemon = pokemon;
                    }
                }
                case PlayerBattleActor playerBattleActor -> {
                    player = playerBattleActor.getEntity();
                }
                default -> {}
            }
        }

        if (player == null || bossPokemon == null)
            return;

        Raid raid = PlayerRaidCache.currentRaid(player);
        if (raid == null)
            return;

        UUID bossUUID = bossPokemon.getUuid();
        int currentHP = bossPokemon.getCurrentHealth();

        if (!novaraids$lastKnownBossHP.containsKey(bossUUID)) {
            novaraids$lastKnownBossHP.put(bossUUID, currentHP);
            return;
        }

        int lastHP = novaraids$lastKnownBossHP.get(bossUUID);
        int damageSinceLastTurn = lastHP - currentHP;

        if (damageSinceLastTurn > 0) {
            int actualDamage = Math.min(damageSinceLastTurn, raid.currentHealth());

            if (actualDamage > 0) {
                raid.applyDamage(actualDamage);
                raid.updatePlayerDamage(player.getUuid(), actualDamage);

                raid.participatingBroadcast(TextUtils.deserialize(
                        TextUtils.parse(NovaRaids.INSTANCE.messagesConfig().getMessage("player_damage_report"),
                                raid, player, actualDamage, -1)));
            }
        }

        novaraids$lastKnownBossHP.put(bossUUID, currentHP);
    }

    @Inject(method = "turn", at = @At("TAIL"), remap = false)
    private void afterTurn(int turnNumber, CallbackInfo ci) {
        novaraids$updateBossDamage();
    }

    @Inject(method = "end", at = @At("HEAD"), remap = false)
    private void onBattleEnd(CallbackInfo ci) {
        PokemonBattle battle = (PokemonBattle) (Object) this;

        ServerPlayerEntity player = null;
        Pokemon bossPokemon = null;

        for (BattleActor actor : battle.getActors()) {
            switch (actor) {
                case PokemonBattleActor pokemonBattleActor -> {
                    Pokemon pokemon = pokemonBattleActor.getPokemon().getEffectedPokemon();
                    if (pokemon.getPersistentData().getBoolean("boss_clone")
                            && pokemon.getPersistentData().getBoolean("battle_clone")) {
                        bossPokemon = pokemon;
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

        if (player != null && bossPokemon != null) {
            Raid raid = PlayerRaidCache.currentRaid(player);
            if (raid != null && !raid.isPlayerFleeing(player)) {
                UUID bossUUID = bossPokemon.getUuid();

                // Get damage BEFORE this battle (stored at battle start!)
                int damageBeforeBattle = novaraids$damageAtBattleStart.getOrDefault(player.getUuid(), 0);

                if (novaraids$lastKnownBossHP.containsKey(bossUUID)) {
                    int currentHP = bossPokemon.getCurrentHealth();
                    int lastHP = novaraids$lastKnownBossHP.get(bossUUID);
                    int remainingDamage = lastHP - currentHP;

                    // Check if there's unprocessed damage
                    if (remainingDamage > 0) {
                        int actualDamage = remainingDamage;
                        if (raid.currentHealth() > 0) {
                            actualDamage = Math.min(remainingDamage, raid.currentHealth());
                        }

                        if (actualDamage > 0) {
                            raid.applyDamage(actualDamage);
                            raid.updatePlayerDamage(player.getUuid(), actualDamage);

                            Integer startHP = novaraids$lastKnownBossHP.get(bossUUID);
                            int totalDamageThisBattle = startHP - currentHP;

                            // Only send damage report if this is NOT the total damage (not a oneshot)
                            if (actualDamage != totalDamageThisBattle) {
                                raid.participatingBroadcast(TextUtils.deserialize(
                                        TextUtils.parse(
                                                NovaRaids.INSTANCE.messagesConfig().getMessage("player_damage_report"),
                                                raid, player, actualDamage, -1)));
                            }
                        }
                    }

                    // Calculate damage dealt ONLY in this battle
                    int damageAfterBattle = raid.getPlayerDamage(player.getUuid());
                    int damageThisBattle = damageAfterBattle - damageBeforeBattle;

                    // Send total damage summary for this battle
                    if (damageThisBattle > 0) {
                        raid.participatingBroadcast(TextUtils.deserialize(
                                TextUtils.parse(NovaRaids.INSTANCE.messagesConfig().getMessage("player_total_damage"),
                                        raid, player, damageThisBattle, -1)));
                    }
                }

                // Automatic battles
                if (NovaRaids.INSTANCE.config().automaticBattles && raid.currentHealth() > 0) {
                    ServerPlayerEntity finalPlayer = player;
                    raid.addTask(player.getServerWorld(), NovaRaids.INSTANCE.config().automaticBattleDelay * 20L,
                            () -> BattleManager.invokeBattle(raid, finalPlayer));
                }
            }
        }

        // Cleanup
        novaraids$lastKnownBossHP.clear();
        novaraids$damageAtBattleStart.clear();
        novaraids$initialized = false;
    }
}