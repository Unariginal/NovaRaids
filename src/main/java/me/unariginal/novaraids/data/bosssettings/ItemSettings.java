package me.unariginal.novaraids.data.bosssettings;

import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;

import java.util.List;

public record ItemSettings(boolean allowGlobalPokeballs,
                           boolean allowCategoryPokeballs,
                           Voucher voucher,
                           Pass pass,
                           List<RaidBall> raidBalls) {
    public RaidBall getRaidBall(String key) {
        for (RaidBall ball : raidBalls) {
            if (ball.id().equals(key)) {
                return ball;
            }
        }
        return null;
    }
}
