package me.unariginal.novaraids.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.rewards.Reward;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RewardPresetsConfig {
    public List<Reward> rewards = new ArrayList<>();

    public RewardPresetsConfig() {
        try {
            loadConfig();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            NovaRaids.INSTANCE.loaded_properly = false;
            NovaRaids.INSTANCE.logError("[RAIDS] Failed to load reward presets file. " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                NovaRaids.INSTANCE.logError("  " + element.toString());
            }
        }
    }

    public void loadConfig() throws IOException, NullPointerException, UnsupportedOperationException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        File file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/reward_presets.json").toFile();
        if (file.createNewFile()) {
            InputStream stream = NovaRaids.class.getResourceAsStream("/raid_config_files/reward_presets.json");
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
            JsonObject reward_object = config.getAsJsonObject(key);
            Reward reward = ConfigHelper.getReward(reward_object, key, "reward_presets");
            if (reward == null) {
                continue;
            }
            rewards.add(reward);
        }
    }

    public Reward getReward(String id) {
        for (Reward reward : rewards) {
            if (reward.name().equalsIgnoreCase(id)) {
                return reward;
            }
        }
        return null;
    }
}
