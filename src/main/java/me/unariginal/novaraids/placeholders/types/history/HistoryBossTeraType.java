package me.unariginal.novaraids.placeholders.types.history;

import me.unariginal.novaraids.config.RaidHistory;
import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.placeholders.interfaces.RaidHistoryPlaceholder;

import java.util.List;

public class HistoryBossTeraType implements RaidHistoryPlaceholder {
    @Override
    public GenericResult handle(RaidHistory raidHistory, List<String> args) {
        return GenericResult.valid(raidHistory.boss.teraType);
    }

    @Override
    public List<String> id() {
        return List.of("history_boss_tera_type");
    }
}
