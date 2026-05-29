package me.unariginal.novaraids.commands;

import com.mojang.brigadier.CommandDispatcher;
import me.unariginal.novaraids.commands.command.*;
import me.unariginal.novaraids.data.*;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.*;

import static net.minecraft.server.command.CommandManager.literal;

public class RaidCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("raid")
                .then(RaidReloadCommand.register())
                .then(RaidStartCommand.register())
                .then(RaidStopCommand.register())
                .then(RaidGiveCommand.register())
                .then(RaidListCommand.register())
                .then(RaidJoinCommand.register())
                .then(RaidLeaveCommand.register())
                .then(RaidQueueCommand.register())
                .then(RaidCheckBannedCommand.register())
                .then(RaidHistoryCommand.register())
                .then(RaidSkipPhaseCommand.register())
                .then(RaidTestRewardsCommand.register())
                .then(RaidWorldCommand.register())
                .then(RaidDamageCommand.register())
                .then(RaidScheduleCommand.register())
                .then(RaidPlayerVisibilityCommand.register())
                .then(RaidPokemonVisibilityCommand.register())
        );
    }
}