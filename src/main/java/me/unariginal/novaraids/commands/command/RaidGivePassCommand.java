package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.unariginal.novaraids.commands.suggestions.BossSuggestions;
import me.unariginal.novaraids.commands.suggestions.CategorySuggestions;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.utils.TextUtils;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static me.unariginal.novaraids.config.ConfigManager.CONFIG;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidGivePassCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("pass")
                .then(argument("boss", StringArgumentType.string())
                        .suggests(new BossSuggestions())
                        .then(argument("count", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    RaidGivePassCommand.execute(
                                            ctx.getSource().getPlayer(),
                                            EntityArgumentType.getPlayers(ctx, "player").stream().toList(),
                                            Boss.getBoss(StringArgumentType.getString(ctx, "boss")),
                                            null,
                                            IntegerArgumentType.getInteger(ctx, "count"),
                                            false
                                    );
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .executes(ctx -> {
                            RaidGivePassCommand.execute(
                                    ctx.getSource().getPlayer(),
                                    EntityArgumentType.getPlayers(ctx, "player").stream().toList(),
                                    Boss.getBoss(StringArgumentType.getString(ctx, "boss")),
                                    null,
                                    1,
                                    false
                            );
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(literal("*")
                        .then(argument("count", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    RaidGivePassCommand.execute(
                                            ctx.getSource().getPlayer(),
                                            EntityArgumentType.getPlayers(ctx, "player").stream().toList(),
                                            null,
                                            null,
                                            IntegerArgumentType.getInteger(ctx, "count"),
                                            false
                                    );
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .executes(ctx -> {
                            RaidGivePassCommand.execute(
                                    ctx.getSource().getPlayer(),
                                    EntityArgumentType.getPlayers(ctx, "player").stream().toList(),
                                    null,
                                    null,
                                    1,
                                    false
                            );
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(argument("category", StringArgumentType.string())
                                .suggests(new CategorySuggestions())
                                .then(argument("count", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            RaidGivePassCommand.execute(
                                                    ctx.getSource().getPlayer(),
                                                    EntityArgumentType.getPlayers(ctx, "player").stream().toList(),
                                                    null,
                                                    Category.getCategory(StringArgumentType.getString(ctx, "category")),
                                                    IntegerArgumentType.getInteger(ctx, "count"),
                                                    false
                                            );
                                            return Command.SINGLE_SUCCESS;
                                        }))
                                .executes(ctx -> {
                                    RaidGivePassCommand.execute(
                                            ctx.getSource().getPlayer(),
                                            EntityArgumentType.getPlayers(ctx, "player").stream().toList(),
                                            null,
                                            Category.getCategory(StringArgumentType.getString(ctx, "category")),
                                            1,
                                            false
                                    );
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(literal("random")
                        .then(argument("count", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    RaidGivePassCommand.execute(
                                            ctx.getSource().getPlayer(),
                                            EntityArgumentType.getPlayers(ctx, "player").stream().toList(),
                                            null,
                                            null,
                                            IntegerArgumentType.getInteger(ctx, "count"),
                                            true
                                    );
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .executes(ctx -> {
                            RaidGivePassCommand.execute(
                                    ctx.getSource().getPlayer(),
                                    EntityArgumentType.getPlayers(ctx, "player").stream().toList(),
                                    null,
                                    null,
                                    1,
                                    true
                            );
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(argument("category", StringArgumentType.string())
                                .suggests(new CategorySuggestions())
                                .then(argument("count", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            RaidGivePassCommand.execute(
                                                    ctx.getSource().getPlayer(),
                                                    EntityArgumentType.getPlayers(ctx, "player").stream().toList(),
                                                    null,
                                                    Category.getCategory(StringArgumentType.getString(ctx, "category")),
                                                    IntegerArgumentType.getInteger(ctx, "count"),
                                                    true
                                            );
                                            return Command.SINGLE_SUCCESS;
                                        }))
                                .executes(ctx -> {
                                    RaidGivePassCommand.execute(
                                            ctx.getSource().getPlayer(),
                                            EntityArgumentType.getPlayers(ctx, "player").stream().toList(),
                                            null,
                                            Category.getCategory(StringArgumentType.getString(ctx, "category")),
                                            1,
                                            true
                                    );
                                    return Command.SINGLE_SUCCESS;
                                })));
    }

    // TODO: Add feedback messages
    public static void execute(@Nullable ServerPlayerEntity executor, List<ServerPlayerEntity> players, @Nullable Boss boss, @Nullable Category category, int count, boolean isRandom) {
        String raidItem = "raid_pass";
        String raidBoss;
        String raidCategory;
        Pass pass;
        Text itemName;
        LoreComponent lore;

        if (!isRandom) {
            if (boss != null) {
                raidBoss = boss.bossId;
                raidCategory = "null";
                pass = boss.itemSettings.bossPass;

                itemName = TextUtils.deserialize(TextUtils.parse(pass.passName, boss));
                List<Text> loreText = new ArrayList<>();
                for (String loreLine : pass.passLore) {
                    loreText.add(TextUtils.deserialize(TextUtils.parse(loreLine, boss)));
                }
                lore = new LoreComponent(loreText);
            } else {
                raidBoss = "*";
                if (category != null) {
                    raidCategory = category.categoryId;
                    pass = category.itemSettings.categoryPass;
                } else {
                    raidCategory = "*";
                    pass = CONFIG.itemSettings.passSettings.globalPass;
                }

                itemName = TextUtils.deserialize(TextUtils.parse(pass.passName));
                List<Text> loreText = new ArrayList<>();
                for (String loreLine : pass.passLore) {
                    loreText.add(TextUtils.deserialize(TextUtils.parse(loreLine)));
                }
                lore = new LoreComponent(loreText);
            }
        } else {
            Boss newBoss;
            if (category != null) newBoss = Boss.getRandomBoss(category.categoryId, null);
            else newBoss = Boss.getRandomBoss(null);
            execute(executor, players, newBoss, null, count, false);
            return;
        }

        ItemStack passItem = pass.passItem.copyWithCount(count);

        NbtCompound customData = new NbtCompound();
        customData.putString("raid_item", raidItem);
        customData.putString("raid_boss", raidBoss);
        customData.putString("raid_category", raidCategory);

        passItem.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
        passItem.set(DataComponentTypes.CUSTOM_NAME, itemName);
        passItem.set(DataComponentTypes.LORE, lore);

        players.forEach(player -> player.giveItemStack(passItem));
    }
}
