package me.unariginal.novaraids.raid;

public enum RaidPhase {
    INIT("init"),
    SETUP("setup"),
    FIGHT("fight"),
    PRE_CATCH("pre_catch"),
    CATCH("catch"),
    STOPPING("stopping");

    private final String name;

    RaidPhase(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
