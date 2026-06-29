package me.unariginal.novaraids.data.events;

import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.ParseContext;
import net.kyori.adventure.title.Title;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static me.unariginal.novaraids.utils.TextUtils.deserialize;

public class TitleEvent {
    public String title;
    public String subtitle;
    public long fadeIn;
    public long stay;
    public long fadeOut;

    public void showTitle(ServerPlayerEntity player, Raid raid, @Nullable Integer damage) {
        ParseContext parseContext = ParseContext.builder().player(player).raid(raid).build();
        Title title = Title.title(
                deserialize(this.title.replaceAll("%damage%", String.valueOf(damage)), parseContext).asComponent(),
                deserialize(this.subtitle.replaceAll("%damage%", String.valueOf(damage)), parseContext).asComponent(),
                Title.Times.times(
                        Duration.of(fadeIn * 50, ChronoUnit.MILLIS),
                        Duration.of(stay * 50, ChronoUnit.MILLIS),
                        Duration.of(fadeOut * 50, ChronoUnit.MILLIS)
                )
        );

        player.showTitle(title);
    }
}
