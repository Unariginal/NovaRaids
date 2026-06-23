package me.unariginal.novaraids.data.events;

import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormParticlePacket;
import me.unariginal.novaraids.config.LocationConfig;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class SnowstormParticleEvent extends ParticleEvent {
    public SnowstormParticleEvent(String particleResource, double xOffset, double yOffset, double zOffset) {
        super(particleResource, xOffset, yOffset, zOffset);
    }

    public void spawnParticle(LocationConfig location) {
        SpawnSnowstormParticlePacket snowstormParticlePacket = new SpawnSnowstormParticlePacket(
                Identifier.of(particleResource),
                new Vec3d(
                        location.bossLocation.xPos + xOffset,
                        location.bossLocation.yPos + yOffset,
                        location.bossLocation.zPos + zOffset
                )
        );

        snowstormParticlePacket.sendToPlayersAround(
                location.bossLocation.xPos + xOffset,
                location.bossLocation.yPos + yOffset,
                location.bossLocation.zPos + zOffset,
                location.borderRadius,
                location.getServerWorld().getRegistryKey(),
                player -> false
        );
    }
}
