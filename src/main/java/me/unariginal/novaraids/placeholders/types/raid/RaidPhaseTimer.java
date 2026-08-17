package me.unariginal.novaraids.placeholders.types.raid;

import me.unariginal.novaraids.placeholders.interfaces.RaidPlaceholder;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.utils.TextUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static me.unariginal.novaraids.placeholders.RaidGetter.getRaid;

public class RaidPhaseTimer implements RaidPlaceholder {
    @Override
    public GenericResult handle(@Nullable Raid raid, List<String> args) {
        if (raid == null) raid = getRaid(args);
        if (raid == null) return GenericResult.invalid("No Active Raid");

        long phaseRemaining = ((raid.phaseStartTime + (raid.phaseLength * 20L)) - raid.location.getServerWorld().getTime()) / 20;
        return GenericResult.valid(TextUtils.hms(phaseRemaining));
    }

    @Override
    public List<String> id() {
        return List.of("raid_phase_timer");
    }
}
