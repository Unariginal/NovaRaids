package me.unariginal.novaraids.placeholders.interfaces;

import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.raid.Raid;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface RaidPlaceholder {
    GenericResult handle(@Nullable Raid raid, List<String> args);
    List<String> id();
}
