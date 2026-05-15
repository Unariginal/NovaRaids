package me.unariginal.novaraids.config;

import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.utils.TextUtils;
import net.kyori.adventure.bossbar.BossBar;

public class BossbarsConfig {
    public String barColor = "blue";
    public String barStyle = "progress";
    public String barText = "<blue>Phase Progress Bossbar!";
    public boolean useActionbar = true;
    public String actionbarText = "<gold>Phase Progress Actionbar!";

    public static BossbarsConfig getBossbar(String id) {
        return ConfigManager.BOSSBARS.get(id);
    }

    public BossBar createBossbar(Raid raid) {
        BossBar.Color color = BossBar.Color.valueOf(barColor);
        BossBar.Overlay style = BossBar.Overlay.valueOf(barStyle);
        return BossBar.bossBar(TextUtils.deserialize(TextUtils.parse(barText, raid)), 1f, color, style);
    }
}