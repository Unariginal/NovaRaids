package me.unariginal.novaraids.data.rewards;

import me.unariginal.novaraids.config.RewardPoolsConfig;
import me.unariginal.novaraids.config.RewardPresetsConfig;

import java.util.*;

public class DistributionSection {
    public boolean allowDuplicates;
    public int minRolls;
    public int maxRolls;
    public List<RewardPoolSection> rewardPools;

    public static class RewardPoolSection {
        public double weight;
        private UUID uuid;

        public RewardPoolSection(double weight) {
            this.weight = weight;
            this.uuid = UUID.randomUUID();
        }

        public UUID getUuid() {
            if (uuid == null) uuid = UUID.randomUUID();
            return uuid;
        }
    }

    public static class PredefinedRewardPoolSection extends RewardPoolSection {
        public String poolPreset;

        public PredefinedRewardPoolSection(double weight, String poolPreset) {
            super(weight);
            this.poolPreset = poolPreset;
        }
    }

    public static class UndefinedRewardPoolSection extends RewardPoolSection {
        public RewardPoolsConfig.RewardPool pool;

        public UndefinedRewardPoolSection(double weight, RewardPoolsConfig.RewardPool pool) {
            super(weight);
            this.pool = pool;
        }
    }

    public List<RewardPresetsConfig.Reward> distributeRewards() {
        List<RewardPresetsConfig.Reward> rewardsToDistribute = new ArrayList<>();
        List<UUID> appliedRewards = new ArrayList<>();

        int rolls = new Random().nextInt(minRolls, maxRolls + 1);
        for (int i = 0; i < rolls; i++) {
            double totalWeight = 0;
            for (RewardPoolSection rewardPool : rewardPools) {
                if (allowDuplicates || !appliedRewards.contains(rewardPool.getUuid())) {
                    totalWeight += rewardPool.weight;
                }
            }

            if (totalWeight > 0) {
                double randomWeight = new Random().nextDouble(totalWeight);
                totalWeight = 0;
                RewardPoolsConfig.RewardPool rewardToGive = null;
                for (RewardPoolSection rewardPool : rewardPools) {
                    if (allowDuplicates || !appliedRewards.contains(rewardPool.getUuid())) {
                        totalWeight += rewardPool.weight;
                        if (randomWeight < totalWeight) {
                            if (rewardPool instanceof PredefinedRewardPoolSection predefinedRewardPoolSection) {
                                rewardToGive = RewardPoolsConfig.getRewardPool(predefinedRewardPoolSection.poolPreset);
                            } else if (rewardPool instanceof UndefinedRewardPoolSection undefinedRewardPoolSection) {
                                rewardToGive = undefinedRewardPoolSection.pool;
                            }
                            break;
                        }
                    }
                }

                if (rewardToGive != null) {
                    rewardsToDistribute.addAll(rewardToGive.distributeRewards());
                    appliedRewards.add(rewardToGive.getUuid());
                }
            }
        }

        return rewardsToDistribute;
    }

    public RewardPoolSection getRandomRewardPool() {
        double totalWeight = 0.0;
        for (RewardPoolSection rewardPoolSection : rewardPools) {
            totalWeight += rewardPoolSection.weight;
        }
        double randomWeight = new Random().nextDouble(totalWeight);
        totalWeight = 0.0;
        for (RewardPoolSection rewardPoolSection : rewardPools) {
            totalWeight += rewardPoolSection.weight;
            if (randomWeight < totalWeight) return rewardPoolSection;
        }
        return null;
    }
}
