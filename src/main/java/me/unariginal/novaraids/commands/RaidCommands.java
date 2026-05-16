package me.unariginal.novaraids.commands;

import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.MiscUtilsKt;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.cache.PlayerRaidCache;
import me.unariginal.novaraids.commands.suggestions.BossSuggestions;
import me.unariginal.novaraids.commands.suggestions.CategorySuggestions;
import me.unariginal.novaraids.config.LocationsConfig;
import me.unariginal.novaraids.config.RewardPoolsConfig;
import me.unariginal.novaraids.config.RewardPresetsConfig;
import me.unariginal.novaraids.config.guis.ContrabandGUIConfig;
import me.unariginal.novaraids.data.*;
import me.unariginal.novaraids.data.bosses.Boss;
import me.unariginal.novaraids.data.bosses.BossDetails;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.guis.BaseGUI;
import me.unariginal.novaraids.data.guis.BaseGUIItem;
import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;
import me.unariginal.novaraids.data.rewards.DistributionSection;
import me.unariginal.novaraids.data.rewards.Place;
import me.unariginal.novaraids.data.rewards.RewardDistribution;
import me.unariginal.novaraids.data.schedule.CronSchedule;
import me.unariginal.novaraids.data.schedule.RandomSchedule;
import me.unariginal.novaraids.data.schedule.Schedule;
import me.unariginal.novaraids.data.schedule.SpecificSchedule;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.utils.GuiUtils;
import me.unariginal.novaraids.utils.TextUtils;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
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
import org.apache.commons.lang3.StringUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

import static me.unariginal.novaraids.config.ConfigManager.*;

public class RaidCommands {
    private final NovaRaids nr = NovaRaids.INSTANCE;

    public RaidCommands() {
        CommandRegistrationCallback.EVENT.register(this::initialize);
    }

    public LiteralArgumentBuilder<ServerCommandSource> reload() {
        return CommandManager.literal("reload")
                .requires(Permissions.require("novaraids.reload", 4))
                .executes((ctx) -> {
                    nr.reloadConfig();
                    ctx.getSource().sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.reload)));
                    return 1;
                });
    }

    public LiteralArgumentBuilder<ServerCommandSource> start() {
        return CommandManager.literal("start")
                .requires(Permissions.require("novaraids.start", 4))
                .then(
                        CommandManager.argument("boss", StringArgumentType.string())
                                .suggests(new BossSuggestions())
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().isExecutedByPlayer() ? ctx.getSource().getPlayer() : null;
                                    return start(Boss.getBoss(StringArgumentType.getString(ctx, "boss")), player, null);
                                })
                )
                .then(
                        CommandManager.literal("random")
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().isExecutedByPlayer() ? ctx.getSource().getPlayer() : null;
                                    return start(Boss.getRandomBoss(null), player, null);
                                })
                                .then(
                                        CommandManager.argument("category", StringArgumentType.string())
                                                .suggests(new CategorySuggestions())
                                                .executes(ctx -> {
                                                    String category = StringArgumentType.getString(ctx, "category");
                                                    Boss boss = Boss.getRandomBoss(category, null);
                                                    ServerPlayerEntity player = ctx.getSource().isExecutedByPlayer() ? ctx.getSource().getPlayer() : null;
                                                    return start(boss, player, null);
                                                })
                                )
                );
    }

    public LiteralArgumentBuilder<ServerCommandSource> stop() {
        return CommandManager.literal("stop")
                .requires(Permissions.require("novaraids.stop", 4))
                .then(
                        CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(this::stop)
                );
    }

    public LiteralArgumentBuilder<ServerCommandSource> give() {
        return CommandManager.literal("give")
                .requires(Permissions.require("novaraids.give", 4))
                .then(
                        CommandManager.argument("player", EntityArgumentType.players())
                                .then(
                                        CommandManager.literal("pass")
                                                .then(
                                                        CommandManager.argument("boss", StringArgumentType.string())
                                                                .suggests(new BossSuggestions())
                                                                .executes(ctx -> {
                                                                    ServerPlayerEntity executor = ctx.getSource().getPlayer();
                                                                    List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                    String boss = StringArgumentType.getString(ctx, "boss");
                                                                    players.forEach(player -> give(executor, player, "pass", boss, null, "", 0));
                                                                    return 1;
                                                                })
                                                )
                                                .then(
                                                        CommandManager.literal("*")
                                                                .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pass", "*", null, "", 0))
                                                                .then(
                                                                        CommandManager.argument("category", StringArgumentType.string())
                                                                                .suggests(new CategorySuggestions())
                                                                                .executes(ctx -> {
                                                                                    ServerPlayerEntity executor = ctx.getSource().getPlayer();
                                                                                    List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                    String category = StringArgumentType.getString(ctx, "category");
                                                                                    players.forEach(player -> give(executor, player, "pass", "*", category, "", 0));
                                                                                    return 1;
                                                                                })
                                                                )
                                                )
                                                .then(
                                                        CommandManager.literal("random")
                                                                .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "pass", "random", null, "", 0))
                                                                .then(
                                                                        CommandManager.argument("category", StringArgumentType.string())
                                                                                .suggests(new CategorySuggestions())
                                                                                .executes(ctx -> {
                                                                                    ServerPlayerEntity executor = ctx.getSource().getPlayer();
                                                                                    List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                    String category = StringArgumentType.getString(ctx, "category");
                                                                                    players.forEach(player -> give(executor, player, "pass", "random", category, "", 0));
                                                                                    return 1;
                                                                                })
                                                                )
                                                )
                                )
                                .then(
                                        CommandManager.literal("voucher")
                                                .then(
                                                        CommandManager.argument("boss", StringArgumentType.string())
                                                                .suggests(new BossSuggestions())
                                                                .executes(ctx -> {
                                                                    ServerPlayerEntity executor = ctx.getSource().getPlayer();
                                                                    List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                    String boss = StringArgumentType.getString(ctx, "boss");
                                                                    players.forEach(player -> give(executor, player, "voucher", boss, null, "", 0));
                                                                    return 1;
                                                                })
                                                )
                                                .then(
                                                        CommandManager.literal("*")
                                                                .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "voucher", "*", null, "", 0))
                                                                .then(
                                                                        CommandManager.argument("category", StringArgumentType.string())
                                                                                .suggests(new CategorySuggestions())
                                                                                .executes(ctx -> {
                                                                                    ServerPlayerEntity executor = ctx.getSource().getPlayer();
                                                                                    List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                    String category = StringArgumentType.getString(ctx, "category");
                                                                                    players.forEach(player -> give(executor, player, "voucher", "*", category, "", 0));
                                                                                    return 1;
                                                                                })
                                                                )
                                                )
                                                .then(
                                                        CommandManager.literal("random")
                                                                .executes(ctx -> give(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player"), "voucher", "random", null, "", 0))
                                                                .then(
                                                                        CommandManager.argument("category", StringArgumentType.string())
                                                                                .suggests(new CategorySuggestions())
                                                                                .executes(ctx -> {
                                                                                    ServerPlayerEntity executor = ctx.getSource().getPlayer();
                                                                                    List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                    String category = StringArgumentType.getString(ctx, "category");
                                                                                    players.forEach(player -> give(executor, player, "voucher", "random", category, "", 0));
                                                                                    return 1;
                                                                                })
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
                                                                                                    Boss boss = Boss.getBoss(StringArgumentType.getString(ctx, "boss"));
                                                                                                    if (boss != null) boss.itemSettings.raidBalls.keySet().forEach(builder::suggest);
                                                                                                    return builder.buildFuture();
                                                                                                })
                                                                                                .then(
                                                                                                        CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                                                                                .executes(ctx -> {
                                                                                                                    ServerPlayerEntity executor = ctx.getSource().getPlayer();
                                                                                                                    List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                                                    String boss = StringArgumentType.getString(ctx, "boss");
                                                                                                                    String pokeball = StringArgumentType.getString(ctx, "pokeball");
                                                                                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                                                                                    players.forEach(player -> give(executor, player, "pokeball", boss, null, pokeball, amount));
                                                                                                                    return 1;
                                                                                                                })
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
                                                                                                    Category category = Category.getCategory(StringArgumentType.getString(ctx, "category"));
                                                                                                    if (category != null) category.itemSettings.raidBalls.keySet().forEach(builder::suggest);
                                                                                                    return builder.buildFuture();
                                                                                                })
                                                                                                .then(
                                                                                                        CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                                                                                .executes(ctx -> {
                                                                                                                    ServerPlayerEntity executor = ctx.getSource().getPlayer();
                                                                                                                    List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                                                    String category = StringArgumentType.getString(ctx, "category");
                                                                                                                    String pokeball = StringArgumentType.getString(ctx, "pokeball");
                                                                                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                                                                                    players.forEach(player -> give(executor, player, "pokeball", null, category, pokeball, amount));
                                                                                                                    return 1;
                                                                                                                })
                                                                                                )
                                                                                )
                                                                )
                                                )
                                                .then(
                                                        CommandManager.literal("global")
                                                                .then(
                                                                        CommandManager.argument("pokeball", StringArgumentType.string())
                                                                                .suggests((ctx, builder) -> {
                                                                                    CONFIG.itemSettings.raidBallSettings.raidBalls.keySet().forEach(builder::suggest);
                                                                                    return builder.buildFuture();
                                                                                })
                                                                                .then(
                                                                                        CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                                                                .executes(ctx -> {
                                                                                                    ServerPlayerEntity executor = ctx.getSource().getPlayer();
                                                                                                    List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                                    String pokeball = StringArgumentType.getString(ctx, "pokeball");
                                                                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                                                                    players.forEach(player -> give(executor, player, "pokeball", "*", null, pokeball, amount));
                                                                                                    return 1;
                                                                                                })
                                                                                )
                                                                )
                                                )

                                )
                );
    }

    public LiteralArgumentBuilder<ServerCommandSource> list() {
        return CommandManager.literal("list")
                .requires(Permissions.require("novaraids.list", 4))
                .executes(this::list);
    }

    public LiteralArgumentBuilder<ServerCommandSource> join() {
        return CommandManager.literal("join")
                .requires(Permissions.require("novaraids.join", 4))
                .then(
                        CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    if (ctx.getSource().isExecutedByPlayer()) {
                                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                                        if (player != null) {
                                            if (nr.activeRaids().containsKey(IntegerArgumentType.getInteger(ctx, "id"))) {
                                                Raid raid = nr.activeRaids().get(IntegerArgumentType.getInteger(ctx, "id"));
                                                if (raid.participatingPlayers.size() < raid.maxPlayers || Permissions.check(player, "novaraids.override") || raid.maxPlayers == -1) {
                                                    if (raid.addPlayer(player.getUuid(), false)) {
                                                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.joinedRaid, raid)));
                                                    }
                                                } else {
                                                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.maxPlayers, raid)));
                                                }
                                            }
                                        }
                                    }
                                    return 1;
                                })
                );
    }

    public LiteralArgumentBuilder<ServerCommandSource> leave() {
        return CommandManager.literal("leave")
                .requires(Permissions.require("novaraids.leave", 4))
                .executes(ctx -> {
                    if (ctx.getSource().isExecutedByPlayer()) {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        var activeRaid = PlayerRaidCache.currentRaid(player);
                        if (activeRaid != null) activeRaid.removePlayer(player.getUuid());
                    }
                    return 1;
                });
    }

    private void initialize(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess var2, CommandManager.RegistrationEnvironment var3) {
        dispatcher.register(
                CommandManager.literal("raid")
                        .executes(this::modInfo)
                        .then(reload())
                        .then(start())
                        .then(stop())
                        .then(give())
                        .then(list())
                        .then(join())
                        .then(leave())
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
                                                                            int id = IntegerArgumentType.getInteger(ctx, "id");
                                                                            if (nr.activeRaids().containsKey(id)) {
                                                                                Raid raid = nr.activeRaids().get(id);
                                                                                if (raid != null) {
                                                                                    if (ctx.getSource().isExecutedByPlayer()) {
                                                                                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                                                        if (player != null) {
                                                                                            int damage = IntegerArgumentType.getInteger(ctx, "amount");
                                                                                            if (damage > raid.currentHealth) {
                                                                                                damage = raid.currentHealth;
                                                                                            }

                                                                                            raid.applyDamage(damage);
                                                                                            raid.updatePlayerDamage(player.getUuid(), damage);
                                                                                            // TODO: Consider using the event?
//                                                                                            raid.participatingBroadcast(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("player_damage_report"), raid, player, damage, -1)));
                                                                                            player.sendMessage(TextUtils.deserialize("<green>The damage has been applied."));
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                            return 1;
                                                                        })
                                                        )
                                        )
                        )
                        .then(
                                CommandManager.literal("schedule")
                                        .requires(Permissions.require("novaraids.schedule", 4))
                                        .executes(ctx -> {
                                            for (Schedule schedule : SCHEDULES.schedules) {
                                                if (schedule instanceof SpecificSchedule specificSchedule) {
                                                    List<LocalTime> closestTimes = new ArrayList<>();

                                                    for (int i = 0; i < specificSchedule.times.size(); i++) {
                                                        LocalTime now = LocalTime.now(SCHEDULES.getTimezone());
                                                        LocalTime closestTime = null;
                                                        for (LocalTime time : specificSchedule.localTimes) {
                                                            if (time.isAfter(now) || time.equals(now)) {
                                                                if ((closestTime == null || time.isBefore(closestTime)) && !closestTimes.contains(time)) {
                                                                    closestTime = time;
                                                                }
                                                            }
                                                        }

                                                        if (closestTime == null) {
                                                            now = LocalTime.of(0, 0);
                                                            for (LocalTime time : specificSchedule.localTimes) {
                                                                if (time.isAfter(now) || time.equals(now)) {
                                                                    if ((closestTime == null || time.isBefore(closestTime)) && !closestTimes.contains(time)) {
                                                                        closestTime = time;
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        closestTimes.add(closestTime);
                                                    }

                                                    ctx.getSource().sendMessage(TextUtils.deserialize("<red>Specific Schedule Nearest Times:"));
                                                    for (LocalTime time : closestTimes) {
                                                        ctx.getSource().sendMessage(TextUtils.deserialize("<gray><i> - " + time.toString()));
                                                    }
                                                } else if (schedule instanceof RandomSchedule randomSchedule) {
                                                    ctx.getSource().sendMessage(TextUtils.deserialize("<red>Random Schedule (<i>" + randomSchedule.minSeconds + "s - " + randomSchedule.maxSeconds + "s<!i>) Next Raid:"));
                                                    ctx.getSource().sendMessage(TextUtils.deserialize("<gray><i> - " + randomSchedule.nextRandom.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withZone(SCHEDULES.getTimezone()))));
                                                } else if (schedule instanceof CronSchedule cronSchedule) {
                                                    ctx.getSource().sendMessage(TextUtils.deserialize("<red>Cron Schedule (<i>" + cronSchedule.expression + "<!i>) Next Raid:"));
                                                    ctx.getSource().sendMessage(TextUtils.deserialize("<gray><i> - " + cronSchedule.nextExecution.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withZone(SCHEDULES.getTimezone()))));
                                                }
                                                ctx.getSource().sendMessage(TextUtils.deserialize(""));
                                            }
                                            return 1;
                                        })
                        )
                        .then(
                                CommandManager.literal("togglePlayerVisibility")
                                        .requires(Permissions.require("novaraids.togglePlayerVisibility", 4))
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                                            if (player == null) return 1;
                                            if (!nr.ignorePlayerVisibility.contains(player.getUuid())) {
                                                nr.ignorePlayerVisibility.add(player.getUuid());
                                            } else {
                                                nr.ignorePlayerVisibility.remove(player.getUuid());
                                            }
                                            return 1;
                                        })
                        )
                        .then(
                                CommandManager.literal("togglePokemonVisibility")
                                        .requires(Permissions.require("novaraids.togglePokemonVisibility", 4))
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                                            if (player == null) return 1;
                                            if (!nr.ignorePokemonVisibility.contains(player.getUuid())) {
                                                nr.ignorePokemonVisibility.add(player.getUuid());
                                            } else {
                                                nr.ignorePokemonVisibility.remove(player.getUuid());
                                            }
                                            return 1;
                                        })
                        )
        );
    }

    private int modInfo(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player != null) {
            player.sendMessage(TextUtils.deserialize("<gray><st><b><i>---------<reset><red> Nova Raids <reset><gray><st><b><i>---------"));
            player.sendMessage(TextUtils.deserialize("<gray><b>Author: <reset><white>Unariginal <i>(Ariginal)"));
            player.sendMessage(TextUtils.deserialize("<gray><b>Version: <reset><white>Beta v0.3.9"));
            player.sendMessage(TextUtils.deserialize("<gray><b><i><u><click:open_url:https://github.com/Unariginal/NovaRaids>Source"));
            player.sendMessage(TextUtils.deserialize("<gray><b><i><u><click:open_url:https://github.com/Unariginal/NovaRaids/wiki>Wiki"));
            player.sendMessage(TextUtils.deserialize("<gray><st><b><i>---------------------------"));
        }
        return 1;
    }

    private int checkbanned(CommandContext<ServerCommandSource> ctx, String type) {
        if (ctx.getSource().isExecutedByPlayer()) {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player != null) {
                ContrabandGUIConfig gui;
                Boss boss = null;
                Category category = null;
                switch (type) {
                    case "global" -> gui = GLOBAL_CONTRABAND_GUI;
                    case "category" -> {
                        String categoryID = StringArgumentType.getString(ctx, "category");
                        category = Category.getCategory(categoryID);
                        if (category != null) {
                            gui = CATEGORY_CONTRABAND_GUI;
                        } else {
                            return 0;
                        }
                    }
                    case "boss" -> {
                        String bossID = StringArgumentType.getString(ctx, "boss");
                        boss = Boss.getBoss(bossID);
                        if (boss != null) {
                            category = Category.getCategory(boss.categoryId);
                            gui = BOSS_CONTRABAND_GUI;
                        } else {
                            return 0;
                        }
                    }
                    default -> {
                        return 0;
                    }
                }
                String title = gui.guiTitle;
                String pokemonItemName = gui.bannedPokemon.itemName;
                List<String> pokemonItemLore = gui.bannedPokemon.itemLore;
                String moveItemName = gui.bannedMoves.itemName;
                List<String> moveItemLore = gui.bannedMoves.itemLore;
                String abilityItemName = gui.bannedAbilities.itemName;
                List<String> abilityItemLore = gui.bannedAbilities.itemLore;
                String heldItemName = gui.bannedHeldItems.itemName;
                List<String> heldItemLore = gui.bannedHeldItems.itemLore;
                String bagItemName = gui.bannedBagItems.itemName;
                List<String> bagItemLore = gui.bannedBagItems.itemLore;
                String backgroundItemName = gui.backgroundItem.itemName;
                List<String> backgroundItemLore = gui.backgroundItem.itemLore;
                String closeItemName = gui.closeItem.itemName;
                List<String> closeItemLore = gui.closeItem.itemLore;

                if (category != null) {
                    title = gui.guiTitle.replaceAll("%category%", category.categoryName);
                    pokemonItemName = gui.bannedPokemon.itemName.replaceAll("%category%", category.categoryName);
                    List<String> lore = new ArrayList<>();
                    for (String line : pokemonItemLore) {
                        lore.add(line.replaceAll("%category%", category.categoryName));
                    }
                    pokemonItemLore = lore;

                    moveItemName = gui.bannedMoves.itemName.replaceAll("%category%", category.categoryName);
                    lore = new ArrayList<>();
                    for (String line : moveItemLore) {
                        lore.add(line.replaceAll("%category%", category.categoryName));
                    }
                    moveItemLore = lore;

                    abilityItemName = gui.bannedAbilities.itemName.replaceAll("%category%", category.categoryName);
                    lore = new ArrayList<>();
                    for (String line : abilityItemLore) {
                        lore.add(line.replaceAll("%category%", category.categoryName));
                    }
                    abilityItemLore = lore;

                    heldItemName = gui.bannedHeldItems.itemName.replaceAll("%category%", category.categoryName);
                    lore = new ArrayList<>();
                    for (String line : heldItemLore) {
                        lore.add(line.replaceAll("%category%", category.categoryName));
                    }
                    heldItemLore = lore;

                    bagItemName = gui.bannedBagItems.itemName.replaceAll("%category%", category.categoryName);
                    lore = new ArrayList<>();
                    for (String line : bagItemLore) {
                        lore.add(line.replaceAll("%category%", category.categoryName));
                    }
                    bagItemLore = lore;

                    backgroundItemName = gui.backgroundItem.itemName.replaceAll("%category%", category.categoryName);
                    lore = new ArrayList<>();
                    for (String line : backgroundItemLore) {
                        lore.add(line.replaceAll("%category%", category.categoryName));
                    }
                    backgroundItemLore = lore;

                    closeItemName = gui.closeItem.itemName.replaceAll("%category%", category.categoryName);
                    lore = new ArrayList<>();
                    for (String line : closeItemLore) {
                        lore.add(line.replaceAll("%category%", category.categoryName));
                    }
                    closeItemLore = lore;
                }
                if (boss != null) {
                    title = TextUtils.parse(title, boss);
                    pokemonItemName = TextUtils.parse(pokemonItemName, boss);
                    List<String> lore = new ArrayList<>();
                    for (String line : pokemonItemLore) {
                        lore.add(TextUtils.parse(line, boss));
                    }
                    pokemonItemLore = lore;

                    moveItemName = TextUtils.parse(moveItemName, boss);
                    lore = new ArrayList<>();
                    for (String line : moveItemLore) {
                        lore.add(TextUtils.parse(line, boss));
                    }
                    moveItemLore = lore;

                    abilityItemName = TextUtils.parse(abilityItemName, boss);
                    lore = new ArrayList<>();
                    for (String line : abilityItemLore) {
                        lore.add(TextUtils.parse(line, boss));
                    }
                    abilityItemLore = lore;

                    heldItemName = TextUtils.parse(heldItemName, boss);
                    lore = new ArrayList<>();
                    for (String line : heldItemLore) {
                        lore.add(TextUtils.parse(line, boss));
                    }
                    heldItemLore = lore;

                    bagItemName = TextUtils.parse(bagItemName, boss);
                    lore = new ArrayList<>();
                    for (String line : bagItemLore) {
                        lore.add(TextUtils.parse(line, boss));
                    }
                    bagItemLore = lore;

                    backgroundItemName = TextUtils.parse(backgroundItemName, boss);
                    lore = new ArrayList<>();
                    for (String line : backgroundItemLore) {
                        lore.add(TextUtils.parse(line, boss));
                    }
                    backgroundItemLore = lore;

                    closeItemName = TextUtils.parse(closeItemName, boss);
                    lore = new ArrayList<>();
                    for (String line : closeItemLore) {
                        lore.add(TextUtils.parse(line, boss));
                    }
                    closeItemLore = lore;
                }

                SimpleGui mainGui;
                if (gui.useHopperGui) mainGui = new SimpleGui(ScreenHandlerType.HOPPER, player, false);
                else mainGui = new SimpleGui(gui.getScreenHandler(), player, false);
                mainGui.setTitle(TextUtils.deserialize(title));

                List<Text> lore = new ArrayList<>();
                for (String line : pokemonItemLore) {
                    lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                }
                for (Integer slot : gui.getSlotsBySymbol(gui.bannedPokemon.symbol)) {
                    ItemStack item = gui.bannedPokemon.item.copy();
                    Boss finalBoss = boss;
                    Category finalCategory = category;
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(TextUtils.parse(pokemonItemName)))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                mainGui.close();
                                openContrabandGui(ctx, player, gui.bannedPokemon, "pokemon", 1, finalBoss, finalCategory);
                            })
                            .build();
                    mainGui.setSlot(slot, element);
                }

                lore = new ArrayList<>();
                for (String line : moveItemLore) {
                    lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                }
                for (Integer slot : gui.getSlotsBySymbol(gui.bannedMoves.symbol)) {
                    ItemStack item = gui.bannedMoves.item.copy();
                    Boss finalBoss = boss;
                    Category finalCategory = category;
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(TextUtils.parse(moveItemName)))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                mainGui.close();
                                openContrabandGui(ctx, player, gui.bannedMoves, "move", 1, finalBoss, finalCategory);
                            })
                            .build();
                    mainGui.setSlot(slot, element);
                }

                lore = new ArrayList<>();
                for (String line : abilityItemLore) {
                    lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                }
                for (Integer slot : gui.getSlotsBySymbol(gui.bannedAbilities.symbol)) {
                    ItemStack item = gui.bannedAbilities.item.copy();
                    Boss finalBoss = boss;
                    Category finalCategory = category;
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(TextUtils.parse(abilityItemName)))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                mainGui.close();
                                openContrabandGui(ctx, player, gui.bannedAbilities, "ability", 1, finalBoss, finalCategory);
                            })
                            .build();
                    mainGui.setSlot(slot, element);
                }

                lore = new ArrayList<>();
                for (String line : heldItemLore) {
                    lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                }
                for (Integer slot : gui.getSlotsBySymbol(gui.bannedHeldItems.symbol)) {
                    ItemStack item = gui.bannedHeldItems.item.copy();
                    Boss finalBoss = boss;
                    Category finalCategory = category;
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(TextUtils.parse(heldItemName)))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                mainGui.close();
                                openContrabandGui(ctx, player, gui.bannedHeldItems, "held_item", 1, finalBoss, finalCategory);
                            })
                            .build();
                    mainGui.setSlot(slot, element);
                }

                lore = new ArrayList<>();
                for (String line : bagItemLore) {
                    lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                }
                for (Integer slot : gui.getSlotsBySymbol(gui.bannedBagItems.symbol)) {
                    ItemStack item = gui.bannedBagItems.item.copy();
                    Boss finalBoss = boss;
                    Category finalCategory = category;
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(TextUtils.parse(bagItemName)))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                mainGui.close();
                                openContrabandGui(ctx, player, gui.bannedBagItems, "bag_item", 1, finalBoss, finalCategory);
                            })
                            .build();
                    mainGui.setSlot(slot, element);
                }

                lore = new ArrayList<>();
                for (String line : backgroundItemLore) {
                    lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                }
                for (Integer slot : gui.getSlotsBySymbol(gui.backgroundItem.symbol)) {
                    ItemStack item = gui.backgroundItem.item.copy();
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(TextUtils.parse(backgroundItemName)))
                            .setLore(lore)
                            .setCallback(clickType -> mainGui.close())
                            .build();
                    mainGui.setSlot(slot, element);
                }

                lore = new ArrayList<>();
                for (String line : closeItemLore) {
                    lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                }
                for (Integer slot : gui.getSlotsBySymbol(gui.closeItem.symbol)) {
                    ItemStack item = gui.closeItem.item.copy();
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(TextUtils.parse(closeItemName)))
                            .setLore(lore)
                            .setCallback(clickType -> mainGui.close())
                            .build();
                    mainGui.setSlot(slot, element);
                }

                mainGui.open();
            }
        }
        return 1;
    }

    private void openContrabandGui(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, BaseGUIItem guiItem, String type, int pageToOpen, Boss boss, Category category) {
        Map<ItemStack, String> displayItems = new HashMap<>();
        BaseGUI gui = null;
        BaseGUIItem displayGuiItem = null;
        if (guiItem instanceof ContrabandGUIConfig.PokemonSubGUIItem pokemonSubGUIItem) {
            gui = pokemonSubGUIItem.guiSettings;
            displayGuiItem = pokemonSubGUIItem.guiSettings.pokemonDisplayItem;
            if (boss != null && category != null) {
                for (Species species : boss.raidDetails.contraband.parsedPokemon) {
                    displayItems.put(PokemonItem.from(species), TextUtils.parse(pokemonSubGUIItem.guiSettings.pokemonDisplayItem.itemName.replaceAll("%pokemon%", species.getName()).replaceAll("%category%", category.categoryName), boss));
                }
            } else if (category != null) {
                for (Species species : category.raidDetails.contraband.parsedPokemon) {
                    displayItems.put(PokemonItem.from(species), TextUtils.parse(pokemonSubGUIItem.guiSettings.pokemonDisplayItem.itemName.replaceAll("%pokemon%", species.getName()).replaceAll("%category%", category.categoryName)));
                }
            } else {
                for (Species species : CONFIG.raidSettings.globalContraband.parsedPokemon) {
                    displayItems.put(PokemonItem.from(species), TextUtils.parse(pokemonSubGUIItem.guiSettings.pokemonDisplayItem.itemName.replaceAll("%pokemon%", species.getName())));
                }
            }
        } else if (guiItem instanceof ContrabandGUIConfig.MovesSubGUIItem movesSubGUIItem) {
            gui = movesSubGUIItem.guiSettings;
            displayGuiItem = movesSubGUIItem.guiSettings.moveDisplayItem;
            if (boss != null && category != null) {
                for (MoveTemplate move : boss.raidDetails.contraband.parsedMoves) {
                    displayItems.put(movesSubGUIItem.guiSettings.moveDisplayItem.item.copy(), TextUtils.parse(movesSubGUIItem.guiSettings.moveDisplayItem.itemName.replaceAll("%move%", move.getDisplayName().getString()).replaceAll("%category%", category.categoryName), boss));
                }
            } else if (category != null) {
                for (MoveTemplate move : category.raidDetails.contraband.parsedMoves) {
                    displayItems.put(movesSubGUIItem.guiSettings.moveDisplayItem.item.copy(), TextUtils.parse(movesSubGUIItem.guiSettings.moveDisplayItem.itemName.replaceAll("%move%", move.getDisplayName().getString()).replaceAll("%category%", category.categoryName)));
                }
            } else {
                for (MoveTemplate move : CONFIG.raidSettings.globalContraband.parsedMoves) {
                    displayItems.put(movesSubGUIItem.guiSettings.moveDisplayItem.item.copy(), TextUtils.parse(movesSubGUIItem.guiSettings.moveDisplayItem.itemName.replaceAll("%move%", move.getDisplayName().getString())));
                }
            }
        } else if (guiItem instanceof ContrabandGUIConfig.AbilitiesSubGUIItem abilitiesSubGUIItem) {
            gui = abilitiesSubGUIItem.guiSettings;
            displayGuiItem = abilitiesSubGUIItem.guiSettings.abilityDisplayItem;
            if (boss != null && category != null) {
                for (AbilityTemplate ability : boss.raidDetails.contraband.parsedAbilities) {
                    displayItems.put(abilitiesSubGUIItem.guiSettings.abilityDisplayItem.item.copy(), TextUtils.parse(abilitiesSubGUIItem.guiSettings.abilityDisplayItem.itemName.replaceAll("%ability%", MiscUtilsKt.asTranslated(ability.getDisplayName()).getString()).replaceAll("%category%", category.categoryName), boss));
                }
            } else if (category != null) {
                for (AbilityTemplate ability : category.raidDetails.contraband.parsedAbilities) {
                    displayItems.put(abilitiesSubGUIItem.guiSettings.abilityDisplayItem.item.copy(), TextUtils.parse(abilitiesSubGUIItem.guiSettings.abilityDisplayItem.itemName.replaceAll("%ability%", MiscUtilsKt.asTranslated(ability.getDisplayName()).getString()).replaceAll("%category%", category.categoryName)));
                }
            } else {
                for (AbilityTemplate ability : CONFIG.raidSettings.globalContraband.parsedAbilities) {
                    displayItems.put(abilitiesSubGUIItem.guiSettings.abilityDisplayItem.item.copy(), TextUtils.parse(abilitiesSubGUIItem.guiSettings.abilityDisplayItem.itemName.replaceAll("%ability%", MiscUtilsKt.asTranslated(ability.getDisplayName()).getString())));
                }
            }
        } else if (guiItem instanceof ContrabandGUIConfig.HeldItemsSubGUIItem heldItemsSubGUIItem) {
            gui = heldItemsSubGUIItem.guiSettings;
            displayGuiItem = heldItemsSubGUIItem.guiSettings.heldItemDisplayItem;
            if (boss != null && category != null) {
                for (Item item : boss.raidDetails.contraband.parsedHeldItems) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(heldItemsSubGUIItem.guiSettings.heldItemDisplayItem.itemName.replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.categoryName), boss));
                }
            } else if (category != null) {
                for (Item item : category.raidDetails.contraband.parsedHeldItems) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(heldItemsSubGUIItem.guiSettings.heldItemDisplayItem.itemName.replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.categoryName)));
                }
            } else {
                for (Item item : CONFIG.raidSettings.globalContraband.parsedHeldItems) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(heldItemsSubGUIItem.guiSettings.heldItemDisplayItem.itemName.replaceAll("%item%", item.getName().getString())));
                }
            }
        } else if (guiItem instanceof ContrabandGUIConfig.BagItemsSubGUIItem bagItemsSubGUIItem) {
            gui = bagItemsSubGUIItem.guiSettings;
            displayGuiItem = bagItemsSubGUIItem.guiSettings.bagItemDisplayItem;
            if (boss != null && category != null) {
                for (Item item : boss.raidDetails.contraband.parsedBagItems) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(bagItemsSubGUIItem.guiSettings.bagItemDisplayItem.itemName.replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.categoryName), boss));
                }
            } else if (category != null) {
                for (Item item : category.raidDetails.contraband.parsedBagItems) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(bagItemsSubGUIItem.guiSettings.bagItemDisplayItem.itemName.replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.categoryName)));
                }
            } else {
                for (Item item : CONFIG.raidSettings.globalContraband.parsedBagItems) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(bagItemsSubGUIItem.guiSettings.bagItemDisplayItem.itemName.replaceAll("%item%", item.getName().getString())));
                }
            }
        }

        if (gui == null || displayGuiItem == null) {
            // TODO: Probably feedback here
            return;
        }

        Map<Integer, SimpleGui> pages = new HashMap<>();
        int pageTotal = GuiUtils.getPageTotal(displayItems.size(), gui.getTotalSlotsBySymbol(displayGuiItem.symbol));
        for (int i = 1; i <= pageTotal; i++) {
            SimpleGui mainGui = new SimpleGui(gui.getScreenHandler(), player, false);
            String title = TextUtils.parse(gui.guiTitle);
            if (category != null) {
                title = title.replaceAll("%category%", category.categoryName);
            }
            if (boss != null) {
                title = TextUtils.parse(title, boss);
            }
            mainGui.setTitle(TextUtils.deserialize(title));
            pages.put(i, mainGui);
        }

        int index = 0;
        for (Map.Entry<Integer, SimpleGui> pageEntry : pages.entrySet()) {
            for (Integer slot : gui.getSlotsBySymbol(displayGuiItem.symbol)) {
                if (index < displayItems.size()) {
                    List<Text> lore = new ArrayList<>();
                    for (String line : displayGuiItem.itemLore) {
                        if (category != null) {
                            line = line.replaceAll("%category%", category.categoryName);
                        }
                        if (boss != null) {
                            line = TextUtils.parse(line, boss);
                        }
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }

                    ItemStack item = displayItems.keySet().stream().toList().get(index);
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(displayItems.values().stream().toList().get(index)))
                            .setLore(lore)
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                    index++;
                } else {
                    ItemStack item = gui.backgroundItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : gui.backgroundItem.itemLore) {
                        if (category != null) {
                            line = line.replaceAll("%category%", category.categoryName);
                        }
                        if (boss != null) {
                            line = TextUtils.parse(line, boss);
                        }
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }

                    String name = TextUtils.parse(gui.backgroundItem.itemName);
                    if (category != null) {
                        name = name.replaceAll("%category%", category.categoryName);
                    }
                    if (boss != null) {
                        name = TextUtils.parse(name, boss);
                    }

                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(name))
                            .setLore(lore)
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }

            if (pageEntry.getKey() < pageTotal) {
                for (Integer slot : gui.getSlotsBySymbol(gui.nextItem.symbol)) {
                    ItemStack item = gui.nextItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : gui.nextItem.itemLore) {
                        if (category != null) {
                            line = line.replaceAll("%category%", category.categoryName);
                        }
                        if (boss != null) {
                            line = TextUtils.parse(line, boss);
                        }
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }

                    String name = TextUtils.parse(gui.nextItem.itemName);
                    if (category != null) {
                        name = name.replaceAll("%category%", category.categoryName);
                    }
                    if (boss != null) {
                        name = TextUtils.parse(name, boss);
                    }

                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(name))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                pageEntry.getValue().close();
                                openContrabandGui(ctx, player, guiItem, type, pageEntry.getKey() + 1, boss, category);
                            })
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }

            if (pageEntry.getKey() > 1) {
                for (Integer slot : gui.getSlotsBySymbol(gui.previousItem.symbol)) {
                    ItemStack item = gui.previousItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : gui.previousItem.itemLore) {
                        if (category != null) {
                            line = line.replaceAll("%category%", category.categoryName);
                        }
                        if (boss != null) {
                            line = TextUtils.parse(line, boss);
                        }
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }

                    String name = TextUtils.parse(gui.previousItem.itemName);
                    if (category != null) {
                        name = name.replaceAll("%category%", category.categoryName);
                    }
                    if (boss != null) {
                        name = TextUtils.parse(name, boss);
                    }

                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(name))
                            .setLore(lore)
                            .setCallback(clickType -> {
                                pageEntry.getValue().close();
                                openContrabandGui(ctx, player, guiItem, type, pageEntry.getKey() - 1, boss, category);
                            })
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }

            for (Integer slot : gui.getSlotsBySymbol(gui.closeItem.symbol)) {
                ItemStack item = gui.closeItem.item.copy();
                List<Text> lore = new ArrayList<>();
                for (String line : gui.closeItem.itemLore) {
                    if (category != null) {
                        line = line.replaceAll("%category%", category.categoryName);
                    }
                    if (boss != null) {
                        line = TextUtils.parse(line, boss);
                    }
                    lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                }

                String name = TextUtils.parse(gui.closeItem.itemName);
                if (category != null) {
                    name = name.replaceAll("%category%", category.categoryName);
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
                pageEntry.getValue().setSlot(slot, element);
            }

            for (Integer slot : gui.getSlotsBySymbol(gui.backgroundItem.symbol)) {
                ItemStack item = gui.backgroundItem.item.copy();
                List<Text> lore = new ArrayList<>();
                for (String line : gui.backgroundItem.itemLore) {
                    if (category != null) {
                        line = line.replaceAll("%category%", category.categoryName);
                    }
                    if (boss != null) {
                        line = TextUtils.parse(line, boss);
                    }
                    lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                }

                String name = TextUtils.parse(gui.backgroundItem.itemName);
                if (category != null) {
                    name = name.replaceAll("%category%", category.categoryName);
                }
                if (boss != null) {
                    name = TextUtils.parse(name, boss);
                }

                GuiElement element = new GuiElementBuilder(item)
                        .setName(TextUtils.deserialize(name))
                        .setLore(lore)
                        .build();
                pageEntry.getValue().setSlot(slot, element);
            }
        }
        if (!pages.isEmpty()) {
            pages.get(pageToOpen).open();
        } else {
            if (type.equalsIgnoreCase("pokemon")) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.checkbannedNoBannedPokemon)));
            } else if (type.equalsIgnoreCase("move")) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.checkbannedNoBannedMoves)));
            } else if (type.equalsIgnoreCase("ability")) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.checkbannedNoBannedAbilities)));
            } else if (type.equalsIgnoreCase("held_item")) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.checkbannedNoBannedHeldItems)));
            } else if (type.equalsIgnoreCase("bag_item")) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.checkbannedNoBannedBagItems)));
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
        String bossId = StringArgumentType.getString(ctx, "boss");
        int placement = IntegerArgumentType.getInteger(ctx, "placement");
        int totalPlayers = IntegerArgumentType.getInteger(ctx, "total-players");

        Boss boss = Boss.getBoss(bossId);
        Category raidBossCategory = Category.getCategory(boss.categoryId);

        List<RewardDistribution> categoryRewards = new ArrayList<>(raidBossCategory.rewardDistribution);
        List<RewardDistribution> bossRewards = new ArrayList<>(boss.raidDetails.rewardDistribution);

        List<RewardDistribution> rewards = new ArrayList<>(bossRewards);

        if (!boss.raidDetails.overrideCategoryDistribution) {
            List<Place> overriddenPlacements = new ArrayList<>();

            for (RewardDistribution bossReward : bossRewards) {
                List<Place> places = bossReward.places;
                for (Place place : places) {
                    if (place.overrideCategoryReward) {
                        overriddenPlacements.add(place);
                    }
                }
            }

            for (RewardDistribution categoryReward : categoryRewards) {
                boolean overridden = false;
                List<Place> places = categoryReward.places;
                outer:
                for (Place place : places) {
                    for (Place overriddenPlacement : overriddenPlacements) {
                        if (overriddenPlacement.place.equalsIgnoreCase(place.place)) {
                            overridden = true;
                            break outer;
                        }
                    }
                }
                if (!overridden) {
                    rewards.add(categoryReward);
                }
            }
        }

        Map<ServerPlayerEntity, String> noMoreRewards = new HashMap<>();
        for (RewardDistribution reward : rewards) {
            List<Place> places = reward.places;
            for (Place place : places) {
                List<ServerPlayerEntity> playersToReward = new ArrayList<>();
                if (StringUtils.isNumeric(place.place)) {
                    int placeInt = Integer.parseInt(place.place);
                    if (placement == placeInt) {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player != null) {
                            playersToReward.add(player);
                        }
                    }
                } else if (place.place.contains("%")) {
                    String percentStr = place.place.replace("%", "");
                    if (StringUtils.isNumeric(percentStr)) {
                        int percent = Integer.parseInt(percentStr);
                        double positions = totalPlayers * ((double) percent / 100);
                        for (int i = 1; i <= ((int) Math.ceil(positions)); i++) {
                            if (placement == i) {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player != null) {
                                    playersToReward.add(player);
                                }
                            }
                        }
                    }
                } else if (place.place.equalsIgnoreCase("participating")) {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player != null) {
                        playersToReward.add(player);
                    }
                }

                for (ServerPlayerEntity player : playersToReward) {
                    if (player != null) {
                        boolean duplicatePlacementExists = false;
                        int placeCount = 0;
                        for (RewardDistribution rewardSection : rewards) {
                            List<Place> rewardPlaces = rewardSection.places;
                            for (Place rewardPlace : rewardPlaces) {
                                if (rewardPlace.place.equalsIgnoreCase(place.place)) {
                                    placeCount++;
                                    break;
                                }
                            }
                            if (placeCount >= 2) {
                                duplicatePlacementExists = true;
                                break;
                            }
                        }

                        if (!noMoreRewards.containsKey(player) || (duplicatePlacementExists && place.place.equalsIgnoreCase(noMoreRewards.get(player)))) {
                            int rolls = new Random().nextInt(reward.rewards.minRolls, reward.rewards.maxRolls + 1);
                            List<UUID> distributedPools = new ArrayList<>();
                            for (int i = 0; i < rolls; i++) {
                                DistributionSection.RewardPoolSection poolSection = reward.rewards.getRandomRewardPool();
                                if (poolSection != null) {
                                    RewardPoolsConfig.RewardPool pool = null;
                                    if (poolSection instanceof DistributionSection.PredefinedRewardPoolSection predefinedPoolSection) {
                                        pool = RewardPoolsConfig.getRewardPool(predefinedPoolSection.poolPreset);
                                    } else if (poolSection instanceof DistributionSection.UndefinedRewardPoolSection undefinedPoolSection) {
                                        pool = undefinedPoolSection.pool;
                                    }
                                    if (pool == null) {
                                        nr.logError("Pool was null!");
                                        continue;
                                    }
                                    if (reward.rewards.allowDuplicates || !distributedPools.contains(pool.getUuid())) {
                                        List<RewardPresetsConfig.Reward> distributionList = pool.distributeRewards();
                                        distributionList.forEach(distributionItem -> distributionItem.grantReward(player));
                                        distributedPools.add(pool.getUuid());
                                    } else {
                                        i--;
                                    }
                                } else {
                                    nr.logError("Pool was null!");
                                }
                            }
                        }
                    }
                }

                for (ServerPlayerEntity player : playersToReward) {
                    if (!place.allowOtherRewards && !noMoreRewards.containsKey(player)) {
                        noMoreRewards.put(player, place.place);
                    }
                }
            }
        }

        return 1;
    }

    private int skipphase(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        if (nr.activeRaids().containsKey(id)) {
            Raid raid = nr.activeRaids().get(id);
            List<Task> tasks = raid.tasks.entrySet().stream().findFirst().orElseThrow().getValue();
            raid.removeTask(raid.tasks.entrySet().stream().findFirst().orElseThrow().getKey());
            for (Task task : tasks) {
                raid.addTask(task.world(), 1L, task.action());
            }
        }
        return 1;
    }

    public int start(Boss bossInfo, ServerPlayerEntity player, ItemStack startingItem) {
        if (!nr.server().getPlayerManager().getPlayerList().isEmpty() || CONFIG.raidSettings.runRaidsWithNoPlayers) {
            if (bossInfo != null) {
                List<BossDetails.WeightedLocation> spawnLocations = bossInfo.bossDetails.locations;
                List<String> validLocations = new ArrayList<>();

                for (BossDetails.WeightedLocation location : spawnLocations) {
                    boolean validSpawn = true;
                    for (Raid raid : nr.activeRaids().values()) {
                        if (raid.locationId.equalsIgnoreCase(location.location)) {
                            validSpawn = false;
                            break;
                        }
                    }
                    if (validSpawn) {
                        validLocations.add(location.location);
                    }
                }

                LocationsConfig spawnLocation;
                String locationId;
                if (!validLocations.isEmpty()) {
                    locationId = validLocations.get(new Random().nextInt(validLocations.size()));
                    spawnLocation = LocationsConfig.getLocation(locationId);
                } else {
                    nr.logInfo("No valid spawn locations found. All possible locations are busy.");
                    if (player != null) {
                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.noAvailableLocations, bossInfo)));
                    }
                    return 0;
                }

                if (spawnLocation != null) {
                    if (!CONFIG.raidSettings.useQueueSystem) {
                        nr.addRaid(new Raid(bossInfo, locationId, (player != null) ? player.getUuid() : null, startingItem));
                    } else {
                        nr.addQueueItem(new QueueItem(UUID.randomUUID(), bossInfo, locationId, (player != null) ? player.getUuid() : null, startingItem));

                        if (nr.activeRaids().isEmpty()) {
                            nr.initNextRaid();
                        } else {
                            if (player != null) {
                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.addedToQueue, bossInfo)));
                            }
                        }
                    }
                } else {
                    nr.logError("Location was null!");
                    return 0;
                }
                return 1;
            }
            nr.logError("Boss was null!");
            return 0;
        }
        return 0;
    }

    private int stop(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        if (nr.activeRaids().containsKey(id)) {
            if (ctx.getSource().isExecutedByPlayer()) {
                if (ctx.getSource().getPlayer() != null) {
                    ctx.getSource().getPlayer().sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.raidStopped, nr.activeRaids().get(id))));
                }
            }
            nr.activeRaids().get(id).stop();
            nr.removeRaid(nr.activeRaids().get(id));
            return 1;
        }
        return 0;
    }

    public int give(ServerPlayerEntity sourcePlayer, ServerPlayerEntity targetPlayer, String itemType, String bossName, String category, String key, int amount) {
        ItemStack itemToGive;
        NbtCompound customData = new NbtCompound();
        ComponentMap.Builder componentBuilder = ComponentMap.builder();
        Text itemName;
        LoreComponent lore;

        if (itemType.equalsIgnoreCase("pass")) {
            String bossID;
            String categoryID;
            Pass pass;
            if (bossName.equalsIgnoreCase("*")) {
                bossID = "*";
                if (category == null) {
                    categoryID = "*";
                    pass = CONFIG.itemSettings.passSettings.globalPass;
                } else {
                    categoryID = category;
                    Category cat = Category.getCategory(categoryID);
                    if (cat == null) {
                        sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.giveInvalidCategory.replaceAll("%category%", category), sourcePlayer, targetPlayer, amount, itemType)));
                        return 0;
                    }
                    pass = cat.itemSettings.categoryPass;
                }
                itemName = TextUtils.deserialize(TextUtils.parse(pass.passName, sourcePlayer, targetPlayer, amount, itemType));
                List<Text> loreText = new ArrayList<>();
                for (String loreLine : pass.passLore) {
                    loreText.add(TextUtils.deserialize(TextUtils.parse(loreLine, sourcePlayer, targetPlayer, amount, itemType)));
                }
                lore = new LoreComponent(loreText);
            } else if (bossName.equalsIgnoreCase("random")) {
                Boss boss;
                if (category == null) {
                    categoryID = "null";
                    boss = Boss.getRandomBoss(null);
                } else {
                    categoryID = category;
                    Category cat = Category.getCategory(categoryID);
                    if (cat == null) {
                        sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.giveInvalidCategory.replaceAll("%category%", category), sourcePlayer, targetPlayer, amount, itemType)));
                        return 0;
                    }
                    boss = Boss.getRandomBoss(categoryID, null);
                }
                if (boss == null) return 0;
                bossID = boss.bossId;
                pass = boss.itemSettings.bossPass;
                itemName = TextUtils.deserialize(TextUtils.parse(TextUtils.parse(pass.passName, boss), sourcePlayer, targetPlayer, amount, itemType));
                List<Text> loreText = new ArrayList<>();
                for (String loreLine : pass.passLore) {
                    loreText.add(TextUtils.deserialize(TextUtils.parse(TextUtils.parse(loreLine, boss), sourcePlayer, targetPlayer, amount, itemType)));
                }
                lore = new LoreComponent(loreText);
            } else {
                categoryID = "null";
                bossID = bossName;
                Boss boss = Boss.getBoss(bossName);
                if (boss == null) {
                    sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.giveInvalidBoss.replaceAll("%boss%", bossName), sourcePlayer, targetPlayer, amount, itemType)));
                    return 0;
                }
                pass = boss.itemSettings.bossPass;
                itemName = TextUtils.deserialize(TextUtils.parse(TextUtils.parse(pass.passName, boss), sourcePlayer, targetPlayer, amount, itemType));
                List<Text> loreText = new ArrayList<>();
                for (String loreLine : pass.passLore) {
                    loreText.add(TextUtils.deserialize(TextUtils.parse(TextUtils.parse(loreLine, boss), sourcePlayer, targetPlayer, amount, itemType)));
                }
                lore = new LoreComponent(loreText);
            }

            itemToGive = pass.passItem.copy();
//                itemToGive.applyComponentsFrom(ComponentMap.builder().add(DataComponentTypes.MAX_STACK_SIZE, 1).build());
            customData.putString("raid_item", "raid_pass");
            customData.putString("raid_boss", bossID);
            customData.putString("raid_category", categoryID);
        } else if (itemType.equalsIgnoreCase("voucher")) {
            String bossID;
            String categoryID;
            Voucher voucher;
            if (bossName.equalsIgnoreCase("*")) {
                bossID = "*";
                if (category == null) {
                    categoryID = "*";
                    voucher = CONFIG.itemSettings.voucherSettings.globalChoiceVoucher;
                } else {
                    categoryID = category;
                    Category cat = Category.getCategory(categoryID);
                    if (cat == null) {
                        sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.giveInvalidCategory.replaceAll("%category%", category), sourcePlayer, targetPlayer, amount, itemType)));
                        return 0;
                    }
                    voucher = cat.itemSettings.categoryChoiceVoucher;
                }
                itemName = TextUtils.deserialize(TextUtils.parse(voucher.voucherName, sourcePlayer, targetPlayer, amount, itemType));
                List<Text> loreText = new ArrayList<>();
                for (String loreLine : voucher.voucherLore) {
                    loreText.add(TextUtils.deserialize(TextUtils.parse(loreLine, sourcePlayer, targetPlayer, amount, itemType)));
                }
                lore = new LoreComponent(loreText);
            } else if (bossName.equalsIgnoreCase("random")) {
                bossID = "random";
                if (category == null) {
                    categoryID = "null";
                    voucher = CONFIG.itemSettings.voucherSettings.globalRandomVoucher;
                } else {
                    categoryID = category;
                    Category cat = Category.getCategory(categoryID);
                    if (cat == null) {
                        sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.giveInvalidCategory.replaceAll("%category%", category), sourcePlayer, targetPlayer, amount, itemType)));
                        return 0;
                    }
                    voucher = cat.itemSettings.categoryRandomVoucher;
                }
                itemName = TextUtils.deserialize(TextUtils.parse(voucher.voucherName, sourcePlayer, targetPlayer, amount, itemType));
                List<Text> loreText = new ArrayList<>();
                for (String loreLine : voucher.voucherLore) {
                    loreText.add(TextUtils.deserialize(TextUtils.parse(loreLine, sourcePlayer, targetPlayer, amount, itemType)));
                }
                lore = new LoreComponent(loreText);
            } else {
                categoryID = "null";
                bossID = bossName;
                Boss boss = Boss.getBoss(bossName);
                if (boss == null) {
                    sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.giveInvalidBoss.replaceAll("%boss%", bossName), sourcePlayer, targetPlayer, amount, itemType)));
                    return 0;
                }
                voucher = boss.itemSettings.bossVoucher;
                itemName = TextUtils.deserialize(TextUtils.parse(TextUtils.parse(voucher.voucherName, boss), sourcePlayer, targetPlayer, amount, itemType));
                List<Text> loreText = new ArrayList<>();
                for (String loreLine : voucher.voucherLore) {
                    loreText.add(TextUtils.deserialize(TextUtils.parse(TextUtils.parse(loreLine, boss), sourcePlayer, targetPlayer, amount, itemType)));
                }
                lore = new LoreComponent(loreText);
            }

            itemToGive = voucher.voucherItem.copy();
//                itemToGive.applyComponentsFrom(ComponentMap.builder().add(DataComponentTypes.MAX_STACK_SIZE, 1).build());
            customData.putString("raid_item", "raid_voucher");
            customData.putString("raid_boss", bossID);
            customData.putString("raid_category", categoryID);
        } else {
            RaidBall raidPokeball;
            String categoryID;
            String bossID;
            if (bossName != null) {
                if (bossName.equalsIgnoreCase("*")) {
                    categoryID = "*";
                    bossID = "*";
                    raidPokeball = CONFIG.getRaidBall(key);
                    if (raidPokeball == null) {
                        if (sourcePlayer != null) {
                            sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.giveInvalidPokeball.replaceAll("%pokeball%", key), sourcePlayer, targetPlayer, amount, itemType)));
                        }
                        return 0;
                    }
                    itemName = TextUtils.deserialize(TextUtils.parse(raidPokeball.pokeballName, sourcePlayer, targetPlayer, amount, itemType));
                    List<Text> loreText = new ArrayList<>();
                    for (String loreLine : raidPokeball.pokeballLore) {
                        loreText.add(TextUtils.deserialize(TextUtils.parse(loreLine, sourcePlayer, targetPlayer, amount, itemType)));
                    }
                    lore = new LoreComponent(loreText);
                } else {
                    categoryID = "null";
                    Boss boss = Boss.getBoss(bossName);
                    if (boss == null) {
                        nr.logInfo("The boss was null");
                        sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.giveInvalidBoss.replaceAll("%boss%", bossName), sourcePlayer, targetPlayer, amount, itemType)));
                        return 0;
                    }
                    bossID = bossName;
                    raidPokeball = boss.itemSettings.getRaidBall(key);
                    if (raidPokeball == null) {
                        if (sourcePlayer != null) {
                            sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.giveInvalidPokeball.replaceAll("%pokeball%", key), sourcePlayer, targetPlayer, amount, itemType)));
                        }
                        return 0;
                    }
                    itemName = TextUtils.deserialize(TextUtils.parse(TextUtils.parse(raidPokeball.pokeballName, boss), sourcePlayer, targetPlayer, amount, itemType));
                    List<Text> loreText = new ArrayList<>();
                    for (String loreLine : raidPokeball.pokeballLore) {
                        loreText.add(TextUtils.deserialize(TextUtils.parse(TextUtils.parse(loreLine, boss), sourcePlayer, targetPlayer, amount, itemType)));
                    }
                    lore = new LoreComponent(loreText);
                }
            } else {
                if (category != null) {
                    bossID = "*";
                    Category cat = Category.getCategory(category);
                    if (cat == null) {
                        nr.logInfo("Category was null");
                        sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.giveInvalidCategory.replaceAll("%category%", category), sourcePlayer, targetPlayer, amount, itemType)));
                        return 0;
                    }
                    categoryID = category;
                    raidPokeball = cat.itemSettings.getRaidBall(key);
                    if (raidPokeball == null) {
                        if (sourcePlayer != null) {
                            sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.giveInvalidPokeball.replaceAll("%pokeball%", key), sourcePlayer, targetPlayer, amount, itemType)));
                        }
                        return 0;
                    }
                    itemName = TextUtils.deserialize(TextUtils.parse(raidPokeball.pokeballName, sourcePlayer, targetPlayer, amount, itemType));
                    List<Text> loreText = new ArrayList<>();
                    for (String loreLine : raidPokeball.pokeballLore) {
                        loreText.add(TextUtils.deserialize(TextUtils.parse(loreLine, sourcePlayer, targetPlayer, amount, itemType)));
                    }
                    lore = new LoreComponent(loreText);
                } else {
                    sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.giveInvalidCategory.replaceAll("%category%", "null"), sourcePlayer, targetPlayer, amount, itemType)));
                    return 0;
                }
            }

            itemToGive = raidPokeball.pokeballItem.copyWithCount(amount);
            customData.putString("raid_item", "raid_ball");
            customData.putUuid("owner_uuid", targetPlayer.getUuid());
            customData.putString("raid_boss", bossID);
            customData.putString("raid_category", categoryID);
        }

        itemToGive.applyComponentsFrom(componentBuilder
                .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData))
                .add(DataComponentTypes.CUSTOM_NAME, itemName)
                .add(DataComponentTypes.LORE, lore)
                .build());
        if (!targetPlayer.giveItemStack(itemToGive)) {
            if (sourcePlayer != null) {
                sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.giveFailedToGive, sourcePlayer, targetPlayer, amount, itemType)));
                return 0;
            }
        } else {
            targetPlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.giveReceivedItem, sourcePlayer, targetPlayer, amount, itemType)));
            if (sourcePlayer != null) {
                sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.commands.giveFeedback, sourcePlayer, targetPlayer, amount, itemType)));
            }
        }
        return 1;
    }

    private int list(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player != null) {
            if (nr.activeRaids().isEmpty()) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.noActiveRaids)));
                return 0;
            }

            Map<Integer, SimpleGui> pages = new HashMap<>();
            int pageTotal = GuiUtils.getPageTotal(nr.activeRaids().size(), RAID_LIST_GUI.getTotalSlotsBySymbol(RAID_LIST_GUI.joinableRaidItem.symbol));
            for (int i = 1; i <= pageTotal; i++) {
                SimpleGui gui = new SimpleGui(RAID_LIST_GUI.getScreenHandler(), player, false);
                gui.setTitle(TextUtils.deserialize(TextUtils.parse(RAID_LIST_GUI.guiTitle)));
                pages.put(i, gui);
            }

            int index = 0;
            for (Map.Entry<Integer, SimpleGui> pageEntry : pages.entrySet()) {
                for (Integer slot : RAID_LIST_GUI.getSlotsBySymbol(RAID_LIST_GUI.joinableRaidItem.symbol)) {
                    if (nr.activeRaids().containsKey(index + 1)) {
                        Raid raid = nr.activeRaids().get(index + 1);
                        List<Text> lore = new ArrayList<>();

                        if (!raid.category.raidDetails.requirePass && raid.stage == 1) {
                            for (String line : RAID_LIST_GUI.joinableRaidItem.itemLore) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line, raid)));
                            }
                        } else if (raid.stage != 1) {
                            for (String line : RAID_LIST_GUI.inProgressRaidItem.itemLore) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line, raid)));
                            }
                        } else {
                            for (String line : RAID_LIST_GUI.passRequiredRaidItem.itemLore) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line, raid)));
                            }
                        }

                        ItemStack item = PokemonItem.from(raid.bossPokemon);
                        // TODO: Item data
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_LIST_GUI.joinableRaidItem.itemName, raid)))
                                .setLore(lore)
                                .setCallback((num, clickType, slotActionType) -> {
                                    if (clickType.isLeft) {
                                        if (raid.participatingPlayers.size() < raid.maxPlayers || Permissions.check(player, "novaraids.override") || raid.maxPlayers == -1) {
                                            if (raid.addPlayer(player.getUuid(), false)) {
                                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.joinedRaid, raid)));
                                            }
                                        } else {
                                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.maxPlayers, raid)));
                                        }
                                        pageEntry.getValue().close();
                                    }
                                }).build();
                        pageEntry.getValue().setSlot(slot, element);
                        index++;
                    } else {
                        ItemStack item = RAID_LIST_GUI.backgroundItem.item.copy();
                        List<Text> lore = new ArrayList<>();
                        for (String line : RAID_LIST_GUI.backgroundItem.itemLore) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                        }
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_LIST_GUI.backgroundItem.itemName)))
                                .setLore(lore)
                                .build();
                        pageEntry.getValue().setSlot(slot, element);
                    }
                }

                if (pageEntry.getKey() < pageTotal) {
                    for (Integer slot : RAID_LIST_GUI.getSlotsBySymbol(RAID_LIST_GUI.nextItem.symbol)) {
                        ItemStack item = RAID_LIST_GUI.nextItem.item.copy();
                        List<Text> lore = new ArrayList<>();
                        for (String line : RAID_LIST_GUI.nextItem.itemLore) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                        }
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_LIST_GUI.nextItem.itemName)))
                                .setLore(lore)
                                .setCallback(clickType -> {
                                    pageEntry.getValue().close();
                                    pages.get(pageEntry.getKey() + 1).open();
                                })
                                .build();
                        pageEntry.getValue().setSlot(slot, element);
                    }
                }

                if (pageEntry.getKey() > 1) {
                    for (Integer slot : RAID_LIST_GUI.getSlotsBySymbol(RAID_LIST_GUI.previousItem.symbol)) {
                        ItemStack item = RAID_LIST_GUI.previousItem.item.copy();
                        List<Text> lore = new ArrayList<>();
                        for (String line : RAID_LIST_GUI.previousItem.itemLore) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                        }
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_LIST_GUI.previousItem.itemName)))
                                .setLore(lore)
                                .setCallback(clickType -> {
                                    pageEntry.getValue().close();
                                    pages.get(pageEntry.getKey() - 1).open();
                                })
                                .build();
                        pageEntry.getValue().setSlot(slot, element);
                    }
                }

                for (Integer slot : RAID_LIST_GUI.getSlotsBySymbol(RAID_LIST_GUI.closeItem.symbol)) {
                    ItemStack item = RAID_LIST_GUI.closeItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : RAID_LIST_GUI.closeItem.itemLore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(TextUtils.parse(RAID_LIST_GUI.closeItem.itemName)))
                            .setLore(lore)
                            .setCallback(clickType -> pageEntry.getValue().close())
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }

                for (Integer slot : RAID_LIST_GUI.getSlotsBySymbol(RAID_LIST_GUI.backgroundItem.symbol)) {
                    ItemStack item = RAID_LIST_GUI.backgroundItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : RAID_LIST_GUI.backgroundItem.itemLore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(TextUtils.parse(RAID_LIST_GUI.backgroundItem.itemName)))
                            .setLore(lore)
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }
            pages.get(1).open();
        }
        return 1;
    }

    private int queue(CommandContext<ServerCommandSource> ctx, int pageToOpen) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player != null) {
            if (nr.queuedRaids().isEmpty()) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.noQueuedRaids)));
                return 0;
            }

            Map<Integer, SimpleGui> pages = new HashMap<>();
            int pageTotal = GuiUtils.getPageTotal(nr.queuedRaids().size(), RAID_QUEUE_GUI.getTotalSlotsBySymbol(RAID_QUEUE_GUI.raidItem.symbol));
            for (int i = 1; i <= pageTotal; i++) {
                SimpleGui gui = new SimpleGui(RAID_QUEUE_GUI.getScreenHandler(), player, false);
                gui.setTitle(TextUtils.deserialize(TextUtils.parse(RAID_QUEUE_GUI.guiTitle)));
                pages.put(i, gui);
            }

            int index = 0;
            for (Map.Entry<Integer, SimpleGui> pageEntry : pages.entrySet()) {
                for (Integer slot : RAID_QUEUE_GUI.getSlotsBySymbol(RAID_QUEUE_GUI.raidItem.symbol)) {
                    if (index < nr.queuedRaids().size()) {
                        Boss boss = nr.queuedRaids().stream().toList().get(index).bossInfo();

                        List<Text> lore = new ArrayList<>();
                        if (Permissions.check(player, "novaraids.cancelqueue", 4)) {
                            for (String line : RAID_QUEUE_GUI.cancelableRaidItem.itemLore) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line, boss)));
                            }
                        } else {
                            for (String line : RAID_QUEUE_GUI.raidItem.itemLore) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line, boss)));
                            }
                        }

                        ItemStack item = PokemonItem.from(boss.pokemonDetails.createPokemon());
                        int finalIndex = index;
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_QUEUE_GUI.raidItem.itemName, boss)))
                                .setLore(lore)
                                .setCallback((num, clickType, slotActionType) -> {
                                    if (clickType.isRight) {
                                        if (Permissions.check(player, "novaraids.cancelqueue", 4)) {
                                            pageEntry.getValue().close();
                                            nr.queuedRaids().stream().toList().get(finalIndex).cancelItem();
                                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.queueItemCancelled, boss)));
                                            nr.queuedRaids().remove(nr.queuedRaids().stream().toList().get(finalIndex));
                                            queue(ctx, pageEntry.getKey());
                                        }
                                    }
                                }).build();
                        pageEntry.getValue().setSlot(slot, element);
                        index++;
                    } else {
                        ItemStack item = RAID_QUEUE_GUI.backgroundItem.item.copy();
                        List<Text> lore = new ArrayList<>();
                        for (String line : RAID_QUEUE_GUI.backgroundItem.itemLore) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                        }
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_QUEUE_GUI.backgroundItem.itemName)))
                                .setLore(lore)
                                .build();
                        pageEntry.getValue().setSlot(slot, element);
                    }
                }

                if (pageEntry.getKey() < pageTotal) {
                    for (Integer slot : RAID_QUEUE_GUI.getSlotsBySymbol(RAID_QUEUE_GUI.nextItem.symbol)) {
                        ItemStack item = RAID_QUEUE_GUI.nextItem.item.copy();
                        List<Text> lore = new ArrayList<>();
                        for (String line : RAID_QUEUE_GUI.nextItem.itemLore) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                        }
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_QUEUE_GUI.nextItem.itemName)))
                                .setLore(lore)
                                .setCallback(clickType -> {
                                    pageEntry.getValue().close();
                                    queue(ctx, pageEntry.getKey() + 1);
                                })
                                .build();
                        pageEntry.getValue().setSlot(slot, element);
                    }
                }

                if (pageEntry.getKey() > 1) {
                    for (Integer slot : RAID_QUEUE_GUI.getSlotsBySymbol(RAID_QUEUE_GUI.previousItem.symbol)) {
                        ItemStack item = RAID_QUEUE_GUI.previousItem.item.copy();
                        List<Text> lore = new ArrayList<>();
                        for (String line : RAID_QUEUE_GUI.previousItem.itemLore) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                        }
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_QUEUE_GUI.previousItem.itemName)))
                                .setLore(lore)
                                .setCallback(clickType -> {
                                    pageEntry.getValue().close();
                                    queue(ctx, pageEntry.getKey() - 1);
                                })
                                .build();
                        pageEntry.getValue().setSlot(slot, element);
                    }
                }

                for (Integer slot : RAID_QUEUE_GUI.getSlotsBySymbol(RAID_QUEUE_GUI.closeItem.symbol)) {
                    ItemStack item = RAID_QUEUE_GUI.closeItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : RAID_QUEUE_GUI.closeItem.itemLore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(TextUtils.parse(RAID_QUEUE_GUI.closeItem.itemName)))
                            .setLore(lore)
                            .setCallback(clickType -> pageEntry.getValue().close())
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }

                for (Integer slot : RAID_QUEUE_GUI.getSlotsBySymbol(RAID_QUEUE_GUI.backgroundItem.symbol)) {
                    ItemStack item = RAID_QUEUE_GUI.backgroundItem.item.copy();
                    List<Text> lore = new ArrayList<>();
                    for (String line : RAID_QUEUE_GUI.backgroundItem.itemLore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(TextUtils.parse(RAID_QUEUE_GUI.backgroundItem.itemName)))
                            .setLore(lore)
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }
            pages.get(pageToOpen).open();
            return 1;
        }
        return 0;
    }
}