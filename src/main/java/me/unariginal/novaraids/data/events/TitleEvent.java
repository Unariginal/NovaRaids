package me.unariginal.novaraids.data.events;

import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.utils.TextUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class TitleEvent {
    public String title;
    public String subtitle;
    public long fadeIn;
    public long stay;
    public long fadeOut;

    public void showTitle(ServerPlayerEntity player, Raid raid, Integer damage) {
        Title title = Title.title(
                MiniMessage.miniMessage().deserialize(TextUtils.parse(this.title.replaceAll("%damage%", String.valueOf(damage)).replaceAll("%player%", player.getNameForScoreboard()), raid)),
                MiniMessage.miniMessage().deserialize(TextUtils.parse(subtitle.replaceAll("%damage%", String.valueOf(damage)).replaceAll("%player%", player.getNameForScoreboard()), raid)),
                Title.Times.times(
                        Duration.of(fadeIn * 50, ChronoUnit.MILLIS),
                        Duration.of(stay * 50, ChronoUnit.MILLIS),
                        Duration.of(fadeOut * 50, ChronoUnit.MILLIS)
                )
        );

        player.showTitle(title);
    }
}
