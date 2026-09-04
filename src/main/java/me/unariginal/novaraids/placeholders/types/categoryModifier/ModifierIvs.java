package me.unariginal.novaraids.placeholders.types.categoryModifier;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import me.unariginal.novaraids.data.categories.modifiers.CategoryModifier;
import me.unariginal.novaraids.placeholders.interfaces.CategoryModifierPlaceholder;
import me.unariginal.novaraids.placeholders.GenericResult;

import java.util.List;

public class ModifierIvs implements CategoryModifierPlaceholder {
    @Override
    public GenericResult handle(CategoryModifier modifier, List<String> args) {
        if (args.isEmpty()) return GenericResult.invalid("Invalid stat key");
        String statStr = args.getFirst();

        Stat stat = Stats.Companion.getStat(statStr);
        if (Stats.Companion.getPERMANENT().stream().noneMatch(perm -> perm.getShowdownId().equalsIgnoreCase(stat.getShowdownId()))) {
            return GenericResult.invalid("Invalid stat key");
        }
        Integer iv = modifier.bossPokemonModifiers.ivsModifier.get(stat);
        if (iv == null) return GenericResult.invalid("Invalid stat key");
        return GenericResult.valid(iv);
    }

    @Override
    public List<String> id() {
        return List.of("modifier_ivs");
    }
}
