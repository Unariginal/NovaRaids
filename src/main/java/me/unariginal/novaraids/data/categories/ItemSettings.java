package me.unariginal.novaraids.data.categories;

import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;

import java.util.Map;

public class ItemSettings {
    public Voucher categoryChoiceVoucher;
    public Voucher categoryRandomVoucher;
    public Pass categoryPass;
    public Map<String, RaidBall> raidBalls;

    public RaidBall getRaidBall(String id) {
        return raidBalls.get(id);
    }
}
