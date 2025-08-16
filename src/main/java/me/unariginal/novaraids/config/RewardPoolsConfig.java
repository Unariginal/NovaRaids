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
            NovaRaids.INSTANCE.logError("Failed to load reward pools file. " + e.getMessage());
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

        File file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/reward_pools.json").toFile();
        JsonObject config = new JsonObject();
        if (file.exists()) config = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

        rewardPools.clear();
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
