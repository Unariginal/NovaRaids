package me.unariginal.novaraids.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.ShowdownActionResponseType;
import com.cobblemon.mod.common.net.messages.server.battle.BattleSelectActionsPacket;
import com.cobblemon.mod.common.net.serverhandling.battle.BattleSelectActionsHandler;
import me.unariginal.novaraids.cache.PlayerRaidCache;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// I'm looking at you CobblemonBattleExtras -.-
@Mixin(value = BattleSelectActionsHandler.class)
public class BattleForfeitMixin {
    @Inject(method = "handle(Lcom/cobblemon/mod/common/net/messages/server/battle/BattleSelectActionsPacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("HEAD"), cancellable = true)
    private void cancelForfeitAction(BattleSelectActionsPacket packet, MinecraftServer server, ServerPlayerEntity player, CallbackInfo ci) {
        PokemonBattle battle = BattleRegistry.getBattle(packet.getBattleId());
        if (battle == null) return;
        if (packet.getShowdownActionResponses().stream().anyMatch(showdownActionResponse ->
                showdownActionResponse.getType() == ShowdownActionResponseType.FORFEIT)
        ) {
            if (PlayerRaidCache.isInRaid(player)) {
                ci.cancel();
            }
        }
    }
}
