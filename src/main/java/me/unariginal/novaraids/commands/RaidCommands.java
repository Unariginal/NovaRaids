package me.unariginal.novaraids.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.Boss;
import me.unariginal.novaraids.data.Category;
import me.unariginal.novaraids.data.Location;
import me.unariginal.novaraids.managers.Raid;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RaidCommands {
    private final NovaRaids nr = NovaRaids.INSTANCE;

    public RaidCommands() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> commandDispatcher.register(
            CommandManager.literal("raid")
                    .then(
                            CommandManager.literal("reload")
                                    .requires(Permissions.require("compoundraids.reload", 4))
                                    .executes(this::reload)
                    )
                    .then(
                            CommandManager.literal("start")
                                    .requires(Permissions.require("compoundraids.start", 4))
                                    .then(
                                            CommandManager.argument("boss", StringArgumentType.string())
                                                    .suggests(new BossSuggestions())
                                                    .executes(ctx -> start(nr.config().getBoss(StringArgumentType.getString(ctx, "boss"))))
                                    )
                                    .then(
                                            CommandManager.literal("random")
                                                    .executes(ctx -> {
                                                        Random rand = new Random();
                                                        return start(nr.config().getBosses().get(rand.nextInt(nr.config().getBosses().size())));
                                                    })
                                                    .then(
                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                    .suggests(new CategorySuggestions())
                                                                    .executes(ctx -> {
                                                                        String categoryStr = StringArgumentType.getString(ctx, "category");
                                                                        Category category = nr.config().getCategory(categoryStr);
                                                                        Random rand = new Random();

                                                                        List<Boss> possible_bosses = new ArrayList<>();
                                                                        for (Boss boss : nr.config().getBosses()) {
                                                                            if (boss.category().equalsIgnoreCase(category.name())) {
                                                                                possible_bosses.add(boss);
                                                                            }
                                                                        }
                                                                        return start(possible_bosses.get(rand.nextInt(possible_bosses.size())));
                                                                    })
                                                    )
                                    )
                    )
                    .then(
                            CommandManager.literal("stop")
                                    .requires(Permissions.require("compoundraids.stop", 4))
                                    .then(
                                            CommandManager.argument("id", IntegerArgumentType.integer(1))
                                                    .executes(this::stop)
                                    )
                    )
                    .then(
                            CommandManager.literal("give")
                                    .requires(Permissions.require("compoundraids.give", 4))
                                    .then(
                                            CommandManager.argument("player", EntityArgumentType.player())
                                                    .then(
                                                            CommandManager.literal("pass")
                                                                    .then(
                                                                            CommandManager.argument("boss", StringArgumentType.string())
                                                                                    .suggests(new BossSuggestions())
                                                                                    .executes(ctx -> give(ctx, "pass", StringArgumentType.getString(ctx, "boss"), null))
                                                                    )
                                                                    .then(
                                                                            CommandManager.literal("*")
                                                                                    .executes(ctx -> give(ctx, "pass", "*", null))
                                                                                    .then(
                                                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                                                    .suggests(new CategorySuggestions())
                                                                                                    .executes(ctx -> give(ctx, "pass", "*", StringArgumentType.getString(ctx, "category")))
                                                                                    )
                                                                    )
                                                                    .then(
                                                                            CommandManager.literal("random")
                                                                                    .executes(ctx -> give(ctx, "pass", "random", null))
                                                                                    .then(
                                                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                                                    .suggests(new CategorySuggestions())
                                                                                                    .executes(ctx -> give(ctx, "pass", "random", StringArgumentType.getString(ctx, "category")))
                                                                                    )
                                                                    )
                                                    )
                                                    .then(
                                                            CommandManager.literal("voucher")
                                                                    .then(
                                                                            CommandManager.argument("boss", StringArgumentType.string())
                                                                                    .suggests(new BossSuggestions())
                                                                                    .executes(ctx -> give(ctx, "voucher", StringArgumentType.getString(ctx, "boss"), null))
                                                                    )
                                                                    .then(
                                                                            CommandManager.literal("*")
                                                                                    .executes(ctx -> give(ctx, "voucher", "*", null))
                                                                                    .then(
                                                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                                                    .suggests(new CategorySuggestions())
                                                                                                    .executes(ctx -> give(ctx, "voucher", "*", StringArgumentType.getString(ctx, "category")))
                                                                                    )
                                                                    )
                                                                    .then(
                                                                            CommandManager.literal("random")
                                                                                    .executes(ctx -> give(ctx, "voucher", "random", null))
                                                                                    .then(
                                                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                                                    .suggests(new CategorySuggestions())
                                                                                                    .executes(ctx -> give(ctx, "voucher", "random", StringArgumentType.getString(ctx, "category")))
                                                                                    )
                                                                    )
                                                    )
                                                    .then(
                                                            CommandManager.literal("pokeball")
                                                                    .then(
                                                                            CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                                                    .executes(ctx -> give(ctx, "pokeball", null, null))
                                                                    )

                                                    )
                                    )
                    )
                    .then(
                            CommandManager.literal("list")
                                    .requires(Permissions.require("compoundraids.list", 4))
                                    .executes(this::list)
                    )
                    .then(
                            CommandManager.literal("join")
                                    .requires(Permissions.require("compoundraids.join", 4))
                                    .then(
                                            CommandManager.argument("id", IntegerArgumentType.integer(1))
                                                    .executes(ctx -> {
                                                        if (ctx.getSource().isExecutedByPlayer()) {
                                                            nr.active_raids().get(IntegerArgumentType.getInteger(ctx, "id")).addPlayer(ctx.getSource().getPlayer());
                                                        }
                                                        return 1;
                                                    })
                                    )
                    )
        ));
    }

    private int reload(CommandContext<ServerCommandSource> ctx) {
        nr.reloadConfig();
        if (ctx.getSource().isExecutedByPlayer()) {
            // TODO: Replace with proper message
            ctx.getSource().sendMessage(nr.mm().deserialize("[Raids] Config reloaded!"));
        } else {
            ctx.getSource().sendMessage(Text.literal("[Raids] Config reloaded!"));
        }

        return 1;
    }

    private int start(Boss boss_info) {
        Map<String, Double> spawn_locations = boss_info.spawn_locations();

        Random rand = new Random();
        double total_weight = 0.0;
        for (String location : spawn_locations.keySet()) {
            total_weight += spawn_locations.get(location);
        }

        double random_weight = rand.nextDouble(total_weight);
        total_weight = 0.0;
        Location spawn_location = null;
        for (String location : spawn_locations.keySet()) {
            total_weight += spawn_locations.get(location);
            if (random_weight < total_weight) {
                spawn_location = nr.config().getLocation(location);
                break;
            }
        }

        if (spawn_location == null) {
            nr.logger().error("[RAIDS] Location could not be found.");
            return 0;
        }

        nr.add_raid(new Raid(boss_info, spawn_location));
        return 1;
    }

    private int stop(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        if (nr.active_raids().containsKey(id)) {
            nr.active_raids().get(id).stop();
            nr.remove_raid(nr.active_raids().get(id));
            return 1;
        }
        return 0;
    }

    private int give(CommandContext<ServerCommandSource> ctx, String item_type, String boss_name, String category) {
        if (!ctx.getSource().isExecutedByPlayer()) {
            ctx.getSource().sendMessage(Text.of("Not yet"));
            return 0;
        }

        ServerPlayerEntity source_player = ctx.getSource().getPlayer();
        assert source_player != null;
        try {
            ServerPlayerEntity target_player = EntityArgumentType.getPlayer(ctx, "player");
            Item voucher_item = nr.config().getSettings().voucher_item();
            Item pass_item = nr.config().getSettings().pass_item();
            Item raid_pokeball = nr.config().getSettings().raid_pokeball();

            ItemStack item_to_give;
            NbtCompound custom_data = new NbtCompound();
            ComponentMap.Builder component_builder = ComponentMap.builder();
            Text item_name;
            LoreComponent lore = LoreComponent.DEFAULT;

            Random rand = new Random();

            if (item_type.equalsIgnoreCase("pass")) {
                item_to_give = new ItemStack(pass_item, 1);
                custom_data.putString("raid_item", "raid_pass");
                item_name = Text.literal("Raid Pass").styled(style -> style.withColor(Formatting.LIGHT_PURPLE).withItalic(false));
                if (boss_name.equalsIgnoreCase("*")) {
                    custom_data.putString("raid_boss", "*");
                    if (category == null) {
                        custom_data.putString("raid_category", "*");

                        lore = lore.with(Text.literal("Raid pass for any raid!").styled(style -> style.withItalic(true).withColor(Formatting.GRAY)));
                    } else {
                        custom_data.putString("raid_category", category);

                        lore = lore.with(Text.literal("Raid pass for any " + category + " raid!").styled(style -> style.withItalic(true).withColor(Formatting.GRAY)));
                    }
                } else if (boss_name.equalsIgnoreCase("random")) {
                    if (category == null) {
                        String random_boss = nr.config().getBosses().get(rand.nextInt(nr.config().getBosses().size())).name();
                        custom_data.putString("raid_boss", random_boss);
                        custom_data.putString("raid_category", "null");

                        lore = lore.with(Text.literal("Raid pass for " + random_boss + "!").styled(style -> style.withItalic(true).withColor(Formatting.GRAY)));
                    } else {
                        List<Boss> possible_bosses = new ArrayList<>();
                        for (Boss boss : nr.config().getBosses()) {
                            if (boss.category().equalsIgnoreCase(category)) {
                                possible_bosses.add(boss);
                            }
                        }
                        String random_boss = possible_bosses.get(rand.nextInt(possible_bosses.size())).name();
                        custom_data.putString("raid_boss", random_boss);
                        custom_data.putString("raid_category", "null");

                        lore = lore.with(Text.literal("Raid pass for " + random_boss + "!").styled(style -> style.withItalic(true).withColor(Formatting.GRAY)));
                    }
                } else {
                    custom_data.putString("raid_boss", boss_name);
                    custom_data.putString("raid_category", "null");

                    lore = lore.with(Text.literal("Raid pass for " + boss_name + "!").styled(style -> style.withItalic(true).withColor(Formatting.GRAY)));
                }
            } else if (item_type.equalsIgnoreCase("voucher")) {
                item_to_give = new ItemStack(voucher_item, 1);
                custom_data.putString("raid_item", "raid_voucher");
                item_name = Text.literal("Raid Voucher").styled(style -> style.withItalic(false).withColor(Formatting.AQUA));
                if (boss_name.equalsIgnoreCase("*")) {
                    custom_data.putString("raid_boss", "*");
                    if (category == null) {
                        custom_data.putString("raid_category", "*");
                        lore = lore.with(Text.literal("Use to start any raid!").styled(style -> style.withItalic(true).withColor(Formatting.GRAY)));
                    } else {
                        custom_data.putString("raid_category", category);
                        lore = lore.with(Text.literal("Use to start any " + category + " raid!").styled(style -> style.withItalic(true).withColor(Formatting.GRAY)));
                    }
                } else if (boss_name.equalsIgnoreCase("random")) {
                    custom_data.putString("raid_boss", "random");
                    if (category == null) {
                        custom_data.putString("raid_category", "null");
                        lore = lore.with(Text.literal("Use to start a random raid!").styled(style -> style.withItalic(true).withColor(Formatting.GRAY)));
                    } else {
                        custom_data.putString("raid_category", category);
                        lore = lore.with(Text.literal("Use to start a random " + category + " raid!").styled(style -> style.withItalic(true).withColor(Formatting.GRAY)));
                    }
                } else {
                    custom_data.putString("raid_boss", boss_name);
                    custom_data.putString("raid_category", "null");

                    lore = lore.with(Text.literal("Use to start a " + boss_name + " raid!").styled(style -> style.withItalic(true).withColor(Formatting.GRAY)));
                }
            } else {
                item_to_give = new ItemStack(raid_pokeball, IntegerArgumentType.getInteger(ctx, "amount"));
                custom_data.putString("raid_item", "raid_ball");
                custom_data.putUuid("owner_uuid", target_player.getUuid());
                item_name = Text.literal("Raid Pokeball").styled(style -> style.withItalic(false).withColor(Formatting.RED));
                lore = lore.with(Text.literal("Use this to try and capture raid bosses!").styled(style -> style.withItalic(true).withColor(Formatting.GRAY)));
            }

            item_to_give.applyComponentsFrom(component_builder
                    .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(custom_data))
                    .add(DataComponentTypes.ITEM_NAME, item_name)
                    .add(DataComponentTypes.LORE, lore)
                    .build());
            if (!target_player.giveItemStack(item_to_give)) {
                source_player.sendMessage(Text.of("Failed to give the item!"));
            } else {
                target_player.sendMessage(Text.of("You received a raid " + item_type + "!"));
                source_player.sendMessage(Text.of("Successfully gave the item!"));
            }
        } catch (CommandSyntaxException e) {
            source_player.sendMessage(Text.of("Error: " + e.getMessage()));
        }
        return 1;
    }

    private int list(CommandContext<ServerCommandSource> ctx) {
        return 1;
    }
}