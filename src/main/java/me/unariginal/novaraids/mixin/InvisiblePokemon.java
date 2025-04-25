package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.managers.Raid;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Entity.class)
public class InvisiblePokemon {
    @Inject(at = {@At("HEAD")}, method = {"canBeSpectated"}, cancellable = true)
    public void canBeSpectated(ServerPlayerEntity spectator, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof PokemonEntity pokemonEntity) {
            boolean inRaid = false;
            for (Raid raid : NovaRaids.INSTANCE.active_raids().values()) {
                if (raid.participating_players().contains(spectator.getUuid())) {
                    inRaid = true;
                    break;
                }
            }
            if (inRaid) {
                if (pokemonEntity.getPokemon().isPlayerOwned()) {
                    if (!Objects.requireNonNull(pokemonEntity.getPokemon().getOwnerPlayer()).getUuid().equals(spectator.getUuid())
                    && !pokemonEntity.getPokemon().getPersistentData().contains("raid_entity")) {
                        cir.setReturnValue(false);
                    }
                }
            }
        }
    }
}
