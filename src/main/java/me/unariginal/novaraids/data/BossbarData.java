package me.unariginal.novaraids.data;

import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.utils.TextUtils;
import net.kyori.adventure.bossbar.BossBar;

public record BossbarData(String name,
                          BossBar.Color bar_color,
                          BossBar.Overlay bar_style,
                          String bar_text,
                          boolean use_actionbar,
                          String actionbar_text) {
    public BossBar createBossBar(Raid raid) {
        return BossBar.bossBar(TextUtils.deserialize(TextUtils.parse(bar_text, raid)), 1f, bar_color, bar_style);
    }
}
