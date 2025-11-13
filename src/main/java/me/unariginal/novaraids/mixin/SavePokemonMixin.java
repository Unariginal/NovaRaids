package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PokemonEntity.class)
public class SavePokemonMixin {
    @Inject(method = "writeNbt", at = @At("HEAD"), cancellable = true)
    public void cancelSave(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        PokemonEntity pokemonEntity = (PokemonEntity) (Object) this;
        if (pokemonEntity.getPokemon().getPersistentData().contains("raid_entity")) {
            cir.cancel();
        }
    }
}
