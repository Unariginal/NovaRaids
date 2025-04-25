package me.unariginal.novaraids.utils;

import me.unariginal.novaraids.NovaRaids;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.text.Text;

public class TextUtils {
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

    public static Component format(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }

    public static Text deserialize(String text) {
        Component component = format(text);
        return NovaRaids.INSTANCE.audience().toNative(component);
    }
}
