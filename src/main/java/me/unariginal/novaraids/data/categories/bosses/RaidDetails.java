package me.unariginal.novaraids.data.categories.bosses;

import me.unariginal.novaraids.data.BossbarSelection;
import me.unariginal.novaraids.data.Contraband;
import me.unariginal.novaraids.data.EventsSelection;
import me.unariginal.novaraids.data.rewards.RewardDistribution;

import java.util.List;

public class RaidDetails {
    public int minimumLevel;
    public int maximumLevel;
    public int setupPhaseTime;
    public int fightPhaseTime;
    public boolean doCatchPhase;
    public int preCatchPhaseTime;
    public int catchPhaseTime;
    public int minimumPartySize;
    public int maximumPartySize;
    public boolean healPartyOnChallenge;
    public Contraband contraband;
    public BossbarSelection bossbars;
    public EventsSelection events;
    public boolean overrideCategoryDistribution;
    public List<RewardDistribution> rewardDistribution;
}
