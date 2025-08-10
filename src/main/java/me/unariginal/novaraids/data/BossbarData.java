package me.unariginal.novaraids.data;

import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.utils.TextUtils;
import net.kyori.adventure.bossbar.BossBar;

public record BossbarData(String name,
                          BossBar.Color barColor,
                          BossBar.Overlay barStyle,
                          String barText,
                          boolean useActionbar,
                          String actionbarText) {
    public BossBar createBossBar(Raid raid) {
        return BossBar.bossBar(TextUtils.deserialize(TextUtils.parse(barText, raid)), 1f, barColor, barStyle);
    }
}
