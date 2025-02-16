package me.unariginal.novaraids.data.rewards;

public class Reward {
    protected String name;
    protected String type;

    public Reward(String name, String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }

    public String name() {
        return name;
    }
}
