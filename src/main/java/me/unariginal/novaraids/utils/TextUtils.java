package me.unariginal.novaraids.utils;

import com.mojang.authlib.GameProfile;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.bosses.Boss;
import me.unariginal.novaraids.raid.Raid;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import static me.unariginal.novaraids.config.ConfigManager.MESSAGES;
import static me.unariginal.novaraids.raid.RaidManager.getRaidId;

public class TextUtils {
    private static final NovaRaids nr = NovaRaids.INSTANCE;
    public static final MiniMessage miniMessage = MiniMessage.builder()
            .tags(TagResolver.builder().build())
            .build();

    public static Text deserialize(String text) {
        return deserialize(text, null);
    }

    public static Text deserialize(String text, @Nullable ServerPlayerEntity player) {
        text = "<!i>" + text;

        if (nr.usingPlaceholderAPI) text = nr.placeholderAPIService.parse(text, player);
        if (nr.usingMiniPlaceholders) text = nr.miniPlaceholdersService.parse(text, player);

        return NovaRaids.INSTANCE.audience.toNative(MiniMessage.miniMessage().deserialize(text));
    }

    public static String parse(String text) {
        return text.replaceAll("%prefix%", MESSAGES.prefix);
    }

    public static String parse(String text, ServerPlayerEntity sourcePlayer, ServerPlayerEntity targetPlayer, int amount, String itemType) {
        text = parse(text);
        return text.replaceAll("%target%", targetPlayer.getNameForScoreboard())
                .replaceAll("%item%", itemType)
                .replaceAll("%amount%", amount == 0 ? "a" : String.valueOf(amount))
                .replaceAll("%source%", sourcePlayer != null ? sourcePlayer.getNameForScoreboard() : "Server");
    }

    public static String parse(String message, Raid raid) {
        message = parse(message);
        message = parse(message, raid.boss);
        long phaseRemaining = ((raid.phaseStartTime + (raid.phaseLength * 20L)) - nr.server.getOverworld().getTime()) / 20;
        if (phaseRemaining < 0) phaseRemaining = 0;

        message = spaceReplace(message, "%boss.form%", !raid.bossPokemon.getForm().getName().equalsIgnoreCase("normal"), raid.bossPokemon.getForm().getName());

        return message
                .replaceAll("%boss.maxhp%", String.valueOf(raid.maxHealth))
                .replaceAll("%raid.defeat_time%", (raid.bossDefeatTime() > 0) ? TextUtils.hms(raid.bossDefeatTime() / 20L) : "")
                .replaceAll("%raid.completion_time%", (raid.raidCompletionTime() > 0) ? TextUtils.hms(raid.raidCompletionTime() / 20) : "")
                .replaceAll("%raid.phase_timer%", TextUtils.hms(phaseRemaining))
                .replaceAll("%boss.currenthp%", String.valueOf(raid.currentHealth))
                .replaceAll("%raid.total_damage%", String.valueOf(raid.maxHealth - raid.currentHealth))
                .replaceAll("%raid.timer%", TextUtils.hms(raid.raidTimer() / 20))
                .replaceAll("%raid.player_count%", String.valueOf(raid.participatingPlayers.size()))
                .replaceAll("%raid.max_players%", (raid.maxPlayers == -1) ? "∞" : String.valueOf(raid.maxPlayers))
                .replaceAll("%raid.phase%", raid.phase.getName())
                .replaceAll("%raid.category%", raid.category.categoryName)
                .replaceAll("%raid.category.id%", raid.category.categoryId)
                .replaceAll("%raid.id%", String.valueOf(getRaidId(raid.uuid)))
                .replaceAll("%raid.min_players%", String.valueOf(raid.minPlayers))
                .replaceAll("%raid.join_method%", (raid.category.raidDetails.requirePass) ? "A Raid Pass" : "/raid list")
                .replaceAll("%raid.location%", raid.location.name);
    }

    public static String parse(String message, Boss boss) {
        message = parse(message);
        message = message
                .replaceAll("%boss%", boss.bossId)
                .replaceAll("%boss.species%", boss.pokemonDetails.species)
                .replaceAll("%boss.level%", String.valueOf(boss.pokemonDetails.level))
                .replaceAll("%boss.minimum_level%", String.valueOf(boss.raidDetails.minimumLevel))
                .replaceAll("%boss.maximum_level%", String.valueOf(boss.raidDetails.maximumLevel))
                .replaceAll("%boss.name%", boss.bossDetails.displayName);

        return message;
    }

    public static String parse(String message, Raid raid, ServerPlayerEntity player, int damage, int place) {
        message = parse(message, raid);
        message = message
                .replaceAll("%place%", String.valueOf(place))
                .replaceAll("%ordinal%", (String.valueOf(place).endsWith("1") ? "st" : (String.valueOf(place).endsWith("2") ? "nd" : (String.valueOf(place).endsWith("3") ? "rd" : "th"))))
                .replaceAll("%player%", player.getNameForScoreboard())
                .replaceAll("%damage%", String.valueOf(damage));

        return message;
    }

    public static String parse(String message, Raid raid, GameProfile player, int damage, int place) {
        message = parse(message, raid);
        message = message
                .replaceAll("%place%", String.valueOf(place))
                .replaceAll("%ordinal%", (String.valueOf(place).endsWith("1") ? "st" : (String.valueOf(place).endsWith("2") ? "nd" : (String.valueOf(place).endsWith("3") ? "rd" : "th"))))
                .replaceAll("%player%", player.getName())
                .replaceAll("%damage%", String.valueOf(damage));

        return message;
    }

    public static String spaceReplace(String text, String placeholder, boolean pass, String replacement) {
        if (text.contains(placeholder + " ")) {
            if (pass) {
                text = text.replaceAll(placeholder, replacement);
            } else {
                text = text.substring(0, text.indexOf(placeholder + " ")).concat(text.substring(text.indexOf(placeholder + " ") + (placeholder + " ").length()));
            }
        }
        if (text.contains(" " + placeholder)) {
            if (pass) {
                text = text.replaceAll(placeholder, replacement);
            } else {
                text = text.substring(0, text.indexOf(" " + placeholder)).concat(text.substring(text.indexOf(" " + placeholder) + (" " + placeholder).length()));
            }
        }
        return text;
    }

    public static String hms(long rawTime) {
        if (rawTime < 0) {
            rawTime = 0;
        }
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
        if (minutes > 0) {
            output = output.concat(minutes + "m ");
        }
        output = output.concat(seconds + "s");

        return output;
    }
}