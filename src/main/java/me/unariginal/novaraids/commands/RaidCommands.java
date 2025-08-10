package me.unariginal.novaraids.commands;

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
import me.unariginal.novaraids.commands.suggestions.BossSuggestions;
import me.unariginal.novaraids.commands.suggestions.CategorySuggestions;
import me.unariginal.novaraids.data.*;
import me.unariginal.novaraids.data.bosssettings.Boss;
import me.unariginal.novaraids.data.guis.ContrabandGui;
import me.unariginal.novaraids.data.guis.DisplayItemGui;
import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;
import me.unariginal.novaraids.data.rewards.DistributionSection;
import me.unariginal.novaraids.data.rewards.Place;
import me.unariginal.novaraids.data.rewards.RewardPool;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.utils.GuiUtils;
import me.unariginal.novaraids.utils.RandomUtils;
import me.unariginal.novaraids.utils.TextUtils;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

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
                                                        if (nr.loadedProperly) {
                                                            return start(nr.bossesConfig().getBoss(StringArgumentType.getString(ctx, "boss")), ctx.getSource().getPlayer(), null);
                                                        } else {
                                                            return 0;
                                                        }
                                                    })
                                    )
                                    .then(
                                            CommandManager.literal("random")
                                                    .executes(ctx -> {
                                                        if (nr.loadedProperly) {
                                                            return start(nr.bossesConfig().getRandomBoss(), ctx.getSource().getPlayer(), null);
                                                        } else {
                                                            return 0;
                                                        }
                                                    })
                                                    .then(
                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                    .suggests(new CategorySuggestions())
                                                                    .executes(ctx -> {
                                                                        if (nr.loadedProperly) {
                                                                            String categoryStr = StringArgumentType.getString(ctx, "category");
                                                                            Boss boss = nr.bossesConfig().getRandomBoss(categoryStr);

                                                                            return start(boss, ctx.getSource().getPlayer(), null);
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
                                                                            CommandManager.literal("boss")
                                                                                    .then(
                                                                                            CommandManager.argument("boss", StringArgumentType.string())
                                                                                                    .suggests(new BossSuggestions())
                                                                                                    .then(
                                                                                                            CommandManager.argument("pokeball", StringArgumentType.string())
                                                                                                                    .suggests((ctx, builder) -> {
                                                                                                                        if (nr.loadedProperly) {
                                                                                                                            Boss boss = nr.bossesConfig().getBoss(StringArgumentType.getString(ctx, "boss"));
                                                                                                                            if (boss != null) {
                                                                                                                                for (RaidBall ball : boss.itemSettings().raidBalls()) {
                                                                                                                                    builder.suggest(ball.id());
                                                                                                                                }
                                                                                                                            }
                                                                                                                        }
                                                                                                                        return builder.buildFuture();
                                                                                                                    })
                                                                                                                    .then(
                                                                                                                            CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pokeball", StringArgumentType.getString(ctx, "boss"), null, StringArgumentType.getString(ctx, "pokeball"), IntegerArgumentType.getInteger(ctx, "amount")))
                                                                                                                    )
                                                                                                    )
                                                                                    )
                                                                    )
                                                                    .then(
                                                                            CommandManager.literal("category")
                                                                                    .then(
                                                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                                                    .suggests(new CategorySuggestions())
                                                                                                    .then(
                                                                                                            CommandManager.argument("pokeball", StringArgumentType.string())
                                                                                                                    .suggests((ctx, builder) -> {
                                                                                                                        if (nr.loadedProperly) {
                                                                                                                            Category cat = nr.bossesConfig().getCategory(StringArgumentType.getString(ctx, "category"));
                                                                                                                            if (cat != null) {
                                                                                                                                for (RaidBall ball : cat.categoryBalls()) {
                                                                                                                                    builder.suggest(ball.id());
                                                                                                                                }
                                                                                                                            }
                                                                                                                        }
                                                                                                                        return builder.buildFuture();
                                                                                                                    })
                                                                                                                    .then(
                                                                                                                            CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pokeball", null, StringArgumentType.getString(ctx, "category"), StringArgumentType.getString(ctx, "pokeball"), IntegerArgumentType.getInteger(ctx, "amount")))
                                                                                                                    )
                                                                                                    )
                                                                                    )
                                                                    )
                                                                    .then(
                                                                            CommandManager.literal("global")
                                                                                    .then(
                                                                                            CommandManager.argument("pokeball", StringArgumentType.string())
                                                                                                    .suggests((ctx, builder) -> {
                                                                                                        if (nr.loadedProperly) {
                                                                                                            for (RaidBall ball : nr.config().raid_balls) {
                                                                                                                builder.suggest(ball.id());
                                                                                                            }
                                                                                                        }
                                                                                                        return builder.buildFuture();
                                                                                                    })
                                                                                                    .then(
                                                                                                            CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                                                                                    .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pokeball", "*", null, StringArgumentType.getString(ctx, "pokeball"), IntegerArgumentType.getInteger(ctx, "amount")))
                                                                                                    )
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
                                                        if (nr.loadedProperly) {
                                                            if (ctx.getSource().isExecutedByPlayer()) {
                                                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                                if (player != null) {
                                                                    if (nr.activeRaids().containsKey(IntegerArgumentType.getInteger(ctx, "id"))) {
                                                                        Raid raid = nr.activeRaids().get(IntegerArgumentType.getInteger(ctx, "id"));
                                                                        if (raid.participatingPlayers().size() < raid.maxPlayers() || Permissions.check(player, "novaraids.override") || raid.maxPlayers() == -1) {
                                                                            if (raid.addPlayer(player.getUuid(), false)) {
                                                                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("joined_raid"), raid)));
                                                                            }
                                                                        } else {
                                                                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("warning_max_players"), raid)));
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
                                        if (nr.loadedProperly) {
                                            if (ctx.getSource().isExecutedByPlayer()) {
                                                for (Raid raid : nr.activeRaids().values()) {
                                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                    if (player != null) {
                                                        if (raid.participatingPlayers().contains(player.getUuid())) {
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
                                    .executes(ctx -> queue(ctx, 1))
                    )
                    .then(
                            CommandManager.literal("checkbanned")
                                    .requires(Permissions.require("novaraids.checkbanned", 4))
                                    .then(
                                            CommandManager.literal("global")
                                                    .executes(ctx -> checkbanned(ctx, "global"))
                                    )
                                    .then(
                                            CommandManager.literal("category")
                                                    .then(
                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                    .suggests(new CategorySuggestions())
                                                                    .executes(ctx -> checkbanned(ctx, "category"))
                                                    )
                                    )
                                    .then(
                                            CommandManager.literal("boss")
                                                    .then(
                                                            CommandManager.argument("boss", StringArgumentType.string())
                                                                    .suggests(new BossSuggestions())
                                                                    .executes(ctx -> checkbanned(ctx, "boss"))
                                                    )
                                    )
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
                    .then(
                            CommandManager.literal("testrewards")
                                    .requires(Permissions.require("novaraids.testrewards", 4))
                                    .then(
                                            CommandManager.argument("boss", StringArgumentType.string())
                                                    .suggests(new BossSuggestions())
                                                    .then(
                                                            CommandManager.argument("placement", IntegerArgumentType.integer(1))
                                                                    .then(
                                                                            CommandManager.argument("total-players", IntegerArgumentType.integer(1))
                                                                                    .executes(this::testRewards)
                                                                    )
                                                    )
                                    )
                    )
                    .then(
                            CommandManager.literal("world")
                                    .requires(Permissions.require("novaraids.world", 4))
                                    .executes(ctx -> {
                                        if (ctx.getSource().isExecutedByPlayer()) {
                                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                                            if (player != null) {
                                                player.sendMessage(TextUtils.deserialize(player.getServerWorld().getRegistryKey().getValue().toString()));
                                            }
                                        }
                                        return 1;
                                    })
                    )
                    .then(
                            CommandManager.literal("damage")
                                    .requires(Permissions.require("novaraids.damage", 4))
                                    .then(
                                            CommandManager.argument("id", IntegerArgumentType.integer(1))
                                                    .then(
                                                            CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                                    .executes(ctx -> {
                                                                        if (nr.loadedProperly) {
                                                                            int id = IntegerArgumentType.getInteger(ctx, "id");
                                                                            if (nr.activeRaids().containsKey(id)) {
                                                                                Raid raid = nr.activeRaids().get(id);
                                                                                if (raid != null) {
                                                                                    if (ctx.getSource().isExecutedByPlayer()) {
                                                                                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                                                        if (player != null) {
                                                                                            int damage = IntegerArgumentType.getInteger(ctx, "amount");
                                                                                            if (damage > raid.currentHealth()) {
                                                                                                damage = raid.currentHealth();
                                                                                            }

                                                                                            raid.applyDamage(damage);
                                                                                            raid.updatePlayerDamage(player.getUuid(), damage);
                                                                                            raid.participatingBroadcast(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("player_damage_report"), raid, player, damage, -1)));
                                                                                            player.sendMessage(TextUtils.deserialize("<green>The damage has been applied."));
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                        return 1;
                                                                    })
                                                    )
                                    )
                    )
        ));
    }

    private int reload(CommandContext<ServerCommandSource> ctx) {
        nr.reloadConfig();
        if (nr.loadedProperly) {
            if (ctx.getSource().isExecutedByPlayer()) {
                if (ctx.getSource().getPlayer() != null) {
                    ctx.getSource().getPlayer().sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("reload_command"))));
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
            player.sendMessage(TextUtils.deserialize("<gray><st><b><i>--------- <reset><red>Nova Raids <reset><gray><st><b><i>---------"));
            player.sendMessage(TextUtils.deserialize("<gray><b>Author: <reset><white>Unariginal <i>(Ariginal)"));
            player.sendMessage(TextUtils.deserialize("<gray><b>Version: <reset><white>Beta v0.3.0"));
            player.sendMessage(TextUtils.deserialize("<gray><b><i><u><click:open_url:https://github.com/Unariginal/NovaRaids>Source"));
            player.sendMessage(TextUtils.deserialize("<gray><b><i><u><click:open_url:https://github.com/Unariginal/NovaRaids/wiki>Wiki"));
            player.sendMessage(TextUtils.deserialize("<gray><st><b><i>----------------------------"));
        }
        return 1;
    }

    private int checkbanned(CommandContext<ServerCommandSource> ctx, String type) {
        if (nr.loadedProperly) {
            if (ctx.getSource().isExecutedByPlayer()) {
                ServerPlayerEntity player = ctx.getSource().getPlayer();
                if (player != null) {
                    ContrabandGui gui;
                    Boss boss = null;
                    Category category = null;
                    switch (type) {
                        case "global" -> gui = nr.guisConfig().global_contraband_gui;
                        case "category" -> {
                            String category_id = StringArgumentType.getString(ctx, "category");
                            category = nr.bossesConfig().getCategory(category_id);
                            if (category != null) {
                                gui = nr.guisConfig().category_contraband_gui;
                            } else {
                                return 0;
                            }
                        }
                        case "boss" -> {
                            String boss_id = StringArgumentType.getString(ctx, "boss");
                            boss = nr.bossesConfig().getBoss(boss_id);
                            if (boss != null) {
                                category = nr.bossesConfig().getCategory(boss.categoryId());
                                gui = nr.guisConfig().boss_contraband_gui;
                            } else {
                                return 0;
                            }
                        }
                        default -> {
                            return 0;
                        }
                    }
                    String title = gui.title;
                    String pokemon_item_name = gui.bannedPokemonButton.itemName();
                    List<String> pokemon_item_lore = gui.bannedPokemonButton.itemLore();
                    String move_item_name = gui.bannedMovesButton.itemName();
                    List<String> move_item_lore = gui.bannedMovesButton.itemLore();
                    String ability_item_name = gui.bannedAbilitiesButton.itemName();
                    List<String> ability_item_lore = gui.bannedAbilitiesButton.itemLore();
                    String held_item_name = gui.bannedHeldItemsButton.itemName();
                    List<String> held_item_lore = gui.bannedHeldItemsButton.itemLore();
                    String bag_item_name = gui.bannedBagItemsButton.itemName();
                    List<String> bag_item_lore = gui.bannedBagItemsButton.itemLore();
                    String background_item_name = gui.backgroundButton.itemName();
                    List<String> background_item_lore = gui.backgroundButton.itemLore();
                    String close_item_name = gui.closeButton.itemName();
                    List<String> close_item_lore = gui.closeButton.itemLore();

                    if (category != null) {
                        title = gui.title.replaceAll("%category%", category.name());
                        pokemon_item_name = gui.bannedPokemonButton.itemName().replaceAll("%category%", category.name());
                        List<String> lore = new ArrayList<>();
                        for (String line : pokemon_item_lore) {
                            lore.add(line.replaceAll("%category%", category.name()));
                        }
                        pokemon_item_lore = lore;

                        move_item_name = gui.bannedMovesButton.itemName().replaceAll("%category%", category.name());
                        lore = new ArrayList<>();
                        for (String line : move_item_lore) {
                            lore.add(line.replaceAll("%category%", category.name()));
                        }
                        move_item_lore = lore;

                        ability_item_name = gui.bannedAbilitiesButton.itemName().replaceAll("%category%", category.name());
                        lore = new ArrayList<>();
                        for (String line : ability_item_lore) {
                            lore.add(line.replaceAll("%category%", category.name()));
                        }
                        ability_item_lore = lore;

                        held_item_name = gui.bannedHeldItemsButton.itemName().replaceAll("%category%", category.name());
                        lore = new ArrayList<>();
                        for (String line : held_item_lore) {
                            lore.add(line.replaceAll("%category%", category.name()));
                        }
                        held_item_lore = lore;

                        bag_item_name = gui.bannedBagItemsButton.itemName().replaceAll("%category%", category.name());
                        lore = new ArrayList<>();
                        for (String line : bag_item_lore) {
                            lore.add(line.replaceAll("%category%", category.name()));
                        }
                        bag_item_lore = lore;

                        background_item_name = gui.backgroundButton.itemName().replaceAll("%category%", category.name());
                        lore = new ArrayList<>();
                        for (String line : background_item_lore) {
                            lore.add(line.replaceAll("%category%", category.name()));
                        }
                        background_item_lore = lore;

                        close_item_name = gui.closeButton.itemName().replaceAll("%category%", category.name());
                        lore = new ArrayList<>();
                        for (String line : close_item_lore) {
                            lore.add(line.replaceAll("%category%", category.name()));
                        }
                        close_item_lore = lore;
                    }
                    if (boss != null) {
                        title = TextUtils.parse(title, boss);
                        pokemon_item_name = TextUtils.parse(pokemon_item_name, boss);
                        List<String> lore = new ArrayList<>();
                        for (String line : pokemon_item_lore) {
                            lore.add(TextUtils.parse(line, boss));
                        }
                        pokemon_item_lore = lore;

                        move_item_name = TextUtils.parse(move_item_name, boss);
                        lore = new ArrayList<>();
                        for (String line : move_item_lore) {
                            lore.add(TextUtils.parse(line, boss));
                        }
                        move_item_lore = lore;

                        ability_item_name = TextUtils.parse(ability_item_name, boss);
                        lore = new ArrayList<>();
                        for (String line : ability_item_lore) {
                            lore.add(TextUtils.parse(line, boss));
                        }
                        ability_item_lore = lore;

                        held_item_name = TextUtils.parse(held_item_name, boss);
                        lore = new ArrayList<>();
                        for (String line : held_item_lore) {
                            lore.add(TextUtils.parse(line, boss));
                        }
                        held_item_lore = lore;

                        bag_item_name = TextUtils.parse(bag_item_name, boss);
                        lore = new ArrayList<>();
                        for (String line : bag_item_lore) {
                            lore.add(TextUtils.parse(line, boss));
                        }
                        bag_item_lore = lore;

                        background_item_name = TextUtils.parse(background_item_name, boss);
                        lore = new ArrayList<>();
                        for (String line : background_item_lore) {
                            lore.add(TextUtils.parse(line, boss));
                        }
                        background_item_lore = lore;

                        close_item_name = TextUtils.parse(close_item_name, boss);
                        lore = new ArrayList<>();
                        for (String line : close_item_lore) {
                            lore.add(TextUtils.parse(line, boss));
                        }
                        close_item_lore = lore;
                    }

                    SimpleGui main_gui;
                    if (gui.useHopperGui) {
                        main_gui = new SimpleGui(ScreenHandlerType.HOPPER, player, false);
                    } else {
                        main_gui = new SimpleGui(GuiUtils.getScreenSize(gui.rows), player, false);
                    }
                    main_gui.setTitle(TextUtils.deserialize(title));

                    List<Text> lore = new ArrayList<>();
                    for (String line : pokemon_item_lore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    for (Integer slot : gui.pokemonSlots()) {
                        Item item = Registries.ITEM.get(Identifier.of(gui.bannedPokemonButton.item()));
                        Boss finalBoss = boss;
                        Category finalCategory = category;
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(pokemon_item_name)))
                                .setLore(lore)
                                .setCallback(clickType -> {
                                    main_gui.close();
                                    openContrabandGui(ctx, player, gui.bannedPokemon, "pokemon", 1, finalBoss, finalCategory);
                                })
                                .build();
                        main_gui.setSlot(slot, element);
                    }

                    lore = new ArrayList<>();
                    for (String line : move_item_lore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    for (Integer slot : gui.moveSlots()) {
                        Item item = Registries.ITEM.get(Identifier.of(gui.bannedMovesButton.item()));
                        Boss finalBoss1 = boss;
                        Category finalCategory1 = category;
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(move_item_name)))
                                .setLore(lore)
                                .setCallback(clickType -> {
                                    main_gui.close();
                                    openContrabandGui(ctx, player, gui.bannedMoves, "move", 1, finalBoss1, finalCategory1);
                                })
                                .build();
                        main_gui.setSlot(slot, element);
                    }

                    lore = new ArrayList<>();
                    for (String line : ability_item_lore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    for (Integer slot : gui.abilitySlots()) {
                        Item item = Registries.ITEM.get(Identifier.of(gui.bannedAbilitiesButton.item()));
                        Boss finalBoss2 = boss;
                        Category finalCategory2 = category;
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(ability_item_name)))
                                .setLore(lore)
                                .setCallback(clickType -> {
                                    main_gui.close();
                                    openContrabandGui(ctx, player, gui.bannedAbilities, "ability", 1, finalBoss2, finalCategory2);
                                })
                                .build();
                        main_gui.setSlot(slot, element);
                    }

                    lore = new ArrayList<>();
                    for (String line : held_item_lore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    for (Integer slot : gui.heldItemSlots()) {
                        Item item = Registries.ITEM.get(Identifier.of(gui.bannedHeldItemsButton.item()));
                        Boss finalBoss3 = boss;
                        Category finalCategory3 = category;
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(held_item_name)))
                                .setLore(lore)
                                .setCallback(clickType -> {
                                    main_gui.close();
                                    openContrabandGui(ctx, player, gui.bannedHeldItems, "held_item", 1, finalBoss3, finalCategory3);
                                })
                                .build();
                        main_gui.setSlot(slot, element);
                    }

                    lore = new ArrayList<>();
                    for (String line : bag_item_lore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    for (Integer slot : gui.bagItemSlots()) {
                        Item item = Registries.ITEM.get(Identifier.of(gui.bannedBagItemsButton.item()));
                        Boss finalBoss4 = boss;
                        Category finalCategory4 = category;
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(bag_item_name)))
                                .setLore(lore)
                                .setCallback(clickType -> {
                                    main_gui.close();
                                    openContrabandGui(ctx, player, gui.bannedBagItems, "bag_item", 1, finalBoss4, finalCategory4);
                                })
                                .build();
                        main_gui.setSlot(slot, element);
                    }

                    lore = new ArrayList<>();
                    for (String line : background_item_lore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    for (Integer slot : gui.backgroundButtonSlots()) {
                        Item item = Registries.ITEM.get(Identifier.of(gui.backgroundButton.item()));
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(background_item_name)))
                                .setLore(lore)
                                .setCallback(clickType -> main_gui.close())
                                .build();
                        main_gui.setSlot(slot, element);
                    }

                    lore = new ArrayList<>();
                    for (String line : close_item_lore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    for (Integer slot : gui.closeButtonSlots()) {
                        Item item = Registries.ITEM.get(Identifier.of(gui.closeButton.item()));
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(close_item_name)))
                                .setLore(lore)
                                .setCallback(clickType -> main_gui.close())
                                .build();
                        main_gui.setSlot(slot, element);
                    }

                    main_gui.open();
                }
            }
        }
        return 1;
    }

    private void openContrabandGui(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, DisplayItemGui gui, String type, int page_to_open, Boss boss, Category category) {
        Map<ItemStack, String> display_items = new HashMap<>();
        if (type.equalsIgnoreCase("pokemon")) {
            if (boss != null && category != null) {
                for (Species species : boss.raidDetails().bannedPokemon()) {
                    display_items.put(PokemonItem.from(species), TextUtils.parse(gui.displayButton.itemName().replaceAll("%pokemon%", species.getName()).replaceAll("%category%", category.name()), boss));
                }
            } else if (category != null) {
                for (Species species : category.bannedPokemon()) {
                    display_items.put(PokemonItem.from(species), TextUtils.parse(gui.displayButton.itemName().replaceAll("%pokemon%", species.getName()).replaceAll("%category%", category.name())));
                }
            } else {
                for (Species species : nr.config().global_banned_pokemon) {
                    display_items.put(PokemonItem.from(species), TextUtils.parse(gui.displayButton.itemName().replaceAll("%pokemon%", species.getName())));
                }
            }
        } else if (type.equalsIgnoreCase("move")) {
            if (boss != null && category != null) {
                for (Move move : boss.raidDetails().bannedMoves()) {
                    display_items.put(Registries.ITEM.get(Identifier.of(gui.displayButton.item())).getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%move%", move.getDisplayName().getString()).replaceAll("%category%", category.name()), boss));
                }
            } else if (category != null) {
                for (Move move : category.bannedMoves()) {
                    display_items.put(Registries.ITEM.get(Identifier.of(gui.displayButton.item())).getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%move%", move.getDisplayName().getString()).replaceAll("%category%", category.name())));
                }
            } else {
                for (Move move : nr.config().global_banned_moves) {
                    display_items.put(Registries.ITEM.get(Identifier.of(gui.displayButton.item())).getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%move%", move.getDisplayName().getString())));
                }
            }
        } else if (type.equalsIgnoreCase("ability")) {
            if (boss != null && category != null) {
                for (Ability ability : boss.raidDetails().bannedAbilities()) {
                    display_items.put(Registries.ITEM.get(Identifier.of(gui.displayButton.item())).getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%ability%", MiscUtilsKt.asTranslated(ability.getDisplayName()).getString()).replaceAll("%category%", category.name()), boss));
                }
            } else if (category != null) {
                for (Ability ability : category.bannedAbilities()) {
                    display_items.put(Registries.ITEM.get(Identifier.of(gui.displayButton.item())).getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%ability%", MiscUtilsKt.asTranslated(ability.getDisplayName()).getString()).replaceAll("%category%", category.name())));
                }
            } else {
                for (Ability ability : nr.config().global_banned_abilities) {
                    display_items.put(Registries.ITEM.get(Identifier.of(gui.displayButton.item())).getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%ability%", MiscUtilsKt.asTranslated(ability.getDisplayName()).getString())));
                }
            }
        } else if (type.equalsIgnoreCase("held_item")) {
            if (boss != null && category != null) {
                for (Item item : boss.raidDetails().bannedHeldItems()) {
                    display_items.put(item.getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.name()), boss));
                }
            } else if (category != null) {
                for (Item item : category.bannedHeldItems()) {
                    display_items.put(item.getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.name())));
                }
            } else {
                for (Item item : nr.config().global_banned_held_items) {
                    display_items.put(item.getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%item%", item.getName().getString())));
                }
            }
        } else if (type.equalsIgnoreCase("bag_item")) {
            if (boss != null && category != null) {
                for (Item item : boss.raidDetails().bannedBagItems()) {
                    display_items.put(item.getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.name()), boss));
                }
            } else if (category != null) {
                for (Item item : category.bannedBagItems()) {
                    display_items.put(item.getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.name())));
                }
            } else {
                for (Item item : nr.config().global_banned_bag_items) {
                    display_items.put(item.getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%item%", item.getName().getString())));
                }
            }
        }

        Map<Integer, SimpleGui> pages = new HashMap<>();
        int page_total = GuiUtils.getPageTotal(display_items.size(), gui.displaySlotTotal());
        for (int i = 1; i <= page_total; i++) {
            SimpleGui main_gui = new SimpleGui(GuiUtils.getScreenSize(gui.rows), player, false);
            String title = TextUtils.parse(gui.title);
            if (category != null) {
                title = title.replaceAll("%category%", category.name());
            }
            if (boss != null) {
                title = TextUtils.parse(title, boss);
            }
            main_gui.setTitle(TextUtils.deserialize(title));
            pages.put(i, main_gui);
        }

        int index = 0;
        for (Map.Entry<Integer, SimpleGui> page_entry : pages.entrySet()) {
            for (Integer slot : gui.displaySlots()) {
                if (index < display_items.size()) {
                    List<Text> lore = new ArrayList<>();
                    for (String line : gui.displayButton.itemLore()) {
                        if (category != null) {
                            line = line.replaceAll("%category%", category.name());
                        }
                        if (boss != null) {
                            line = TextUtils.parse(line, boss);
                        }
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }

                    ItemStack item = display_items.keySet().stream().toList().get(index);
                    item.applyChanges(gui.displayButton.itemData());
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(display_items.values().stream().toList().get(index)))
                            .setLore(lore)
                            .build();
                    page_entry.getValue().setSlot(slot, element);
                    index++;
                } else {
                    ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(gui.backgroundButton.item())));
                    item.applyChanges(gui.backgroundButton.itemData());
                    List<Text> lore = new ArrayList<>();
                    for (String line : gui.backgroundButton.itemLore()) {
                        if (category != null) {
                            line = line.replaceAll("%category%", category.name());
                        }
                        if (boss != null) {
                            line = TextUtils.parse(line, boss);
                        }
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }

                    String name = TextUtils.parse(gui.backgroundButton.itemName());
                    if (category != null) {
                        name = name.replaceAll("%category%", category.name());
                    }
                    if (boss != null) {
                        name = TextUtils.parse(name, boss);
                    }

                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(name))
                            .setLore(lore)
                            .build();
                    page_entry.getValue().setSlot(slot, element);
                }
            }

            if (page_entry.getKey() < page_total) {
                for (Integer slot : gui.nextButtonSlots()) {
                    ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(gui.nextButton.item())));
                    item.applyChanges(gui.nextButton.itemData());
                    List<Text> lore = new ArrayList<>();
                    for (String line : gui.nextButton.itemLore()) {
                        if (category != null) {
                            line = line.replaceAll("%category%", category.name());
                        }
                        if (boss != null) {
                            line = TextUtils.parse(line, boss);
                        }
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }

                    String name = TextUtils.parse(gui.nextButton.itemName());
                    if (category != null) {
                        name = name.replaceAll("%category%", category.name());
                    }
                    if (boss != null) {
                        name = TextUtils.parse(name, boss);
                    }

                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(name))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                page_entry.getValue().close();
                                openContrabandGui(ctx, player, gui, type, page_entry.getKey() + 1, boss, category);
                            })
                            .build();
                    page_entry.getValue().setSlot(slot, element);
                }
            }

            if (page_entry.getKey() > 1) {
                for (Integer slot : gui.previousButtonSlots()) {
                    ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(gui.previousButton.item())));
                    item.applyChanges(gui.previousButton.itemData());
                    List<Text> lore = new ArrayList<>();
                    for (String line : gui.previousButton.itemLore()) {
                        if (category != null) {
                            line = line.replaceAll("%category%", category.name());
                        }
                        if (boss != null) {
                            line = TextUtils.parse(line, boss);
                        }
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }

                    String name = TextUtils.parse(gui.previousButton.itemName());
                    if (category != null) {
                        name = name.replaceAll("%category%", category.name());
                    }
                    if (boss != null) {
                        name = TextUtils.parse(name, boss);
                    }

                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(name))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                page_entry.getValue().close();
                                openContrabandGui(ctx, player, gui, type, page_entry.getKey() - 1, boss, category);
                            })
                            .build();
                    page_entry.getValue().setSlot(slot, element);
                }
            }

            for (Integer slot : gui.closeButtonSlots()) {
                ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(gui.closeButton.item())));
                item.applyChanges(gui.closeButton.itemData());
                List<Text> lore = new ArrayList<>();
                for (String line : gui.closeButton.itemLore()) {
                    if (category != null) {
                        line = line.replaceAll("%category%", category.name());
                    }
                    if (boss != null) {
                        line = TextUtils.parse(line, boss);
                    }
                    lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                }

                String name = TextUtils.parse(gui.closeButton.itemName());
                if (category != null) {
                    name = name.replaceAll("%category%", category.name());
                }
                if (boss != null) {
                    name = TextUtils.parse(name, boss);
                }

                String guiType;
                if (boss != null) {
                    guiType = "boss";
                } else if (category != null) {
                    guiType = "category";
                } else {
                    guiType = "global";
                }

                GuiElement element = new GuiElementBuilder(item)
                        .setName(TextUtils.deserialize(name))
                        .setLore(lore)
                        .setCallback(clickType -> checkbanned(ctx, guiType))
                        .build();
                page_entry.getValue().setSlot(slot, element);
            }

            for (Integer slot : gui.backgroundButtonSlots()) {
                ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(gui.backgroundButton.item())));
                item.applyChanges(gui.backgroundButton.itemData());
                List<Text> lore = new ArrayList<>();
                for (String line : gui.backgroundButton.itemLore()) {
                    if (category != null) {
                        line = line.replaceAll("%category%", category.name());
                    }
                    if (boss != null) {
                        line = TextUtils.parse(line, boss);
                    }
                    lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                }

                String name = TextUtils.parse(gui.backgroundButton.itemName());
                if (category != null) {
                    name = name.replaceAll("%category%", category.name());
                }
                if (boss != null) {
                    name = TextUtils.parse(name, boss);
                }

                GuiElement element = new GuiElementBuilder(item)
                        .setName(TextUtils.deserialize(name))
                        .setLore(lore)
                        .build();
                page_entry.getValue().setSlot(slot, element);
            }
        }
        if (!pages.isEmpty()) {
            pages.get(page_to_open).open();
        } else {
            if (type.equalsIgnoreCase("pokemon")) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("checkbanned_command_no_banned_pokemon"))));
            } else if (type.equalsIgnoreCase("move")) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("checkbanned_command_no_banned_moves"))));
            } else if (type.equalsIgnoreCase("ability")) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("checkbanned_command_no_banned_abilities"))));
            } else if (type.equalsIgnoreCase("held_item")) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("checkbanned_command_no_banned_held_items"))));
            } else if (type.equalsIgnoreCase("bag_item")) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("checkbanned_command_no_banned_bag_items"))));
            }

            String guiType;
            if (boss != null) {
                guiType = "boss";
            } else if (category != null) {
                guiType = "category";
            } else {
                guiType = "global";
            }
            checkbanned(ctx, guiType);
        }
    }

    private int testRewards(CommandContext<ServerCommandSource> ctx) {
        String boss = StringArgumentType.getString(ctx, "boss");
        int placement = IntegerArgumentType.getInteger(ctx, "placement");
        int total_players = IntegerArgumentType.getInteger(ctx, "total-players");

        Boss boss_info = nr.bossesConfig().getBoss(boss);
        Category raidBoss_category = nr.bossesConfig().getCategory(boss_info.categoryId());

        List<DistributionSection> category_rewards = new ArrayList<>(raidBoss_category.rewards());
        List<DistributionSection> boss_rewards = new ArrayList<>(boss_info.raidDetails().rewards());

        List<Place> overridden_placements = new ArrayList<>();

        for (DistributionSection boss_reward : boss_rewards) {
            List<Place> places = boss_reward.places();
            for (Place place : places) {
                if (place.overrideCategoryReward()) {
                    overridden_placements.add(place);
                }
            }
        }

        List<DistributionSection> rewards = new ArrayList<>(boss_rewards);

        for (DistributionSection category_reward : category_rewards) {
            boolean overridden = false;
            List<Place> places = category_reward.places();
            outer:
            for (Place place : places) {
                for (Place overridden_placement : overridden_placements) {
                    if (overridden_placement.place().equalsIgnoreCase(place.place())) {
                        overridden = true;
                        break outer;
                    }
                }
            }
            if (!overridden) {
                rewards.add(category_reward);
            }
        }

        Map<ServerPlayerEntity, String> no_more_rewards = new HashMap<>();
        for (DistributionSection reward : rewards) {
            List<Place> places = reward.places();
            for (Place place : places) {
                List<ServerPlayerEntity> players_to_reward = new ArrayList<>();
                if (StringUtils.isNumeric(place.place())) {
                    int placeInt = Integer.parseInt(place.place());
                    if (placement == placeInt) {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player != null) {
                            players_to_reward.add(player);
                        }
                    }
                } else if (place.place().contains("%")) {
                    String percentStr = place.place().replace("%", "");
                    if (StringUtils.isNumeric(percentStr)) {
                        int percent = Integer.parseInt(percentStr);
                        double positions = total_players * ((double) percent / 100);
                        for (int i = 1; i <= ((int) Math.ceil(positions)); i++) {
                            if (placement == i) {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player != null) {
                                    players_to_reward.add(player);
                                }
                            }
                        }
                    }
                } else if (place.place().equalsIgnoreCase("participating")) {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player != null) {
                        players_to_reward.add(player);
                    }
                }

                for (ServerPlayerEntity player : players_to_reward) {
                    if (player != null) {
                        boolean duplicate_placement_exists = false;
                        int place_count = 0;
                        for (DistributionSection rewardSection : rewards) {
                            List<Place> rewardPlaces = rewardSection.places();
                            for (Place rewardPlace : rewardPlaces) {
                                if (rewardPlace.place().equalsIgnoreCase(place.place())) {
                                    place_count++;
                                    break;
                                }
                            }
                            if (place_count >= 2) {
                                duplicate_placement_exists = true;
                                break;
                            }
                        }

                        if (!no_more_rewards.containsKey(player) || (duplicate_placement_exists && place.place().equalsIgnoreCase(no_more_rewards.get(player)))) {
                            int rolls = new Random().nextInt(reward.minRolls(), reward.maxRolls() + 1);
                            List<UUID> distributed_pools = new ArrayList<>();
                            for (int i = 0; i < rolls; i++) {
                                Map.Entry<?, Double> pool_entry = RandomUtils.getRandomEntry(reward.pools());
                                if (pool_entry != null) {
                                    RewardPool pool = (RewardPool) pool_entry.getKey();
                                    if (reward.allowDuplicates() || !distributed_pools.contains(pool.uuid())) {
                                        pool.distributeRewards(player);
                                        distributed_pools.add(pool.uuid());
                                    } else {
                                        i--;
                                    }
                                } else {
                                    nr.logError("[RAIDS] Pool was null!");
                                }
                            }
                        }
                    }
                }

                for (ServerPlayerEntity player : players_to_reward) {
                    if (!place.allowOtherRewards() && !no_more_rewards.containsKey(player)) {
                        no_more_rewards.put(player, place.place());
                    }
                }
            }
        }

        return 1;
    }

    private int skipphase(CommandContext<ServerCommandSource> ctx) {
        if (nr.loadedProperly) {
            int id = IntegerArgumentType.getInteger(ctx, "id");
            if (nr.activeRaids().containsKey(id)) {
                Raid raid = nr.activeRaids().get(id);
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
        if (nr.loadedProperly) {
            if (!nr.server().getPlayerManager().getPlayerList().isEmpty() || nr.config().run_raids_with_no_players) {
                if (boss_info != null) {
                    Map<String, Double> spawn_locations = boss_info.spawnLocations();
                    Map<String, Double> valid_locations = new HashMap<>();

                    for (String key : spawn_locations.keySet()) {
                        boolean valid_spawn = true;
                        for (Raid raid : nr.activeRaids().values()) {
                            if (raid.raidBossLocation().id().equalsIgnoreCase(key)) {
                                valid_spawn = false;
                                break;
                            }
                        }
                        if (valid_spawn) {
                            valid_locations.put(key, spawn_locations.get(key));
                        }
                    }

                    String spawn_location_string;
                    Location spawn_location;
                    if (!valid_locations.isEmpty()) {
                        Map.Entry<?, Double> spawn_location_entry = RandomUtils.getRandomEntry(valid_locations);
                        if (spawn_location_entry != null) {
                            spawn_location_string = (String) spawn_location_entry.getKey();
                            spawn_location = nr.locationsConfig().getLocation(spawn_location_string);
                        } else {
                            nr.logError("[RAIDS] Location could not be found.");
                            return 0;
                        }
                    } else {
                        nr.logInfo("[RAIDS] No valid spawn locations found. All possible locations are busy.");
                        if (player != null) {
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("no_available_locations"), boss_info)));
                        }
                        return 0;
                    }

                    if (spawn_location != null) {
                        if (!nr.config().use_queue_system) {
                            nr.logInfo("[RAIDS] Starting raid.");
                            nr.addRaid(new Raid(boss_info, spawn_location, (player != null) ? player.getUuid() : null, starting_item));
                        } else {
                            nr.logInfo("[RAIDS] Adding queue raid.");
                            nr.addQueueItem(new QueueItem(UUID.randomUUID(), boss_info, spawn_location, (player != null) ? player.getUuid() : null, starting_item));

                            if (nr.activeRaids().isEmpty()) {
                                nr.initNextRaid();
                            } else {
                                if (player != null) {
                                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("added_to_queue"), boss_info)));
                                }
                            }
                        }
                    } else {
                        nr.logError("[RAIDS] Location was null!");
                        return 0;
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
        if (nr.loadedProperly) {
            int id = IntegerArgumentType.getInteger(ctx, "id");
            if (nr.activeRaids().containsKey(id)) {
                if (ctx.getSource().isExecutedByPlayer()) {
                    if (ctx.getSource().getPlayer() != null) {
                        ctx.getSource().getPlayer().sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("raid_stopped"), nr.activeRaids().get(id))));
                    }
                }
                nr.activeRaids().get(id).stop();
                nr.removeRaid(nr.activeRaids().get(id));
                return 1;
            }
        }
        return 0;
    }

    public int give(ServerPlayerEntity source_player, ServerPlayerEntity target_player, String item_type, String boss_name, String category, String key, int amount) {
        if (nr.loadedProperly) {
            ItemStack item_to_give;
            NbtCompound custom_data = new NbtCompound();
            ComponentMap.Builder component_builder = ComponentMap.builder();
            Text item_name;
            LoreComponent lore;

            if (item_type.equalsIgnoreCase("pass")) {
                String boss_id;
                String category_id;
                Pass pass;
                if (boss_name.equalsIgnoreCase("*")) {
                    boss_id = "*";
                    if (category == null) {
                        category_id = "*";
                        pass = nr.config().global_pass;
                    } else {
                        category_id = category;
                        Category cat = nr.bossesConfig().getCategory(category_id);
                        if (cat == null) {
                            source_player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_category").replaceAll("%category%", category), source_player, target_player, amount, item_type)));
                            return 0;
                        }
                        pass = cat.categoryPass();
                    }
                    item_name = TextUtils.deserialize(TextUtils.parse(pass.passName(), source_player, target_player, amount, item_type));
                    List<Text> lore_text = new ArrayList<>();
                    for (String lore_line : pass.passLore()) {
                        lore_text.add(TextUtils.deserialize(TextUtils.parse(lore_line, source_player, target_player, amount, item_type)));
                    }
                    lore = new LoreComponent(lore_text);
                } else if (boss_name.equalsIgnoreCase("random")) {
                    Boss boss;
                    if (category == null) {
                        category_id = "null";
                        boss = nr.bossesConfig().getRandomBoss();
                    } else {
                        category_id = category;
                        Category cat = nr.bossesConfig().getCategory(category_id);
                        if (cat == null) {
                            source_player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_category").replaceAll("%category%", category), source_player, target_player, amount, item_type)));
                            return 0;
                        }
                        boss = nr.bossesConfig().getRandomBoss(category_id);
                    }
                    boss_id = boss.bossId();
                    pass = boss.itemSettings().pass();
                    item_name = TextUtils.deserialize(TextUtils.parse(TextUtils.parse(pass.passName(), boss), source_player, target_player, amount, item_type));
                    List<Text> lore_text = new ArrayList<>();
                    for (String lore_line : pass.passLore()) {
                        lore_text.add(TextUtils.deserialize(TextUtils.parse(TextUtils.parse(lore_line, boss), source_player, target_player, amount, item_type)));
                    }
                    lore = new LoreComponent(lore_text);
                } else {
                    category_id = "null";
                    boss_id = boss_name;
                    Boss boss = nr.bossesConfig().getBoss(boss_name);
                    if (boss == null) {
                        source_player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_boss").replaceAll("%boss%", boss_name), source_player, target_player, amount, item_type)));
                        return 0;
                    }
                    pass = boss.itemSettings().pass();
                    item_name = TextUtils.deserialize(TextUtils.parse(TextUtils.parse(pass.passName(), boss), source_player, target_player, amount, item_type));
                    List<Text> lore_text = new ArrayList<>();
                    for (String lore_line : pass.passLore()) {
                        lore_text.add(TextUtils.deserialize(TextUtils.parse(TextUtils.parse(lore_line, boss), source_player, target_player, amount, item_type)));
                    }
                    lore = new LoreComponent(lore_text);
                }

                item_to_give = new ItemStack(pass.passItem(), 1);
                if (pass.passData() != null) {
                    item_to_give.applyChanges(pass.passData());
                }
                item_to_give.applyComponentsFrom(ComponentMap.builder().add(DataComponentTypes.MAX_STACK_SIZE, 1).build());
                custom_data.putString("raid_item", "raid_pass");
                custom_data.putString("raid_boss", boss_id);
                custom_data.putString("raid_category", category_id);
            } else if (item_type.equalsIgnoreCase("voucher")) {
                String boss_id;
                String category_id;
                Voucher voucher;
                if (boss_name.equalsIgnoreCase("*")) {
                    boss_id = "*";
                    if (category == null) {
                        category_id = "*";
                        voucher = nr.config().global_choice_voucher;
                    } else {
                        category_id = category;
                        Category cat = nr.bossesConfig().getCategory(category_id);
                        if (cat == null) {
                            source_player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_category").replaceAll("%category%", category), source_player, target_player, amount, item_type)));
                            return 0;
                        }
                        voucher = cat.categoryChoiceVoucher();
                    }
                    item_name = TextUtils.deserialize(TextUtils.parse(voucher.voucherName(), source_player, target_player, amount, item_type));
                    List<Text> lore_text = new ArrayList<>();
                    for (String lore_line : voucher.voucherLore()) {
                        lore_text.add(TextUtils.deserialize(TextUtils.parse(lore_line, source_player, target_player, amount, item_type)));
                    }
                    lore = new LoreComponent(lore_text);
                } else if (boss_name.equalsIgnoreCase("random")) {
                    boss_id = "random";
                    if (category == null) {
                        category_id = "null";
                        voucher = nr.config().global_random_voucher;
                    } else {
                        category_id = category;
                        Category cat = nr.bossesConfig().getCategory(category_id);
                        if (cat == null) {
                            source_player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_category").replaceAll("%category%", category), source_player, target_player, amount, item_type)));
                            return 0;
                        }
                        voucher = cat.categoryRandomVoucher();
                    }
                    item_name = TextUtils.deserialize(TextUtils.parse(voucher.voucherName(), source_player, target_player, amount, item_type));
                    List<Text> lore_text = new ArrayList<>();
                    for (String lore_line : voucher.voucherLore()) {
                        lore_text.add(TextUtils.deserialize(TextUtils.parse(lore_line, source_player, target_player, amount, item_type)));
                    }
                    lore = new LoreComponent(lore_text);
                } else {
                    category_id = "null";
                    boss_id = boss_name;
                    Boss boss = nr.bossesConfig().getBoss(boss_name);
                    if (boss == null) {
                        source_player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_boss").replaceAll("%boss%", boss_name), source_player, target_player, amount, item_type)));
                        return 0;
                    }
                    voucher = boss.itemSettings().voucher();
                    item_name = TextUtils.deserialize(TextUtils.parse(TextUtils.parse(voucher.voucherName(), boss), source_player, target_player, amount, item_type));
                    List<Text> lore_text = new ArrayList<>();
                    for (String lore_line : voucher.voucherLore()) {
                        lore_text.add(TextUtils.deserialize(TextUtils.parse(TextUtils.parse(lore_line, boss), source_player, target_player, amount, item_type)));
                    }
                    lore = new LoreComponent(lore_text);
                }

                item_to_give = new ItemStack(voucher.voucherItem(), 1);
                if (voucher.voucherData() != null) {
                    item_to_give.applyChanges(voucher.voucherData());
                }
                item_to_give.applyComponentsFrom(ComponentMap.builder().add(DataComponentTypes.MAX_STACK_SIZE, 1).build());
                custom_data.putString("raid_item", "raid_voucher");
                custom_data.putString("raid_boss", boss_id);
                custom_data.putString("raid_category", category_id);
            } else {
                RaidBall raid_pokeball;
                String category_id;
                String boss_id;
                if (boss_name != null) {
                    if (boss_name.equalsIgnoreCase("*")) {
//                        nr.logInfo("[Raids] this should be a global ball");
                        category_id = "*";
                        boss_id = "*";
                        raid_pokeball = nr.config().getRaidBall(key);
                        if (raid_pokeball == null) {
                            if (source_player != null) {
                                source_player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_pokeball").replaceAll("%pokeball%", key), source_player, target_player, amount, item_type)));
                            }
                            return 0;
                        }
                        item_name = TextUtils.deserialize(TextUtils.parse(raid_pokeball.ballName(), source_player, target_player, amount, item_type));
                        List<Text> lore_text = new ArrayList<>();
                        for (String lore_line : raid_pokeball.ballLore()) {
                            lore_text.add(TextUtils.deserialize(TextUtils.parse(lore_line, source_player, target_player, amount, item_type)));
                        }
                        lore = new LoreComponent(lore_text);
                    } else {
//                        nr.logInfo("[Raids] this should be a specific boss ball for " + boss_name);
                        category_id = "null";
                        Boss boss = nr.bossesConfig().getBoss(boss_name);
                        if (boss == null) {
                            nr.logInfo("[Raids] the boss was null");
                            source_player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_boss").replaceAll("%boss%", boss_name), source_player, target_player, amount, item_type)));
                            return 0;
                        }
                        boss_id = boss_name;
                        raid_pokeball = boss.itemSettings().getRaidBall(key);
                        if (raid_pokeball == null) {
                            if (source_player != null) {
                                source_player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_pokeball").replaceAll("%pokeball%", key), source_player, target_player, amount, item_type)));
                            }
                            return 0;
                        }
                        item_name = TextUtils.deserialize(TextUtils.parse(TextUtils.parse(raid_pokeball.ballName(), boss), source_player, target_player, amount, item_type));
                        List<Text> lore_text = new ArrayList<>();
                        for (String lore_line : raid_pokeball.ballLore()) {
                            lore_text.add(TextUtils.deserialize(TextUtils.parse(TextUtils.parse(lore_line, boss), source_player, target_player, amount, item_type)));
                        }
                        lore = new LoreComponent(lore_text);
                    }
                } else {
                    if (category != null) {
//                        nr.logInfo("[Raids] this should be a specific category ball. " + category);
                        boss_id = "*";
                        Category cat = nr.bossesConfig().getCategory(category);
                        if (cat == null) {
                            nr.logInfo("[Raids] category was null");
                            source_player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_category").replaceAll("%category%", category), source_player, target_player, amount, item_type)));
                            return 0;
                        }
                        category_id = category;
                        raid_pokeball = cat.getRaidBall(key);
                        if (raid_pokeball == null) {
                            if (source_player != null) {
                                source_player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_pokeball").replaceAll("%pokeball%", key), source_player, target_player, amount, item_type)));
                            }
                            return 0;
                        }
                        item_name = TextUtils.deserialize(TextUtils.parse(raid_pokeball.ballName(), source_player, target_player, amount, item_type));
                        List<Text> lore_text = new ArrayList<>();
                        for (String lore_line : raid_pokeball.ballLore()) {
                            lore_text.add(TextUtils.deserialize(TextUtils.parse(lore_line, source_player, target_player, amount, item_type)));
                        }
                        lore = new LoreComponent(lore_text);
                    } else {
                        source_player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_category").replaceAll("%category%", "null"), source_player, target_player, amount, item_type)));
                        return 0;
                    }
                }

                item_to_give = new ItemStack(raid_pokeball.ballItem(), amount);
                custom_data.putString("raid_item", "raid_ball");
                custom_data.putUuid("owner_uuid", target_player.getUuid());
                custom_data.putString("raid_boss", boss_id);
                custom_data.putString("raid_category", category_id);
                if (raid_pokeball.ballData() != null) {
                    item_to_give.applyChanges(raid_pokeball.ballData());
                }
            }

            item_to_give.applyComponentsFrom(component_builder
                    .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(custom_data))
                    .add(DataComponentTypes.CUSTOM_NAME, item_name)
                    .add(DataComponentTypes.LORE, lore)
                    .build());
            if (!target_player.giveItemStack(item_to_give)) {
                if (source_player != null) {
                    source_player.sendMessage(
                            TextUtils.deserialize(
                                    TextUtils.parse(nr.messagesConfig().getMessage("give_command_failed_to_give"), source_player, target_player, amount, item_type)
                            )
                    );
                    return 0;
                }
            } else {
                target_player.sendMessage(
                        TextUtils.deserialize(
                            TextUtils.parse(nr.messagesConfig().getMessage("give_command_received_item"), source_player, target_player, amount, item_type)
                        )
                );
                if (source_player != null) {
                    source_player.sendMessage(
                            TextUtils.deserialize(
                                    TextUtils.parse(nr.messagesConfig().getMessage("give_command_feedback"), source_player, target_player, amount, item_type)
                            )
                    );
                }
            }
        }
        return 1;
    }

    private int list(CommandContext<ServerCommandSource> ctx) {
        if (nr.loadedProperly) {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player != null) {
                if (nr.activeRaids().isEmpty()) {
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("no_active_raids"))));
                    return 0;
                }

                Map<Integer, SimpleGui> pages = new HashMap<>();
                int page_total = GuiUtils.getPageTotal(nr.activeRaids().size(), nr.guisConfig().raid_list_gui.displaySlotTotal());
                for (int i = 1; i <= page_total; i++) {
                    SimpleGui gui = new SimpleGui(GuiUtils.getScreenSize(nr.guisConfig().raid_list_gui.rows), player, false);
                    gui.setTitle(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().raid_list_gui.title)));
                    pages.put(i, gui);
                }

                int index = 0;
                for (Map.Entry<Integer, SimpleGui> page_entry : pages.entrySet()) {
                    for (Integer slot : nr.guisConfig().raid_list_gui.displaySlots()) {
                        if (nr.activeRaids().containsKey(index + 1)) {
                            Raid raid = nr.activeRaids().get(index + 1);
                            List<Text> lore = new ArrayList<>();

                            if (!raid.raidBossCategory().requirePass() && raid.stage() == 1) {
                                for (String line : nr.guisConfig().raid_list_gui.joinableLore) {
                                    lore.add(TextUtils.deserialize(TextUtils.parse(line, raid)));
                                }
                            } else if (raid.stage() != 1) {
                                for (String line : nr.guisConfig().raid_list_gui.inProgressLore) {
                                    lore.add(TextUtils.deserialize(TextUtils.parse(line, raid)));
                                }
                            } else {
                                for (String line : nr.guisConfig().raid_list_gui.requiresPassLore) {
                                    lore.add(TextUtils.deserialize(TextUtils.parse(line, raid)));
                                }
                            }

                            ItemStack item = PokemonItem.from(raid.raidBossPokemon());
                            item.applyChanges(nr.guisConfig().raid_list_gui.displayData);
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().raid_list_gui.displayName, raid)))
                                    .setLore(lore)
                                    .setCallback((num, clickType, slotActionType) -> {
                                        if (clickType.isLeft) {
                                            if (raid.participatingPlayers().size() < raid.maxPlayers() || Permissions.check(player, "novaraids.override") || raid.maxPlayers() == -1) {
                                                if (raid.addPlayer(player.getUuid(), false)) {
                                                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("joined_raid"), raid)));
                                                }
                                            } else {
                                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("warning_max_players"), raid)));
                                            }
                                            page_entry.getValue().close();
                                        }
                                    }).build();
                            page_entry.getValue().setSlot(slot, element);
                            index++;
                        } else {
                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().raid_list_gui.backgroundButton.item())));
                            item.applyChanges(nr.guisConfig().raid_list_gui.backgroundButton.itemData());
                            List<Text> lore = new ArrayList<>();
                            for (String line : nr.guisConfig().raid_list_gui.backgroundButton.itemLore()) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                            }
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().raid_list_gui.backgroundButton.itemName())))
                                    .setLore(lore)
                                    .build();
                            page_entry.getValue().setSlot(slot, element);
                        }
                    }

                    if (page_entry.getKey() < page_total) {
                        for (Integer slot : nr.guisConfig().raid_list_gui.nextButtonSlots()) {
                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().raid_list_gui.nextButton.item())));
                            item.applyChanges(nr.guisConfig().raid_list_gui.nextButton.itemData());
                            List<Text> lore = new ArrayList<>();
                            for (String line : nr.guisConfig().raid_list_gui.nextButton.itemLore()) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                            }
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().raid_list_gui.nextButton.itemName())))
                                    .setLore(lore)
                                    .setCallback(clickType -> {
                                        page_entry.getValue().close();
                                        pages.get(page_entry.getKey() + 1).open();
                                    })
                                    .build();
                            page_entry.getValue().setSlot(slot, element);
                        }
                    }

                    if (page_entry.getKey() > 1) {
                        for (Integer slot : nr.guisConfig().raid_list_gui.previousButtonSlots()) {
                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().raid_list_gui.previousButton.item())));
                            item.applyChanges(nr.guisConfig().raid_list_gui.previousButton.itemData());
                            List<Text> lore = new ArrayList<>();
                            for (String line : nr.guisConfig().raid_list_gui.previousButton.itemLore()) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                            }
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().raid_list_gui.previousButton.itemName())))
                                    .setLore(lore)
                                    .setCallback(clickType -> {
                                        page_entry.getValue().close();
                                        pages.get(page_entry.getKey() - 1).open();
                                    })
                                    .build();
                            page_entry.getValue().setSlot(slot, element);
                        }
                    }

                    for (Integer slot : nr.guisConfig().raid_list_gui.closeButtonSlots()) {
                        ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().raid_list_gui.closeButton.item())));
                        item.applyChanges(nr.guisConfig().raid_list_gui.closeButton.itemData());
                        List<Text> lore = new ArrayList<>();
                        for (String line : nr.guisConfig().raid_list_gui.closeButton.itemLore()) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                        }
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().raid_list_gui.closeButton.itemName())))
                                .setLore(lore)
                                .setCallback(clickType -> page_entry.getValue().close())
                                .build();
                        page_entry.getValue().setSlot(slot, element);
                    }

                    for (Integer slot : nr.guisConfig().raid_list_gui.backgroundButtonSlots()) {
                        ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().raid_list_gui.backgroundButton.item())));
                        item.applyChanges(nr.guisConfig().raid_list_gui.backgroundButton.itemData());
                        List<Text> lore = new ArrayList<>();
                        for (String line : nr.guisConfig().raid_list_gui.backgroundButton.itemLore()) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                        }
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().raid_list_gui.backgroundButton.itemName())))
                                .setLore(lore)
                                .build();
                        page_entry.getValue().setSlot(slot, element);
                    }
                }
                pages.get(1).open();
            }
        }
        return 1;
    }

    private int queue(CommandContext<ServerCommandSource> ctx, int page_to_open) {
        if (nr.loadedProperly) {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player != null) {
                if (nr.queuedRaids().isEmpty()) {
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("no_queued_raids"))));
                    return 0;
                }

                Map<Integer, SimpleGui> pages = new HashMap<>();
                int page_total = GuiUtils.getPageTotal(nr.queuedRaids().size(), nr.guisConfig().queue_gui.displaySlotTotal());
                for (int i = 1; i <= page_total; i++) {
                    SimpleGui gui = new SimpleGui(GuiUtils.getScreenSize(nr.guisConfig().queue_gui.rows), player, false);
                    gui.setTitle(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().queue_gui.title)));
                    pages.put(i, gui);
                }

                int index = 0;
                for (Map.Entry<Integer, SimpleGui> page_entry : pages.entrySet()) {
                    for (Integer slot : nr.guisConfig().raid_list_gui.displaySlots()) {
                        if (index < nr.queuedRaids().size()) {
                            Boss boss = nr.queuedRaids().stream().toList().get(index).bossInfo();

                            List<Text> lore = new ArrayList<>();
                            if (Permissions.check(player, "novaraids.cancelqueue", 4)) {
                                for (String line : nr.guisConfig().queue_gui.cancelLore) {
                                    lore.add(TextUtils.deserialize(TextUtils.parse(line, boss)));
                                }
                            } else {
                                for (String line : nr.guisConfig().queue_gui.defaultLore) {
                                    lore.add(TextUtils.deserialize(TextUtils.parse(line, boss)));
                                }
                            }

                            ItemStack item = PokemonItem.from(boss.pokemonDetails().createPokemon());
                            item.applyChanges(nr.guisConfig().queue_gui.displayData);
                            int finalIndex = index;
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().queue_gui.displayName, boss)))
                                    .setLore(lore)
                                    .setCallback((num, clickType, slotActionType) -> {
                                        if (clickType.isRight) {
                                            if (Permissions.check(player, "novaraids.cancelqueue", 4)) {
                                                page_entry.getValue().close();
                                                nr.queuedRaids().stream().toList().get(finalIndex).cancel_item();
                                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("queue_item_cancelled"), boss)));
                                                nr.queuedRaids().remove(nr.queuedRaids().stream().toList().get(finalIndex));
                                                queue(ctx, page_entry.getKey());
                                            }
                                        }
                                    }).build();
                            page_entry.getValue().setSlot(slot, element);
                            index++;
                        } else {
                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().queue_gui.backgroundButton.item())));
                            item.applyChanges(nr.guisConfig().queue_gui.backgroundButton.itemData());
                            List<Text> lore = new ArrayList<>();
                            for (String line : nr.guisConfig().queue_gui.backgroundButton.itemLore()) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                            }
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().queue_gui.backgroundButton.itemName())))
                                    .setLore(lore)
                                    .build();
                            page_entry.getValue().setSlot(slot, element);
                        }
                    }

                    if (page_entry.getKey() < page_total) {
                        for (Integer slot : nr.guisConfig().queue_gui.nextButtonSlots()) {
                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().queue_gui.nextButton.item())));
                            item.applyChanges(nr.guisConfig().queue_gui.nextButton.itemData());
                            List<Text> lore = new ArrayList<>();
                            for (String line : nr.guisConfig().queue_gui.nextButton.itemLore()) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                            }
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().queue_gui.nextButton.itemName())))
                                    .setLore(lore)
                                    .setCallback(clickType -> {
                                        page_entry.getValue().close();
                                        queue(ctx, page_entry.getKey() + 1);
                                    })
                                    .build();
                            page_entry.getValue().setSlot(slot, element);
                        }
                    }

                    if (page_entry.getKey() > 1) {
                        for (Integer slot : nr.guisConfig().queue_gui.previousButtonSlots()) {
                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().queue_gui.previousButton.item())));
                            item.applyChanges(nr.guisConfig().queue_gui.previousButton.itemData());
                            List<Text> lore = new ArrayList<>();
                            for (String line : nr.guisConfig().queue_gui.previousButton.itemLore()) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                            }
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().queue_gui.previousButton.itemName())))
                                    .setLore(lore)
                                    .setCallback(clickType -> {
                                        page_entry.getValue().close();
                                        queue(ctx, page_entry.getKey() - 1);
                                    })
                                    .build();
                            page_entry.getValue().setSlot(slot, element);
                        }
                    }

                    for (Integer slot : nr.guisConfig().queue_gui.closeButtonSlots()) {
                        ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().queue_gui.closeButton.item())));
                        item.applyChanges(nr.guisConfig().queue_gui.closeButton.itemData());
                        List<Text> lore = new ArrayList<>();
                        for (String line : nr.guisConfig().queue_gui.closeButton.itemLore()) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                        }
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().queue_gui.closeButton.itemName())))
                                .setLore(lore)
                                .setCallback(clickType -> page_entry.getValue().close())
                                .build();
                        page_entry.getValue().setSlot(slot, element);
                    }

                    for (Integer slot : nr.guisConfig().queue_gui.backgroundButtonSlots()) {
                        ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().queue_gui.backgroundButton.item())));
                        item.applyChanges(nr.guisConfig().queue_gui.backgroundButton.itemData());
                        List<Text> lore = new ArrayList<>();
                        for (String line : nr.guisConfig().queue_gui.backgroundButton.itemLore()) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                        }
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().queue_gui.backgroundButton.itemName())))
                                .setLore(lore)
                                .build();
                        page_entry.getValue().setSlot(slot, element);
                    }
                }
                pages.get(page_to_open).open();
                return 1;
            }
        }
        return 0;
    }
}