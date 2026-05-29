package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.NovaRaids;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.literal;

public class RaidPlayerVisibilityCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("togglePlayerVisibility")
                .requires(Permissions.require("novaraids.togglePlayerVisibility", 4))
                .executes(RaidPlayerVisibilityCommand::execute);
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 1;
        if (!NovaRaids.INSTANCE.ignorePlayerVisibility.contains(player.getUuid())) {
            NovaRaids.INSTANCE.ignorePlayerVisibility.add(player.getUuid());
        } else {
            NovaRaids.INSTANCE.ignorePlayerVisibility.remove(player.getUuid());
        }
        return Command.SINGLE_SUCCESS;
    }
}
