package me.unariginal.novaraids.data.events;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.unariginal.novaraids.config.ConfigManager;
import me.unariginal.novaraids.config.LocationsConfig;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.utils.TextUtils;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;

public class Event {
    public transient String eventId;
    public List<String> messages;
    public List<String> commands;
    public List<SoundEvent> sounds;
    public List<ParticleEvent> particles;
    public List<TitleEvent> titles;
    public List<MoLangEvent> molang;
    public WebhookEvent discordWebhook;

    public void sendMessages(ServerPlayerEntity player, Raid raid) {
        for (String message : messages) {
            player.sendMessage(TextUtils.deserialize(TextUtils.parse(message, raid)));
        }
    }

    public void executeCommands(ServerPlayerEntity player) {
        CommandManager cmdManager = Objects.requireNonNull(player.getServer()).getCommandManager();
        ServerCommandSource source = player.getServer().getCommandSource();
        for (String command : commands) {
            cmdManager.executeWithPrefix(source, command.replaceAll("%player%", player.getNameForScoreboard()));
        }
    }

    public void playSounds(LocationsConfig location) {
        sounds.forEach(soundEvent -> soundEvent.playSound(location));
    }

    public void spawnParticles(LocationsConfig location, PokemonEntity pokemonEntity) {
        particles.forEach(particleEvent -> {
            if (particleEvent instanceof VanillaParticleEvent vanillaParticleEvent) {
                vanillaParticleEvent.spawnParticle(location);
            } else if (particleEvent instanceof SnowstormParticleEvent snowstormParticleEvent) {
                snowstormParticleEvent.spawnParticle(location);
            } else if (particleEvent instanceof SnowstormEntityParticleEvent snowstormEntityParticleEvent) {
                snowstormEntityParticleEvent.spawnParticle(location, pokemonEntity);
            }
        });
    }

    public void showTitles(ServerPlayerEntity player, Raid raid) {
        titles.forEach(titleEvent -> titleEvent.showTitle(player, raid));
    }

    public void runMolang(ServerPlayerEntity player, PokemonEntity pokemonEntity, Integer damage) {
        molang.forEach(moLangEvent -> moLangEvent.runMoLang(player, pokemonEntity, damage));
    }

    public static Event getEvent(String key, String id) {
        return ConfigManager.EVENTS.get(Identifier.of(key, id));
    }
}
