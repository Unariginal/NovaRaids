package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.cache.PlayerRaidCache;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.raid.RaidPhase;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

import static me.unariginal.novaraids.config.ConfigManager.CONFIG;

@Mixin(Entity.class)
public class HidePlayersAndPokemon {
    @Inject(at = {@At("HEAD")}, method = {"canBeSpectated"}, cancellable = true)
    public void canBeSpectated(ServerPlayerEntity spectator, CallbackInfoReturnable<Boolean> cir) {
        Entity spectatedEntity = (Entity) (Object) this;
        if (spectatedEntity instanceof PokemonEntity pokemonEntity) {
            Pokemon pokemon = pokemonEntity.getPokemon();
            if (pokemon == null) return;

            NbtCompound raidData = null;
            if (pokemon.getPersistentData().contains("raid_data")) {
                raidData = pokemon.getPersistentData().getCompound("raid_data");
            }

            if (raidData != null) {
                // Hide boss battle clones
                if (!CONFIG.debug && raidData.contains("raid_entity") && raidData.contains("boss_clone") && raidData.contains("battle_clone")) {
                    cir.setReturnValue(false);
                    return;
                }

                Raid raid = PlayerRaidCache.currentRaid(spectator);
                if (raid == null) return;
                if (NovaRaids.INSTANCE.ignorePokemonVisibility.contains(spectator.getUuid())) return;

                // Hide other player's catch encounters
                if (raid.phase == RaidPhase.CATCH && CONFIG.raidSettings.hideOtherCatchEncounters && raidData.contains("catch_encounter")) {
                    UUID battleId = pokemonEntity.getBattleId();
                    if (battleId != null) {
                        PokemonBattle battle = BattleRegistry.getBattle(battleId);
                        if (battle != null && !battle.getPlayers().contains(spectator)) {
                            cir.setReturnValue(false);
                        }
                    }
                }
            } else {
                // Hide other pokemon that the player doesn't own. eg. wild pokemon, other player's pokemon
                if (CONFIG.raidSettings.hideOtherPokemonInRaid && !NovaRaids.INSTANCE.ignorePokemonVisibility.contains(spectator.getUuid())) {
                    if (pokemon.getOwnerPlayer() != null) {
                        if (!pokemon.getOwnerPlayer().getUuid().equals(spectator.getUuid())) {
                            cir.setReturnValue(false);
                        }
                    } else {
                        cir.setReturnValue(false);
                    }
                }
            }
        } else if (spectatedEntity instanceof ServerPlayerEntity) {
            if (!PlayerRaidCache.isInRaid(spectator)) return;
            if (CONFIG.raidSettings.hideOtherPlayersInRaid && !NovaRaids.INSTANCE.ignorePlayerVisibility.contains(spectator.getUuid())) {
                cir.setReturnValue(false);
            }
        }
    }
}
