package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.literal;

public class RaidCheckBannedCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("checkbanned")
                .requires(Permissions.require("novaraids.checkbanned", 4))
                .then(RaidCheckGlobalBannedCommand.register())
                .then(RaidCheckCategoryBannedCommand.register())
                .then(RaidCheckBossBannedCommand.register());
    }
}
