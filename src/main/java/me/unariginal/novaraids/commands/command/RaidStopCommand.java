package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.raid.RaidManager;
import me.unariginal.novaraids.utils.TextUtils;
import net.minecraft.server.command.ServerCommandSource;

import static me.unariginal.novaraids.config.ConfigManager.MESSAGES;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidStopCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("stop")
                .requires(Permissions.require("novaraids.stop", 4))
                .then(argument("id", IntegerArgumentType.integer(1))
                        .executes(RaidStopCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        Raid raid = RaidManager.getRaid(id - 1);
        if (raid == null) return 0;

        ctx.getSource().sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.raidStopped, raid)));

        RaidManager.stopRaid(raid.uuid);
        return Command.SINGLE_SUCCESS;
    }
}
