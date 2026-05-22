package me.unariginal.novaraids.data.events;

public class ParticleEvent {
    public String particleResource;
    public double xOffset;
    public double yOffset;
    public double zOffset;

    public ParticleEvent(String particleResource, double xOffset, double yOffset, double zOffset) {
        this.particleResource = particleResource;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
    }
}
