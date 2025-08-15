package me.unariginal.novaraids.data.rewards;

import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;

public abstract class Reward {
    protected JsonObject rewardObject;
    protected String name;
    protected String type;

    public Reward(JsonObject rewardObject, String name, String type) {
        this.rewardObject = rewardObject;
        this.name = name;
        this.type = type;
    }

    public JsonObject rewardObject() {
        return rewardObject;
    }

    public String type() {
        return type;
    }

    public String name() {
        return name;
    }

    public void applyReward(ServerPlayerEntity player) {}
}
