package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.cache.PlayerRaidCache;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.literal;

public class RaidLeaveCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("leave")
                .requires(Permissions.require("novaraids.leave", 4))
                .executes(RaidLeaveCommand::execute);
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        if (ctx.getSource().isExecutedByPlayer()) {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player == null) return 0;
            var activeRaid = PlayerRaidCache.currentRaid(player);
            if (activeRaid != null) activeRaid.removePlayer(player.getUuid());
        }
        return Command.SINGLE_SUCCESS;
    }
}
