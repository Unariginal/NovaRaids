package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static me.unariginal.novaraids.config.ConfigManager.GLOBAL_CONTRABAND_GUI;
import static me.unariginal.novaraids.guis.ContrabandGui.openContrabandGui;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidCheckGlobalBannedCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("global")
                .executes(RaidCheckGlobalBannedCommand::execute);
    }

    public static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        openContrabandGui(player, GLOBAL_CONTRABAND_GUI, null, null);
        return Command.SINGLE_SUCCESS;
    }
}
