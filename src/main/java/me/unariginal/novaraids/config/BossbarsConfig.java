package me.unariginal.novaraids.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.BossbarData;
import me.unariginal.novaraids.data.bosssettings.Boss;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.bossbar.BossBar;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BossbarsConfig {
    public List<BossbarData> bossbars = new ArrayList<>(
            List.of(
                    new BossbarData(
                            "setup_phase_example",
                            BossBar.Color.BLUE,
                            BossBar.Overlay.PROGRESS,
                            "<blue>Prepare for battle against %boss.form% %boss.species%!",
                            true,
                            "<gold>Raid starts in %raid.phase_timer%"
                    ),
                    new BossbarData(
                            "fight_phase_example",
                            BossBar.Color.RED,
                            BossBar.Overlay.NOTCHED_20,
                            "<red>Boss %boss.form% %boss.species%",
                            true,
                            "<red>Raid ends in %raid.phase_timer% <dark_gray>| <red>HP %boss.currenthp%/%boss.maxhp%"
                    ),
                    new BossbarData(
                            "pre_catch_phase_example",
                            BossBar.Color.RED,
                            BossBar.Overlay.PROGRESS,
                            "<red>Catching event starts soon, get to a safe place!",
                            true,
                            "<red>Catching event starts in %raid.phase_timer%"
                    ),
                    new BossbarData(
                            "catch_phase_example",
                            BossBar.Color.YELLOW,
                            BossBar.Overlay.PROGRESS,
                            "<yellow>Time to catch %boss.form% %boss.species%",
                            true,
                            "<yellow>Catching event ends in %raid.phase_timer%"
                    )
            )
    );

    public BossbarsConfig() {
        try {
            loadConfig();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            NovaRaids.LOADED = false;
            NovaRaids.LOGGER.error("[NovaRaids] Failed to load bossbars file. ", e);
        }
    }

    public void loadConfig() throws IOException, NullPointerException, UnsupportedOperationException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) rootFolder.mkdirs();

        File file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/bossbars.json").toFile();

        JsonObject config = new JsonObject();
        if (file.exists()) config = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

        if (!config.keySet().isEmpty()) bossbars.clear();

        for (String key : config.keySet()) {
            JsonObject bossbarObject = config.getAsJsonObject(key);
            String color = "blue";
            if (bossbarObject.has("bar_color"))
                color = bossbarObject.get("bar_color").getAsString();
            bossbarObject.remove("bar_color");
            bossbarObject.addProperty("bar_color", color);

            BossBar.Color bar_color;
            try {
                bar_color = BossBar.Color.valueOf(color.toUpperCase());
            } catch (IllegalArgumentException e) {
                bar_color = BossBar.Color.BLUE;
                NovaRaids.INSTANCE.logError("Invalid bossbar color: " + color);
            }

            String style = "progress";
            if (bossbarObject.has("bar_style"))
                style = bossbarObject.get("bar_style").getAsString();
            bossbarObject.remove("bar_style");
            bossbarObject.addProperty("bar_style", style);

            BossBar.Overlay bar_style;
            try {
                bar_style = BossBar.Overlay.valueOf(style.toUpperCase());
            } catch (IllegalArgumentException e) {
                bar_style = BossBar.Overlay.PROGRESS;
                NovaRaids.INSTANCE.logError("Invalid bossbar style: " + style);
            }

            String text = "<red>If you're seeing this message your config is wrong!";
            if (bossbarObject.has("bar_text"))
                text = bossbarObject.get("bar_text").getAsString();
            bossbarObject.remove("bar_text");
            bossbarObject.addProperty("bar_text", text);

            boolean use_actionbar = true;
            if (bossbarObject.has("use_actionbar"))
                use_actionbar = bossbarObject.get("use_actionbar").getAsBoolean();

            String actionbar_text = "<red>If you're seeing this your config is wrong!";
            if (bossbarObject.has("actionbar_text"))
                actionbar_text = bossbarObject.get("actionbar_text").getAsString();
            bossbarObject.remove("actionbar_text");
            bossbarObject.addProperty("actionbar_text", actionbar_text);

            bossbars.add(new BossbarData(key, bar_color, bar_style, text, use_actionbar, actionbar_text));
        }

        for (BossbarData data : bossbars) {
            config.remove(data.name());
            JsonObject bossBarObject = new JsonObject();
            bossBarObject.addProperty("bar_color", data.barColor().name());
            bossBarObject.addProperty("bar_style", data.barStyle().name());
            bossBarObject.addProperty("bar_text", data.barText());
            bossBarObject.addProperty("use_actionbar", data.useActionbar());
            bossBarObject.addProperty("actionbar_text", data.actionbarText());
            config.add(data.name(), bossBarObject);
        }

        file.delete();
        file.createNewFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(file);
        gson.toJson(config, writer);
        writer.close();
    }

    public BossbarData getBossbar(String id) {
        for (BossbarData bar : bossbars) {
            if (bar.name().equalsIgnoreCase(id)) {
                return bar;
            }
        }
        return null;
    }

    public BossbarData getBossbar(Boss boss, String phase) {
        String bossbar_id = "";
        switch (phase) {
            case "setup":
                if (boss.raidDetails().setupBossbar().isEmpty()) {
                    bossbar_id = NovaRaids.INSTANCE.bossesConfig().getCategory(boss.categoryId()).setupBossbar();
                } else {
                    bossbar_id = boss.raidDetails().setupBossbar();
                }
                break;
            case "fight":
                if (boss.raidDetails().fightBossbar().isEmpty()) {
                    bossbar_id = NovaRaids.INSTANCE.bossesConfig().getCategory(boss.categoryId()).fightBossbar();
                } else {
                    bossbar_id = boss.raidDetails().fightBossbar();
                }
                break;
            case "pre_catch":
                if (boss.raidDetails().preCatchBossbar().isEmpty()) {
                    bossbar_id = NovaRaids.INSTANCE.bossesConfig().getCategory(boss.categoryId()).preCatchBossbar();
                } else {
                    bossbar_id = boss.raidDetails().preCatchBossbar();
                }
                break;
            case "catch":
                if (boss.raidDetails().catchBossbar().isEmpty()) {
                    bossbar_id = NovaRaids.INSTANCE.bossesConfig().getCategory(boss.categoryId()).catchBossbar();
                } else {
                    bossbar_id = boss.raidDetails().catchBossbar();
                }
                break;
        }
        return getBossbar(bossbar_id);
    }
}