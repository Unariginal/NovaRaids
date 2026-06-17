package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.raid.RaidManager;
import me.unariginal.novaraids.placeholders.ParseContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static me.unariginal.novaraids.config.ConfigManager.MESSAGES;
import static me.unariginal.novaraids.utils.TextUtils.deserialize;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidJoinCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("join")
                .requires(Permissions.require("novaraids.join", 4))
                .executes(RaidListCommand::execute)
                .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                        .executes(RaidJoinCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        Raid raid = RaidManager.getRaid(id - 1);
        if (raid == null) return 0;

        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        ParseContext parseContext = ParseContext.builder().player(player).raid(raid).build();

        if (raid.participatingPlayers.size() < raid.maxPlayers || Permissions.check(player, "novaraids.override") || raid.maxPlayers == -1) {
            if (raid.addPlayer(player.getUuid(), false)) {
                player.sendMessage(deserialize(MESSAGES.feedback.joinedRaid, parseContext));
                return Command.SINGLE_SUCCESS;
            } else {
                return 0;
            }
        } else {
            player.sendMessage(deserialize(MESSAGES.feedback.warnings.maxPlayers, parseContext));
            return 0;
        }
    }
}
