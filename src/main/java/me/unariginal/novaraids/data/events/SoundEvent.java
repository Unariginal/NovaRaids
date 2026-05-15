package me.unariginal.novaraids.data.events;

import com.cobblemon.mod.common.net.messages.client.sound.UnvalidatedPlaySoundS2CPacket;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.config.LocationsConfig;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

public class SoundEvent {
    public String soundResource;
    public String soundCategory = "master";
    public float volume;
    public float pitch;

    public void playSound(LocationsConfig location) {
        SoundCategory soundCategory = SoundCategory.MASTER;
        try {
            soundCategory = SoundCategory.valueOf(this.soundCategory);
        } catch (IllegalArgumentException e) {
            NovaRaids.LOGGER.warn("[NovaRaids] Invalid sound category {}. Using master.", this.soundCategory);
        }

        UnvalidatedPlaySoundS2CPacket soundS2CPacket = new UnvalidatedPlaySoundS2CPacket(
                Identifier.of(soundResource),
                soundCategory,
                location.bossLocation.xPos,
                location.bossLocation.yPos,
                location.bossLocation.zPos,
                volume,
                pitch
        );

        soundS2CPacket.sendToPlayersAround(
                location.bossLocation.xPos,
                location.bossLocation.yPos,
                location.bossLocation.zPos,
                location.borderRadius,
                location.getServerWorld().getRegistryKey(),
                player -> false
        );
    }
}
