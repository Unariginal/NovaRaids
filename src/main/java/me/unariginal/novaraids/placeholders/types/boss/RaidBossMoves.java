package me.unariginal.novaraids.placeholders.types.boss;

import com.cobblemon.mod.common.api.moves.Move;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.placeholders.ServerPlaceholder;
import me.unariginal.novaraids.raid.RaidManager;

import java.util.List;

public class RaidBossMoves implements ServerPlaceholder {
    @Override
    public GenericResult handle(List<String> args) {
        int raidSlot = 1;
        int moveSlot = 0;
        if (!args.isEmpty()) {
            try {
                raidSlot = Integer.parseInt(args.getFirst());
                if (args.size() > 1) moveSlot = Integer.parseInt(args.get(1));
            } catch (NumberFormatException ignored) {
                // No Crashy
            }
        }
        Raid raid = RaidManager.getRaid(raidSlot - 1);
        if (raid == null) return GenericResult.invalid("No Active Raid");
        Move move = raid.bossPokemon.getMoveSet().get(moveSlot);
        if (move == null) return GenericResult.invalid("null");

        return GenericResult.valid(move.getDisplayName().getString());
    }

    @Override
    public List<String> id() {
        return List.of("raid_boss_moves");
    }
}
