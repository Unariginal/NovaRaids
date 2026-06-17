package me.unariginal.novaraids.placeholders.types.history;

import me.unariginal.novaraids.config.RaidHistory;
import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.placeholders.interfaces.RaidHistoryPlaceholder;

import java.util.List;

import static me.unariginal.novaraids.utils.TextUtils.hms;

public class HistoryFightDuration implements RaidHistoryPlaceholder {
    @Override
    public GenericResult handle(RaidHistory raidHistory, List<String> args) {
        return GenericResult.valid(hms((raidHistory.fightEndTime - raidHistory.fightStartTime)/20));
    }

    @Override
    public List<String> id() {
        return List.of("history_fight_duration");
    }
}
