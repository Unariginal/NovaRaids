package me.unariginal.novaraids.placeholders.types.raid;

import me.unariginal.novaraids.placeholders.interfaces.RaidPlaceholder;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.utils.TextUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static me.unariginal.novaraids.placeholders.RaidGetter.getRaid;

public class RaidDefeatedTime implements RaidPlaceholder {
    @Override
    public GenericResult handle(@Nullable Raid raid, List<String> args) {
        if (raid == null) raid = getRaid(args);
        if (raid == null) return GenericResult.invalid("No Active Raid");
        if (raid.bossDefeatTime() <= 0) return GenericResult.invalid("Raid boss is not defeated!");

        return GenericResult.valid(TextUtils.hms(raid.bossDefeatTime() / 20));
    }

    @Override
    public List<String> id() {
        return List.of("raid_defeated_time");
    }
}
