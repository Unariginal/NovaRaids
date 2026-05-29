package me.unariginal.novaraids.data.categories.modifiers;

import me.unariginal.novaraids.data.rewards.RewardDistribution;

import java.util.List;

public class RaidDetailModifiers {
    public int minimumLevelOffset;
    public int maximumLevelOffset;
    public int setupPhaseTimeOffset;
    public int fightPhaseTimeOffset;
    public boolean catchPhaseOverrideToggle;
    public boolean doCatchPhaseOverride;
    public int preCatchPhaseTimeOffset;
    public int catchPhaseTimeOffset;
    public int minimumPartySizeOffset;
    public int maximumPartySizeOffset;
    public boolean overrideCategoryDistribution;
    public List<RewardDistribution> rewardDistribution;
}
