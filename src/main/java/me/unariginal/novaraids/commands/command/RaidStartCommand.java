package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.commands.suggestions.BossSuggestions;
import me.unariginal.novaraids.commands.suggestions.CategorySuggestions;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.raid.RaidManager;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class RaidStartCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("start")
                .requires(Permissions.require("novaraids.start", 4))
                .then(argument("boss", StringArgumentType.string())
                        .suggests(new BossSuggestions())
                        .executes(ctx -> {
                            Boss boss = Boss.getBoss(StringArgumentType.getString(ctx, "boss"));
                            if (boss == null) return 0;
                            RaidManager.queueRaid(boss, ctx.getSource().getPlayer(), null, null);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(argument("require_pass", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    Boss boss = Boss.getBoss(StringArgumentType.getString(ctx, "boss"));
                                    if (boss == null) return 0;
                                    RaidManager.queueRaid(boss, ctx.getSource().getPlayer(), null, BoolArgumentType.getBool(ctx, "require_pass"));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(literal("random")
                        .then(argument("require_pass", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    Boss boss = Boss.getRandomBoss(null);
                                    if (boss == null) return 0;
                                    RaidManager.queueRaid(boss, ctx.getSource().getPlayer(), null, BoolArgumentType.getBool(ctx, "require_pass"));
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .executes(ctx -> {
                            Boss boss = Boss.getRandomBoss(null);
                            if (boss == null) return 0;
                            RaidManager.queueRaid(boss, ctx.getSource().getPlayer(), null, null);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(argument("category", StringArgumentType.string())
                                .suggests(new CategorySuggestions())
                                .executes(ctx -> {
                                    Boss boss = Boss.getRandomBoss(StringArgumentType.getString(ctx, "category"), null);
                                    if (boss == null) return 0;
                                    RaidManager.queueRaid(boss, ctx.getSource().getPlayer(), null, null);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(argument("require_pass", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            Boss boss = Boss.getRandomBoss(StringArgumentType.getString(ctx, "category"), null);
                                            if (boss == null) return 0;
                                            RaidManager.queueRaid(boss, ctx.getSource().getPlayer(), null, BoolArgumentType.getBool(ctx, "require_pass"));
                                            return Command.SINGLE_SUCCESS;
                                        }))));
    }
}
