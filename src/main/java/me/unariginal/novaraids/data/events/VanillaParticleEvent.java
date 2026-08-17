package me.unariginal.novaraids.data.events;

import me.unariginal.novaraids.config.LocationConfig;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class VanillaParticleEvent extends ParticleEvent {
    public int count;
    public double deltaX;
    public double deltaY;
    public double deltaZ;
    public double speed;

    public VanillaParticleEvent(
            String particleResource,
            double xOffset,
            double yOffset,
            double zOffset,
            int count,
            double deltaX,
            double deltaY,
            double deltaZ,
            double speed
    ) {
        super(particleResource, xOffset, yOffset, zOffset);
        this.count = count;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.deltaZ = deltaZ;
        this.speed = speed;
    }

    public void spawnParticle(LocationConfig location) {
        Optional<ParticleType<?>> particleType = Registries.PARTICLE_TYPE.getOrEmpty(Identifier.of(particleResource));
        if (particleType.isPresent() && particleType.get() instanceof SimpleParticleType simpleParticleType) {
            location.getServerWorld().spawnParticles(
                    simpleParticleType,
                    location.bossLocation.xPos + xOffset,
                    location.bossLocation.yPos + yOffset,
                    location.bossLocation.zPos + zOffset,
                    count,
                    deltaX,
                    deltaY,
                    deltaZ,
                    speed
            );
        }
    }
}
