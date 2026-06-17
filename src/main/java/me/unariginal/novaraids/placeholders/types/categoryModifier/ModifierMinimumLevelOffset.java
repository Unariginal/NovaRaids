package me.unariginal.novaraids.placeholders.types.categoryModifier;

import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
import me.unariginal.novaraids.placeholders.interfaces.CategoryModifierPlaceholder;
import me.unariginal.novaraids.placeholders.GenericResult;

import java.util.List;

public class ModifierMinimumLevelOffset implements CategoryModifierPlaceholder {
    @Override
    public GenericResult handle(CategoryModifier modifier, List<String> args) {
        return GenericResult.valid(modifier.raidDetailModifiers.minimumLevelOffset);
    }

    @Override
    public List<String> id() {
        return List.of("modifier_minimum_level_offset", "modifier_min_level_offset");
    }
}
