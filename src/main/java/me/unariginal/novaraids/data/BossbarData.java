package me.unariginal.novaraids.data;

import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.utils.TextUtil;
import net.kyori.adventure.bossbar.BossBar;

import java.util.List;

public record BossbarData(String name, String phase, boolean use_overlay, String overlay_text, String bar_color, String bar_style, String bar_text, List<String> bosses, List<String> categories) {
    private static final NovaRaids nr = NovaRaids.INSTANCE;
    public BossBar createBossBar(Raid raid) {
        return BossBar.bossBar(TextUtil.format(nr.config().getMessages().parse(bar_text, raid)), 1f, BossBar.Color.valueOf(bar_color.toUpperCase()), BossBar.Overlay.valueOf(bar_style.toUpperCase()));
    }
}
