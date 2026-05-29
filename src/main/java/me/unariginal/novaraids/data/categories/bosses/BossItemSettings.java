package me.unariginal.novaraids.data.categories.bosses;

import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;

import java.util.Map;

public class BossItemSettings {
    public boolean allowGlobalPokeballs;
    public boolean allowCategoryPokeballs;
    public Voucher bossVoucher;
    public Pass bossPass;
    public Map<String, RaidBall> raidBalls;

    public RaidBall getRaidBall(String id) {
        return raidBalls.get(id);
    }
}
