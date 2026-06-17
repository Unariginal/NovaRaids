package me.unariginal.novaraids.data.players;

//import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;

public class BattleAttempt {
    public int damage;
    public int lengthSeconds;
    public int turns;
//    public PlayerPartyStore party;

    public BattleAttempt(int damage, int lengthSeconds, int turns) {
        this.damage = damage;
        this.lengthSeconds = lengthSeconds;
        this.turns = turns;
    }
}
