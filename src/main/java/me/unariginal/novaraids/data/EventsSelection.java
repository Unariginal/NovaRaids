package me.unariginal.novaraids.data;

public class EventsSelection {
    public EventData bossDamaged;
    public EventData bossDefeated;
    public EventData raidStart;
    public EventData raidEnd;
    public EventData raidLost;
    public EventData setupPhase;
    public EventData fightPhase;
    public EventData catchWarningPhase;
    public EventData catchPhase;

    public static class EventData {
        public String pre;
        public String post;
    }
}
