package me.unariginal.novaraids.data.rewards;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

public record DistributionSection(JsonObject distributionObject, boolean isCategorySection, List<Place> places, boolean allowDuplicates, int minRolls, int maxRolls, Map<RewardPool, Double> pools) {
}
