package me.unariginal.novaraids.placeholders.types.raid;

import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.placeholders.ServerPlaceholder;
import me.unariginal.novaraids.raid.RaidManager;
import me.unariginal.novaraids.utils.TextUtils;

import java.util.List;

public class RaidCompletionTime implements ServerPlaceholder {
    @Override
    public GenericResult handle(List<String> args) {
        int raidSlot = 1;
        if (!args.isEmpty()) {
            try {
                raidSlot = Integer.parseInt(args.getFirst());
            } catch (NumberFormatException ignored) {
                // No Crashy
            }
        }
        Raid raid = RaidManager.getRaid(raidSlot - 1);
        if (raid == null) return GenericResult.invalid("No Active Raid");
        if (raid.raidCompletionTime() <= 0) return GenericResult.invalid("Raid Is Ongoing");

        return GenericResult.valid(TextUtils.hms(raid.raidCompletionTime() / 20));
    }

    @Override
    public List<String> id() {
        return List.of("raid_completion_time");
    }
}
