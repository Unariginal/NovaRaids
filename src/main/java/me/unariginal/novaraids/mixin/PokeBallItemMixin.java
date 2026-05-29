package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.item.PokeBallItem;
import com.cobblemon.mod.common.pokeball.PokeBall;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.cache.PlayerRaidCache;
import me.unariginal.novaraids.data.DataPokeBallEntity;
import me.unariginal.novaraids.raid.Raid;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.cobblemon.mod.common.CobblemonEntities.EMPTY_POKEBALL;

@Mixin(value = PokeBallItem.class)
public abstract class PokeBallItemMixin extends Item {
    @Final
    @Shadow (remap = false)
    private PokeBall pokeBall;

    public PokeBallItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void useInject(World world, PlayerEntity player, Hand usedHand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        NovaRaids.LOGGER.error("[NovaRaids] Entering use injection!");
        ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) player;
        Raid raid = PlayerRaidCache.currentRaid(serverPlayerEntity);
        if (raid == null) return;

        ItemStack itemStack = player.getStackInHand(usedHand);
        if (!world.isClient) {
            throwPokeBallAlternative(world, serverPlayerEntity, itemStack);
        }
        cir.setReturnValue(TypedActionResult.success(itemStack, world.isClient));
    }

    @Unique
    private void throwPokeBallAlternative(World world, ServerPlayerEntity player, ItemStack originalStack) {
        NovaRaids.LOGGER.error("[NovaRaids] Entering alternative throw method!");
        EmptyPokeBallEntity emptyPokeBallEntity = new EmptyPokeBallEntity(pokeBall, player.getWorld(), player, EMPTY_POKEBALL);
        float overhandFactor;
        if (player.getPitch() < 0) {
            overhandFactor = (float) (5.0F * Math.cos(Math.toRadians(player.getPitch())));
        } else {
            overhandFactor = 5.0F;
        }

        emptyPokeBallEntity.setVelocity(player, player.getPitch() - overhandFactor, player.getYaw(), 0.0F, pokeBall.getThrowPower(), 1.0F);
        emptyPokeBallEntity.setPosition(emptyPokeBallEntity.getPos().add(emptyPokeBallEntity.getVelocity().normalize().multiply(1.0)));
        emptyPokeBallEntity.setOwner(player);

        world.spawnEntity(emptyPokeBallEntity);
        NbtComponent component = originalStack.getComponents().get(DataComponentTypes.CUSTOM_DATA);
        if (component != null) {
            NovaRaids.LOGGER.error("[NovaRaids] Applying custom data!");
            ((DataPokeBallEntity) (Object) emptyPokeBallEntity).novaRaids$setCustomData(component.copyNbt());
        }
    }
}
