package me.unariginal.novaraids.data.rewards;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Objects;

public class CommandReward extends Reward {
    private final List<String> commands;

    public CommandReward(String name, List<String> commands) {
        super(name, "command");
        this.commands = commands;
    }

    public List<String> commands() {
        return commands;
    }

    @Override
    public void apply_reward(ServerPlayerEntity player) {
        CommandManager cmdManager = Objects.requireNonNull(player.getServer()).getCommandManager();
        ServerCommandSource source = player.getServer().getCommandSource();
        for (String command : commands) {
            cmdManager.executeWithPrefix(source, command.replaceAll("%player%", player.getNameForScoreboard()));
        }
    }
}
