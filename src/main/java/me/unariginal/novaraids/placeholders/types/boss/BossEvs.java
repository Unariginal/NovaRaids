package me.unariginal.novaraids.placeholders.types.boss;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.placeholders.interfaces.BossPlaceholder;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.GenericResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static me.unariginal.novaraids.placeholders.RaidGetter.getRaid;

public class BossEvs implements BossPlaceholder {
    @Override
    public GenericResult handle(@Nullable Raid raid, @Nullable Boss boss, Boolean prioritizeRaid, List<String> args) {
        if (prioritizeRaid || boss == null) {
            if (raid == null) {
                String statStr;
                if (args.size() < 2) return GenericResult.invalid("Invalid stat key");

                statStr = args.get(1);
                raid = getRaid(args);
                if (raid == null) {
                    if (boss != null) {
                        Stat stat = Stats.Companion.getStat(statStr);
                        if (Stats.Companion.getPERMANENT().stream().noneMatch(perm -> perm.getShowdownId().equalsIgnoreCase(stat.getShowdownId()))) {
                            return GenericResult.invalid("Invalid stat key");
                        }
                        Integer ev = boss.pokemonDetails.evs.get(stat);
                        if (ev == null) return GenericResult.invalid("Invalid stat key");

                        return GenericResult.valid(ev);
                    }
                    return GenericResult.invalid("No Active Raid");
                }
            }
            if (args.isEmpty()) return GenericResult.invalid("Invalid stat key");
            String statStr = args.getFirst();

            Stat stat = Stats.Companion.getStat(statStr);
            if (Stats.Companion.getPERMANENT().stream().noneMatch(perm -> perm.getShowdownId().equalsIgnoreCase(stat.getShowdownId()))) {
                return GenericResult.invalid("Invalid stat key");
            }
            Integer ev = raid.bossPokemon.getEvs().get(stat);
            if (ev == null) return GenericResult.invalid("Invalid stat key");

            return GenericResult.valid(ev);
        }

        if (args.isEmpty()) return GenericResult.invalid("Invalid stat key");
        String stat = args.getFirst();

        Integer ev = boss.pokemonDetails.evs.get(Stats.valueOf(stat.toUpperCase()));
        if (ev == null) return GenericResult.invalid("Invalid stat key");

        return GenericResult.valid(ev);
    }

    @Override
    public List<String> id() {
        return List.of("boss_evs");
    }
}
