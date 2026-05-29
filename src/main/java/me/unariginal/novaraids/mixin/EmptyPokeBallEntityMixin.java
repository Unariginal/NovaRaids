package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.pokeball.PokeBall;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.DataPokeBallEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmptyPokeBallEntity.class, remap = false)
public class EmptyPokeBallEntityMixin extends ThrownItemEntity implements DataPokeBallEntity {
    @Shadow
    private PokeBall pokeBall;

    @Unique
    private NbtCompound novaRaids$customData;

    public EmptyPokeBallEntityMixin(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public NbtCompound novaRaids$getCustomData() {
        return novaRaids$customData;
    }

    @Override
    public void novaRaids$setCustomData(NbtCompound compound) {
        this.novaRaids$customData = compound;
    }

    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    public void dropInject(CallbackInfo ci) {
        NovaRaids.LOGGER.error("[NovaRaids] Entering injected drop method!");
        Entity owner = getOwner();
        discard();
        ServerPlayerEntity player = null;
        if (owner instanceof ServerPlayerEntity) {
            player = (ServerPlayerEntity) owner;
        }
        if (player != null && !player.isCreative()) {
            NovaRaids.LOGGER.error("[NovaRaids] Overriding drop!");
            ItemStack itemStack = getDefaultItem().getDefaultStack();
            NbtComponent component = null;
            if (novaRaids$customData != null)
                component = NbtComponent.of(novaRaids$customData);
            if (component != null)
                itemStack.set(DataComponentTypes.CUSTOM_DATA, component);
            // This is the implementation of dropStack() in Entity.class
            ItemEntity itemEntity = new ItemEntity(getWorld(), getX(), getY(), getZ(), itemStack);
            itemEntity.setToDefaultPickupDelay();
            getWorld().spawnEntity(itemEntity);
        }
        ci.cancel();
    }

    @Override
    protected Item getDefaultItem() {
        return pokeBall.item();
    }
}
