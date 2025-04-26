package me.unariginal.novaraids.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.rewards.RewardPool;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RewardPoolsConfig {
    public List<RewardPool> reward_pools = new ArrayList<>();

    public RewardPoolsConfig() {
        try {
            loadConfig();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            NovaRaids.INSTANCE.logError("[RAIDS] Failed to load reward pools file.");
        }
    }

    public void loadConfig() throws IOException, NullPointerException, UnsupportedOperationException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        File file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/reward_pools.json").toFile();
        if (file.createNewFile()) {
            InputStream stream = NovaRaids.class.getResourceAsStream("/raid_config_files/reward_pools.json");
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
            RewardPool reward_pool = ConfigHelper.getRewardPool(reward_object, key, "reward_pools");
            if (reward_pool == null) {
                continue;
            }
            reward_pools.add(reward_pool);
        }
    }

    public RewardPool getRewardPool(String id) {
        for (RewardPool reward_pool : reward_pools) {
            if (reward_pool.name().equalsIgnoreCase(id)) {
                return reward_pool;
            }
        }
        return null;
    }
}
