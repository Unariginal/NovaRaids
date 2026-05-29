package me.unariginal.novaraids.data.categories.modifiers;

import me.unariginal.novaraids.config.ConfigManager;
import me.unariginal.novaraids.data.categories.Category;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class CategoryModifier {
    public transient String categoryId;
    public String modifierId;
    public String modifierName;
    public int modifierWeight;
    public BossPokemonModifiers bossPokemonModifiers;
    public BossDetailModifiers bossDetailModifiers;
    public RaidDetailModifiers raidDetailModifiers;
    public CatchSettingModifiers catchSettingModifiers;

    public static @Nullable CategoryModifier getModifier(String id) {
        return ConfigManager.CATEGORY_MODIFIERS.get(id);
    }

    public static @Nullable CategoryModifier getRandomModifier(String categoryId) {
        Category category = Category.getCategory(categoryId);
        if (category == null) return null;

        double totalWeight = category.noModifierWeight;
        for (CategoryModifier modifier : category.modifiers.values()) {
            totalWeight += modifier.modifierWeight;
        }

        if (totalWeight > 0) {
            double randomWeight = new Random().nextDouble(totalWeight);
            totalWeight = category.noModifierWeight;
            if (randomWeight < totalWeight) return null;
            for (CategoryModifier modifier : category.modifiers.values()) {
                totalWeight += modifier.modifierWeight;
                if (randomWeight < totalWeight) {
                    return modifier;
                }
            }
        }
        return null;
    }
}
