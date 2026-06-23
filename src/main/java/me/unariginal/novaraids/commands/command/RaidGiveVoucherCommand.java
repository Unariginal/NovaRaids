package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.unariginal.novaraids.commands.suggestions.BossSuggestions;
import me.unariginal.novaraids.commands.suggestions.CategorySuggestions;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.items.Voucher;
import me.unariginal.novaraids.placeholders.ParseContext;
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
import static me.unariginal.novaraids.utils.TextUtils.deserialize;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidGiveVoucherCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("voucher")
                .then(argument("boss", StringArgumentType.string())
                        .suggests(new BossSuggestions())
                        .then(argument("count", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    RaidGiveVoucherCommand.execute(
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
                            RaidGiveVoucherCommand.execute(
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
                                    RaidGiveVoucherCommand.execute(
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
                            RaidGiveVoucherCommand.execute(
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
                                            RaidGiveVoucherCommand.execute(
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
                                    RaidGiveVoucherCommand.execute(
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
                                    RaidGiveVoucherCommand.execute(
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
                            RaidGiveVoucherCommand.execute(
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
                                            RaidGiveVoucherCommand.execute(
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
                                    RaidGiveVoucherCommand.execute(
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
        String raidItem = "raid_voucher";
        String raidBoss;
        String raidCategory;
        Voucher voucher;
        Text itemName;
        LoreComponent lore;

        ParseContext.Builder parseContextBuilder = ParseContext.builder();

        if (!isRandom) {
            if (boss != null) {
                ParseContext parseContext = parseContextBuilder.boss(boss).prioritizeRaid(false).build();

                raidBoss = boss.bossId;
                raidCategory = "null";
                voucher = boss.itemSettings.bossVoucher;

                itemName = deserialize(voucher.voucherName, parseContext);
                List<Text> loreText = new ArrayList<>();
                for (String loreLine : voucher.voucherLore) {
                    loreText.add(deserialize(loreLine, parseContext));
                }
                lore = new LoreComponent(loreText);
            } else {
                raidBoss = "*";
                if (category != null) {
                    parseContextBuilder.category(category);
                    raidCategory = category.categoryId;
                    voucher = category.itemSettings.categoryChoiceVoucher;
                } else {
                    raidCategory = "*";
                    voucher = CONFIG.itemSettings.voucherSettings.globalChoiceVoucher;
                }
                ParseContext parseContext = parseContextBuilder.build();

                itemName = deserialize(voucher.voucherName, parseContext);
                List<Text> loreText = new ArrayList<>();
                for (String loreLine : voucher.voucherLore) {
                    loreText.add(deserialize(loreLine, parseContext));
                }
                lore = new LoreComponent(loreText);
            }
        } else {
            raidBoss = "random";
            if (category != null) {
                parseContextBuilder.category(category);
                raidCategory = category.categoryId;
                voucher = category.itemSettings.categoryRandomVoucher;
            } else {
                raidCategory = "null";
                voucher = CONFIG.itemSettings.voucherSettings.globalRandomVoucher;
            }
            ParseContext parseContext = parseContextBuilder.build();

            itemName = deserialize(voucher.voucherName, parseContext);
            List<Text> loreText = new ArrayList<>();
            for (String loreLine : voucher.voucherLore) {
                loreText.add(deserialize(loreLine, parseContext));
            }
            lore = new LoreComponent(loreText);
        }

        ItemStack voucherItem = voucher.voucherItem.copyWithCount(count);

        NbtCompound customData = new NbtCompound();
        customData.putString("raid_item", raidItem);
        customData.putString("raid_boss", raidBoss);
        customData.putString("raid_category", raidCategory);

        voucherItem.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
        voucherItem.set(DataComponentTypes.CUSTOM_NAME, itemName);
        voucherItem.set(DataComponentTypes.LORE, lore);

        players.forEach(player -> {
            if (!player.giveItemStack(voucherItem)) {
                player.getInventory().offerOrDrop(voucherItem);
            }
        });
    }
}
