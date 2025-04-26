package me.unariginal.novaraids.data.bosssettings;

import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;

import java.util.List;

public record ItemSettings(boolean allow_global_pokeballs,
                           boolean allow_category_pokeballs,
                           Voucher voucher,
                           Pass pass,
                           List<RaidBall> raid_balls) {
}
