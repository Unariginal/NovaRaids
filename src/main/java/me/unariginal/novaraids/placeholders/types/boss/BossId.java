package me.unariginal.novaraids.placeholders.types.boss;

import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.placeholders.interfaces.BossPlaceholder;
import me.unariginal.novaraids.raid.Raid;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static me.unariginal.novaraids.placeholders.RaidGetter.getRaid;

public class BossId implements BossPlaceholder {
    @Override
    public GenericResult handle(@Nullable Raid raid, @Nullable Boss boss, Boolean prioritizeRaid, List<String> args) {
        if (prioritizeRaid || boss == null) {
            if (raid == null) {
                raid = getRaid(args);
                if (raid == null) {
                    if (boss != null) {
                        return GenericResult.valid(boss.bossId);
                    }
                    return GenericResult.invalid("No Active Raid");
                }
            }
            return GenericResult.valid(raid.boss.bossId);
        }

        return GenericResult.valid(boss.bossId);
    }

    @Override
    public List<String> id() {
        return List.of("boss_id");
    }
}
