package me.unariginal.novaraids.commands;

import com.cobblemon.mod.common.item.PokemonItem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.Boss;
import me.unariginal.novaraids.data.Category;
import me.unariginal.novaraids.data.Location;
import me.unariginal.novaraids.data.QueueItem;
import me.unariginal.novaraids.managers.Messages;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.utils.TextUtil;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
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
                                    .requires(Permissions.require("novaraids.reload", 4))
                                    .executes(this::reload)
                    )
                    .then(
                            CommandManager.literal("start")
                                    .requires(Permissions.require("novaraids.start", 4))
                                    .then(
                                            CommandManager.argument("boss", StringArgumentType.string())
                                                    .suggests(new BossSuggestions())
                                                    .executes(ctx -> start(nr.config().getBoss(StringArgumentType.getString(ctx, "boss")), ctx.getSource().getPlayer(), null))
                                    )
                                    .then(
                                            CommandManager.literal("random")
                                                    .executes(ctx -> start(nr.config().getBosses().get(new Random().nextInt(nr.config().getBosses().size())), ctx.getSource().getPlayer(), null))
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
                                                                        return start(possible_bosses.get(rand.nextInt(possible_bosses.size())), ctx.getSource().getPlayer(), null);
                                                                    })
                                                    )
                                    )
                    )
                    .then(
                            CommandManager.literal("stop")
                                    .requires(Permissions.require("novaraids.stop", 4))
                                    .then(
                                            CommandManager.argument("id", IntegerArgumentType.integer(1))
                                                    .executes(this::stop)
                                    )
                    )
                    .then(
                            CommandManager.literal("give")
                                    .requires(Permissions.require("novaraids.give", 4))
                                    .then(
                                            CommandManager.argument("player", EntityArgumentType.player())
                                                    .then(
                                                            CommandManager.literal("pass")
                                                                    .then(
                                                                            CommandManager.argument("boss", StringArgumentType.string())
                                                                                    .suggests(new BossSuggestions())
                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pass", StringArgumentType.getString(ctx, "boss"), null, 0))
                                                                    )
                                                                    .then(
                                                                            CommandManager.literal("*")
                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pass", "*", null, 0))
                                                                                    .then(
                                                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                                                    .suggests(new CategorySuggestions())
                                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pass", "*", StringArgumentType.getString(ctx, "category"), 0))
                                                                                    )
                                                                    )
                                                                    .then(
                                                                            CommandManager.literal("random")
                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pass", "random", null, 0))
                                                                                    .then(
                                                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                                                    .suggests(new CategorySuggestions())
                                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pass", "random", StringArgumentType.getString(ctx, "category"), 0))
                                                                                    )
                                                                    )
                                                    )
                                                    .then(
                                                            CommandManager.literal("voucher")
                                                                    .then(
                                                                            CommandManager.argument("boss", StringArgumentType.string())
                                                                                    .suggests(new BossSuggestions())
                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "voucher", StringArgumentType.getString(ctx, "boss"), null, 0))
                                                                    )
                                                                    .then(
                                                                            CommandManager.literal("*")
                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "voucher", "*", null, 0))
                                                                                    .then(
                                                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                                                    .suggests(new CategorySuggestions())
                                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "voucher", "*", StringArgumentType.getString(ctx, "category"), 0))
                                                                                    )
                                                                    )
                                                                    .then(
                                                                            CommandManager.literal("random")
                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "voucher", "random", null, 0))
                                                                                    .then(
                                                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                                                    .suggests(new CategorySuggestions())
                                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "voucher", "random", StringArgumentType.getString(ctx, "category"), 0))
                                                                                    )
                                                                    )
                                                    )
                                                    .then(
                                                            CommandManager.literal("pokeball")
                                                                    .then(
                                                                            CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pokeball", null, null, IntegerArgumentType.getInteger(ctx, "amount")))
                                                                    )

                                                    )
                                    )
                    )
                    .then(
                            CommandManager.literal("list")
                                    .requires(Permissions.require("novaraids.list", 4))
                                    .executes(this::list)
                    )
                    .then(
                            CommandManager.literal("join")
                                    .requires(Permissions.require("novaraids.join", 4))
                                    .then(
                                            CommandManager.argument("id", IntegerArgumentType.integer(1))
                                                    .executes(ctx -> {
                                                        if (ctx.getSource().isExecutedByPlayer()) {
                                                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                            if (player != null) {
                                                                for (Raid raid : nr.active_raids().values()) {
                                                                    if (raid.participating_players().contains(player)) {
                                                                        return 0;
                                                                    }
                                                                }

                                                                if (nr.active_raids().containsKey(IntegerArgumentType.getInteger(ctx, "id"))) {
                                                                    Raid raid = nr.active_raids().get(IntegerArgumentType.getInteger(ctx, "id"));
                                                                    if (raid.participating_players().size() < raid.max_players() || Permissions.check(player, "novaraids.join.override")) {
                                                                        if (raid.addPlayer(player)) {
                                                                            player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("joined_raid"), raid)));
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        return 1;
                                                    })
                                    )
                    )
                    .then(
                            CommandManager.literal("leave")
                                    .requires(Permissions.require("novaraids.leave", 4))
                                    .executes(ctx -> {
                                        if (ctx.getSource().isExecutedByPlayer()) {
                                            for (Raid raid : nr.active_raids().values()) {
                                                if (raid.participating_players().contains(ctx.getSource().getPlayer())) {
                                                    raid.removePlayer(ctx.getSource().getPlayer());
                                                }
                                            }
                                        }
                                        return 1;
                                    })
                    )
                    .then(
                            CommandManager.literal("queue")
                                    .requires(Permissions.require("novaraids.queue", 4))
                                    .executes(this::queue)
                    )
        ));
    }

    private int reload(CommandContext<ServerCommandSource> ctx) {
        nr.reloadConfig();
        if (ctx.getSource().isExecutedByPlayer()) {
            if (ctx.getSource().getPlayer() != null) {
                ctx.getSource().getPlayer().sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("reload_command"))));
            }
        } else {
            ctx.getSource().sendMessage(Text.literal("[Raids] Config reloaded!"));
        }

        return 1;
    }

    public int start(Boss boss_info, ServerPlayerEntity player, ItemStack starting_item) {
        Map<String, Double> spawn_locations = boss_info.spawn_locations();

        Random rand = new Random();
        double total_weight = 0.0;
        for (String location : spawn_locations.keySet()) {
            total_weight += spawn_locations.get(location);
        }

        double random_weight = rand.nextDouble(total_weight + 1);
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
            nr.logError("[RAIDS] Location could not be found.");
            return 0;
        }

        if (!nr.config().getSettings().use_queue_system()) {
            nr.add_raid(new Raid(boss_info, spawn_location, player, starting_item));
        } else {
            nr.add_queue_item(new QueueItem(boss_info, spawn_location, player, starting_item));
            if (nr.active_raids().isEmpty()) {
                nr.init_next_raid();
            }
        }
        return 1;
    }

    private int stop(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        if (nr.active_raids().containsKey(id)) {
            if (ctx.getSource().isExecutedByPlayer()) {
                if (ctx.getSource().getPlayer() != null) {
                    ctx.getSource().getPlayer().sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("raid_stopped"), nr.active_raids().get(id))));
                }
            }
            nr.active_raids().get(id).stop();
            nr.remove_raid(nr.active_raids().get(id));
            return 1;
        }
        return 0;
    }

    public int give(ServerPlayerEntity source_player, ServerPlayerEntity target_player, String item_type, String boss_name, String category, int amount) {
        Item voucher_item = nr.config().getSettings().voucher_item();
        Item pass_item = nr.config().getSettings().pass_item();
        Item raid_pokeball = nr.config().getSettings().raid_pokeball();

        ItemStack item_to_give = null;
        NbtCompound custom_data = new NbtCompound();
        ComponentMap.Builder component_builder = ComponentMap.builder();
        Text item_name = null;
        LoreComponent lore = LoreComponent.DEFAULT;

        Random rand = new Random();

        if (item_type.equalsIgnoreCase("pass")) {
            item_to_give = new ItemStack(pass_item, 1);
            if (nr.config().getSettings().pass_item_data() != null) {
                item_to_give.applyChanges(nr.config().getSettings().pass_item_data());
            }
            item_to_give.applyComponentsFrom(ComponentMap.builder().add(DataComponentTypes.MAX_STACK_SIZE, 1).build());
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
            if (nr.config().getSettings().voucher_item_data() != null) {
                item_to_give.applyChanges(nr.config().getSettings().voucher_item_data());
            }
            item_to_give.applyComponentsFrom(ComponentMap.builder().add(DataComponentTypes.MAX_STACK_SIZE, 1).build());
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
            if (raid_pokeball != null) {
                item_to_give = new ItemStack(raid_pokeball, amount);
                custom_data.putString("raid_item", "raid_ball");
                custom_data.putUuid("owner_uuid", target_player.getUuid());
                item_name = Text.literal("Raid Pokeball").styled(style -> style.withItalic(false).withColor(Formatting.RED));
                lore = lore.with(Text.literal("Use this to try and capture raid bosses!").styled(style -> style.withItalic(true).withColor(Formatting.GRAY)));
                if (nr.config().getSettings().raid_pokeball_data() != null) {
                    item_to_give.applyChanges(nr.config().getSettings().raid_pokeball_data());
                }
            } else {
                if (source_player != null) {
                    source_player.sendMessage(Text.literal("Raid Pokeball not found!"));
                }
            }
        }

        if (item_to_give != null && item_name != null) {
            item_to_give.applyComponentsFrom(component_builder
                    .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(custom_data))
                    .add(DataComponentTypes.ITEM_NAME, item_name)
                    .add(DataComponentTypes.LORE, lore)
                    .build());
            if (!target_player.giveItemStack(item_to_give)) {
                if (source_player != null) {
                    source_player.sendMessage(Text.of("Failed to give the item!"));
                }
            } else {
                target_player.sendMessage(Text.of("You received a raid " + item_type + "!"));
                if (source_player != null) {
                    source_player.sendMessage(Text.of("Successfully gave the item!"));
                }
            }
        } else {
            if (source_player != null) {
                source_player.sendMessage(Text.literal("Failed to give item"));
            }
        }
        return 1;
    }

    private int list(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        Messages messages = nr.config().getMessages();
        if (nr.active_raids().isEmpty()) {
            player.sendMessage(TextUtil.format(messages.parse(messages.message("no_active_raids"))));
            return 0;
        }

        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, false);
        gui.setTitle(Text.literal(messages.parse(messages.message("raid_list_gui_title"))));
        int slot = 0;
        for (Map.Entry<Integer, Raid> entry : nr.active_raids().entrySet()) {
            Raid raid = entry.getValue();
            GuiElement element = new GuiElementBuilder(PokemonItem.from(raid.raidBoss_pokemon()))
                    .setName(Text.literal(messages.parse("[ID: %raid.id%] %boss.form% %boss.species%", raid)).styled(style -> style.withColor(Formatting.LIGHT_PURPLE).withItalic(false)))
                    .setLore(
                            List.of(Text.literal(messages.parse("HP: %boss.currenthp%/%boss.maxhp%", raid)).styled(style -> style.withColor(Formatting.GRAY).withItalic(false)),
                                    Text.literal(messages.parse("Category: %raid.category%", raid)).styled(style -> style.withColor(Formatting.GRAY).withItalic(false)),
                                    Text.literal(messages.parse("Phase: %raid.phase%", raid)).styled(style -> style.withColor(Formatting.GRAY).withItalic(false)),
                                    Text.literal(messages.parse("Players: %raid.player_count%/%raid.max_players%", raid)).styled(style -> style.withColor(Formatting.GRAY).withItalic(false)),
                                    Text.literal(messages.parse("Raid Timer: %raid.timer%", raid)).styled(style -> style.withColor(Formatting.GRAY).withItalic(false))
                            )
            ).setCallback((i, clickType, slotActionType) -> {
                if (clickType.isLeft) {
                    if (player != null) {
                        if (raid.addPlayer(player)) {
                            player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("joined_raid"), raid)));
                        }
                        gui.close();
                    }
                }
            }).build();
            gui.setSlot(slot, element);
            slot++;
            if (slot > 53) {
                break;
            }
        }
        gui.open();
        return 1;
    }

    private int queue(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player != null) {
            Messages messages = nr.config().getMessages();
            if (nr.queued_raids().isEmpty()) {
                player.sendMessage(TextUtil.format(messages.parse(messages.message("no_queued_raids"))));
                return 0;
            }

            List<Text> lore = List.of(Text.empty());
            if (Permissions.check(player, "novaraids.queue.cancel")) {
                lore = List.of(Text.empty(), Text.literal("Right click to cancel this raid!").styled(style -> style.withItalic(false).withColor(Formatting.RED)));
            }

            SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, false);
            gui.setTitle(Text.literal(messages.parse(messages.message("raid_queue_gui_title"))));
            int slot = 0;
            for (QueueItem item : nr.queued_raids()) {
                GuiElement element = new GuiElementBuilder(PokemonItem.from(item.boss_info().createPokemon()))
                        .setName(Text.literal(messages.parse("%boss.form% %boss.species%", item.boss_info())).styled(style -> style.withColor(Formatting.LIGHT_PURPLE).withItalic(false)))
                        .setLore(lore)
                        .setCallback((i, clickType, slotActionType) -> {
                            if (clickType.isRight) {
                                if (Permissions.check(player, "novaraids.queue.cancel")) {
                                    gui.close();
                                    item.cancel_item();
                                    player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("queue_item_cancelled"), item.boss_info())));
                                    nr.queued_raids().remove(item); // TODO: Make sure this doesn't cause a concurrent modification exception!!!!
                                }
                            }
                        })
                        .build();
                gui.setSlot(slot, element);
                slot++;
                if (slot > 53) {
                    break;
                }
            }
            gui.open();
            return 1;
        }
        return 0;
    }
}