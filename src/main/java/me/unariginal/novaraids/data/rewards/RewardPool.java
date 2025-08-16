package me.unariginal.novaraids.data.rewards;

import com.google.gson.JsonObject;
import me.unariginal.novaraids.NovaRaids;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

public record RewardPool(JsonObject poolObject, UUID uuid, String name, boolean allowDuplicates, int minRolls, int maxRolls, Map<Reward, Double> rewards) {
    public void distributeRewards(ServerPlayerEntity player) {
        List<String> appliedRewards = new ArrayList<>();

        int rolls = new Random().nextInt(minRolls(), maxRolls() + 1);
        for (int i = 0; i < rolls; i++) {
            double totalWeight = 0.0;
            for (Reward reward : rewards().keySet()) {
                if (allowDuplicates() || !appliedRewards.contains(reward.name)) {
                    totalWeight += rewards().get(reward);
                }
            }

            if (totalWeight > 0.0) {
                double randomWeight = new Random().nextDouble(totalWeight);
                totalWeight = 0.0;
                Reward toGive = null;
                for (Reward reward : rewards().keySet()) {
                    if (allowDuplicates() || !appliedRewards.contains(reward.name)) {
                        totalWeight += rewards().get(reward);
                        if (randomWeight < totalWeight) {
                            toGive = reward;
                            break;
                        }
                    }
                }

                if (toGive != null) {
                    toGive.applyReward(player);
                    appliedRewards.add(toGive.name());
                } else {
                    NovaRaids.INSTANCE.logError("Failed to distribute reward. No reward was found to give.");
                }
            } else {
                NovaRaids.INSTANCE.logError("Reward pool total weight was zero. Possibly caused by a reward pool having more rolls than available rewards with duplicates disabled.");
            }
        }
    }
}
