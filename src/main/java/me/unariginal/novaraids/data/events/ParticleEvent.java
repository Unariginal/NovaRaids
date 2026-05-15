package me.unariginal.novaraids.data.events;

public class ParticleEvent {
    public String type;
    public String particleResource;
    public double xOffset;
    public double yOffset;
    public double zOffset;

    public ParticleEvent(String type, String particleResource, double xOffset, double yOffset, double zOffset) {
        this.type = type;
        this.particleResource = particleResource;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
    }
}
