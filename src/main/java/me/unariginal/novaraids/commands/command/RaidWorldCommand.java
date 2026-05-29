package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.utils.TextUtils;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.literal;

public class RaidWorldCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("world")
                .requires(Permissions.require("novaraids.world", 4))
                .executes(RaidWorldCommand::execute);
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player != null) player.sendMessage(TextUtils.deserialize(player.getServerWorld().getRegistryKey().getValue().toString()));
        return Command.SINGLE_SUCCESS;
    }
}
