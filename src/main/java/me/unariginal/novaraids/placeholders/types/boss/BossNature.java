package me.unariginal.novaraids.placeholders.types.boss;

import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.util.MiscUtilsKt;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.placeholders.interfaces.BossPlaceholder;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.GenericResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static me.unariginal.novaraids.placeholders.RaidGetter.getRaid;

public class BossNature implements BossPlaceholder {
    @Override
    public GenericResult handle(@Nullable Raid raid, @Nullable Boss boss, Boolean prioritizeRaid, List<String> args) {
        if (prioritizeRaid || boss == null) {
            if (raid == null) {
                raid = getRaid(args);
                if (raid == null) {
                    if (boss != null) {
                        Nature nature = Natures.getNature(boss.pokemonDetails.getRandomNature());
                        if (nature == null) return GenericResult.invalid("null");
                        return GenericResult.valid(MiscUtilsKt.asTranslated(nature.getDisplayName()).getString());
                    }
                    return GenericResult.invalid("No Active Raid");
                }
            }
            return GenericResult.valid(MiscUtilsKt.asTranslated(raid.bossPokemon.getNature().getDisplayName()).getString());
        }

        Nature nature = Natures.getNature(boss.pokemonDetails.getRandomNature());
        if (nature == null) return GenericResult.invalid("null");
        return GenericResult.valid(MiscUtilsKt.asTranslated(nature.getDisplayName()).getString());
    }

    @Override
    public List<String> id() {
        return List.of("boss_nature");
    }
}
