package me.unariginal.novaraids.data.rewards;

import me.unariginal.novaraids.NovaRaids;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RewardPool {
    private final NovaRaids nr = NovaRaids.INSTANCE;
    private final String name;
    private final boolean allow_duplicates;
    private final int min_rolls;
    private final int max_rolls;
    private final Map<String, Double> rewards;

    public RewardPool(String name, boolean allow_duplicates, int min_rolls, int max_rolls, Map<String, Double> rewards) {
        this.name = name;
        this.allow_duplicates = allow_duplicates;
        this.min_rolls = min_rolls;
        this.max_rolls = max_rolls;
        this.rewards = rewards;
    }

    public String name() {
        return name;
    }

    public boolean allow_duplicates() {
        return allow_duplicates;
    }

    public int min_rolls() {
        return min_rolls;
    }

    public int max_rolls() {
        return max_rolls;
    }

    public Map<String, Double> rewards() {
        return rewards;
    }

    public void distributeRewards(ServerPlayerEntity player) {
        List<String> applied_rewards = new ArrayList<>();

        Random rand = new Random();
        int rolls = rand.nextInt(min_rolls(), max_rolls() + 1);
        for (int i = 0; i < rolls; i++) {
            double total_weight = 0.0;
            for (String reward_name : rewards().keySet()) {
                if (allow_duplicates() || !applied_rewards.contains(reward_name)) {
                    total_weight += rewards().get(reward_name);
                }
            }

            double random_weight = rand.nextDouble(total_weight + 1);
            total_weight = 0.0;
            Reward reward = null;
            for (String reward_name : rewards().keySet()) {
                if (allow_duplicates() || !applied_rewards.contains(reward_name)) {
                    total_weight += rewards().get(reward_name);
                    if (random_weight < total_weight) {
                        reward = nr.config().getReward(reward_name);
                        break;
                    }
                }
            }

            if (reward != null) {
                reward.apply_reward(player);
                applied_rewards.add(reward.name());
            }
        }
    }
}
