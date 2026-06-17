package me.unariginal.novaraids.placeholders.types.categoryModifier;

import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
import me.unariginal.novaraids.placeholders.interfaces.CategoryModifierPlaceholder;
import me.unariginal.novaraids.placeholders.GenericResult;

import java.util.List;

public class ModifierId implements CategoryModifierPlaceholder {
    @Override
    public GenericResult handle(CategoryModifier modifier, List<String> args) {
        return GenericResult.valid(modifier.modifierId);
    }

    @Override
    public List<String> id() {
        return List.of("modifier_id");
    }
}
