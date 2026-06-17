package me.unariginal.novaraids.placeholders.types.categoryModifier;

import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
import me.unariginal.novaraids.placeholders.interfaces.CategoryModifierPlaceholder;
import me.unariginal.novaraids.placeholders.GenericResult;

import java.util.List;

public class ModifierChance implements CategoryModifierPlaceholder {
    @Override
    public GenericResult handle(CategoryModifier modifier, List<String> args) {
        Category category = Category.getCategory(modifier.categoryId);
        if (category == null) return GenericResult.invalid("No Category");

        int totalWeight = category.noModifierWeight;
        for (CategoryModifier loopModifier : category.modifiers.values()) {
            totalWeight += loopModifier.modifierWeight;
        }

        double chance = ((double) modifier.modifierWeight / totalWeight) * 100;

        return GenericResult.valid(String.format("%.2f%%", chance));
    }

    @Override
    public List<String> id() {
        return List.of("modifier_chance");
    }
}
