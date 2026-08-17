package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static me.unariginal.novaraids.config.ConfigManager.RAID_HISTORY;
import static me.unariginal.novaraids.guis.history.HistoryGui.openHistoryGui;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidHistoryCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("history")
                .requires(Permissions.require("novaraids.history", 4))
                .then(argument("category", StringArgumentType.string())
                        .suggests((ctx, builder) -> {
                            RAID_HISTORY.keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(RaidHistoryCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        String categoryId = StringArgumentType.getString(ctx, "category");
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        openHistoryGui(player, categoryId, 1);
        return Command.SINGLE_SUCCESS;
    }
}
