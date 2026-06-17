package me.unariginal.novaraids.placeholders.types.raid;

import me.unariginal.novaraids.placeholders.interfaces.RaidPlaceholder;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.GenericResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static me.unariginal.novaraids.placeholders.RaidGetter.getRaid;

public class RaidMaximumLevel implements RaidPlaceholder {
    @Override
    public GenericResult handle(@Nullable Raid raid, List<String> args) {
        if (raid == null) raid = getRaid(args);
        if (raid == null) return GenericResult.invalid("No Active Raid");

        return GenericResult.valid(raid.boss.raidDetails.maximumLevel);
    }

    @Override
    public List<String> id() {
        return List.of("raid_maximum_level", "raid_max_level");
    }
}
