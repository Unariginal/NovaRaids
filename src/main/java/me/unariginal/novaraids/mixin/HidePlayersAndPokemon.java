package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.cache.PlayerRaidCache;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public class HidePlayersAndPokemon {
    @Inject(at = {@At("HEAD")}, method = {"canBeSpectated"}, cancellable = true)
    public void canBeSpectated(ServerPlayerEntity spectator, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof PokemonEntity pokemonEntity) {
            Pokemon pokemon = pokemonEntity.getPokemon();
            if (pokemon != null) {
                if (pokemon.getPersistentData().contains("raid_entity")
                        && pokemon.getPersistentData().contains("boss_clone")
                        && pokemon.getPersistentData().contains("battle_clone")) {
                    if (!NovaRaids.INSTANCE.debug) {
                        cir.setReturnValue(false);
                    }
                }
            }

            if (NovaRaids.INSTANCE.ignorePokemonVisibility.contains(spectator.getUuid())) return;

            if (NovaRaids.INSTANCE.config().hideOtherCatchEncounters) {
                if (PlayerRaidCache.isInRaid(spectator)) {
                    if (pokemon != null) {
                        if (pokemon.getPersistentData().contains("catch_encounter")) {
                            if (pokemonEntity.isBattling()) {
                                UUID battleID = pokemonEntity.getBattleId();
                                if (battleID != null) {
                                    PokemonBattle battle = BattleRegistry.getBattle(battleID);
                                    if (battle != null) {
                                        if (!battle.getPlayers().contains(spectator)) {
                                            cir.setReturnValue(false);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (NovaRaids.INSTANCE.config().hideOtherPokemonInRaid) {
                if (PlayerRaidCache.isInRaid(spectator)) {
                    if (pokemon != null) {
                        if (!pokemon.getPersistentData().contains("raid_entity")) {
                            if (pokemon.isPlayerOwned()) {
                                ServerPlayerEntity owner = pokemon.getOwnerPlayer();
                                if (owner != null) {
                                    if (!owner.getUuid().equals(spectator.getUuid())) {
                                        cir.setReturnValue(false);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (self instanceof ServerPlayerEntity) {
            if (NovaRaids.INSTANCE.config().hideOtherPlayersInRaid && !NovaRaids.INSTANCE.ignorePlayerVisibility.contains(spectator.getUuid())) {
                if (PlayerRaidCache.isInRaid(spectator)) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
