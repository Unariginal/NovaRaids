package me.unariginal.novaraids.data.categories;

import me.unariginal.novaraids.config.ConfigManager;
import me.unariginal.novaraids.data.bosses.Boss;
import me.unariginal.novaraids.data.rewards.RewardDistribution;

import java.util.List;
import java.util.Map;

public class Category {
    public String categoryId;
    public String categoryName;
    public RaidDetails raidDetails;
    public ItemSettings itemSettings;
    public List<RewardDistribution> rewardDistribution;
    public transient Map<String, Boss> bosses;

    public static Category getCategory(String id) {
        return ConfigManager.CATEGORIES.get(id);
    }

    public void fillBosses() {
        for (Boss boss : ConfigManager.BOSSES.values()) {
            if (boss.categoryId.equalsIgnoreCase(categoryId)) bosses.put(boss.bossId, boss);
        }
    }
}
