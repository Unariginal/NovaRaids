package me.unariginal.novaraids.config;

import java.io.*;
import java.util.*;

public class RewardPoolsConfig {
    public static class RewardPool {
        private UUID uuid;
        public boolean allowDuplicates;
        public int minRolls;
        public int maxRolls;
        public List<RewardItem> rewards;

        public RewardPool(boolean allowDuplicates, int minRolls, int maxRolls, List<RewardItem> rewards) {
            this.uuid = UUID.randomUUID();
            this.allowDuplicates = allowDuplicates;
            this.minRolls = minRolls;
            this.maxRolls = maxRolls;
            this.rewards = rewards;
        }

        public UUID getUuid() {
            if (uuid == null) uuid = UUID.randomUUID();
            return uuid;
        }

        public List<RewardPresetsConfig.Reward> distributeRewards() {
            List<RewardPresetsConfig.Reward> rewardsToDistribute = new ArrayList<>();
            List<UUID> appliedRewards = new ArrayList<>();

            int rolls = new Random().nextInt(minRolls, maxRolls + 1);
            for (int i = 0; i < rolls; i++) {
                double totalWeight = 0;
                for (RewardItem reward : rewards) {
                    if (allowDuplicates || !appliedRewards.contains(reward.getUuid())) {
                        totalWeight += reward.weight;
                    }
                }

                if (totalWeight > 0) {
                    double randomWeight = new Random().nextDouble(totalWeight);
                    totalWeight = 0;
                    RewardItem rewardToGive = null;
                    for (RewardItem reward : rewards) {
                        if (allowDuplicates || !appliedRewards.contains(reward.getUuid())) {
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

                        appliedRewards.add(rewardToGive.getUuid());
                    }
                }
            }

            return rewardsToDistribute;
        }
    }

    public static class RewardItem {
        public double weight;
        private UUID uuid;

        public RewardItem(double weight) {
            this.weight = weight;
            this.uuid = UUID.randomUUID();
        }

        public UUID getUuid() {
            if (uuid == null) uuid = UUID.randomUUID();
            return uuid;
        }
    }

    public static class RewardItemPredefined extends RewardItem {
        public String rewardPreset;

        public RewardItemPredefined(double weight, String rewardPreset) {
            super(weight);
            this.rewardPreset = rewardPreset;
        }
    }

    public static class RewardItemUndefined extends RewardItem {
        public RewardPresetsConfig.Reward reward;

        public RewardItemUndefined(double weight, RewardPresetsConfig.Reward reward) {
            super(weight);
            this.reward = reward;
        }
    }

    public static RewardPool getRewardPool(String id) {
        return ConfigManager.REWARD_POOLS.get(id);
    }
}
