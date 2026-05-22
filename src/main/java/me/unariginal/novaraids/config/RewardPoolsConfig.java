package me.unariginal.novaraids.config;

import java.io.*;
import java.util.*;

public class RewardPoolsConfig {
    public static class RewardPool {
        public transient String rewardPoolId;
        public final transient UUID uuid = UUID.randomUUID();
        public boolean allowDuplicates;
        public int minRolls;
        public int maxRolls;
        public List<RewardItem> rewards;

        public List<RewardPresetsConfig.Reward> distributeRewards() {
            List<RewardPresetsConfig.Reward> rewardsToDistribute = new ArrayList<>();
            List<UUID> appliedRewards = new ArrayList<>();

            int rolls = new Random().nextInt(minRolls, maxRolls + 1);
            for (int i = 0; i < rolls; i++) {
                double totalWeight = 0;
                for (RewardItem reward : rewards) {
                    if (allowDuplicates || !appliedRewards.contains(reward.uuid)) {
                        totalWeight += reward.weight;
                    }
                }

                if (totalWeight > 0) {
                    double randomWeight = new Random().nextDouble(totalWeight);
                    totalWeight = 0;
                    RewardItem rewardToGive = null;
                    for (RewardItem reward : rewards) {
                        if (allowDuplicates || !appliedRewards.contains(reward.uuid)) {
                            totalWeight += reward.weight;
                            if (randomWeight < totalWeight) {
                                rewardToGive = reward;
                                break;
                            }
                        }
                    }

                    if (rewardToGive != null) {
                        if (rewardToGive instanceof RewardItemPredefined rewardItemPredefined) {
                            rewardsToDistribute.add(RewardPresetsConfig.getReward(rewardItemPredefined.rewardPreset));
                        } else if (rewardToGive instanceof RewardItemUndefined rewardItemUndefined) {
                            rewardsToDistribute.add(rewardItemUndefined.reward);
                        }

                        appliedRewards.add(rewardToGive.uuid);
                    }
                }
            }

            return rewardsToDistribute;
        }
    }

    public static class RewardItem {
        public double weight;
        public transient UUID uuid = UUID.randomUUID();
    }

    public static class RewardItemPredefined extends RewardItem {
        public String rewardPreset;
    }

    public static class RewardItemUndefined extends RewardItem {
        public RewardPresetsConfig.Reward reward;
    }

    public static RewardPool getRewardPool(String id) {
        return ConfigManager.REWARD_POOLS.get(id);
    }
}
