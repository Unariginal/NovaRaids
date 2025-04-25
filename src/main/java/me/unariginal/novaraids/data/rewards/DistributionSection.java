package me.unariginal.novaraids.data.rewards;

import java.util.List;
import java.util.Map;

public record DistributionSection(List<Place> places, boolean allow_duplicates, int min_rolls, int max_rolls, Map<RewardPool, Double> pools) {
}
