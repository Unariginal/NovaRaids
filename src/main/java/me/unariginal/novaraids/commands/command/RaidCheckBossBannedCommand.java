package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.unariginal.novaraids.commands.suggestions.BossSuggestions;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static me.unariginal.novaraids.config.ConfigManager.*;
import static me.unariginal.novaraids.guis.ContrabandGui.openContrabandGui;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidCheckBossBannedCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("boss")
                .then(argument("boss", StringArgumentType.string())
                        .suggests(new BossSuggestions())
                        .executes(RaidCheckBossBannedCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        Boss boss = Boss.getBoss(StringArgumentType.getString(ctx, "boss"));
        if (boss == null) return 0;
        openContrabandGui(player, BOSS_CONTRABAND_GUI, null, boss);
        return Command.SINGLE_SUCCESS;
    }
}
