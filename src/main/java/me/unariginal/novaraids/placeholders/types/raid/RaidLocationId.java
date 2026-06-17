package me.unariginal.novaraids.placeholders.types.raid;

import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.placeholders.interfaces.RaidPlaceholder;
import me.unariginal.novaraids.raid.Raid;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static me.unariginal.novaraids.placeholders.RaidGetter.getRaid;

public class RaidLocationId implements RaidPlaceholder {
    @Override
    public GenericResult handle(@Nullable Raid raid, List<String> args) {
        if (raid == null) raid = getRaid(args);
        if (raid == null) return GenericResult.invalid("No Active Raid");

        return GenericResult.valid(raid.location.locationId);
    }

    @Override
    public List<String> id() {
        return List.of("raid_location_id");
    }
}
