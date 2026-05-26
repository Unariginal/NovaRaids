package me.unariginal.novaraids.raid;

public enum RaidStatus {
    IN_PROGRESS("in_progress"),
    WON("won"),
    LOST("lost");

    private final String name;

    RaidStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
