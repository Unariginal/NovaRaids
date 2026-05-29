package me.unariginal.novaraids.placeholders.types.raid;

import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.placeholders.ServerPlaceholder;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.raid.RaidManager;

import java.util.List;

public class RaidMaximumPartySize implements ServerPlaceholder {
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

        return GenericResult.valid(raid.boss.raidDetails.maximumPartySize);
    }

    @Override
    public List<String> id() {
        return List.of("raid_maximum_party_size", "raid_max_party_size");
    }
}
