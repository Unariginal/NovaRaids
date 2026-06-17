package me.unariginal.novaraids.data.players;

import java.util.LinkedList;

public class PlayerRaidData {
    public String uuid;
    public String username;
    public int leaderboardPlacement;
    public int totalDamage;
    public boolean leftRaid;
    public LinkedList<BattleAttempt> battleAttempts;
    public CatchDetails catchResult;

    public PlayerRaidData(String uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.leaderboardPlacement = -1;
        this.totalDamage = 0;
        this.leftRaid = false;
        this.battleAttempts = new LinkedList<>();
        this.catchResult = new CatchDetails(false, null, null, null, null, false);
    }
}
