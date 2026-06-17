package me.unariginal.novaraids.config;

import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.ParseContext;
import net.kyori.adventure.bossbar.BossBar;

import static me.unariginal.novaraids.utils.TextUtils.deserialize;

public class BossbarsConfig {
    public transient String bossbarId;
    public String barColor = "blue";
    public String barStyle = "progress";
    public String barText = "<blue>Phase Progress Bossbar!";
    public boolean useActionbar = true;
    public String actionbarText = "<gold>Phase Progress Actionbar!";

    public static BossbarsConfig getBossbar(String id) {
        return ConfigManager.BOSSBARS.get(id);
    }

    public BossBar createBossbar(Raid raid) {
        BossBar.Color color = BossBar.Color.valueOf(barColor.toUpperCase());
        BossBar.Overlay style = BossBar.Overlay.valueOf(barStyle.toUpperCase());
        return BossBar.bossBar(deserialize(barText, ParseContext.builder().raid(raid).build()), 1f, color, style);
    }
}