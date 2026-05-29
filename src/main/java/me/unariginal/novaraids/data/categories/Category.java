package me.unariginal.novaraids.data.categories;

import me.unariginal.novaraids.config.ConfigManager;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
import me.unariginal.novaraids.data.rewards.RewardDistribution;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Category {
    public String categoryId;
    public String categoryName;
    public int noModifierWeight;
    public RaidDetails raidDetails;
    public ItemSettings itemSettings;
    public List<RewardDistribution> rewardDistribution;
    public transient Map<String, CategoryModifier> modifiers = new HashMap<>();
    public transient Map<String, Boss> bosses = new HashMap<>();

    public static @Nullable Category getCategory(String id) {
        return ConfigManager.CATEGORIES.get(id);
    }

    public void fillBosses() {
        for (Boss boss : ConfigManager.BOSSES.values()) {
            if (boss.categoryId.equalsIgnoreCase(categoryId)) bosses.put(boss.bossId, boss);
        }
    }

    public void fillModifiers() {
        for (CategoryModifier modifier : ConfigManager.CATEGORY_MODIFIERS.values()) {
            if (modifier.categoryId.equalsIgnoreCase(categoryId)) modifiers.put(modifier.modifierId, modifier);
        }
    }
}
