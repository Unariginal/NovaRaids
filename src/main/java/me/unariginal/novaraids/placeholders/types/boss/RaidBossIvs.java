package me.unariginal.novaraids.placeholders.types.boss;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.placeholders.ServerPlaceholder;
import me.unariginal.novaraids.raid.RaidManager;

import java.util.List;

public class RaidBossIvs implements ServerPlaceholder {
    @Override
    public GenericResult handle(List<String> args) {
        int raidSlot = 1;
        String stat;
        if (args.size() < 2) return GenericResult.invalid("Invalid stat key");

        stat = args.get(1);

        try {
            raidSlot = Integer.parseInt(args.getFirst());
        } catch (NumberFormatException ignored) {
            // No Crashy
        }

        Raid raid = RaidManager.getRaid(raidSlot - 1);
        if (raid == null) return GenericResult.invalid("No Active Raid");

        Integer iv = raid.bossPokemon.getIvs().get(Stats.valueOf(stat.toUpperCase()));
        if (iv == null) return GenericResult.invalid("Invalid stat key");

        return GenericResult.valid(iv);
    }

    @Override
    public List<String> id() {
        return List.of("raid_boss_ivs");
    }
}
