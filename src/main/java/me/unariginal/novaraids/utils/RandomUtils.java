package me.unariginal.novaraids.utils;

import me.unariginal.novaraids.NovaRaids;

import java.util.Map;
import java.util.Random;

public class RandomUtils {
    public static Map.Entry<?, Double> getRandomEntry(Map<?, Double> map) {
        double total_weight = 0.0;

        if (!map.isEmpty()) {
            for (Map.Entry<?, Double> entry : map.entrySet()) {
                total_weight += entry.getValue();
            }
            NovaRaids.INSTANCE.logInfo("[RAIDS] Total weight: " + total_weight);

            if (total_weight > 0.0) {
                double random_weight = new Random().nextDouble(total_weight);
                total_weight = 0.0;
                NovaRaids.INSTANCE.logInfo("[RAIDS] Random weight: " + random_weight);

                for (Map.Entry<?, Double> entry : map.entrySet()) {
                    total_weight += entry.getValue();
                    NovaRaids.INSTANCE.logInfo("[RAIDS] Current Weight: " + total_weight + " (Random weight: " + random_weight + ")");
                    if (random_weight < total_weight) {
                        return entry;
                    }
                }
            }
            return map.entrySet().stream().findFirst().orElse(null);
        }
        return null;
    }
}
