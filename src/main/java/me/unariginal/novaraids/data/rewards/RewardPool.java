package me.unariginal.novaraids.data.rewards;

import me.unariginal.novaraids.NovaRaids;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

public record RewardPool(UUID uuid, String name, boolean allowDuplicates, int minRolls, int maxRolls, Map<Reward, Double> rewards) {
    public void distributeRewards(ServerPlayerEntity player) {
        List<String> appliedRewards = new ArrayList<>();

        int rolls = new Random().nextInt(minRolls(), maxRolls() + 1);
        for (int i = 0; i < rolls; i++) {
            double total_weight = 0.0;
            for (Reward reward : rewards().keySet()) {
                if (allowDuplicates() || !appliedRewards.contains(reward.name)) {
                    total_weight += rewards().get(reward);
                }
            }

            if (total_weight > 0.0) {
                double random_weight = new Random().nextDouble(total_weight);
                total_weight = 0.0;
                Reward to_give = null;
                for (Reward reward : rewards().keySet()) {
                    if (allowDuplicates() || !appliedRewards.contains(reward.name)) {
                        total_weight += rewards().get(reward);
                        if (random_weight < total_weight) {
                            to_give = reward;
                            break;
                        }
                    }
                }

                if (to_give != null) {
                    to_give.applyReward(player);
                    appliedRewards.add(to_give.name());
                } else {
                    NovaRaids.INSTANCE.logError("[RAIDS] Failed to distribute reward. No reward was found to give.");
                }
            } else {
                NovaRaids.INSTANCE.logError("[RAIDS] Reward pool total weight was zero. Possibly caused by a reward pool having more rolls than available rewards with duplicates disabled.");
            }
        }
    }
}
