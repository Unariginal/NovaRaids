//package me.unariginal.novaraids.mixin;
//
//import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
//import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
//import com.mojang.serialization.Codec;
//import me.unariginal.novaraids.NovaRaids;
//import me.unariginal.novaraids.managers.EventManager;
//import me.unariginal.novaraids.managers.Raid;
//import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
//import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
//import net.minecraft.component.ComponentMap;
//import net.minecraft.component.DataComponentTypes;
//import net.minecraft.entity.EntityAttachmentType;
//import net.minecraft.entity.EntityAttachments;
//import net.minecraft.entity.data.TrackedDataHandler;
//import net.minecraft.entity.data.TrackedDataHandlerRegistry;
//import net.minecraft.item.ItemStack;
//import net.minecraft.nbt.NbtCompound;
//import net.minecraft.server.network.ServerPlayerEntity;
//import net.minecraft.util.Identifier;
//import net.minecraft.util.hit.EntityHitResult;
//import org.jetbrains.annotations.NotNull;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.Unique;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//import java.util.Objects;
//
//@Mixin(EmptyPokeBallEntity.class)
//public abstract class CaptureMixin {
//    @Shadow protected abstract void onEntityHit(@NotNull EntityHitResult hitResult);
//    @Shadow protected abstract void attemptCatch(PokemonEntity pokemonEntity);
//
//    @Inject(method = "tick", at = @At("HEAD"))
//    private void onTick(CallbackInfo ci) {
//        CompoundRaids cr = CompoundRaids.INSTANCE;
//        cr.logger().info("The mixin do be happening..");
//        EmptyPokeBallEntity entity = (EmptyPokeBallEntity) (Object) this;
//
//        if (!(entity.getOwner() instanceof ServerPlayerEntity player)) return;
//        cr.logger().info("Okay player good good");
//
//        AttachmentType<String> feature_attachment = AttachmentRegistry.createPersistent(Identifier.of("compoundraids", "persistent"), Codec.STRING);
//        ComponentMap components = heldItem.getItem().getComponents();
//        NbtCompound nbt = getRaidItemNbt(components);
//
//        if (nbt == null) return;
//        cr.logger().info("NBT has been received");
//
//        if (!isValidRaidBall(nbt, player)) return;
//        cr.logger().info("Is raid ball");
//
//        for (Raid raid : cr.active_raids().values()) {
//            if (isValidRaidCapture(raid, player, entity)) {
//                cr.logger().info("Handling the capture");
//                EventManager.handle_capture(player, entity, raid);
//                break;
//            }
//        }
//    }
//
//    @Unique
//    private NbtCompound getRaidItemNbt(ComponentMap components) {
//        if (!components.contains(DataComponentTypes.CUSTOM_DATA)) return null;
//        NbtCompound nbt = Objects.requireNonNull(components.get(DataComponentTypes.CUSTOM_DATA)).copyNbt();
//        return (nbt != null && nbt.contains("raid_item") && nbt.getString("raid_item").equalsIgnoreCase("raid_ball"))
//                ? nbt : null;
//    }
//
//    @Unique
//    private boolean isValidRaidBall(NbtCompound nbt, ServerPlayerEntity player) {
//        return nbt.contains("owner_uuid") && nbt.getUuid("owner_uuid").equals(player.getUuid());
//    }
//
//    @Unique
//    private boolean isValidRaidCapture(Raid raid, ServerPlayerEntity player, EmptyPokeBallEntity entity) {
//        return raid.participating_players().contains(player) &&
//                raid.stage() == 4 &&
//                raid.raidBoss_entity().distanceTo(entity) < 2.0f;
//    }
//}
