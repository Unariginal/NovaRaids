package me.unariginal.novaraids.data.events;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormEntityParticlePacket;
import me.unariginal.novaraids.config.LocationsConfig;
import net.minecraft.util.Identifier;

import java.util.List;

public class SnowstormEntityParticleEvent extends ParticleEvent {
    public List<String> locators;

    public SnowstormEntityParticleEvent(String type, String particleResource, double xOffset, double yOffset, double zOffset, List<String> locators) {
        super(type, particleResource, xOffset, yOffset, zOffset);
        this.locators = locators;
    }

    public void spawnParticle(LocationsConfig location, PokemonEntity pokemonEntity) {
        SpawnSnowstormEntityParticlePacket snowstormEntityParticlePacket = new SpawnSnowstormEntityParticlePacket(
                Identifier.of(particleResource),
                pokemonEntity.getId(),
                locators,
                null,
                null
        );

        snowstormEntityParticlePacket.sendToPlayersAround(
                pokemonEntity.getX(),
                pokemonEntity.getY(),
                pokemonEntity.getZ(),
                location.borderRadius,
                location.getServerWorld().getRegistryKey(),
                player -> false
        );
    }
}
