package me.unariginal.novaraids.managers;

import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.Boss;
import me.unariginal.novaraids.utils.TextUtil;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;

public record Messages(String prefix, Map<String, String> messages) {
    public String message(String key) {
        return messages.get(key);
    }

    public String parse(String message, Raid raid) {
        String output = message;
        output = parse(output);
        output = parse(output, raid.boss_info());
        output = output
                .replaceAll("%boss.form%", (raid.raidBoss_pokemon().getForm().getName().equalsIgnoreCase("normal") ? "" : raid.raidBoss_pokemon().getForm().getName()))
                .replaceAll("%boss.maxhp%", String.valueOf(raid.max_health()))
                .replaceAll("%raid.defeat_time%", (raid.boss_defeat_time() > 0) ? TextUtil.hms(raid.boss_defeat_time()) : "")
                .replaceAll("%raid.completion_time%", (raid.raid_completion_time() > 0) ? TextUtil.hms(raid.raid_completion_time()) : "")
                .replaceAll("%raid.phase_timer%", TextUtil.hms(((raid.phase_start_time() + (raid.phase_length() * 20L)) - NovaRaids.INSTANCE.server().getOverworld().getTime())/20))
                .replaceAll("%boss.currenthp%", String.valueOf(raid.current_health()))
                .replaceAll("%raid.timer%", TextUtil.hms(raid.raid_timer() / 20))
                .replaceAll("%raid.player_count%", String.valueOf(raid.participating_players().size()))
                .replaceAll("%raid.max_players%", (raid.max_players() == -1) ? "∞" : String.valueOf(raid.max_players()))
                .replaceAll("%raid.phase%", raid.get_phase())
                .replaceAll("%raid.category%", raid.raidBoss_category().name())
                .replaceAll("%raid.id%", String.valueOf(NovaRaids.INSTANCE.get_raid_id(raid)))
                .replaceAll("%raid.min_players%", String.valueOf(raid.min_players()));

        return output;
    }

    public String parse(String message, Boss boss) {
        String output = message;
        output = parse(output);
        output = output
                .replaceAll("%boss.species%", boss.species().getName())
                .replaceAll("%boss%", boss.name());

        return output;
    }

    public String parse(String message, Raid raid, ServerPlayerEntity player, int damage) {
        String output = message;
        output = parse(output, raid);
        output = output
                .replaceAll("%raid.player%", player.getName().getString())
                .replaceAll("%raid.player.damage%", String.valueOf(damage));

        return output;
    }

    public String parse(String message) {
        String output = message;
        output = output.replaceAll("%prefix%", prefix);

        return output;
    }
}
