package me.unariginal.novaraids.data.bosssettings;

import me.unariginal.novaraids.utils.RandomUtils;

import java.util.Map;

public record Boss(String bossId,
                   String categoryId,
                   double globalWeight,
                   double categoryWeight,
                   PokemonDetails pokemonDetails,
                   String displayName,
                   int baseHealth,
                   int healthIncreasePerPlayer,
                   boolean applyGlowing,
                   int aiSkillLevel,
                   boolean rerollFeaturesEachBattle,
                   boolean rerollGimmickEachBattle,
                   Map<String, Double> spawnLocations,
                   ItemSettings itemSettings,
                   RaidDetails raidDetails,
                   CatchSettings catchSettings) {

    public String chooseLocation() {
        Map.Entry<?, Double> entry = RandomUtils.getRandomEntry(spawnLocations);
        if (entry != null) {
            return (String) entry.getKey();
        }
        return null;
    }
}
