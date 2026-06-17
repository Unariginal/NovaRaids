package me.unariginal.novaraids.placeholders.types.boss;

import com.cobblemon.mod.common.api.types.tera.TeraType;
import com.cobblemon.mod.common.api.types.tera.TeraTypes;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.placeholders.interfaces.BossPlaceholder;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.GenericResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static me.unariginal.novaraids.placeholders.RaidGetter.getRaid;

public class BossTeraType implements BossPlaceholder {
    @Override
    public GenericResult handle(@Nullable Raid raid, @Nullable Boss boss, Boolean prioritizeRaid, List<String> args) {
        if (prioritizeRaid || boss == null) {
            if (raid == null) {
                raid = getRaid(args);
                if (raid == null) {
                    if (boss != null) {
                        TeraType teraType = TeraTypes.getByName(boss.pokemonDetails.teraType);
                        if (teraType == null) return GenericResult.invalid("null");
                        return GenericResult.valid(teraType.getDisplayName().getString());
                    }
                    return GenericResult.invalid("No Active Raid");
                }
            }
            return GenericResult.valid(raid.bossPokemon.getTeraType().getDisplayName().getString());
        }

        TeraType teraType = TeraTypes.getByName(boss.pokemonDetails.teraType);
        if (teraType == null) return GenericResult.invalid("null");
        return GenericResult.valid(teraType.getDisplayName().getString());
    }

    @Override
    public List<String> id() {
        return List.of("boss_tera_type");
    }
}
