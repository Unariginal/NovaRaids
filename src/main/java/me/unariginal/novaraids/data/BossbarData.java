package me.unariginal.novaraids.data;

import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.utils.TextUtils;
import net.kyori.adventure.bossbar.BossBar;
import org.w3c.dom.Text;

public record BossbarData(String name,
                          BossBar.Color bar_color,
                          BossBar.Overlay bar_style,
                          String bar_text,
                          boolean use_actionbar,
                          Text actionbar_text) {
    private static final NovaRaids nr = NovaRaids.INSTANCE;

    public BossBar createBossBar(Raid raid) {
        return BossBar.bossBar(TextUtils.format(nr.config().getMessages().parse(bar_text, raid)), 1f, bar_color, bar_style);
    }
}
