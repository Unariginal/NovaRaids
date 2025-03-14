package me.unariginal.novaraids.commands;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.MiscUtilsKt;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.*;
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
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class RaidCommands {
    private final NovaRaids nr = NovaRaids.INSTANCE;

    public RaidCommands() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> commandDispatcher.register(
            CommandManager.literal("raid")
                    .executes(this::modInfo)
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
                                                    .executes(ctx -> {
                                                        if (nr.config().loadedProperly()) {
                                                            return start(nr.config().getBoss(StringArgumentType.getString(ctx, "boss")), ctx.getSource().getPlayer(), null);
                                                        } else {
                                                            return 0;
                                                        }
                                                    })
                                    )
                                    .then(
                                            CommandManager.literal("random")
                                                    .executes(ctx -> start(nr.config().getBosses().get(new Random().nextInt(nr.config().getBosses().size())), ctx.getSource().getPlayer(), null))
                                                    .then(
                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                    .suggests(new CategorySuggestions())
                                                                    .executes(ctx -> {
                                                                        if (nr.config().loadedProperly()) {
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
                                                                        } else {
                                                                            return 0;
                                                                        }
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
                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pass", StringArgumentType.getString(ctx, "boss"), null, "", 0))
                                                                    )
                                                                    .then(
                                                                            CommandManager.literal("*")
                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pass", "*", null, "", 0))
                                                                                    .then(
                                                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                                                    .suggests(new CategorySuggestions())
                                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pass", "*", StringArgumentType.getString(ctx, "category"), "", 0))
                                                                                    )
                                                                    )
                                                                    .then(
                                                                            CommandManager.literal("random")
                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pass", "random", null, "", 0))
                                                                                    .then(
                                                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                                                    .suggests(new CategorySuggestions())
                                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pass", "random", StringArgumentType.getString(ctx, "category"), "", 0))
                                                                                    )
                                                                    )
                                                    )
                                                    .then(
                                                            CommandManager.literal("voucher")
                                                                    .then(
                                                                            CommandManager.argument("boss", StringArgumentType.string())
                                                                                    .suggests(new BossSuggestions())
                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "voucher", StringArgumentType.getString(ctx, "boss"), null, "", 0))
                                                                    )
                                                                    .then(
                                                                            CommandManager.literal("*")
                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "voucher", "*", null, "", 0))
                                                                                    .then(
                                                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                                                    .suggests(new CategorySuggestions())
                                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "voucher", "*", StringArgumentType.getString(ctx, "category"), "", 0))
                                                                                    )
                                                                    )
                                                                    .then(
                                                                            CommandManager.literal("random")
                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "voucher", "random", null, "", 0))
                                                                                    .then(
                                                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                                                    .suggests(new CategorySuggestions())
                                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "voucher", "random", StringArgumentType.getString(ctx, "category"), "", 0))
                                                                                    )
                                                                    )
                                                    )
                                                    .then(
                                                            CommandManager.literal("pokeball")
                                                                    .then(
                                                                            CommandManager.argument("pokeball", StringArgumentType.string())
                                                                                    .suggests((ctx, builder) -> {
                                                                                        if (nr.config().loadedProperly()) {
                                                                                            for (String key : nr.config().getSettings().raid_pokeballs().keySet()) {
                                                                                                builder.suggest(key);
                                                                                            }
                                                                                        }
                                                                                        return builder.buildFuture();
                                                                                    })
                                                                                    .then(
                                                                                            CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pokeball", null, null, StringArgumentType.getString(ctx, "pokeball"), IntegerArgumentType.getInteger(ctx, "amount")))
                                                                                    )
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
                                                        if (nr.config().loadedProperly()) {
                                                            if (ctx.getSource().isExecutedByPlayer()) {
                                                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                                if (player != null) {
                                                                    if (nr.active_raids().containsKey(IntegerArgumentType.getInteger(ctx, "id"))) {
                                                                        Raid raid = nr.active_raids().get(IntegerArgumentType.getInteger(ctx, "id"));
                                                                        if (raid.participating_players().size() < raid.max_players() || Permissions.check(player, "novaraids.override") || raid.max_players() == -1) {
                                                                            if (raid.addPlayer(player.getUuid(), false)) {
                                                                                player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("joined_raid"), raid)));
                                                                            }
                                                                        } else {
                                                                            player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("warning_max_players"), raid)));
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
                                        if (nr.config().loadedProperly()) {
                                            if (ctx.getSource().isExecutedByPlayer()) {
                                                for (Raid raid : nr.active_raids().values()) {
                                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                    if (player != null) {
                                                        if (raid.participating_players().contains(player.getUuid())) {
                                                            raid.removePlayer(player.getUuid());
                                                        }
                                                    }
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
                    .then(
                            CommandManager.literal("checkbanned")
                                    .requires(Permissions.require("novaraids.checkbanned", 4))
                                    .executes(this::checkbanned)
                    )
                    .then(
                            CommandManager.literal("history")
                                    .requires(Permissions.require("novaraids.history", 4))
                                    .executes(ctx -> {
                                        ctx.getSource().sendMessage(Text.literal("Not Implemented"));
                                        return 1;
                                    })
                    )
                    .then(
                            CommandManager.literal("skipphase")
                                    .requires(Permissions.require("novaraids.skipphase", 4))
                                    .then(
                                            CommandManager.argument("id", IntegerArgumentType.integer(1))
                                                    .executes(this::skipphase)
                                    )
                    )
        ));
    }

    private int reload(CommandContext<ServerCommandSource> ctx) {
        nr.reloadConfig();
        if (nr.config().loadedProperly()) {
            if (ctx.getSource().isExecutedByPlayer()) {
                if (ctx.getSource().getPlayer() != null) {
                    ctx.getSource().getPlayer().sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("reload_command"))));
                }
            } else {
                ctx.getSource().sendMessage(Text.literal("[Raids] Config reloaded!"));
            }
        }

        return 1;
    }

    private int modInfo(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player != null) {
            player.sendMessage(Text.literal("--------- Nova Raids ---------"));
            player.sendMessage(Text.literal("Author: Unariginal ").append(Text.literal("(Ariginal)").styled(style -> style.withItalic(true))));
            player.sendMessage(Text.literal("Version: Beta v0.1.1"));
            player.sendMessage(Text.literal("Source").styled(style -> style.withUnderline(true).withItalic(true).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/Unariginal/NovaRaids"))));
            player.sendMessage(Text.literal("Wiki").styled(style -> style.withItalic(true).withUnderline(true).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/Unariginal/NovaRaids/wiki"))));
            player.sendMessage(Text.literal("----------------------------"));
        }
        return 1;
    }

    private int checkbanned(CommandContext<ServerCommandSource> ctx) {
        if (nr.config().loadedProperly()) {
            if (ctx.getSource().isExecutedByPlayer()) {
                ServerPlayerEntity player = ctx.getSource().getPlayer();
                if (player != null) {
                    try {
                        SimpleGui main_gui = new SimpleGui(ScreenHandlerType.HOPPER, player, false);
                        main_gui.setTitle(Text.literal(nr.config().getMessages().message("contraband_gui_title")));

                        SimpleGui pokemon_gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, false);
                        pokemon_gui.setTitle(Text.literal("Banned Pokemon"));

                        int slot = 0;
                        for (Species species : nr.config().getSettings().banned_pokemon()) {
                            GuiElement banned_pokemon = new GuiElementBuilder(PokemonItem.from(species))
                                    .setName(Text.literal(species.getName()))
                                    .build();
                            pokemon_gui.setSlot(slot, banned_pokemon);
                            slot++;
                        }

                        SimpleGui move_gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, false);
                        move_gui.setTitle(Text.literal("Banned Moves"));

                        slot = 0;
                        for (Move move : nr.config().getSettings().banned_moves()) {
                            GuiElement banned_move = new GuiElementBuilder(Items.PAPER)
                                    .setName(move.getDisplayName())
                                    .build();
                            move_gui.setSlot(slot, banned_move);
                            slot++;
                        }

                        SimpleGui ability_gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, false);
                        ability_gui.setTitle(Text.literal("Banned Abilities"));

                        slot = 0;
                        for (Ability ability : nr.config().getSettings().banned_abilities()) {
                            GuiElement banned_ability = new GuiElementBuilder(Items.NETHER_STAR)
                                    .setName(MiscUtilsKt.asTranslated(ability.getDisplayName()))
                                    .build();
                            ability_gui.setSlot(slot, banned_ability);
                            slot++;
                        }

                        SimpleGui held_item_gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, false);
                        held_item_gui.setTitle(Text.literal("Banned Held Items"));

                        slot = 0;
                        for (Item item : nr.config().getSettings().banned_held_items()) {
                            GuiElement banned_held_item = new GuiElementBuilder(item)
                                    .build();
                            held_item_gui.setSlot(slot, banned_held_item);
                            slot++;
                        }

                        SimpleGui bag_item_gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, false);
                        bag_item_gui.setTitle(Text.literal("Banned Bag Items"));

                        slot = 0;
                        for (Item item : nr.config().getSettings().banned_bag_items()) {
                            GuiElement banned_bag_item = new GuiElementBuilder(item)
                                    .build();
                            bag_item_gui.setSlot(slot, banned_bag_item);
                            slot++;
                        }

                        GuiElement banned_pokemon_main = new GuiElementBuilder(CobblemonItems.POKE_BALL)
                                .setName(Text.literal("Banned Pokemon").styled(style -> style.withColor(Formatting.RED).withItalic(false)))
                                .setCallback(((i, clickType, slotActionType) -> {
                                    main_gui.close();
                                    pokemon_gui.open();
                                })).build();

                        main_gui.setSlot(0, banned_pokemon_main);

                        GuiElement banned_moves_main = new GuiElementBuilder(CobblemonItems.RAZOR_CLAW)
                                .setName(Text.literal("Banned Moves").styled(style -> style.withColor(Formatting.RED).withItalic(false)))
                                .setCallback(((i, clickType, slotActionType) -> {
                                    main_gui.close();
                                    move_gui.open();
                                }))
                                .build();
                        main_gui.setSlot(1, banned_moves_main);

                        GuiElement banned_abilities_main = new GuiElementBuilder(CobblemonItems.ABILITY_PATCH)
                                .setName(Text.literal("Banned Abilities").styled(style -> style.withColor(Formatting.RED).withItalic(false)))
                                .setCallback(((i, clickType, slotActionType) -> {
                                    main_gui.close();
                                    ability_gui.open();
                                }))
                                .build();
                        main_gui.setSlot(2, banned_abilities_main);

                        GuiElement banned_held_items_main = new GuiElementBuilder(CobblemonItems.LEFTOVERS)
                                .setName(Text.literal("Banned Held Items").styled(style -> style.withColor(Formatting.RED).withItalic(false)))
                                .setCallback(((i, clickType, slotActionType) -> {
                                    main_gui.close();
                                    held_item_gui.open();
                                }))
                                .build();
                        main_gui.setSlot(3, banned_held_items_main);

                        GuiElement banned_bag_items_main = new GuiElementBuilder(CobblemonItems.POTION)
                                .setName(Text.literal("Banned Bag Items").styled(style -> style.withColor(Formatting.RED).withItalic(false)))
                                .setCallback(((i, clickType, slotActionType) -> {
                                    main_gui.close();
                                    bag_item_gui.open();
                                }))
                                .build();
                        main_gui.setSlot(4, banned_bag_items_main);
                        main_gui.open();
                    } catch (Exception e) {
                        nr.logError(e.getMessage());
                    }
                }
            }
        }
        return 1;
    }

    private int skipphase(CommandContext<ServerCommandSource> ctx) {
        if (nr.config().loadedProperly()) {
            int id = IntegerArgumentType.getInteger(ctx, "id");
            if (nr.active_raids().containsKey(id)) {
                Raid raid = nr.active_raids().get(id);
                List<Task> tasks = raid.getTasks().entrySet().stream().findFirst().orElseThrow().getValue();
                raid.removeTask(raid.getTasks().entrySet().stream().findFirst().orElseThrow().getKey());
                for (Task task : tasks) {
                    raid.addTask(task.world(), 1L, task.action());
                }
            }
        }
        return 1;
    }

    public int start(Boss boss_info, ServerPlayerEntity player, ItemStack starting_item) {
        if (nr.config().loadedProperly()) {
            if (!nr.server().getPlayerManager().getPlayerList().isEmpty()) {
                if (boss_info != null) {
                    Map<String, Double> spawn_locations = boss_info.spawn_locations();
                    Map<String, Double> valid_locations = new HashMap<>();

                    for (String key : spawn_locations.keySet()) {
                        boolean valid_spawn = true;
                        for (Raid raid : nr.active_raids().values()) {
                            if (raid.raidBoss_location().name().equalsIgnoreCase(key)) {
                                valid_spawn = false;
                                break;
                            }
                        }
                        if (valid_spawn) {
                            valid_locations.put(key, spawn_locations.get(key));
                        }
                    }

                    Location spawn_location = null;

                    if (!valid_locations.isEmpty()) {
                        Random rand = new Random();
                        double total_weight = 0.0;
                        for (String location : valid_locations.keySet()) {
                            total_weight += valid_locations.get(location);
                        }

                        double random_weight = rand.nextDouble(total_weight);
                        total_weight = 0.0;

                        for (String location : valid_locations.keySet()) {
                            total_weight += valid_locations.get(location);
                            if (random_weight < total_weight) {
                                spawn_location = nr.config().getLocation(location);
                                break;
                            }
                        }

                        if (spawn_location == null) {
                            nr.logError("[RAIDS] Location could not be found.");
                            return 0;
                        }
                    } else {
                        nr.logInfo("[RAIDS] No valid spawn locations found. All possible locations are busy.");
                        if (player != null) {
                            player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("no_available_locations"), boss_info)));
                        }
                        return 0;
                    }

                    if (!nr.config().getSettings().use_queue_system()) {
                        nr.add_raid(new Raid(boss_info, spawn_location, (player != null) ? player.getUuid() : null, starting_item));
                    } else {
                        nr.add_queue_item(new QueueItem(UUID.randomUUID(), boss_info, spawn_location, (player != null) ? player.getUuid() : null, starting_item));

                        if (nr.active_raids().isEmpty()) {
                            nr.init_next_raid();
                        } else {
                            if (player != null) {
                                player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("added_to_queue"), boss_info)));
                            }
                        }
                    }
                    return 1;
                }
                nr.logError("[RAIDS] Boss was null!");
                return 0;
            }
        }
        return 0;
    }

    private int stop(CommandContext<ServerCommandSource> ctx) {
        if (nr.config().loadedProperly()) {
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
        }
        return 0;
    }

    public int give(ServerPlayerEntity source_player, ServerPlayerEntity target_player, String item_type, String boss_name, String category, String key, int amount) {
        if (nr.config().loadedProperly()) {
            Item voucher_item = nr.config().getSettings().voucher_item();
            Item pass_item = nr.config().getSettings().pass_item();

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
                Item raid_pokeball = nr.config().getSettings().raid_pokeballs().get(key);
                if (raid_pokeball != null) {
                    item_to_give = new ItemStack(raid_pokeball, amount);
                    custom_data.putString("raid_item", "raid_ball");
                    custom_data.putUuid("owner_uuid", target_player.getUuid());
                    item_name = Text.literal("Raid Pokeball").styled(style -> style.withItalic(false).withColor(Formatting.RED));
                    lore = lore.with(Text.literal("Use this to try and capture raid bosses!").styled(style -> style.withItalic(true).withColor(Formatting.GRAY)));
                    if (nr.config().getSettings().raid_pokeball_data() != null) {
                        item_to_give.applyChanges(nr.config().getSettings().raid_pokeball_data().get(key));
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
        }
        return 1;
    }

    private int list(CommandContext<ServerCommandSource> ctx) {
        if (nr.config().loadedProperly()) {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            Messages messages = nr.config().getMessages();
            if (player != null) {
                if (nr.active_raids().isEmpty()) {
                    player.sendMessage(TextUtil.format(messages.parse(messages.message("no_active_raids"))));
                    return 0;
                }

                SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, false);
                gui.setTitle(Text.literal(messages.parse(messages.message("raid_list_gui_title"))));
                int slot = 0;
                for (Map.Entry<Integer, Raid> entry : nr.active_raids().entrySet()) {
                    Raid raid = entry.getValue();
                    List<Text> lore = new ArrayList<>();
                    lore.add(Text.literal(messages.parse("HP: %boss.currenthp%/%boss.maxhp%", raid)).styled(style -> style.withColor(Formatting.GRAY).withItalic(false)));
                    lore.add(Text.literal(messages.parse("Category: %raid.category%", raid)).styled(style -> style.withColor(Formatting.GRAY).withItalic(false)));
                    lore.add(Text.literal(messages.parse("Phase: %raid.phase%", raid)).styled(style -> style.withColor(Formatting.GRAY).withItalic(false)));
                    lore.add(Text.literal(messages.parse("Players: %raid.player_count%/%raid.max_players%", raid)).styled(style -> style.withColor(Formatting.GRAY).withItalic(false)));
                    lore.add(Text.literal(messages.parse("Raid Timer: %raid.timer%", raid)).styled(style -> style.withColor(Formatting.GRAY).withItalic(false)));

                    if (!raid.raidBoss_category().require_pass() && raid.stage() == 1) {
                        lore.add(Text.empty());
                        lore.add(Text.literal("Click to join this raid!").styled(style -> style.withColor(Formatting.GREEN).withItalic(false)));
                    } else {
                        lore.add(Text.empty());
                        lore.add(Text.literal("This raid requires a pass!").styled(style -> style.withColor(Formatting.RED).withItalic(false)));
                    }

                    GuiElement element = new GuiElementBuilder(PokemonItem.from(raid.raidBoss_pokemon()))
                            .setName(Text.literal(messages.parse("[ID: %raid.id%] %boss.form% %boss.species%", raid)).styled(style -> style.withColor(Formatting.LIGHT_PURPLE).withItalic(false)))
                            .setLore(lore)
                            .setCallback((i, clickType, slotActionType) -> {
                                if (clickType.isLeft) {
                                    if (raid.participating_players().size() < raid.max_players() || Permissions.check(player, "novaraids.override") || raid.max_players() == -1) {
                                        if (raid.addPlayer(player.getUuid(), false)) {
                                            player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("joined_raid"), raid)));
                                        }
                                    } else {
                                        player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("warning_max_players"), raid)));
                                    }
                                    gui.close();
                                }
                            }).build();
                    gui.setSlot(slot, element);
                    slot++;
                    if (slot > 53) {
                        break;
                    }
                }
                gui.open();
            }
        }
        return 1;
    }

    private int queue(CommandContext<ServerCommandSource> ctx) {
        if (nr.config().loadedProperly()) {
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
                nr.logInfo("[RAIDS] Total queued raids: " + nr.queued_raids().size());
                for (QueueItem item : nr.queued_raids().stream().toList()) {
                    GuiElement element = new GuiElementBuilder(PokemonItem.from(item.boss_info().createPokemon()))
                            .setName(Text.literal(messages.parse("%boss.form% %boss.species%", item.boss_info())).styled(style -> style.withColor(Formatting.LIGHT_PURPLE).withItalic(false)))
                            .setLore(lore)
                            .setCallback((i, clickType, slotActionType) -> {
                                if (clickType.isRight) {
                                    if (Permissions.check(player, "novaraids.queue.cancel")) {
                                        gui.close();
                                        item.cancel_item();
                                        player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("queue_item_cancelled"), item.boss_info())));
                                        nr.queued_raids().remove(item);
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
        }
        return 0;
    }
}