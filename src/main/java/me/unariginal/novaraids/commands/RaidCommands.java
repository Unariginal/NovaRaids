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
import me.unariginal.novaraids.data.schedule.CronSchedule;
import me.unariginal.novaraids.data.schedule.RandomSchedule;
import me.unariginal.novaraids.data.schedule.Schedule;
import me.unariginal.novaraids.data.schedule.SpecificSchedule;
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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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
                                                        if (NovaRaids.LOADED) {
                                                            if (ctx.getSource().isExecutedByPlayer())
                                                                return start(nr.bossesConfig().getBoss(StringArgumentType.getString(ctx, "boss")), ctx.getSource().getPlayer(), null);
                                                            else
                                                                return start(nr.bossesConfig().getBoss(StringArgumentType.getString(ctx, "boss")), null, null);
                                                        } else {
                                                            return 0;
                                                        }
                                                    })
                                    )
                                    .then(
                                            CommandManager.literal("random")
                                                    .executes(ctx -> {
                                                        if (NovaRaids.LOADED) {
                                                            if (ctx.getSource().isExecutedByPlayer())
                                                                return start(nr.bossesConfig().getRandomBoss(), ctx.getSource().getPlayer(), null);
                                                            else
                                                                return start(nr.bossesConfig().getRandomBoss(), null, null);
                                                        } else {
                                                            return 0;
                                                        }
                                                    })
                                                    .then(
                                                            CommandManager.argument("category", StringArgumentType.string())
                                                                    .suggests(new CategorySuggestions())
                                                                    .executes(ctx -> {
                                                                        if (NovaRaids.LOADED) {
                                                                            String categoryStr = StringArgumentType.getString(ctx, "category");
                                                                            Boss boss = nr.bossesConfig().getRandomBoss(categoryStr);

                                                                            if (ctx.getSource().isExecutedByPlayer())
                                                                                return start(boss, ctx.getSource().getPlayer(), null);
                                                                            else
                                                                                return start(boss, null, null);
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
                                            CommandManager.argument("player", EntityArgumentType.players())
                                                    .then(
                                                            CommandManager.literal("pass")
                                                                    .then(
                                                                            CommandManager.argument("boss", StringArgumentType.string())
                                                                                    .suggests(new BossSuggestions())
                                                                                    .executes(ctx -> {
                                                                                        List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                        for (ServerPlayerEntity player : players) {
                                                                                            give(ctx.getSource().getPlayer(), player, "pass", StringArgumentType.getString(ctx, "boss"), null, "", 0);
                                                                                        }
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
                                                                                                        List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                                        for (ServerPlayerEntity player : players) {
                                                                                                            give(ctx.getSource().getPlayer(), player, "pass", "*", StringArgumentType.getString(ctx, "category"), "", 0);
                                                                                                        }
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
                                                                                                        List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                                        for (ServerPlayerEntity player : players) {
                                                                                                            give(ctx.getSource().getPlayer(), player, "pass", "random", StringArgumentType.getString(ctx, "category"), "", 0);
                                                                                                        }
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
                                                                                        List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                        for (ServerPlayerEntity player : players) {
                                                                                            give(ctx.getSource().getPlayer(), player, "voucher", StringArgumentType.getString(ctx, "boss"), null, "", 0);
                                                                                        }
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
                                                                                                        List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                                        for (ServerPlayerEntity player : players) {
                                                                                                            give(ctx.getSource().getPlayer(), player, "voucher", "*", StringArgumentType.getString(ctx, "category"), "", 0);
                                                                                                        }
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
                                                                                                        List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                                        for (ServerPlayerEntity player : players) {
                                                                                                            give(ctx.getSource().getPlayer(), player, "voucher", "random", StringArgumentType.getString(ctx, "category"), "", 0);
                                                                                                        }
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
                                                                                                                        if (NovaRaids.LOADED) {
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
                                                                                                                                    .executes(ctx -> {
                                                                                                                                        List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                                                                        for (ServerPlayerEntity player : players) {
                                                                                                                                            give(ctx.getSource().getPlayer(), player, "pokeball", StringArgumentType.getString(ctx, "boss"), null, StringArgumentType.getString(ctx, "pokeball"), IntegerArgumentType.getInteger(ctx, "amount"));
                                                                                                                                        }
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
                                                                                                                        if (NovaRaids.LOADED) {
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
                                                                                                                                    .executes(ctx -> {
                                                                                                                                        List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                                                                        for (ServerPlayerEntity player : players) {
                                                                                                                                            give(ctx.getSource().getPlayer(), player, "pokeball", null, StringArgumentType.getString(ctx, "category"), StringArgumentType.getString(ctx, "pokeball"), IntegerArgumentType.getInteger(ctx, "amount"));
                                                                                                                                        }
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
                                                                                                        if (NovaRaids.LOADED) {
                                                                                                            for (RaidBall ball : nr.config().raidBalls) {
                                                                                                                builder.suggest(ball.id());
                                                                                                            }
                                                                                                        }
                                                                                                        return builder.buildFuture();
                                                                                                    })
                                                                                                    .then(
                                                                                                            CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                                                                                    .executes(ctx -> {
                                                                                                                        List<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player").stream().toList();
                                                                                                                        for (ServerPlayerEntity player : players) {
                                                                                                                            give(ctx.getSource().getPlayer(), player, "pokeball", "*", null, StringArgumentType.getString(ctx, "pokeball"), IntegerArgumentType.getInteger(ctx, "amount"));
                                                                                                                        }
                                                                                                                        return 1;
                                                                                                                    })
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
                                                        if (NovaRaids.LOADED) {
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
                                        if (NovaRaids.LOADED) {
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
                                                                        if (NovaRaids.LOADED) {
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
                    .then(
                            CommandManager.literal("schedule")
                                    .requires(Permissions.require("novaraids.schedule", 4))
                                    .executes(ctx -> {
                                        for (Schedule schedule : nr.schedulesConfig().schedules) {
                                            if (schedule instanceof SpecificSchedule specificSchedule) {
                                                List<LocalTime> closestTimes = new ArrayList<>();

                                                for (int i = 0; i < specificSchedule.setTimes.size(); i++) {
                                                    LocalTime now = LocalTime.now(nr.schedulesConfig().zone);
                                                    LocalTime closestTime = null;
                                                    for (LocalTime time : specificSchedule.setTimes) {
                                                        if (time.isAfter(now) || time.equals(now)) {
                                                            if ((closestTime == null || time.isBefore(closestTime)) && !closestTimes.contains(time)) {
                                                                closestTime = time;
                                                            }
                                                        }
                                                    }

                                                    if (closestTime == null) {
                                                        now = LocalTime.of(0,0);
                                                        for (LocalTime time : specificSchedule.setTimes) {
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
                                                ctx.getSource().sendMessage(TextUtils.deserialize("<red>Random Schedule (<i>" + randomSchedule.minBound + "s - " + randomSchedule.maxBound + "s<!i>) Next Raid:"));
                                                ctx.getSource().sendMessage(TextUtils.deserialize("<gray><i> - " + randomSchedule.nextRandom.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withZone(nr.schedulesConfig().zone))));
                                            } else if (schedule instanceof CronSchedule cronSchedule) {
                                                ctx.getSource().sendMessage(TextUtils.deserialize("<red>Cron Schedule (<i>" + cronSchedule.expression + "<!i>) Next Raid:"));
                                                ctx.getSource().sendMessage(TextUtils.deserialize("<gray><i> - " + cronSchedule.nextExecution.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withZone(nr.schedulesConfig().zone))));
                                            }
                                            ctx.getSource().sendMessage(TextUtils.deserialize(""));
                                        }
                                        return 1;
                                    })
                    )
        ));
    }

    private int reload(CommandContext<ServerCommandSource> ctx) {
        nr.reloadConfig();
        if (NovaRaids.LOADED)
            ctx.getSource().sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("reload_command"))));
        return 1;
    }

    private int modInfo(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player != null) {
            player.sendMessage(TextUtils.deserialize("<gray><st><b><i>---------<reset><red> Nova Raids <reset><gray><st><b><i>---------"));
            player.sendMessage(TextUtils.deserialize("<gray><b>Author: <reset><white>Unariginal <i>(Ariginal)"));
            player.sendMessage(TextUtils.deserialize("<gray><b>Version: <reset><white>Beta v0.3.4"));
            player.sendMessage(TextUtils.deserialize("<gray><b><i><u><click:open_url:https://github.com/Unariginal/NovaRaids>Source"));
            player.sendMessage(TextUtils.deserialize("<gray><b><i><u><click:open_url:https://github.com/Unariginal/NovaRaids/wiki>Wiki"));
            player.sendMessage(TextUtils.deserialize("<gray><st><b><i>---------------------------"));
        }
        return 1;
    }

    private int checkbanned(CommandContext<ServerCommandSource> ctx, String type) {
        if (NovaRaids.LOADED) {
            if (ctx.getSource().isExecutedByPlayer()) {
                ServerPlayerEntity player = ctx.getSource().getPlayer();
                if (player != null) {
                    ContrabandGui gui;
                    Boss boss = null;
                    Category category = null;
                    switch (type) {
                        case "global" -> gui = nr.guisConfig().globalContrabandGui;
                        case "category" -> {
                            String categoryID = StringArgumentType.getString(ctx, "category");
                            category = nr.bossesConfig().getCategory(categoryID);
                            if (category != null) {
                                gui = nr.guisConfig().categoryContrabandGui;
                            } else {
                                return 0;
                            }
                        }
                        case "boss" -> {
                            String bossID = StringArgumentType.getString(ctx, "boss");
                            boss = nr.bossesConfig().getBoss(bossID);
                            if (boss != null) {
                                category = nr.bossesConfig().getCategory(boss.categoryId());
                                gui = nr.guisConfig().bossContrabandGui;
                            } else {
                                return 0;
                            }
                        }
                        default -> {
                            return 0;
                        }
                    }
                    String title = gui.title;
                    String pokemonItemName = gui.bannedPokemonButton.itemName();
                    List<String> pokemonItemLore = gui.bannedPokemonButton.itemLore();
                    String moveItemName = gui.bannedMovesButton.itemName();
                    List<String> moveItemLore = gui.bannedMovesButton.itemLore();
                    String abilityItemName = gui.bannedAbilitiesButton.itemName();
                    List<String> abilityItemLore = gui.bannedAbilitiesButton.itemLore();
                    String heldItemName = gui.bannedHeldItemsButton.itemName();
                    List<String> heldItemLore = gui.bannedHeldItemsButton.itemLore();
                    String bagItemName = gui.bannedBagItemsButton.itemName();
                    List<String> bagItemLore = gui.bannedBagItemsButton.itemLore();
                    String backgroundItemName = gui.backgroundButton.itemName();
                    List<String> backgroundItemLore = gui.backgroundButton.itemLore();
                    String closeItemName = gui.closeButton.itemName();
                    List<String> closeItemLore = gui.closeButton.itemLore();

                    if (category != null) {
                        title = gui.title.replaceAll("%category%", category.name());
                        pokemonItemName = gui.bannedPokemonButton.itemName().replaceAll("%category%", category.name());
                        List<String> lore = new ArrayList<>();
                        for (String line : pokemonItemLore) {
                            lore.add(line.replaceAll("%category%", category.name()));
                        }
                        pokemonItemLore = lore;

                        moveItemName = gui.bannedMovesButton.itemName().replaceAll("%category%", category.name());
                        lore = new ArrayList<>();
                        for (String line : moveItemLore) {
                            lore.add(line.replaceAll("%category%", category.name()));
                        }
                        moveItemLore = lore;

                        abilityItemName = gui.bannedAbilitiesButton.itemName().replaceAll("%category%", category.name());
                        lore = new ArrayList<>();
                        for (String line : abilityItemLore) {
                            lore.add(line.replaceAll("%category%", category.name()));
                        }
                        abilityItemLore = lore;

                        heldItemName = gui.bannedHeldItemsButton.itemName().replaceAll("%category%", category.name());
                        lore = new ArrayList<>();
                        for (String line : heldItemLore) {
                            lore.add(line.replaceAll("%category%", category.name()));
                        }
                        heldItemLore = lore;

                        bagItemName = gui.bannedBagItemsButton.itemName().replaceAll("%category%", category.name());
                        lore = new ArrayList<>();
                        for (String line : bagItemLore) {
                            lore.add(line.replaceAll("%category%", category.name()));
                        }
                        bagItemLore = lore;

                        backgroundItemName = gui.backgroundButton.itemName().replaceAll("%category%", category.name());
                        lore = new ArrayList<>();
                        for (String line : backgroundItemLore) {
                            lore.add(line.replaceAll("%category%", category.name()));
                        }
                        backgroundItemLore = lore;

                        closeItemName = gui.closeButton.itemName().replaceAll("%category%", category.name());
                        lore = new ArrayList<>();
                        for (String line : closeItemLore) {
                            lore.add(line.replaceAll("%category%", category.name()));
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
                    if (gui.useHopperGui) {
                        mainGui = new SimpleGui(ScreenHandlerType.HOPPER, player, false);
                    } else {
                        mainGui = new SimpleGui(GuiUtils.getScreenSize(gui.rows), player, false);
                    }
                    mainGui.setTitle(TextUtils.deserialize(title));

                    List<Text> lore = new ArrayList<>();
                    for (String line : pokemonItemLore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    for (Integer slot : gui.pokemonSlots()) {
                        Item item = Registries.ITEM.get(Identifier.of(gui.bannedPokemonButton.item()));
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
                    for (Integer slot : gui.moveSlots()) {
                        Item item = Registries.ITEM.get(Identifier.of(gui.bannedMovesButton.item()));
                        Boss finalBoss1 = boss;
                        Category finalCategory1 = category;
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(moveItemName)))
                                .setLore(lore)
                                .setCallback(clickType -> {
                                    mainGui.close();
                                    openContrabandGui(ctx, player, gui.bannedMoves, "move", 1, finalBoss1, finalCategory1);
                                })
                                .build();
                        mainGui.setSlot(slot, element);
                    }

                    lore = new ArrayList<>();
                    for (String line : abilityItemLore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    for (Integer slot : gui.abilitySlots()) {
                        Item item = Registries.ITEM.get(Identifier.of(gui.bannedAbilitiesButton.item()));
                        Boss finalBoss2 = boss;
                        Category finalCategory2 = category;
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(abilityItemName)))
                                .setLore(lore)
                                .setCallback(clickType -> {
                                    mainGui.close();
                                    openContrabandGui(ctx, player, gui.bannedAbilities, "ability", 1, finalBoss2, finalCategory2);
                                })
                                .build();
                        mainGui.setSlot(slot, element);
                    }

                    lore = new ArrayList<>();
                    for (String line : heldItemLore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    for (Integer slot : gui.heldItemSlots()) {
                        Item item = Registries.ITEM.get(Identifier.of(gui.bannedHeldItemsButton.item()));
                        Boss finalBoss3 = boss;
                        Category finalCategory3 = category;
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(heldItemName)))
                                .setLore(lore)
                                .setCallback(clickType -> {
                                    mainGui.close();
                                    openContrabandGui(ctx, player, gui.bannedHeldItems, "held_item", 1, finalBoss3, finalCategory3);
                                })
                                .build();
                        mainGui.setSlot(slot, element);
                    }

                    lore = new ArrayList<>();
                    for (String line : bagItemLore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    for (Integer slot : gui.bagItemSlots()) {
                        Item item = Registries.ITEM.get(Identifier.of(gui.bannedBagItemsButton.item()));
                        Boss finalBoss4 = boss;
                        Category finalCategory4 = category;
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(bagItemName)))
                                .setLore(lore)
                                .setCallback(clickType -> {
                                    mainGui.close();
                                    openContrabandGui(ctx, player, gui.bannedBagItems, "bag_item", 1, finalBoss4, finalCategory4);
                                })
                                .build();
                        mainGui.setSlot(slot, element);
                    }

                    lore = new ArrayList<>();
                    for (String line : backgroundItemLore) {
                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                    }
                    for (Integer slot : gui.backgroundButtonSlots()) {
                        Item item = Registries.ITEM.get(Identifier.of(gui.backgroundButton.item()));
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
                    for (Integer slot : gui.closeButtonSlots()) {
                        Item item = Registries.ITEM.get(Identifier.of(gui.closeButton.item()));
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
        }
        return 1;
    }

    private void openContrabandGui(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, DisplayItemGui gui, String type, int pageToOpen, Boss boss, Category category) {
        Map<ItemStack, String> displayItems = new HashMap<>();
        if (type.equalsIgnoreCase("pokemon")) {
            if (boss != null && category != null) {
                for (Species species : boss.raidDetails().contraband().bannedPokemon()) {
                    displayItems.put(PokemonItem.from(species), TextUtils.parse(gui.displayButton.itemName().replaceAll("%pokemon%", species.getName()).replaceAll("%category%", category.name()), boss));
                }
            } else if (category != null) {
                for (Species species : category.contraband().bannedPokemon()) {
                    displayItems.put(PokemonItem.from(species), TextUtils.parse(gui.displayButton.itemName().replaceAll("%pokemon%", species.getName()).replaceAll("%category%", category.name())));
                }
            } else {
                for (Species species : nr.config().globalContraband.bannedPokemon()) {
                    displayItems.put(PokemonItem.from(species), TextUtils.parse(gui.displayButton.itemName().replaceAll("%pokemon%", species.getName())));
                }
            }
        } else if (type.equalsIgnoreCase("move")) {
            if (boss != null && category != null) {
                for (Move move : boss.raidDetails().contraband().bannedMoves()) {
                    displayItems.put(Registries.ITEM.get(Identifier.of(gui.displayButton.item())).getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%move%", move.getDisplayName().getString()).replaceAll("%category%", category.name()), boss));
                }
            } else if (category != null) {
                for (Move move : category.contraband().bannedMoves()) {
                    displayItems.put(Registries.ITEM.get(Identifier.of(gui.displayButton.item())).getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%move%", move.getDisplayName().getString()).replaceAll("%category%", category.name())));
                }
            } else {
                for (Move move : nr.config().globalContraband.bannedMoves()) {
                    displayItems.put(Registries.ITEM.get(Identifier.of(gui.displayButton.item())).getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%move%", move.getDisplayName().getString())));
                }
            }
        } else if (type.equalsIgnoreCase("ability")) {
            if (boss != null && category != null) {
                for (Ability ability : boss.raidDetails().contraband().bannedAbilities()) {
                    displayItems.put(Registries.ITEM.get(Identifier.of(gui.displayButton.item())).getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%ability%", MiscUtilsKt.asTranslated(ability.getDisplayName()).getString()).replaceAll("%category%", category.name()), boss));
                }
            } else if (category != null) {
                for (Ability ability : category.contraband().bannedAbilities()) {
                    displayItems.put(Registries.ITEM.get(Identifier.of(gui.displayButton.item())).getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%ability%", MiscUtilsKt.asTranslated(ability.getDisplayName()).getString()).replaceAll("%category%", category.name())));
                }
            } else {
                for (Ability ability : nr.config().globalContraband.bannedAbilities()) {
                    displayItems.put(Registries.ITEM.get(Identifier.of(gui.displayButton.item())).getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%ability%", MiscUtilsKt.asTranslated(ability.getDisplayName()).getString())));
                }
            }
        } else if (type.equalsIgnoreCase("held_item")) {
            if (boss != null && category != null) {
                for (Item item : boss.raidDetails().contraband().bannedHeldItems()) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.name()), boss));
                }
            } else if (category != null) {
                for (Item item : category.contraband().bannedHeldItems()) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.name())));
                }
            } else {
                for (Item item : nr.config().globalContraband.bannedHeldItems()) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%item%", item.getName().getString())));
                }
            }
        } else if (type.equalsIgnoreCase("bag_item")) {
            if (boss != null && category != null) {
                for (Item item : boss.raidDetails().contraband().bannedBagItems()) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.name()), boss));
                }
            } else if (category != null) {
                for (Item item : category.contraband().bannedBagItems()) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%item%", item.getName().getString()).replaceAll("%category%", category.name())));
                }
            } else {
                for (Item item : nr.config().globalContraband.bannedBagItems()) {
                    displayItems.put(item.getDefaultStack(), TextUtils.parse(gui.displayButton.itemName().replaceAll("%item%", item.getName().getString())));
                }
            }
        }

        Map<Integer, SimpleGui> pages = new HashMap<>();
        int pageTotal = GuiUtils.getPageTotal(displayItems.size(), gui.displaySlotTotal());
        for (int i = 1; i <= pageTotal; i++) {
            SimpleGui mainGui = new SimpleGui(GuiUtils.getScreenSize(gui.rows), player, false);
            String title = TextUtils.parse(gui.title);
            if (category != null) {
                title = title.replaceAll("%category%", category.name());
            }
            if (boss != null) {
                title = TextUtils.parse(title, boss);
            }
            mainGui.setTitle(TextUtils.deserialize(title));
            pages.put(i, mainGui);
        }

        int index = 0;
        for (Map.Entry<Integer, SimpleGui> pageEntry : pages.entrySet()) {
            for (Integer slot : gui.displaySlots()) {
                if (index < displayItems.size()) {
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

                    ItemStack item = displayItems.keySet().stream().toList().get(index);
                    item.applyChanges(gui.displayButton.itemData());
                    GuiElement element = new GuiElementBuilder(item)
                            .setName(TextUtils.deserialize(displayItems.values().stream().toList().get(index)))
                            .setLore(lore)
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
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
                    pageEntry.getValue().setSlot(slot, element);
                }
            }

            if (pageEntry.getKey() < pageTotal) {
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
                                pageEntry.getValue().close();
                                openContrabandGui(ctx, player, gui, type, pageEntry.getKey() + 1, boss, category);
                            })
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
                }
            }

            if (pageEntry.getKey() > 1) {
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
                                pageEntry.getValue().close();
                                openContrabandGui(ctx, player, gui, type, pageEntry.getKey() - 1, boss, category);
                            })
                            .build();
                    pageEntry.getValue().setSlot(slot, element);
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
                pageEntry.getValue().setSlot(slot, element);
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
                pageEntry.getValue().setSlot(slot, element);
            }
        }
        if (!pages.isEmpty()) {
            pages.get(pageToOpen).open();
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
        int totalPlayers = IntegerArgumentType.getInteger(ctx, "total-players");

        Boss bossInfo = nr.bossesConfig().getBoss(boss);
        Category raidBossCategory = nr.bossesConfig().getCategory(bossInfo.categoryId());

        List<DistributionSection> categoryRewards = new ArrayList<>(raidBossCategory.rewards());
        List<DistributionSection> bossRewards = new ArrayList<>(bossInfo.raidDetails().rewards());

        List<DistributionSection> rewards = new ArrayList<>(bossRewards);

        if (!bossInfo.raidDetails().overrideCategoryDistribution()) {
            List<Place> overriddenPlacements = new ArrayList<>();

            for (DistributionSection bossReward : bossRewards) {
                List<Place> places = bossReward.places();
                for (Place place : places) {
                    if (place.overrideCategoryReward()) {
                        overriddenPlacements.add(place);
                    }
                }
            }

            for (DistributionSection categoryReward : categoryRewards) {
                boolean overridden = false;
                List<Place> places = categoryReward.places();
                outer:
                for (Place place : places) {
                    for (Place overriddenPlacement : overriddenPlacements) {
                        if (overriddenPlacement.place().equalsIgnoreCase(place.place())) {
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
        for (DistributionSection reward : rewards) {
            List<Place> places = reward.places();
            for (Place place : places) {
                List<ServerPlayerEntity> playersToReward = new ArrayList<>();
                if (StringUtils.isNumeric(place.place())) {
                    int placeInt = Integer.parseInt(place.place());
                    if (placement == placeInt) {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player != null) {
                            playersToReward.add(player);
                        }
                    }
                } else if (place.place().contains("%")) {
                    String percentStr = place.place().replace("%", "");
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
                } else if (place.place().equalsIgnoreCase("participating")) {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player != null) {
                        playersToReward.add(player);
                    }
                }

                for (ServerPlayerEntity player : playersToReward) {
                    if (player != null) {
                        boolean duplicatePlacementExists = false;
                        int placeCount = 0;
                        for (DistributionSection rewardSection : rewards) {
                            List<Place> rewardPlaces = rewardSection.places();
                            for (Place rewardPlace : rewardPlaces) {
                                if (rewardPlace.place().equalsIgnoreCase(place.place())) {
                                    placeCount++;
                                    break;
                                }
                            }
                            if (placeCount >= 2) {
                                duplicatePlacementExists = true;
                                break;
                            }
                        }

                        if (!noMoreRewards.containsKey(player) || (duplicatePlacementExists && place.place().equalsIgnoreCase(noMoreRewards.get(player)))) {
                            int rolls = new Random().nextInt(reward.minRolls(), reward.maxRolls() + 1);
                            List<UUID> distributedPools = new ArrayList<>();
                            for (int i = 0; i < rolls; i++) {
                                Map.Entry<?, Double> poolEntry = RandomUtils.getRandomEntry(reward.pools());
                                if (poolEntry != null) {
                                    RewardPool pool = (RewardPool) poolEntry.getKey();
                                    if (reward.allowDuplicates() || !distributedPools.contains(pool.uuid())) {
                                        pool.distributeRewards(player);
                                        distributedPools.add(pool.uuid());
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
                    if (!place.allowOtherRewards() && !noMoreRewards.containsKey(player)) {
                        noMoreRewards.put(player, place.place());
                    }
                }
            }
        }

        return 1;
    }

    private int skipphase(CommandContext<ServerCommandSource> ctx) {
        if (NovaRaids.LOADED) {
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

    public int start(Boss bossInfo, ServerPlayerEntity player, ItemStack startingItem) {
        if (NovaRaids.LOADED) {
            if (!nr.server().getPlayerManager().getPlayerList().isEmpty() || nr.config().runRaidsWithNoPlayers) {
                if (bossInfo != null) {
                    Map<String, Double> spawnLocations = bossInfo.spawnLocations();
                    Map<String, Double> validLocations = new HashMap<>();

                    for (String key : spawnLocations.keySet()) {
                        boolean validSpawn = true;
                        for (Raid raid : nr.activeRaids().values()) {
                            if (raid.raidBossLocation().id().equalsIgnoreCase(key)) {
                                validSpawn = false;
                                break;
                            }
                        }
                        if (validSpawn) {
                            validLocations.put(key, spawnLocations.get(key));
                        }
                    }

                    String spawnLocationString;
                    Location spawnLocation;
                    if (!validLocations.isEmpty()) {
                        Map.Entry<?, Double> spawnLocationEntry = RandomUtils.getRandomEntry(validLocations);
                        if (spawnLocationEntry != null) {
                            spawnLocationString = (String) spawnLocationEntry.getKey();
                            spawnLocation = nr.locationsConfig().getLocation(spawnLocationString);
                        } else {
                            nr.logError("Location could not be found.");
                            return 0;
                        }
                    } else {
                        nr.logInfo("No valid spawn locations found. All possible locations are busy.");
                        if (player != null) {
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("no_available_locations"), bossInfo)));
                        }
                        return 0;
                    }

                    if (spawnLocation != null) {
                        if (!nr.config().useQueueSystem) {
                            nr.logInfo("Starting raid.");
                            nr.addRaid(new Raid(bossInfo, spawnLocation, (player != null) ? player.getUuid() : null, startingItem));
                        } else {
                            nr.logInfo("Adding queue raid.");
                            nr.addQueueItem(new QueueItem(UUID.randomUUID(), bossInfo, spawnLocation, (player != null) ? player.getUuid() : null, startingItem));

                            if (nr.activeRaids().isEmpty()) {
                                nr.initNextRaid();
                            } else {
                                if (player != null) {
                                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("added_to_queue"), bossInfo)));
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
        }
        return 0;
    }

    private int stop(CommandContext<ServerCommandSource> ctx) {
        if (NovaRaids.LOADED) {
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

    public int give(ServerPlayerEntity sourcePlayer, ServerPlayerEntity targetPlayer, String itemType, String bossName, String category, String key, int amount) {
        if (NovaRaids.LOADED) {
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
                        pass = nr.config().globalPass;
                    } else {
                        categoryID = category;
                        Category cat = nr.bossesConfig().getCategory(categoryID);
                        if (cat == null) {
                            sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_category").replaceAll("%category%", category), sourcePlayer, targetPlayer, amount, itemType)));
                            return 0;
                        }
                        pass = cat.categoryPass();
                    }
                    itemName = TextUtils.deserialize(TextUtils.parse(pass.passName(), sourcePlayer, targetPlayer, amount, itemType));
                    List<Text> loreText = new ArrayList<>();
                    for (String loreLine : pass.passLore()) {
                        loreText.add(TextUtils.deserialize(TextUtils.parse(loreLine, sourcePlayer, targetPlayer, amount, itemType)));
                    }
                    lore = new LoreComponent(loreText);
                } else if (bossName.equalsIgnoreCase("random")) {
                    Boss boss;
                    if (category == null) {
                        categoryID = "null";
                        boss = nr.bossesConfig().getRandomBoss();
                    } else {
                        categoryID = category;
                        Category cat = nr.bossesConfig().getCategory(categoryID);
                        if (cat == null) {
                            sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_category").replaceAll("%category%", category), sourcePlayer, targetPlayer, amount, itemType)));
                            return 0;
                        }
                        boss = nr.bossesConfig().getRandomBoss(categoryID);
                    }
                    bossID = boss.bossId();
                    pass = boss.itemSettings().pass();
                    itemName = TextUtils.deserialize(TextUtils.parse(TextUtils.parse(pass.passName(), boss), sourcePlayer, targetPlayer, amount, itemType));
                    List<Text> loreText = new ArrayList<>();
                    for (String loreLine : pass.passLore()) {
                        loreText.add(TextUtils.deserialize(TextUtils.parse(TextUtils.parse(loreLine, boss), sourcePlayer, targetPlayer, amount, itemType)));
                    }
                    lore = new LoreComponent(loreText);
                } else {
                    categoryID = "null";
                    bossID = bossName;
                    Boss boss = nr.bossesConfig().getBoss(bossName);
                    if (boss == null) {
                        sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_boss").replaceAll("%boss%", bossName), sourcePlayer, targetPlayer, amount, itemType)));
                        return 0;
                    }
                    pass = boss.itemSettings().pass();
                    itemName = TextUtils.deserialize(TextUtils.parse(TextUtils.parse(pass.passName(), boss), sourcePlayer, targetPlayer, amount, itemType));
                    List<Text> loreText = new ArrayList<>();
                    for (String loreLine : pass.passLore()) {
                        loreText.add(TextUtils.deserialize(TextUtils.parse(TextUtils.parse(loreLine, boss), sourcePlayer, targetPlayer, amount, itemType)));
                    }
                    lore = new LoreComponent(loreText);
                }

                itemToGive = new ItemStack(pass.passItem(), 1);
                if (pass.passData() != null) {
                    itemToGive.applyChanges(pass.passData());
                }
                itemToGive.applyComponentsFrom(ComponentMap.builder().add(DataComponentTypes.MAX_STACK_SIZE, 1).build());
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
                        voucher = nr.config().globalChoiceVoucher;
                    } else {
                        categoryID = category;
                        Category cat = nr.bossesConfig().getCategory(categoryID);
                        if (cat == null) {
                            sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_category").replaceAll("%category%", category), sourcePlayer, targetPlayer, amount, itemType)));
                            return 0;
                        }
                        voucher = cat.categoryChoiceVoucher();
                    }
                    itemName = TextUtils.deserialize(TextUtils.parse(voucher.voucherName(), sourcePlayer, targetPlayer, amount, itemType));
                    List<Text> loreText = new ArrayList<>();
                    for (String loreLine : voucher.voucherLore()) {
                        loreText.add(TextUtils.deserialize(TextUtils.parse(loreLine, sourcePlayer, targetPlayer, amount, itemType)));
                    }
                    lore = new LoreComponent(loreText);
                } else if (bossName.equalsIgnoreCase("random")) {
                    bossID = "random";
                    if (category == null) {
                        categoryID = "null";
                        voucher = nr.config().globalRandomVoucher;
                    } else {
                        categoryID = category;
                        Category cat = nr.bossesConfig().getCategory(categoryID);
                        if (cat == null) {
                            sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_category").replaceAll("%category%", category), sourcePlayer, targetPlayer, amount, itemType)));
                            return 0;
                        }
                        voucher = cat.categoryRandomVoucher();
                    }
                    itemName = TextUtils.deserialize(TextUtils.parse(voucher.voucherName(), sourcePlayer, targetPlayer, amount, itemType));
                    List<Text> loreText = new ArrayList<>();
                    for (String loreLine : voucher.voucherLore()) {
                        loreText.add(TextUtils.deserialize(TextUtils.parse(loreLine, sourcePlayer, targetPlayer, amount, itemType)));
                    }
                    lore = new LoreComponent(loreText);
                } else {
                    categoryID = "null";
                    bossID = bossName;
                    Boss boss = nr.bossesConfig().getBoss(bossName);
                    if (boss == null) {
                        sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_boss").replaceAll("%boss%", bossName), sourcePlayer, targetPlayer, amount, itemType)));
                        return 0;
                    }
                    voucher = boss.itemSettings().voucher();
                    itemName = TextUtils.deserialize(TextUtils.parse(TextUtils.parse(voucher.voucherName(), boss), sourcePlayer, targetPlayer, amount, itemType));
                    List<Text> loreText = new ArrayList<>();
                    for (String loreLine : voucher.voucherLore()) {
                        loreText.add(TextUtils.deserialize(TextUtils.parse(TextUtils.parse(loreLine, boss), sourcePlayer, targetPlayer, amount, itemType)));
                    }
                    lore = new LoreComponent(loreText);
                }

                itemToGive = new ItemStack(voucher.voucherItem(), 1);
                if (voucher.voucherData() != null) {
                    itemToGive.applyChanges(voucher.voucherData());
                }
                itemToGive.applyComponentsFrom(ComponentMap.builder().add(DataComponentTypes.MAX_STACK_SIZE, 1).build());
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
                        raidPokeball = nr.config().getRaidBall(key);
                        if (raidPokeball == null) {
                            if (sourcePlayer != null) {
                                sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_pokeball").replaceAll("%pokeball%", key), sourcePlayer, targetPlayer, amount, itemType)));
                            }
                            return 0;
                        }
                        itemName = TextUtils.deserialize(TextUtils.parse(raidPokeball.ballName(), sourcePlayer, targetPlayer, amount, itemType));
                        List<Text> loreText = new ArrayList<>();
                        for (String loreLine : raidPokeball.ballLore()) {
                            loreText.add(TextUtils.deserialize(TextUtils.parse(loreLine, sourcePlayer, targetPlayer, amount, itemType)));
                        }
                        lore = new LoreComponent(loreText);
                    } else {
                        categoryID = "null";
                        Boss boss = nr.bossesConfig().getBoss(bossName);
                        if (boss == null) {
                            nr.logInfo("The boss was null");
                            sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_boss").replaceAll("%boss%", bossName), sourcePlayer, targetPlayer, amount, itemType)));
                            return 0;
                        }
                        bossID = bossName;
                        raidPokeball = boss.itemSettings().getRaidBall(key);
                        if (raidPokeball == null) {
                            if (sourcePlayer != null) {
                                sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_pokeball").replaceAll("%pokeball%", key), sourcePlayer, targetPlayer, amount, itemType)));
                            }
                            return 0;
                        }
                        itemName = TextUtils.deserialize(TextUtils.parse(TextUtils.parse(raidPokeball.ballName(), boss), sourcePlayer, targetPlayer, amount, itemType));
                        List<Text> loreText = new ArrayList<>();
                        for (String loreLine : raidPokeball.ballLore()) {
                            loreText.add(TextUtils.deserialize(TextUtils.parse(TextUtils.parse(loreLine, boss), sourcePlayer, targetPlayer, amount, itemType)));
                        }
                        lore = new LoreComponent(loreText);
                    }
                } else {
                    if (category != null) {
                        bossID = "*";
                        Category cat = nr.bossesConfig().getCategory(category);
                        if (cat == null) {
                            nr.logInfo("Category was null");
                            sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_category").replaceAll("%category%", category), sourcePlayer, targetPlayer, amount, itemType)));
                            return 0;
                        }
                        categoryID = category;
                        raidPokeball = cat.getRaidBall(key);
                        if (raidPokeball == null) {
                            if (sourcePlayer != null) {
                                sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_pokeball").replaceAll("%pokeball%", key), sourcePlayer, targetPlayer, amount, itemType)));
                            }
                            return 0;
                        }
                        itemName = TextUtils.deserialize(TextUtils.parse(raidPokeball.ballName(), sourcePlayer, targetPlayer, amount, itemType));
                        List<Text> loreText = new ArrayList<>();
                        for (String loreLine : raidPokeball.ballLore()) {
                            loreText.add(TextUtils.deserialize(TextUtils.parse(loreLine, sourcePlayer, targetPlayer, amount, itemType)));
                        }
                        lore = new LoreComponent(loreText);
                    } else {
                        sourcePlayer.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("give_command_invalid_category").replaceAll("%category%", "null"), sourcePlayer, targetPlayer, amount, itemType)));
                        return 0;
                    }
                }

                itemToGive = new ItemStack(raidPokeball.ballItem(), amount);
                customData.putString("raid_item", "raid_ball");
                customData.putUuid("owner_uuid", targetPlayer.getUuid());
                customData.putString("raid_boss", bossID);
                customData.putString("raid_category", categoryID);
                if (raidPokeball.ballData() != null) {
                    itemToGive.applyChanges(raidPokeball.ballData());
                }
            }

            itemToGive.applyComponentsFrom(componentBuilder
                    .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData))
                    .add(DataComponentTypes.CUSTOM_NAME, itemName)
                    .add(DataComponentTypes.LORE, lore)
                    .build());
            if (!targetPlayer.giveItemStack(itemToGive)) {
                if (sourcePlayer != null) {
                    sourcePlayer.sendMessage(
                            TextUtils.deserialize(
                                    TextUtils.parse(nr.messagesConfig().getMessage("give_command_failed_to_give"), sourcePlayer, targetPlayer, amount, itemType)
                            )
                    );
                    return 0;
                }
            } else {
                targetPlayer.sendMessage(
                        TextUtils.deserialize(
                            TextUtils.parse(nr.messagesConfig().getMessage("give_command_received_item"), sourcePlayer, targetPlayer, amount, itemType)
                        )
                );
                if (sourcePlayer != null) {
                    sourcePlayer.sendMessage(
                            TextUtils.deserialize(
                                    TextUtils.parse(nr.messagesConfig().getMessage("give_command_feedback"), sourcePlayer, targetPlayer, amount, itemType)
                            )
                    );
                }
            }
        }
        return 1;
    }

    private int list(CommandContext<ServerCommandSource> ctx) {
        if (NovaRaids.LOADED) {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player != null) {
                if (nr.activeRaids().isEmpty()) {
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("no_active_raids"))));
                    return 0;
                }

                Map<Integer, SimpleGui> pages = new HashMap<>();
                int pageTotal = GuiUtils.getPageTotal(nr.activeRaids().size(), nr.guisConfig().raidListGui.displaySlotTotal());
                for (int i = 1; i <= pageTotal; i++) {
                    SimpleGui gui = new SimpleGui(GuiUtils.getScreenSize(nr.guisConfig().raidListGui.rows), player, false);
                    gui.setTitle(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().raidListGui.title)));
                    pages.put(i, gui);
                }

                int index = 0;
                for (Map.Entry<Integer, SimpleGui> pageEntry : pages.entrySet()) {
                    for (Integer slot : nr.guisConfig().raidListGui.displaySlots()) {
                        if (nr.activeRaids().containsKey(index + 1)) {
                            Raid raid = nr.activeRaids().get(index + 1);
                            List<Text> lore = new ArrayList<>();

                            if (!raid.raidBossCategory().requirePass() && raid.stage() == 1) {
                                for (String line : nr.guisConfig().raidListGui.joinableLore) {
                                    lore.add(TextUtils.deserialize(TextUtils.parse(line, raid)));
                                }
                            } else if (raid.stage() != 1) {
                                for (String line : nr.guisConfig().raidListGui.inProgressLore) {
                                    lore.add(TextUtils.deserialize(TextUtils.parse(line, raid)));
                                }
                            } else {
                                for (String line : nr.guisConfig().raidListGui.requiresPassLore) {
                                    lore.add(TextUtils.deserialize(TextUtils.parse(line, raid)));
                                }
                            }

                            ItemStack item = PokemonItem.from(raid.raidBossPokemon());
                            item.applyChanges(nr.guisConfig().raidListGui.displayData);
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().raidListGui.displayName, raid)))
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
                                            pageEntry.getValue().close();
                                        }
                                    }).build();
                            pageEntry.getValue().setSlot(slot, element);
                            index++;
                        } else {
                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().raidListGui.backgroundButton.item())));
                            item.applyChanges(nr.guisConfig().raidListGui.backgroundButton.itemData());
                            List<Text> lore = new ArrayList<>();
                            for (String line : nr.guisConfig().raidListGui.backgroundButton.itemLore()) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                            }
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().raidListGui.backgroundButton.itemName())))
                                    .setLore(lore)
                                    .build();
                            pageEntry.getValue().setSlot(slot, element);
                        }
                    }

                    if (pageEntry.getKey() < pageTotal) {
                        for (Integer slot : nr.guisConfig().raidListGui.nextButtonSlots()) {
                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().raidListGui.nextButton.item())));
                            item.applyChanges(nr.guisConfig().raidListGui.nextButton.itemData());
                            List<Text> lore = new ArrayList<>();
                            for (String line : nr.guisConfig().raidListGui.nextButton.itemLore()) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                            }
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().raidListGui.nextButton.itemName())))
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
                        for (Integer slot : nr.guisConfig().raidListGui.previousButtonSlots()) {
                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().raidListGui.previousButton.item())));
                            item.applyChanges(nr.guisConfig().raidListGui.previousButton.itemData());
                            List<Text> lore = new ArrayList<>();
                            for (String line : nr.guisConfig().raidListGui.previousButton.itemLore()) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                            }
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().raidListGui.previousButton.itemName())))
                                    .setLore(lore)
                                    .setCallback(clickType -> {
                                        pageEntry.getValue().close();
                                        pages.get(pageEntry.getKey() - 1).open();
                                    })
                                    .build();
                            pageEntry.getValue().setSlot(slot, element);
                        }
                    }

                    for (Integer slot : nr.guisConfig().raidListGui.closeButtonSlots()) {
                        ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().raidListGui.closeButton.item())));
                        item.applyChanges(nr.guisConfig().raidListGui.closeButton.itemData());
                        List<Text> lore = new ArrayList<>();
                        for (String line : nr.guisConfig().raidListGui.closeButton.itemLore()) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                        }
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().raidListGui.closeButton.itemName())))
                                .setLore(lore)
                                .setCallback(clickType -> pageEntry.getValue().close())
                                .build();
                        pageEntry.getValue().setSlot(slot, element);
                    }

                    for (Integer slot : nr.guisConfig().raidListGui.backgroundButtonSlots()) {
                        ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().raidListGui.backgroundButton.item())));
                        item.applyChanges(nr.guisConfig().raidListGui.backgroundButton.itemData());
                        List<Text> lore = new ArrayList<>();
                        for (String line : nr.guisConfig().raidListGui.backgroundButton.itemLore()) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                        }
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().raidListGui.backgroundButton.itemName())))
                                .setLore(lore)
                                .build();
                        pageEntry.getValue().setSlot(slot, element);
                    }
                }
                pages.get(1).open();
            }
        }
        return 1;
    }

    private int queue(CommandContext<ServerCommandSource> ctx, int pageToOpen) {
        if (NovaRaids.LOADED) {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player != null) {
                if (nr.queuedRaids().isEmpty()) {
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("no_queued_raids"))));
                    return 0;
                }

                Map<Integer, SimpleGui> pages = new HashMap<>();
                int pageTotal = GuiUtils.getPageTotal(nr.queuedRaids().size(), nr.guisConfig().queueGui.displaySlotTotal());
                for (int i = 1; i <= pageTotal; i++) {
                    SimpleGui gui = new SimpleGui(GuiUtils.getScreenSize(nr.guisConfig().queueGui.rows), player, false);
                    gui.setTitle(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().queueGui.title)));
                    pages.put(i, gui);
                }

                int index = 0;
                for (Map.Entry<Integer, SimpleGui> pageEntry : pages.entrySet()) {
                    for (Integer slot : nr.guisConfig().raidListGui.displaySlots()) {
                        if (index < nr.queuedRaids().size()) {
                            Boss boss = nr.queuedRaids().stream().toList().get(index).bossInfo();

                            List<Text> lore = new ArrayList<>();
                            if (Permissions.check(player, "novaraids.cancelqueue", 4)) {
                                for (String line : nr.guisConfig().queueGui.cancelLore) {
                                    lore.add(TextUtils.deserialize(TextUtils.parse(line, boss)));
                                }
                            } else {
                                for (String line : nr.guisConfig().queueGui.defaultLore) {
                                    lore.add(TextUtils.deserialize(TextUtils.parse(line, boss)));
                                }
                            }

                            ItemStack item = PokemonItem.from(boss.pokemonDetails().createPokemon(false));
                            item.applyChanges(nr.guisConfig().queueGui.displayData);
                            int finalIndex = index;
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().queueGui.displayName, boss)))
                                    .setLore(lore)
                                    .setCallback((num, clickType, slotActionType) -> {
                                        if (clickType.isRight) {
                                            if (Permissions.check(player, "novaraids.cancelqueue", 4)) {
                                                pageEntry.getValue().close();
                                                nr.queuedRaids().stream().toList().get(finalIndex).cancelItem();
                                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(nr.messagesConfig().getMessage("queue_item_cancelled"), boss)));
                                                nr.queuedRaids().remove(nr.queuedRaids().stream().toList().get(finalIndex));
                                                queue(ctx, pageEntry.getKey());
                                            }
                                        }
                                    }).build();
                            pageEntry.getValue().setSlot(slot, element);
                            index++;
                        } else {
                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().queueGui.backgroundButton.item())));
                            item.applyChanges(nr.guisConfig().queueGui.backgroundButton.itemData());
                            List<Text> lore = new ArrayList<>();
                            for (String line : nr.guisConfig().queueGui.backgroundButton.itemLore()) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                            }
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().queueGui.backgroundButton.itemName())))
                                    .setLore(lore)
                                    .build();
                            pageEntry.getValue().setSlot(slot, element);
                        }
                    }

                    if (pageEntry.getKey() < pageTotal) {
                        for (Integer slot : nr.guisConfig().queueGui.nextButtonSlots()) {
                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().queueGui.nextButton.item())));
                            item.applyChanges(nr.guisConfig().queueGui.nextButton.itemData());
                            List<Text> lore = new ArrayList<>();
                            for (String line : nr.guisConfig().queueGui.nextButton.itemLore()) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                            }
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().queueGui.nextButton.itemName())))
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
                        for (Integer slot : nr.guisConfig().queueGui.previousButtonSlots()) {
                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().queueGui.previousButton.item())));
                            item.applyChanges(nr.guisConfig().queueGui.previousButton.itemData());
                            List<Text> lore = new ArrayList<>();
                            for (String line : nr.guisConfig().queueGui.previousButton.itemLore()) {
                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                            }
                            GuiElement element = new GuiElementBuilder(item)
                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().queueGui.previousButton.itemName())))
                                    .setLore(lore)
                                    .setCallback(clickType -> {
                                        pageEntry.getValue().close();
                                        queue(ctx, pageEntry.getKey() - 1);
                                    })
                                    .build();
                            pageEntry.getValue().setSlot(slot, element);
                        }
                    }

                    for (Integer slot : nr.guisConfig().queueGui.closeButtonSlots()) {
                        ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().queueGui.closeButton.item())));
                        item.applyChanges(nr.guisConfig().queueGui.closeButton.itemData());
                        List<Text> lore = new ArrayList<>();
                        for (String line : nr.guisConfig().queueGui.closeButton.itemLore()) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                        }
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().queueGui.closeButton.itemName())))
                                .setLore(lore)
                                .setCallback(clickType -> pageEntry.getValue().close())
                                .build();
                        pageEntry.getValue().setSlot(slot, element);
                    }

                    for (Integer slot : nr.guisConfig().queueGui.backgroundButtonSlots()) {
                        ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().queueGui.backgroundButton.item())));
                        item.applyChanges(nr.guisConfig().queueGui.backgroundButton.itemData());
                        List<Text> lore = new ArrayList<>();
                        for (String line : nr.guisConfig().queueGui.backgroundButton.itemLore()) {
                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                        }
                        GuiElement element = new GuiElementBuilder(item)
                                .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().queueGui.backgroundButton.itemName())))
                                .setLore(lore)
                                .build();
                        pageEntry.getValue().setSlot(slot, element);
                    }
                }
                pages.get(pageToOpen).open();
                return 1;
            }
        }
        return 0;
    }
}