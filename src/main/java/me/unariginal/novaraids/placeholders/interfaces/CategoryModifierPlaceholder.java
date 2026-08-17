package me.unariginal.novaraids.placeholders.interfaces;

import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
import me.unariginal.novaraids.placeholders.GenericResult;

import java.util.List;

public interface CategoryModifierPlaceholder {
    GenericResult handle(CategoryModifier modifier, List<String> args);
    List<String> id();
}
