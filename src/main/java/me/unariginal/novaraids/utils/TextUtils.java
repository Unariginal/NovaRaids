package me.unariginal.novaraids.utils;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.mojang.authlib.GameProfile;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.config.RaidHistory;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.players.PlayerRaidData;
import me.unariginal.novaraids.placeholders.ParseContext;
import me.unariginal.novaraids.placeholders.interfaces.BossPlaceholder;
import me.unariginal.novaraids.placeholders.interfaces.RaidHistoryPlaceholder;
import me.unariginal.novaraids.placeholders.interfaces.RaidPlaceholder;
import me.unariginal.novaraids.placeholders.interfaces.ServerPlaceholder;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.raid.RaidManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

import static me.unariginal.novaraids.config.ConfigManager.CONFIG;
import static me.unariginal.novaraids.placeholders.PlaceholderManager.*;

public class TextUtils {
    public static final MiniMessage miniMessage = MiniMessage.builder()
            .tags(TagResolver.builder().build())
            .build();

    public static Text deserialize(String text) {
        return deserialize(text, ParseContext.builder().build());
    }

    public static Text deserialize(String text, ParseContext parseContext) {
        text = "<!i>" + text;

        if (CONFIG.usePlaceholderApi && usingPlaceholderAPI) text = placeholderAPIService.parse(text, parseContext);
        if (usingMiniPlaceholders) text = miniPlaceholdersService.parse(text, parseContext.getPlayer(), parseContext.getRaidHistory());

        text = parse(text);
        if (parseContext.getRaid() != null) text = parse(text, parseContext.getRaid());
        Boss boss = parseContext.getBoss();
        if (boss == null && parseContext.getRaid() != null) boss = parseContext.getRaid().boss;
        if (boss != null) text = parse(text, boss);
        Category category = parseContext.getCategory();
        if (category == null && parseContext.getRaid() != null) category = parseContext.getRaid().category;
        if (category != null) text = parse(text, category);
        if (parseContext.getRaidHistory() != null) text = parse(text, parseContext.getRaidHistory());
        if (parseContext.getPlayerRaidData() != null) text = parse(text, parseContext.getPlayerRaidData());
//        if (!CONFIG.usePlaceholderApi || (!nr.usingPlaceholderAPI && !nr.usingMiniPlaceholders)) {
//
//        }

        return NovaRaids.INSTANCE.audience.toNative(MiniMessage.miniMessage().deserialize(text));
    }

    public static String parse(String text) {
        for (ServerPlaceholder placeholder : serverPlaceholders) {
            for (String id : placeholder.id()) {
                text = text.replaceAll("%" + id + "%", placeholder.handle(List.of()).string);
            }
        }
        return text;
    }

    public static String parse(String text, Raid raid) {
        for (RaidPlaceholder placeholder : raidPlaceholders) {
            for (String id : placeholder.id()) {
                text = text.replaceAll("%" + id + "%", placeholder.handle(raid, List.of(String.valueOf(RaidManager.getRaidId(raid.uuid) + 1))).string);
            }
        }
        return text;
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

    public static String parse(String text, Boss boss) {
        for (BossPlaceholder placeholder : bossPlaceholders) {
            for (String id : placeholder.id()) {
                text = text.replaceAll("%" + id + "%", placeholder.handle(null, boss, false, List.of()).string);
            }
        }
        return text;
    }

    public static String parse(String text, RaidHistory raidHistory) {
        for (RaidHistoryPlaceholder placeholder : raidHistoryPlaceholders) {
            for (String id : placeholder.id()) {
                text = text.replaceAll("%" + id + "%", placeholder.handle(raidHistory, List.of()).string);
            }
        }
        return text;
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