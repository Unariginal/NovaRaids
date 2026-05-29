package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.NovaRaids;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.literal;

public class RaidPokemonVisibilityCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("togglePokemonVisibility")
                .requires(Permissions.require("novaraids.togglePokemonVisibility", 4))
                .executes(RaidPokemonVisibilityCommand::execute);
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 1;
        if (!NovaRaids.INSTANCE.ignorePokemonVisibility.contains(player.getUuid())) {
            NovaRaids.INSTANCE.ignorePokemonVisibility.add(player.getUuid());
        } else {
            NovaRaids.INSTANCE.ignorePokemonVisibility.remove(player.getUuid());
        }
        return Command.SINGLE_SUCCESS;
    }
}
