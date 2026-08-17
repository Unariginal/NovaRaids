package me.unariginal.novaraids.placeholders.interfaces;

import me.unariginal.novaraids.config.RaidHistory;
import me.unariginal.novaraids.placeholders.GenericResult;

import java.util.List;

public interface RaidHistoryPlaceholder {
    GenericResult handle(RaidHistory raidHistory, List<String> args);
    List<String> id();
}
