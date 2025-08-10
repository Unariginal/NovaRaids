package me.unariginal.novaraids.data.rewards;

import net.minecraft.server.network.ServerPlayerEntity;

public abstract class Reward {
    protected String name;
    protected String type;

    public Reward(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String type() {
        return type;
    }

    public String name() {
        return name;
    }

    public void applyReward(ServerPlayerEntity player) {}
}
