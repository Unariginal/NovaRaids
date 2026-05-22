package me.unariginal.novaraids.data.events;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class EffectEvent {
    public String effectResource;
    public int duration;
    public int amplifier;
    public boolean ambient;
    public boolean showParticles;
    public boolean showIcon;

    public void applyEffect(ServerPlayerEntity player) {
        if (Registries.STATUS_EFFECT.containsId(Identifier.of(effectResource))) {
            player.addStatusEffect(new StatusEffectInstance(
                    RegistryEntry.of(Registries.STATUS_EFFECT.get(Identifier.of(effectResource))),
                    duration,
                    amplifier,
                    ambient,
                    showParticles,
                    showIcon
            ));
        }
    }

    public void clearEffect(ServerPlayerEntity player) {
        if (Registries.STATUS_EFFECT.containsId(Identifier.of(effectResource))) {
            player.removeStatusEffect(RegistryEntry.of(Registries.STATUS_EFFECT.get(Identifier.of(effectResource))));
        }
    }
}
