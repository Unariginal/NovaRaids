package me.unariginal.novaraids.data.rewards;

import me.unariginal.novaraids.config.RewardPoolsConfig;

import java.util.*;

public class DistributionSection {
    public boolean allowDuplicates;
    public int minRolls;
    public int maxRolls;
    public List<RewardPoolSection> rewardPools;

    public static class RewardPoolSection {
        public double weight;
        public transient UUID uuid = UUID.randomUUID();
    }

    public static class PredefinedRewardPoolSection extends RewardPoolSection {
        public String poolPreset;
    }

    public static class UndefinedRewardPoolSection extends RewardPoolSection {
        public RewardPoolsConfig.RewardPool pool;
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
