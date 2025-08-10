package me.unariginal.novaraids.utils;

import java.util.Map;
import java.util.Random;

public class RandomUtils {
    public static Map.Entry<?, Double> getRandomEntry(Map<?, Double> map) {
        double totalWeight = 0.0;

        if (!map.isEmpty()) {
            for (Map.Entry<?, Double> entry : map.entrySet()) {
                totalWeight += entry.getValue();
            }

            if (totalWeight > 0.0) {
                double randomWeight = new Random().nextDouble(totalWeight);
                totalWeight = 0.0;

                for (Map.Entry<?, Double> entry : map.entrySet()) {
                    totalWeight += entry.getValue();
                    if (randomWeight < totalWeight) {
                        return entry;
                    }
                }
            }
            return map.entrySet().stream().findFirst().orElse(null);
        }
        return null;
    }
}
