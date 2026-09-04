package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.commands.suggestions.BossSuggestions;
import me.unariginal.novaraids.commands.suggestions.CategorySuggestions;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
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
                            RaidManager.queueRaid(boss, ctx.getSource().getPlayer(), null, null, null);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(argument("modifier", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    String bossId = StringArgumentType.getString(ctx, "boss");
                                    Boss boss = Boss.getBoss(bossId);
                                    if (boss == null) return builder.buildFuture();
                                    Category category = Category.getCategory(boss.categoryId);
                                    if (category == null) return builder.buildFuture();
                                    category.modifiers.keySet().forEach(builder::suggest);
                                    builder.suggest("no_modifier");
                                    return builder.buildFuture();
                                })
                                .then(argument("require_pass", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            Boss boss = Boss.getBoss(StringArgumentType.getString(ctx, "boss"));
                                            if (boss == null) return 0;
                                            CategoryModifier modifier = CategoryModifier.getModifier(StringArgumentType.getString(ctx, "modifier"));
                                            RaidManager.queueRaid(boss, ctx.getSource().getPlayer(), null, BoolArgumentType.getBool(ctx, "require_pass"), modifier);
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .then(literal("random")
                        .then(argument("require_pass", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    Boss boss = Boss.getRandomBoss(null);
                                    if (boss == null) return 0;
                                    RaidManager.queueRaid(boss, ctx.getSource().getPlayer(), null, BoolArgumentType.getBool(ctx, "require_pass"), null);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .executes(ctx -> {
                            Boss boss = Boss.getRandomBoss(null);
                            if (boss == null) return 0;
                            RaidManager.queueRaid(boss, ctx.getSource().getPlayer(), null, null, null);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(argument("category", StringArgumentType.string())
                                .suggests(new CategorySuggestions())
                                .executes(ctx -> {
                                    Boss boss = Boss.getRandomBoss(StringArgumentType.getString(ctx, "category"), null);
                                    if (boss == null) return 0;
                                    RaidManager.queueRaid(boss, ctx.getSource().getPlayer(), null, null, null);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(argument("modifier", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            Category category = Category.getCategory(StringArgumentType.getString(ctx, "category"));
                                            if (category == null) return builder.buildFuture();
                                            category.modifiers.keySet().forEach(builder::suggest);
                                            builder.suggest("no_modifier");
                                            return builder.buildFuture();
                                        })
                                        .then(argument("require_pass", BoolArgumentType.bool())
                                                .executes(ctx -> {
                                                    Boss boss = Boss.getRandomBoss(StringArgumentType.getString(ctx, "category"), null);
                                                    if (boss == null) return 0;
                                                    CategoryModifier modifier = CategoryModifier.getModifier(StringArgumentType.getString(ctx, "modifier"));
                                                    RaidManager.queueRaid(boss, ctx.getSource().getPlayer(), null, BoolArgumentType.getBool(ctx, "require_pass"), modifier);
                                                    return Command.SINGLE_SUCCESS;
                                                })))));
    }
}
