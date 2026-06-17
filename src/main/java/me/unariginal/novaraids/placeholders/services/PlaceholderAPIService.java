package me.unariginal.novaraids.placeholders.services;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.config.RaidHistory;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
import me.unariginal.novaraids.placeholders.*;
import me.unariginal.novaraids.placeholders.interfaces.*;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.ParseContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

import static me.unariginal.novaraids.NovaRaids.LOGGER;

public class PlaceholderAPIService {
    public void registerPlayer(PlayerPlaceholder placeholder) {
        placeholder.id().forEach(id -> Placeholders.register(Identifier.of(NovaRaids.MOD_ID, id), (ctx, arg) -> {
            ServerPlayerEntity player = ctx.player();
            if (player == null) return PlaceholderResult.invalid("NO PLAYER");

            List<String> args = arg != null ? List.of(parse(arg, ParseContext.builder().player(player).build()).split(":")) : new ArrayList<>();
            GenericResult result = placeholder.handle(player, args);
            if (result.isSuccessful) {
                return PlaceholderResult.value(NovaRaids.INSTANCE.audience.toNative(result.asComponent()));
            } else {
                return PlaceholderResult.invalid(result.string);
            }
        }));
    }

    public void registerRaid(RaidPlaceholder placeholder) {
        placeholder.id().forEach(id -> Placeholders.register(Identifier.of(NovaRaids.MOD_ID, id), (ctx, arg) -> {
            List<String> args = arg != null ? List.of(parse(arg, ParseContext.builder().player(ctx.player()).build()).split(":")) : new ArrayList<>();
            Raid raid = ctx.asParserContext().get(new ParserContext.Key<>("raid", Raid.class));

            LOGGER.error("Raid: {}", raid != null);

            GenericResult result = placeholder.handle(raid, args);
            if (result.isSuccessful) {
                return PlaceholderResult.value(NovaRaids.INSTANCE.audience.toNative(result.asComponent()));
            } else {
                return PlaceholderResult.invalid(result.string);
            }
        }));
    }

    public void registerBoss(BossPlaceholder placeholder) {
        placeholder.id().forEach(id -> Placeholders.register(Identifier.of(NovaRaids.MOD_ID, id), (ctx, arg) -> {
            List<String> args = arg != null ? List.of(parse(arg, ParseContext.builder().player(ctx.player()).build()).split(":")) : new ArrayList<>();
            Raid raid = ctx.asParserContext().get(new ParserContext.Key<>("raid", Raid.class));
            Boss boss = ctx.asParserContext().get(new ParserContext.Key<>("boss", Boss.class));
            Boolean prioritizeRaid = ctx.asParserContext().get(new ParserContext.Key<>("prioritize_raid", Boolean.class));

            LOGGER.error("Raid: {}", raid != null);
            LOGGER.error("Boss: {}", boss != null);
            LOGGER.error("Prioritize Raid: {}", prioritizeRaid);
            // todo: Temp I guess
            if (prioritizeRaid == null) prioritizeRaid = true;

            GenericResult result = placeholder.handle(raid, boss, prioritizeRaid, args);
            if (result.isSuccessful) {
                return PlaceholderResult.value(NovaRaids.INSTANCE.audience.toNative(result.asComponent()));
            } else {
                return PlaceholderResult.invalid(result.string);
            }
        }));
    }

    public void registerCategoryModifier(CategoryModifierPlaceholder placeholder) {
        placeholder.id().forEach(id -> Placeholders.register(Identifier.of(NovaRaids.MOD_ID, id), (ctx, arg) -> {
            List<String> args = arg != null ? List.of(parse(arg, ParseContext.builder().player(ctx.player()).build()).split(":")) : new ArrayList<>();
            CategoryModifier modifier = ctx.asParserContext().get(new ParserContext.Key<>("category_modifier", CategoryModifier.class));
            if (modifier == null) return PlaceholderResult.invalid("Invalid Category Modifier");
            GenericResult result = placeholder.handle(modifier, args);
            if (result.isSuccessful) {
                return PlaceholderResult.value(NovaRaids.INSTANCE.audience.toNative(result.asComponent()));
            } else {
                return PlaceholderResult.invalid(result.string);
            }
        }));
    }

    public void registerRaidHistory(RaidHistoryPlaceholder placeholder) {
        placeholder.id().forEach(id -> Placeholders.register(Identifier.of(NovaRaids.MOD_ID, id), (ctx, arg) -> {
            List<String> args = arg != null ? List.of(parse(arg, ParseContext.builder().player(ctx.player()).build()).split(":")) : new ArrayList<>();
            RaidHistory raidHistory = ctx.asParserContext().get(new ParserContext.Key<>("raid_history", RaidHistory.class));
            if (raidHistory == null) return PlaceholderResult.invalid("Invalid Raid History");
            GenericResult result = placeholder.handle(raidHistory, args);
            if (result.isSuccessful) {
                return PlaceholderResult.value(NovaRaids.INSTANCE.audience.toNative(result.asComponent()));
            } else {
                return PlaceholderResult.invalid(result.string);
            }
        }));
    }

    public void registerServer(ServerPlaceholder placeholder) {
        placeholder.id().forEach(id -> Placeholders.register(Identifier.of(NovaRaids.MOD_ID, id), (ctx, arg) -> {
            List<String> args = arg != null ? List.of(parse(arg, ParseContext.builder().player(ctx.player()).build()).split(":")) : new ArrayList<>();
            GenericResult result = placeholder.handle(args);
            if (result.isSuccessful) {
                return PlaceholderResult.value(NovaRaids.INSTANCE.audience.toNative(result.asComponent()));
            } else {
                return PlaceholderResult.invalid(result.string);
            }
        }));
    }

    public String parse(String input, ParseContext parseContext) {
        PlaceholderContext context = PlaceholderContext.of(NovaRaids.INSTANCE.server);
        if (parseContext.getPlayer() != null) context = PlaceholderContext.of(parseContext.getPlayer());
        // todo: This whole thing doesn't work because I'm stupid so we're gonna ignore it and use the old placeholders for internal files. Placeholders should still be usable for things like holograms and whatever
        if (parseContext.getRaid() != null) context.asParserContext().with(new ParserContext.Key<>("raid", Raid.class), parseContext.getRaid());
        if (parseContext.getBoss() != null) context.asParserContext().with(new ParserContext.Key<>("boss", Boss.class), parseContext.getBoss());
        context.asParserContext().with(new ParserContext.Key<>("prioritize_raid", Boolean.class), parseContext.prioritizeRaid());
        if (parseContext.getCategory() != null) context.asParserContext().with(new ParserContext.Key<>("category", Category.class), parseContext.getCategory());
        if (parseContext.getModifier() != null) context.asParserContext().with(new ParserContext.Key<>("category_modifier", CategoryModifier.class), parseContext.getModifier());
        if (parseContext.getRaidHistory() != null) context.asParserContext().with(new ParserContext.Key<>("raid_history", RaidHistory.class), parseContext.getRaidHistory());
        return Placeholders.parseText(Text.literal(input), context).getString();
    }
}
