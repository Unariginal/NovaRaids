package me.unariginal.novaraids.data;

import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;
import me.unariginal.novaraids.data.rewards.DistributionSection;

import java.util.List;

public record Category(String name,
                      boolean require_pass,
                      int min_players,
                      int max_players,
                      String setup_bossbar,
                      String fight_bossbar,
                      String pre_catch_bossbar,
                      String catch_bossbar,
                      Voucher category_choice_voucher,
                      Voucher category_random_voucher,
                      Pass category_pass,
                      List<RaidBall> category_balls,
                      List<DistributionSection> rewards) {
    public RaidBall getRaidBall(String key) {
        for (RaidBall ball : category_balls) {
            if (ball.id().equals(key)) {
                return ball;
            }
        }
        return null;
    }
}
