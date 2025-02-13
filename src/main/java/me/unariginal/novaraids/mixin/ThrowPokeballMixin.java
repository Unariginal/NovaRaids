//package me.unariginal.compoundraids.mixin;
//
//import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
//import com.cobblemon.mod.common.item.PokeBallItem;
//import net.minecraft.component.ComponentMap;
//import net.minecraft.component.DataComponentTypes;
//import net.minecraft.nbt.NbtCompound;
//import net.minecraft.server.network.ServerPlayerEntity;
//import net.minecraft.world.World;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
//
//@Mixin(PokeBallItem.class)
//public class ThrowPokeballMixin {
//    @Inject(method = {"throwPokeBall"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"), locals = LocalCapture.CAPTURE_FAILHARD)
//    private void throwPokeBall(World world, ServerPlayerEntity player, CallbackInfo ci, EmptyPokeBallEntity pokeBallEntity) {
//        PokeBallItem pokeBallItem = (PokeBallItem) (Object) this;
//        ComponentMap components = pokeBallItem.getComponents();
//        NbtCompound nbt = components.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
//        if (nbt != null && nbt.contains("raid_item") && nbt.getString("raid_item").equalsIgnoreCase("raid_ball")
//            && nbt.contains("owner_uuid") && nbt.getUuid("owner_uuid").equals(player.getUuid())) {
//            pokeBallEntity.writeNbt(nbt);
//        }
//    }
//}
