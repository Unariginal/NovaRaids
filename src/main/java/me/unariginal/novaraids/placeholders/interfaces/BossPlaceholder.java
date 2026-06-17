package me.unariginal.novaraids.placeholders.interfaces;

import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.raid.Raid;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface BossPlaceholder {
    GenericResult handle(@Nullable Raid raid, @Nullable Boss boss, Boolean prioritizeRaid, List<String> args);
    List<String> id();
}
