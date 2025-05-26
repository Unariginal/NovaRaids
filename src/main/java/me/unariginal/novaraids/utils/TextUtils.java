package me.unariginal.novaraids.utils;

import com.mojang.authlib.GameProfile;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.bosssettings.Boss;
import me.unariginal.novaraids.managers.Raid;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class TextUtils {
    private static final NovaRaids nr = NovaRaids.INSTANCE;

    public static Text deserialize(String text) {
        Text toReturn = Text.empty();
        try {
            Component component = MiniMessage.miniMessage().deserialize("<!i>" + text);
            toReturn = NovaRaids.INSTANCE.audience().toNative(component);
        } catch (Exception e) {
            nr.logError("[RAIDS] Error deserializing string: " + e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                nr.logError("  " + ste.toString());
            }
        }
        return toReturn;
    }

    public static String parse(String text) {
        return text.replaceAll("%prefix%", nr.messagesConfig().prefix);
    }

    public static String parse(String text, ServerPlayerEntity source_player, ServerPlayerEntity target_player, int amount, String item_type) {
        text = parse(text);
        return text.replaceAll("%target%", target_player.getNameForScoreboard())
                .replaceAll("%raid_item%", item_type)
                .replaceAll("%amount%", amount == 0 ? "a" : String.valueOf(amount))
                .replaceAll("%source%", source_player != null ? source_player.getNameForScoreboard() : "Server");
    }

    public static String parse(String message, Raid raid) {
        message = parse(message);
        message = parse(message, raid.boss_info());
        return message
                .replaceAll("%boss.maxhp%", String.valueOf(raid.max_health()))
                .replaceAll("%raid.defeat_time%", (raid.boss_defeat_time() > 0) ? TextUtils.hms(raid.boss_defeat_time() * 20L) : "")
                .replaceAll("%raid.completion_time%", (raid.raid_completion_time() > 0) ? TextUtils.hms(raid.raid_completion_time()) : "")
                .replaceAll("%raid.phase_timer%", TextUtils.hms(((raid.phase_start_time() + (raid.phase_length() * 20L)) - NovaRaids.INSTANCE.server().getOverworld().getTime())/20))
                .replaceAll("%boss.currenthp%", String.valueOf(raid.current_health()))
                .replaceAll("%raid.total_damage%", String.valueOf(raid.max_health() - raid.current_health()))
                .replaceAll("%raid.timer%", TextUtils.hms(raid.raid_timer() / 20))
                .replaceAll("%raid.player_count%", String.valueOf(raid.participating_players().size()))
                .replaceAll("%raid.max_players%", (raid.max_players() == -1) ? "∞" : String.valueOf(raid.max_players()))
                .replaceAll("%raid.phase%", raid.get_phase())
                .replaceAll("%raid.category%", raid.raidBoss_category().name())
                .replaceAll("%raid.category.id%", raid.raidBoss_category().id())
                .replaceAll("%raid.id%", String.valueOf(NovaRaids.INSTANCE.get_raid_id(raid)))
                .replaceAll("%raid.min_players%", String.valueOf(raid.min_players()))
                .replaceAll("%raid.join_method%", (raid.raidBoss_category().require_pass()) ? "A Raid Pass" : "/raid list")
                .replaceAll("%raid.location%", raid.raidBoss_location().name())
                .replaceAll("%raid.location.id%", raid.raidBoss_location().id());
    }

    public static String parse(String message, Boss boss) {
        message = parse(message);
        message = message
                .replaceAll("%boss%", boss.boss_id())
                .replaceAll("%boss.species%", boss.pokemonDetails().species().getName())
                .replaceAll("%boss.level%", String.valueOf(boss.pokemonDetails().level()))
                .replaceAll("%boss.minimum_level%", String.valueOf(boss.raid_details().minimum_level()))
                .replaceAll("%boss.maximum_level%", String.valueOf(boss.raid_details().maximum_level()));
        message = spaceReplace(message, "%boss.form%", !boss.pokemonDetails().form().getName().equalsIgnoreCase("normal"), boss.pokemonDetails().form().getName());
        message = message
                .replaceAll("%boss.form%", boss.pokemonDetails().form().getName())
                .replaceAll("%boss.name%", boss.display_name());

        return message;
    }

    public static String parse(String message, Raid raid, ServerPlayerEntity player, int damage, int place) {
        message = parse(message, raid);
        message = message
                .replaceAll("%raid.player.place%", String.valueOf(place))
                .replaceAll("%place_suffix%", (String.valueOf(place).endsWith("1") ? "st" : (String.valueOf(place).endsWith("2") ? "nd" : (String.valueOf(place).endsWith("3") ? "rd" : "th"))))
                .replaceAll("%raid.player%", player.getName().getString())
                .replaceAll("%raid.player.damage%", String.valueOf(damage));

        return message;
    }

    public static String parse(String message, Raid raid, GameProfile player, int damage, int place) {
        message = parse(message, raid);
        message = message
                .replaceAll("%raid.player.place%", String.valueOf(place))
                .replaceAll("%place_suffix%", (String.valueOf(place).endsWith("1") ? "st" : (String.valueOf(place).endsWith("2") ? "nd" : (String.valueOf(place).endsWith("3") ? "rd" : "th"))))
                .replaceAll("%raid.player%", player.getName())
                .replaceAll("%raid.player.damage%", String.valueOf(damage));

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

    public static String hms(long raw_time) {
        long hours;
        long minutes;
        long seconds = raw_time;
        long temp;

        String output = "";

        if (raw_time >= 3600) {
            seconds = raw_time % 3600;
            hours = (raw_time - seconds) / 3600;
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
