package me.unariginal.novaraids.placeholders.types.categoryModifier;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
import me.unariginal.novaraids.placeholders.interfaces.CategoryModifierPlaceholder;
import me.unariginal.novaraids.placeholders.GenericResult;

import java.util.List;

public class ModifierEvs implements CategoryModifierPlaceholder {
    @Override
    public GenericResult handle(CategoryModifier modifier, List<String> args) {
        if (args.isEmpty()) return GenericResult.invalid("Invalid stat key");
        String stat = args.getFirst();

        Integer ev = modifier.bossPokemonModifiers.evsModifier.get(Stats.valueOf(stat.toUpperCase()));
        if (ev == null) return GenericResult.invalid("Invalid stat key");
        return GenericResult.valid(ev);
    }

    @Override
    public List<String> id() {
        return List.of("modifier_evs");
    }
}
