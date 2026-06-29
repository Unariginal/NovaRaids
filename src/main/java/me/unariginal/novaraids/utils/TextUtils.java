package me.unariginal.novaraids.utils;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.mojang.authlib.GameProfile;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.config.RaidHistory;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
import me.unariginal.novaraids.data.players.PlayerRaidData;
import me.unariginal.novaraids.placeholders.ParseContext;
import me.unariginal.novaraids.placeholders.interfaces.*;
import me.unariginal.novaraids.raid.Raid;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.unariginal.novaraids.config.ConfigManager.CONFIG;
import static me.unariginal.novaraids.placeholders.PlaceholderManager.*;

public class TextUtils {
    private static final Pattern pattern = Pattern.compile("%([^%]+)%");
    public static final MiniMessage miniMessage = MiniMessage.builder()
            .tags(TagResolver.builder().build())
            .build();

    public static Text deserialize(String text) {
        return deserialize(text, ParseContext.builder().build());
    }

    public static Text deserialize(String text, ParseContext parseContext) {
        text = parse(text, parseContext);
        text = "<!i>" + text;

        return NovaRaids.INSTANCE.audience.toNative(MiniMessage.miniMessage().deserialize(text));
    }

    public static String parse(String text, ParseContext parseContext) {
        if (CONFIG.usePlaceholderApi && usingPlaceholderAPI) text = placeholderAPIService.parse(text, parseContext);
        if (usingMiniPlaceholders) text = miniPlaceholdersService.parse(text, parseContext.getPlayer(), parseContext.getRaidHistory());

        text = parse(text);
        if (parseContext.getRaid() != null) text = parse(text, parseContext.getRaid());
        Boss boss = parseContext.getBoss();
        if (boss == null && parseContext.getRaid() != null) boss = parseContext.getRaid().boss;
        if (boss != null) text = parse(text, boss, parseContext.prioritizeRaid());
        Category category = parseContext.getCategory();
        if (category == null && parseContext.getRaid() != null) category = parseContext.getRaid().category;
        if (category == null && boss != null) category = Category.getCategory(boss.categoryId);
        if (category != null) text = parse(text, category);
        if (parseContext.getPlayer() != null) text = parse(text, parseContext.getPlayer());
        if (parseContext.getRaidHistory() != null) text = parse(text, parseContext.getRaidHistory());
        if (parseContext.getPlayerRaidData() != null) text = parse(text, parseContext.getPlayerRaidData());
        CategoryModifier modifier = parseContext.getModifier();
        if (modifier == null && parseContext.getRaid() != null) modifier = parseContext.getRaid().modifier;
        if (modifier != null) text = parse(text, modifier);
        return text;
    }

    public static String parse(String text) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String content = matcher.group(1);

            String[] parts = content.split(" ");
            String id = parts[0];

            List<String> args = Arrays.asList(parts).subList(1, parts.length);

            for (ServerPlaceholder placeholder : serverPlaceholders) {
                if (placeholder.id().contains(id)) {
                    String replacement = placeholder.handle(args).string;
                    matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                    break;
                }
            }
        }

        matcher.appendTail(result);

        return result.toString();
    }

    public static String parse(String text, Raid raid) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String content = matcher.group(1);

            String[] parts = content.split(" ");
            String id = parts[0];

            List<String> args = Arrays.asList(parts).subList(1, parts.length);

            for (RaidPlaceholder placeholder : raidPlaceholders) {
                if (placeholder.id().contains(id)) {
                    String replacement = placeholder.handle(raid, args).string;
                    matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                    break;
                }
            }
        }

        matcher.appendTail(result);

        return result.toString();
    }

    // TODO: Create a placeholder type!
    public static String parse(String text, Category category) {
        return text
                .replaceAll("%category%", category.categoryName)
                .replaceAll("%category.id%", category.categoryId)
                .replaceAll("%category.pass_required%", String.valueOf(category.raidDetails.requirePass))
                .replaceAll("%category.min_players%", String.valueOf(category.raidDetails.minPlayerCount))
                .replaceAll("%category.max_players%", String.valueOf(category.raidDetails.maxPlayerCount));
    }

    public static String parse(String text, Boss boss, boolean prioritizeRaid) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String content = matcher.group(1);

            String[] parts = content.split(" ");
            String id = parts[0];

            List<String> args = Arrays.asList(parts).subList(1, parts.length);

            for (BossPlaceholder placeholder : bossPlaceholders) {
                if (placeholder.id().contains(id)) {
                    String replacement = placeholder.handle(null, boss, prioritizeRaid, args).string;
                    matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                    break;
                }
            }
        }

        matcher.appendTail(result);

        return result.toString();
    }

    public static String parse(String text, CategoryModifier modifier) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String content = matcher.group(1);

            String[] parts = content.split(" ");
            String id = parts[0];

            List<String> args = Arrays.asList(parts).subList(1, parts.length);

            for (CategoryModifierPlaceholder placeholder : categoryModifierPlaceholders) {
                if (placeholder.id().contains(id)) {
                    String replacement = placeholder.handle(modifier, args).string;
                    matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                    break;
                }
            }
        }

        matcher.appendTail(result);

        return result.toString();
    }

    public static String parse(String text, ServerPlayerEntity player) {
        return text.replaceAll("%player%", player.getNameForScoreboard());
    }

    public static String parse(String text, RaidHistory raidHistory) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String content = matcher.group(1);

            String[] parts = content.split(" ");
            String id = parts[0];

            List<String> args = Arrays.asList(parts).subList(1, parts.length);

            for (RaidHistoryPlaceholder placeholder : raidHistoryPlaceholders) {
                if (placeholder.id().contains(id)) {
                    String replacement = placeholder.handle(raidHistory, args).string;
                    matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                    break;
                }
            }
        }

        matcher.appendTail(result);

        return result.toString();
    }

    // TODO: Create a placeholder type!
    public static String parse(String text, PlayerRaidData playerRaidData) {
        return text
                .replaceAll("%player_uuid%", playerRaidData.uuid)
                .replaceAll("%player%", playerRaidData.username)
                .replaceAll("%placement%", String.valueOf(playerRaidData.leaderboardPlacement))
                .replaceAll("%damage%", String.valueOf(playerRaidData.totalDamage))
                .replaceAll("%left_raid%", String.valueOf(playerRaidData.leftRaid))
                .replaceAll("%battle_count%", String.valueOf(playerRaidData.battleAttempts.size()))
                .replaceAll("%caught_boss%", String.valueOf(playerRaidData.catchResult.caught))
                .replaceAll("%caught_species%", playerRaidData.catchResult.species)
                .replaceAll("%caught_form%", playerRaidData.catchResult.formId)
                .replaceAll("%caught_hp_iv%", String.valueOf(playerRaidData.catchResult.ivs.get(Stats.HP)))
                .replaceAll("%caught_attack_iv%", String.valueOf(playerRaidData.catchResult.ivs.get(Stats.ATTACK)))
                .replaceAll("%caught_defence_iv%", String.valueOf(playerRaidData.catchResult.ivs.get(Stats.DEFENCE)))
                .replaceAll("%caught_special_attack_iv%", String.valueOf(playerRaidData.catchResult.ivs.get(Stats.SPECIAL_ATTACK)))
                .replaceAll("%caught_special_defence_iv%", String.valueOf(playerRaidData.catchResult.ivs.get(Stats.SPECIAL_DEFENCE)))
                .replaceAll("%caught_speed_iv%", String.valueOf(playerRaidData.catchResult.ivs.get(Stats.SPEED)))
                .replaceAll("%caught_hp_ev%", String.valueOf(playerRaidData.catchResult.evs.get(Stats.HP)))
                .replaceAll("%caught_attack_ev%", String.valueOf(playerRaidData.catchResult.evs.get(Stats.ATTACK)))
                .replaceAll("%caught_defence_ev%", String.valueOf(playerRaidData.catchResult.evs.get(Stats.DEFENCE)))
                .replaceAll("%caught_special_attack_ev%", String.valueOf(playerRaidData.catchResult.evs.get(Stats.SPECIAL_ATTACK)))
                .replaceAll("%caught_special_defence_ev%", String.valueOf(playerRaidData.catchResult.evs.get(Stats.SPECIAL_DEFENCE)))
                .replaceAll("%caught_speed_ev%", String.valueOf(playerRaidData.catchResult.evs.get(Stats.SPEED)))
                .replaceAll("%caught_shiny%", String.valueOf(playerRaidData.catchResult.shiny));
    }

    // This is the only parse that's out in the wild, damage parsing. Todo: Add damage to ParsingContext
    public static String parse(String text, ServerPlayerEntity player, int damage, int place) {
        GameProfile profile = player.getGameProfile();
        return parse(text, profile, damage, place);
    }

    public static String parse(String text, GameProfile player, int damage, int place) {
        return text
                .replaceAll("%place%", String.valueOf(place))
                .replaceAll("%ordinal%", (String.valueOf(place).endsWith("1") ? "st" : (String.valueOf(place).endsWith("2") ? "nd" : (String.valueOf(place).endsWith("3") ? "rd" : "th"))))
                .replaceAll("%player%", player.getName())
                .replaceAll("%damage%", String.valueOf(damage));
    }

    public static String hms(long rawTime) {
        if (rawTime < 0) rawTime = 0;
        long hours;
        long minutes;
        long seconds = rawTime;
        long temp;

        String output = "";

        if (rawTime >= 3600) {
            seconds = rawTime % 3600;
            hours = (rawTime - seconds) / 3600;
            output = output.concat(hours + "h ");
        }
        temp = seconds;
        seconds = seconds % 60;
        temp = temp - seconds;
        minutes = temp / 60;
        if (minutes > 0) output = output.concat(minutes + "m ");
        output = output.concat(seconds + "s");

        return output;
    }
}