package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.util.WorldExtensionsKt;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.DataPokeBallEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public class ProjectileEntityMixin {
    @Inject(method = "onBlockHit", at = @At("HEAD"), cancellable = true)
    public void onBlockHitInject(BlockHitResult hitResult, CallbackInfo ci) {
        if (!((Object) this instanceof EmptyPokeBallEntity pokeBallEntity)) return;

        if (pokeBallEntity.getCaptureState() == EmptyPokeBallEntity.CaptureState.NOT) {
            if (!pokeBallEntity.getWorld().isClient) {
//                onBlockHit(hitResult);
                WorldExtensionsKt.sendParticlesServer(
                        pokeBallEntity.getWorld(),
                        ParticleTypes.CLOUD,
                        hitResult.getPos(),
                        2,
                        hitResult.getPos().subtract(pokeBallEntity.getPos()).normalize().multiply(-0.1),
                        0.0
                );
                WorldExtensionsKt.playSoundServer(
                        pokeBallEntity.getWorld(),
                        pokeBallEntity.getPos(),
                        SoundEvents.BLOCK_WOOD_PLACE,
                        SoundCategory.NEUTRAL,
                        1F,
                        2.5F
                );
                pokeBallEntity.discard();
                Entity owner = pokeBallEntity.getOwner();
                ServerPlayerEntity player = null;
                if (owner instanceof ServerPlayerEntity) {
                    player = (ServerPlayerEntity) owner;
                }
                if (player != null && !player.isCreative()) {
                    NovaRaids.LOGGER.error("[NovaRaids] Overriding drop!");
                    ItemStack itemStack = pokeBallEntity.getPokeBall().getItem$common().getDefaultStack();
                    if ((Object) pokeBallEntity instanceof DataPokeBallEntity dataPokeBallEntity) {
                        NbtComponent component = null;
                        if (dataPokeBallEntity.novaRaids$getCustomData() != null)
                            component = NbtComponent.of(dataPokeBallEntity.novaRaids$getCustomData());
                        if (component != null)
                            itemStack.set(DataComponentTypes.CUSTOM_DATA, component);
                    }

                    // This is the implementation of dropStack() in Entity.class
                    ItemEntity itemEntity = new ItemEntity(pokeBallEntity.getWorld(), pokeBallEntity.getX(), pokeBallEntity.getY(), pokeBallEntity.getZ(), itemStack);
                    itemEntity.setToDefaultPickupDelay();
                    pokeBallEntity.getWorld().spawnEntity(itemEntity);
                }
            }
        } else {
            pokeBallEntity.setNoGravity(false);
            pokeBallEntity.setVelocity(Vec3d.ZERO);
        }
        ci.cancel();
    }
}
