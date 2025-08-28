package me.unariginal.novaraids.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.rewards.RewardPool;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RewardPoolsConfig {
    public List<RewardPool> rewardPools = new ArrayList<>();

    public RewardPoolsConfig() {
        try {
            loadConfig();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            NovaRaids.LOADED = false;
            NovaRaids.LOGGER.error("[NovaRaids] Failed to load reward pools file.", e);
        }
    }

    public void loadConfig() throws IOException, NullPointerException, UnsupportedOperationException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        File file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/reward_pools.json").toFile();
        JsonObject config = new JsonObject();
        if (file.exists()) config = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

        if (config.keySet().isEmpty()) {
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

            config = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
        }

        for (String key : config.keySet()) {
            JsonObject rewardObject = config.getAsJsonObject(key);
            RewardPool rewardPool = ConfigHelper.getRewardPool(rewardObject, key);
            rewardPools.add(rewardPool);
        }

        for (RewardPool rewardPool : rewardPools) {
            config.remove(rewardPool.name());
            config.add(rewardPool.name(), rewardPool.poolObject());
        }

        file.delete();
        file.createNewFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(file);
        gson.toJson(config, writer);
        writer.close();
    }

    public RewardPool getRewardPool(String id) {
        for (RewardPool rewardPool : rewardPools) {
            if (rewardPool.name().equalsIgnoreCase(id)) {
                return rewardPool;
            }
        }
        return null;
    }
}
