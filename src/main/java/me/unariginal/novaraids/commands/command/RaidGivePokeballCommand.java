package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.unariginal.novaraids.commands.suggestions.BossSuggestions;
import me.unariginal.novaraids.commands.suggestions.CategorySuggestions;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.items.RaidBall;
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

public class RaidGivePokeballCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("pokeball")
                .then(argument("boss", StringArgumentType.string())
                        .suggests(new BossSuggestions())
                        .then(argument("pokeball", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    Boss boss = Boss.getBoss(StringArgumentType.getString(ctx, "boss"));
                                    if (boss != null) boss.itemSettings.raidBalls.keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .then(argument("count", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            RaidGivePokeballCommand.execute(
                                                    ctx.getSource().getPlayer(),
                                                    EntityArgumentType.getPlayers(ctx, "players").stream().toList(),
                                                    StringArgumentType.getString(ctx, "pokeball"),
                                                    Boss.getBoss(StringArgumentType.getString(ctx, "boss")),
                                                    null,
                                                    IntegerArgumentType.getInteger(ctx, "count")
                                            );
                                            return Command.SINGLE_SUCCESS;
                                        })))
                .then(literal("category")
                        .then(argument("category", StringArgumentType.string())
                                .suggests(new CategorySuggestions())
                                .then(argument("pokeball", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            Category category = Category.getCategory(StringArgumentType.getString(ctx, "category"));
                                            if (category != null) category.itemSettings.raidBalls.keySet().forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .then(argument("count", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    RaidGivePokeballCommand.execute(
                                                            ctx.getSource().getPlayer(),
                                                            EntityArgumentType.getPlayers(ctx, "players").stream().toList(),
                                                            StringArgumentType.getString(ctx, "pokeball"),
                                                            null,
                                                            Category.getCategory(StringArgumentType.getString(ctx, "category")),
                                                            IntegerArgumentType.getInteger(ctx, "count")
                                                    );
                                                    return Command.SINGLE_SUCCESS;
                                                }))))))
                .then(literal("global")
                        .then(argument("pokeball", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    CONFIG.itemSettings.raidBallSettings.raidBalls.keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .then(argument("count", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            RaidGivePokeballCommand.execute(
                                                    ctx.getSource().getPlayer(),
                                                    EntityArgumentType.getPlayers(ctx, "players").stream().toList(),
                                                    StringArgumentType.getString(ctx, "pokeball"),
                                                    null,
                                                    null,
                                                    IntegerArgumentType.getInteger(ctx, "count")
                                            );
                                            return Command.SINGLE_SUCCESS;
                                        }))));
    }

    // TODO: Add feedback messages
    public static void execute(@Nullable ServerPlayerEntity executor, List<ServerPlayerEntity> players, String raidBallId, @Nullable Boss boss, @Nullable Category category, int count) {
        String raidItem = "raid_ball";
        String raidBoss;
        String raidCategory;
        RaidBall raidBall;
        Text itemName;
        LoreComponent lore;

        if (boss != null) {
            raidBoss = boss.bossId;
            raidCategory = "null";
            raidBall = boss.itemSettings.getRaidBall(raidBallId);

            itemName = TextUtils.deserialize(TextUtils.parse(raidBall.pokeballName, boss));
            List<Text> loreText = new ArrayList<>();
            for (String loreLine : raidBall.pokeballLore) {
                loreText.add(TextUtils.deserialize(TextUtils.parse(loreLine, boss)));
            }
            lore = new LoreComponent(loreText);
        } else {
            raidBoss = "*";
            if (category != null) {
                raidCategory = category.categoryId;
                raidBall = category.itemSettings.getRaidBall(raidBallId);
            } else {
                raidCategory = "*";
                raidBall = CONFIG.getRaidBall(raidBallId);
            }
            itemName = TextUtils.deserialize(TextUtils.parse(raidBall.pokeballName));
            List<Text> loreText = new ArrayList<>();
            for (String loreLine : raidBall.pokeballLore) {
                loreText.add(TextUtils.deserialize(TextUtils.parse(loreLine)));
            }
            lore = new LoreComponent(loreText);
        }

        players.forEach(player -> {
            ItemStack ballItem = raidBall.pokeballItem.copyWithCount(count);

            NbtCompound customData = new NbtCompound();
            customData.putString("raid_item", raidItem);
            customData.putUuid("owner_uuid", player.getUuid());
            customData.putString("raid_boss", raidBoss);
            customData.putString("raid_category", raidCategory);

            ballItem.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
            ballItem.set(DataComponentTypes.CUSTOM_NAME, itemName);
            ballItem.set(DataComponentTypes.LORE, lore);

            player.giveItemStack(ballItem);
        });
    }
}
