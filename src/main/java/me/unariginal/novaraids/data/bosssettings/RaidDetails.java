package me.unariginal.novaraids.data.bosssettings;

import me.unariginal.novaraids.data.Contraband;
import me.unariginal.novaraids.data.rewards.DistributionSection;

import java.util.List;

public record RaidDetails(int minimumLevel,
                          int maximumLevel,
                          int setupPhaseTime,
                          int fightPhaseTime,
                          boolean doCatchPhase,
                          int preCatchPhaseTime,
                          int catchPhaseTime,
                          boolean healPartyOnChallenge,
                          Contraband contraband,
                          String setupBossbar,
                          String fightBossbar,
                          String preCatchBossbar,
                          String catchBossbar,
                          boolean overrideCategoryDistribution,
                          List<DistributionSection> rewards) {
}
