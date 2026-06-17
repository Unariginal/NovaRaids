package me.unariginal.novaraids.placeholders.types.categoryModifier;

import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
import me.unariginal.novaraids.placeholders.interfaces.CategoryModifierPlaceholder;
import me.unariginal.novaraids.placeholders.GenericResult;

import java.util.List;

public class ModifierHealthIncreaseOffset implements CategoryModifierPlaceholder {
    @Override
    public GenericResult handle(CategoryModifier modifier, List<String> args) {
        return GenericResult.valid(modifier.bossDetailModifiers.healthIncreaseOffset);
    }

    @Override
    public List<String> id() {
        return List.of("modifier_health_increase_offset");
    }
}
