package me.unariginal.novaraids.placeholders.types.history;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import me.unariginal.novaraids.config.RaidHistory;
import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.placeholders.interfaces.RaidHistoryPlaceholder;

import java.util.List;

public class HistoryBossEvs implements RaidHistoryPlaceholder {
    @Override
    public GenericResult handle(RaidHistory raidHistory, List<String> args) {
        String statStr;
        if (args.isEmpty()) return GenericResult.invalid("Invalid stat key");

        statStr = args.getFirst();

        Stat stat = Stats.Companion.getStat(statStr);
        if (Stats.Companion.getPERMANENT().stream().noneMatch(perm -> perm.getShowdownId().equalsIgnoreCase(stat.getShowdownId()))) {
            return GenericResult.invalid("Invalid stat key");
        }

        Integer ev = raidHistory.boss.evs.get(stat);
        if (ev == null) return GenericResult.invalid("Invalid stat key");

        return GenericResult.valid(ev);
    }

    @Override
    public List<String> id() {
        return List.of("history_boss_evs");
    }
}
