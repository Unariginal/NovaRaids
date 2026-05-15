package me.unariginal.novaraids.data.bosses;

import me.unariginal.novaraids.config.ConfigManager;
import me.unariginal.novaraids.data.categories.Category;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class Boss {
    public transient String categoryId;
    public String bossId;
    public double globalWeight;
    public double categoryWeight;
    public PokemonDetails pokemonDetails;
    public BossDetails bossDetails;
    public BossItemSettings itemSettings;
    public RaidDetails raidDetails;
    public CatchSettings catchSettings;

    public static Boss getBoss(String id) {
        return ConfigManager.BOSSES.get(id);
    }

    public static Boss getRandomBoss(@Nullable List<String> blacklist) {
        double totalWeight = 0;
        for (Boss boss : ConfigManager.BOSSES.values()) {
            if (blacklist == null || !blacklist.contains(boss.bossId))
                totalWeight += boss.globalWeight;
        }

        if (totalWeight > 0) {
            double randomWeight = new Random().nextDouble(totalWeight);
            totalWeight = 0;
            for (Boss boss : ConfigManager.BOSSES.values()) {
                if (blacklist == null || !blacklist.contains(boss.bossId)) {
                    totalWeight += boss.globalWeight;
                    if (randomWeight < totalWeight) {
                        return boss;
                    }
                }
            }
        }
        return null;
    }

    public static Boss getRandomBoss(String categoryId, @Nullable List<String> blacklist) {
        double totalWeight = 0;
        Category category = Category.getCategory(categoryId);
        if (category == null) return null;
        for (Boss boss : category.bosses.values()) {
            if (blacklist == null || !blacklist.contains(boss.bossId))
                totalWeight += boss.categoryWeight;
        }

        if (totalWeight > 0) {
            double randomWeight = new Random().nextDouble(totalWeight);
            totalWeight = 0;
            for (Boss boss : category.bosses.values()) {
                if (blacklist == null || !blacklist.contains(boss.bossId)) {
                    totalWeight += boss.categoryWeight;
                    if (randomWeight < totalWeight) {
                        return boss;
                    }
                }
            }
        }
        return null;
    }
}
