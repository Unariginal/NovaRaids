package me.unariginal.novaraids.utils;

import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.Boss;
import me.unariginal.novaraids.managers.Messages;
import me.unariginal.novaraids.managers.Raid;

public class TextUtil {
    private static final NovaRaids nr = NovaRaids.INSTANCE;
    private static final Messages messages = nr.config().getMessages();

    public static String parse(String message, Raid raid) {
        String output = message;
        Boss boss = raid.boss_info();

        output = output.replaceAll("%prefix%", messages.prefix());
        output = output.replaceAll("%boss%", boss.name());
        output = output.replaceAll("%phase_length%", hms(raid.phase_length()));

        String form = "Normal";
        if (!boss.form().getName().isEmpty()) {
            form = boss.form().getName();
        }
        output = output.replaceAll("%form%", form);

        output = output.replaceAll("%pokemon%", boss.species().getName());
        output = output.replaceAll("%boss_defeat_time%", "tba");
        output = output.replaceAll("%id%", String.valueOf(nr.get_raid_id(raid)));
        output = output.replaceAll("%phase_timer%", hms(((raid.phase_start_time() + (raid.phase_length() * 20L)) - nr.server().getOverworld().getTime())/20));
        output = output.replaceAll("%current_hp%", String.valueOf(raid.current_health()));
        output = output.replaceAll("%max_hp%", String.valueOf(raid.max_health()));

        return output;
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
