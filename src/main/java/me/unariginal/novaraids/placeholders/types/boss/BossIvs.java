package me.unariginal.novaraids.placeholders.types.boss;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.placeholders.interfaces.BossPlaceholder;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.GenericResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static me.unariginal.novaraids.placeholders.RaidGetter.getRaid;

public class BossIvs implements BossPlaceholder {
    @Override
    public GenericResult handle(@Nullable Raid raid, @Nullable Boss boss, Boolean prioritizeRaid, List<String> args) {
        if (prioritizeRaid || boss == null) {
            if (raid == null) {
                String stat;
                if (args.size() < 2) return GenericResult.invalid("Invalid stat key");

                stat = args.get(1);
                raid = getRaid(args);
                if (raid == null) {
                    if (boss != null) {
                        Integer iv = boss.pokemonDetails.ivs.get(Stats.valueOf(stat.toUpperCase()));
                        if (iv == null) return GenericResult.invalid("Invalid stat key");

                        return GenericResult.valid(iv);
                    }
                    return GenericResult.invalid("No Active Raid");
                }
            }
            if (args.isEmpty()) return GenericResult.invalid("Invalid stat key");
            String stat = args.getFirst();

            Integer iv = raid.bossPokemon.getIvs().get(Stats.valueOf(stat.toUpperCase()));
            if (iv == null) return GenericResult.invalid("Invalid stat key");

            return GenericResult.valid(iv);
        }

        if (args.isEmpty()) return GenericResult.invalid("Invalid stat key");
        String stat = args.getFirst();

        Integer iv = boss.pokemonDetails.ivs.get(Stats.valueOf(stat.toUpperCase()));
        if (iv == null) return GenericResult.invalid("Invalid stat key");

        return GenericResult.valid(iv);
    }

    @Override
    public List<String> id() {
        return List.of("boss_ivs");
    }
}
