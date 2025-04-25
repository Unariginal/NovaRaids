package me.unariginal.novaraids.data.rewards;

import java.util.List;

public record DistributionSection(List<Place> places, boolean allow_duplicates, int min_rolls, int max_rolls, List<RewardPool> pools) {
}
