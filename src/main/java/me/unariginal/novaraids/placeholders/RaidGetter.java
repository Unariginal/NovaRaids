package me.unariginal.novaraids.placeholders;

import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.raid.RaidManager;

import java.util.List;

public class RaidGetter {
    public static Raid getRaid(List<String> args) {
        int raidSlot = 1;
        if (!args.isEmpty()) {
            try {
                raidSlot = Integer.parseInt(args.getFirst());
            } catch (NumberFormatException ignored) {}
        }
        return RaidManager.getRaid(raidSlot - 1);
    }
}
