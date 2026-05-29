package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.utils.TextUtils;
import net.minecraft.server.command.ServerCommandSource;

import static me.unariginal.novaraids.NovaRaids.reloadConfig;
import static me.unariginal.novaraids.config.ConfigManager.MESSAGES;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidReloadCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("reload")
                .requires(Permissions.require("novaraids.reload", 4))
                .executes(RaidReloadCommand::execute);
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        reloadConfig();
        ctx.getSource().sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.reload)));
        return Command.SINGLE_SUCCESS;
    }
}
