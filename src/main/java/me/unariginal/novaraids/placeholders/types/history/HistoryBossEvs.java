package me.unariginal.novaraids.placeholders.types.history;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import me.unariginal.novaraids.config.RaidHistory;
import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.placeholders.interfaces.RaidHistoryPlaceholder;

import java.util.List;

public class HistoryBossEvs implements RaidHistoryPlaceholder {
    @Override
    public GenericResult handle(RaidHistory raidHistory, List<String> args) {
        String stat;
        if (args.isEmpty()) return GenericResult.invalid("Invalid stat key");

        stat = args.getFirst();

        Integer ev = raidHistory.boss.evs.get(Stats.valueOf(stat.toUpperCase()));
        if (ev == null) return GenericResult.invalid("Invalid stat key");

        return GenericResult.valid(ev);
    }

    @Override
    public List<String> id() {
        return List.of("history_boss_evs");
    }
}
