package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class RaidHistoryCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("history")
                .requires(Permissions.require("novaraids.history", 4))
                .executes(RaidHistoryCommand::execute);
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendMessage(Text.literal("Not Implemented"));
        return Command.SINGLE_SUCCESS;
    }
}
