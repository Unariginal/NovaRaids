package me.unariginal.novaraids.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.BossbarData;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.bossbar.BossBar;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BossbarsConfig {
    public List<BossbarData> bossbars = new ArrayList<>();

    public BossbarsConfig() {
        try {
            loadConfig();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            NovaRaids.INSTANCE.logError("[RAIDS] Failed to load bossbars file.");
        }
    }

    public void loadConfig() throws IOException, NullPointerException, UnsupportedOperationException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        File file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/bossbars.json").toFile();
        if (file.createNewFile()) {
            InputStream stream = NovaRaids.class.getResourceAsStream("/raid_config_files/bossbars.json");
            assert stream != null;
            OutputStream out = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = stream.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            stream.close();
            out.close();
        }

        JsonElement root = JsonParser.parseReader(new FileReader(file));
        assert root != null;
        JsonObject config = root.getAsJsonObject();
        for (String key : config.keySet()) {
            JsonObject bossbar_object = config.getAsJsonObject(key);
            String color = "blue";
            if (ConfigHelper.checkProperty(bossbar_object, "bar_color", "bossbars")) {
                color = bossbar_object.get("bar_color").getAsString();
            }
            BossBar.Color bar_color = BossBar.Color.BLUE;
            try {
                bar_color = BossBar.Color.valueOf(color.toLowerCase());
            } catch (IllegalArgumentException e) {
                NovaRaids.INSTANCE.logError("[RAIDS] Invalid bossbar color: " + color);
            }
            String style = "progress";
            if (ConfigHelper.checkProperty(bossbar_object, "bar_style", "bossbars")) {
                style = bossbar_object.get("bar_style").getAsString();
            }
            BossBar.Overlay bar_style = BossBar.Overlay.PROGRESS;
            try {
                bar_style = BossBar.Overlay.valueOf(style);
            } catch (IllegalArgumentException e) {
                NovaRaids.INSTANCE.logError("[RAIDS] Invalid bossbar style: " + style);
            }
            String text = "<red>If you're seeing this message your config is wrong!";
            if (ConfigHelper.checkProperty(bossbar_object, "bar_text", "bossbars")) {
                text = bossbar_object.get("bar_text").getAsString();
            }
            boolean use_actionbar = true;
            if (ConfigHelper.checkProperty(bossbar_object, "use_actionbar", "bossbars")) {
                use_actionbar = bossbar_object.get("use_actionbar").getAsBoolean();
            }
            String actionbar_text = "<red>If you're seeing this your config is wrong!";
            if (use_actionbar) {
                if (ConfigHelper.checkProperty(bossbar_object, "actionbar_text", "bossbars")) {
                    actionbar_text = bossbar_object.get("actionbar_text").getAsString();
                }
            }

            bossbars.add(new BossbarData(key, bar_color, bar_style, text, use_actionbar, actionbar_text));
        }
    }

    public BossbarData getBossbar(String id) {
        for (BossbarData bar : bossbars) {
            if (bar.name().equalsIgnoreCase(id)) {
                return bar;
            }
        }
        return null;
    }
}