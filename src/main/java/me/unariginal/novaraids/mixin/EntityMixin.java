package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "dropStack(Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    public void dropStackInject(ItemStack stack, float yOffset, CallbackInfoReturnable<ItemEntity> cir) {
        if ((Object)this instanceof EmptyPokeBallEntity) cir.cancel();
    }
}
