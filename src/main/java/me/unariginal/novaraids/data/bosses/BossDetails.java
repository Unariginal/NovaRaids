package me.unariginal.novaraids.data.bosses;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class BossDetails {
    public String displayName;
    public int baseHealth;
    public int healthIncreasePerPlayer;
    public boolean applyGlowing;
    public int aiSkillLevel;
    public boolean rerollFeaturesEachBattle;
    public boolean rerollGimmickEachBattle;
    public List<WeightedLocation> locations;

    public static class WeightedLocation {
        public String location;
        public double weight;
    }

    public @Nullable String getRandomLocation() {
        return getRandomLocation(locations);
    }

    public @Nullable String getRandomLocation(List<WeightedLocation> locations) {
        double totalWeight = 0.0;
        for (WeightedLocation weightedObject : locations) {
            totalWeight += weightedObject.weight;
        }
        if (totalWeight <= 0.0) return null;
        double randomWeight = new Random().nextDouble(totalWeight);
        totalWeight = 0.0;
        for (WeightedLocation weightedObject : locations) {
            totalWeight += weightedObject.weight;
            if (randomWeight < totalWeight) return weightedObject.location;
        }
        return null;
    }
}
