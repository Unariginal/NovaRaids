package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.managers.Raid;
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
            if (NovaRaids.INSTANCE.config().hide_other_catch_encounters && !Permissions.check(spectator, "novaraids.showpokemon")) {
                boolean inRaid = false;
                for (Raid raid : NovaRaids.INSTANCE.active_raids().values()) {
                    if (raid.participating_players().contains(spectator.getUuid())) {
                        inRaid = true;
                        break;
                    }
                }
                if (inRaid) {
                    Pokemon pokemon = pokemonEntity.getPokemon();
                    if (pokemon != null) {
                        if (pokemon.getPersistentData().contains("catch_encounter")) {
                            if (pokemonEntity.isBattling()) {
                                UUID battle_id = pokemonEntity.getBattleId();
                                if (battle_id != null) {
                                    PokemonBattle battle = Cobblemon.INSTANCE.getBattleRegistry().getBattle(battle_id);
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
            if (NovaRaids.INSTANCE.config().hide_other_pokemon_in_raid && !Permissions.check(spectator, "novaraids.showpokemon")) {
                boolean inRaid = false;
                for (Raid raid : NovaRaids.INSTANCE.active_raids().values()) {
                    if (raid.participating_players().contains(spectator.getUuid())) {
                        inRaid = true;
                        break;
                    }
                }
                if (inRaid) {
                    Pokemon pokemon = pokemonEntity.getPokemon();
                    if (pokemon != null) {
                        if (!pokemon.getPersistentData().contains("raid_entity")) {
                            if (pokemon.isPlayerOwned()) {
                                ServerPlayerEntity owner = pokemon.getOwnerPlayer();
                                if (owner != null) {
                                    boolean pokemonInRaid = false;
                                    for (Raid raid : NovaRaids.INSTANCE.active_raids().values()) {
                                        if (raid.participating_players().contains(owner.getUuid())) {
                                            pokemonInRaid = true;
                                        }
                                    }
                                    if (!owner.getUuid().equals(spectator.getUuid()) && pokemonInRaid) {
                                        cir.setReturnValue(false);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (self instanceof ServerPlayerEntity serverPlayerEntity) {
            if (NovaRaids.INSTANCE.config().hide_other_players_in_raid && !Permissions.check(spectator, "novaraids.showplayers")) {
                boolean inRaid = false;
                for (Raid raid : NovaRaids.INSTANCE.active_raids().values()) {
                    if (raid.participating_players().contains(spectator.getUuid())) {
                        inRaid = true;
                        break;
                    }
                }
                boolean otherPlayerInRaid = false;
                for (Raid raid : NovaRaids.INSTANCE.active_raids().values()) {
                    if (raid.participating_players().contains(serverPlayerEntity.getUuid())) {
                        otherPlayerInRaid = true;
                        break;
                    }
                }
                if (inRaid && otherPlayerInRaid) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
