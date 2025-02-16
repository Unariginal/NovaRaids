package me.unariginal.novaraids.data.rewards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RewardPool {
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
}
