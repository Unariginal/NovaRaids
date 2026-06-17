package me.unariginal.novaraids.placeholders.types.boss;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.placeholders.interfaces.BossPlaceholder;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.GenericResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static me.unariginal.novaraids.placeholders.RaidGetter.getRaid;

public class BossMoves implements BossPlaceholder {
    @Override
    public GenericResult handle(@Nullable Raid raid, @Nullable Boss boss, Boolean prioritizeRaid, List<String> args) {
        int moveSlot = 0;
        if (prioritizeRaid || boss == null) {
            if (raid == null) {
                if (args.size() < 2) return GenericResult.invalid("No Move");
                try {
                    moveSlot = Integer.parseInt(args.getFirst());
                } catch (NumberFormatException ignored) {}

                raid = getRaid(args);
                if (raid == null) {
                    if (boss != null) {
                        if (moveSlot >= boss.pokemonDetails.moves.size()) return GenericResult.invalid("No Move");
                        String moveName = boss.pokemonDetails.moves.get(moveSlot);
                        MoveTemplate moveTemplate = Moves.getByName(moveName);
                        if (moveTemplate == null) return GenericResult.invalid("No Move");
                        return GenericResult.valid(moveTemplate.getDisplayName().getString());
                    }
                    return GenericResult.invalid("No Active Raid");
                }
            }
            if (args.isEmpty()) return GenericResult.invalid("No Move");
            try {
                moveSlot = Integer.parseInt(args.getFirst());
            } catch (NumberFormatException ignored) {}
            Move move = raid.bossPokemon.getMoveSet().get(moveSlot);
            if (move == null) return GenericResult.invalid("No Move");

            return GenericResult.valid(move.getDisplayName().getString());
        }

        if (args.isEmpty()) return GenericResult.invalid("No Move");
        try {
            moveSlot = Integer.parseInt(args.getFirst());
        } catch (NumberFormatException ignored) {}
        if (moveSlot >= boss.pokemonDetails.moves.size()) return GenericResult.invalid("No Move");
        String moveName = boss.pokemonDetails.moves.get(moveSlot);
        MoveTemplate moveTemplate = Moves.getByName(moveName);
        if (moveTemplate == null) return GenericResult.invalid("No Move");

        return GenericResult.valid(moveTemplate.getDisplayName().getString());
    }

    @Override
    public List<String> id() {
        return List.of("boss_moves");
    }
}
