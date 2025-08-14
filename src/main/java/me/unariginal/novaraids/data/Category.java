package me.unariginal.novaraids.data;

import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;
import me.unariginal.novaraids.data.rewards.DistributionSection;

import java.util.List;

public record Category(String id,
                       String name,
                       boolean requirePass,
                       int minPlayers,
                       int maxPlayers,
                       Contraband contraband,
                       String setupBossbar,
                       String fightBossbar,
                       String preCatchBossbar,
                       String catchBossbar,
                       Voucher categoryChoiceVoucher,
                       Voucher categoryRandomVoucher,
                       Pass categoryPass,
                       List<RaidBall> categoryBalls,
                       List<DistributionSection> rewards) {
    public RaidBall getRaidBall(String key) {
        for (RaidBall ball : categoryBalls) {
            if (ball.id().equals(key)) {
                return ball;
            }
        }
        return null;
    }
}
