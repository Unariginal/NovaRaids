package me.unariginal.novaraids.managers;

import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.bosssettings.Boss;
import me.unariginal.novaraids.utils.TextUtils;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.Objects;

public record Messages(String prefix, String raid_start_command, Map<String, String> messages) {
    public String message(String key) {
        return messages.get(key);
    }

    public void execute_command(Raid raid) {
        if (!raid_start_command.isEmpty()) {
            CommandManager cmdManager = Objects.requireNonNull(NovaRaids.INSTANCE.server()).getCommandManager();
            ServerCommandSource source = NovaRaids.INSTANCE.server().getCommandSource();
            cmdManager.executeWithPrefix(source, parse(raid_start_command, raid));
        }
    }

    public String parse(String message, Raid raid) {
        String output = message;
        output = parse(output);
        output = parse(output, raid.boss_info());
        output = output
                .replaceAll("%boss.maxhp%", String.valueOf(raid.max_health()))
                .replaceAll("%raid.defeat_time%", (raid.boss_defeat_time() > 0) ? TextUtils.hms(raid.boss_defeat_time() * 20L) : "")
                .replaceAll("%raid.completion_time%", (raid.raid_completion_time() > 0) ? TextUtils.hms(raid.raid_completion_time()) : "")
                .replaceAll("%raid.phase_timer%", TextUtils.hms(((raid.phase_start_time() + (raid.phase_length() * 20L)) - NovaRaids.INSTANCE.server().getOverworld().getTime())/20))
                .replaceAll("%boss.currenthp%", String.valueOf(raid.current_health()))
                .replaceAll("%raid.timer%", TextUtils.hms(raid.raid_timer() / 20))
                .replaceAll("%raid.player_count%", String.valueOf(raid.participating_players().size()))
                .replaceAll("%raid.max_players%", (raid.max_players() == -1) ? "∞" : String.valueOf(raid.max_players()))
                .replaceAll("%raid.phase%", raid.get_phase())
                .replaceAll("%raid.category%", raid.raidBoss_category().name())
                .replaceAll("%raid.id%", String.valueOf(NovaRaids.INSTANCE.get_raid_id(raid)))
                .replaceAll("%raid.min_players%", String.valueOf(raid.min_players()))
                .replaceAll("%raid.join_method%", (raid.raidBoss_category().require_pass()) ? "A Raid Pass" : "/raid list");

        return output;
    }

    public String parse(String message, Boss boss) {
        String output = message;
        output = parse(output);
        output = output
                .replaceAll("%boss%", boss.boss_id())
                .replaceAll("%boss.species%", boss.pokemonDetails().species().getName())
                .replaceAll("%boss.level%", String.valueOf(boss.pokemonDetails().level()))
                .replaceAll("%boss.minimum_level%", String.valueOf(boss.raid_details().minimum_level()));

        boolean normal = boss.display_form().isEmpty() || boss.display_form().equalsIgnoreCase("normal");
        if (output.contains(" %boss.form%")) {
            if (normal) {
                output = output.substring(0, output.indexOf(" %boss.form%")).concat(output.substring(output.indexOf(" %boss.form%") + " %boss.form%".length()));
            } else {
                output = output.replaceAll("%boss.form%", boss.display_form());
            }
        } else if (output.contains("%boss.form% ")) {
            if (normal) {
                output = output.substring(0, output.indexOf("%boss.form% ")).concat(output.substring(output.indexOf("%boss.form% ") + "%boss.form% ".length()));
            } else {
                output = output.replaceAll("%boss.form%", boss.display_form());
            }
        }

        return output;
    }

    public String parse(String message, Raid raid, ServerPlayerEntity player, int damage, int place) {
        String output = message;
        output = parse(output, raid);
        output = output
                .replaceAll("%raid.player.place%", String.valueOf(place))
                .replaceAll("%place_suffix%", ((place == 1) ? "st" : ((place == 2) ? "nd" : ((place == 3) ? "rd" : "th"))))
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
