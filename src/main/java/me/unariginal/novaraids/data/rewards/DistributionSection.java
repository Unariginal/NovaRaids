package me.unariginal.novaraids.data.rewards;

import java.util.List;
import java.util.Map;

public record DistributionSection(boolean isCategorySection, List<Place> places, boolean allowDuplicates, int minRolls, int maxRolls, Map<RewardPool, Double> pools) {
}
