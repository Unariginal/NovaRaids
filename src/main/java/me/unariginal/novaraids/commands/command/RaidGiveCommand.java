package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidGiveCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("give")
                .requires(Permissions.require("novaraids.give", 4))
                .then(argument("player", EntityArgumentType.players())
                        .then(RaidGivePassCommand.register())
                        .then(RaidGiveVoucherCommand.register())
                        .then(RaidGivePokeballCommand.register()));
    }
}
