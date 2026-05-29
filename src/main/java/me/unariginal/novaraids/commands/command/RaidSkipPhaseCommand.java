package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.data.Task;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.raid.RaidManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidSkipPhaseCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("skipphase")
                .requires(Permissions.require("novaraids.skipphase", 4))
                .then(argument("id", IntegerArgumentType.integer(1))
                        .executes(RaidSkipPhaseCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        Raid raid = RaidManager.getRaid(id - 1);
        if (raid == null) return 0;

        List<Task> tasks = raid.tasks.entrySet().stream().findFirst().orElseThrow().getValue();
        raid.removeTask(raid.tasks.entrySet().stream().findFirst().orElseThrow().getKey());
        for (Task task : tasks) {
            raid.addTask(task.world(), 1L, task.action());
        }
        return Command.SINGLE_SUCCESS;
    }
}
