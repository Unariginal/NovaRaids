package me.unariginal.novaraids.data.events;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.config.ConfigManager;
import me.unariginal.novaraids.config.LocationConfig;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.placeholders.ParseContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static me.unariginal.novaraids.utils.TextUtils.deserialize;

public class Event {
    public transient String eventId;
    public boolean global;
    public List<String> messages;
    public List<String> playerCommands;
    public List<String> globalCommands;
    public List<SoundEvent> sounds;
    public List<EffectEvent> effects;
    public List<ParticleEvent> particles;
    public List<TitleEvent> titles;
    public List<MoLangEvent> molang;
    public WebhookEvent discordWebhook;

    public void sendMessages(ServerPlayerEntity player, Raid raid, @Nullable Integer damage, @Nullable ServerPlayerEntity eventPlayer) {
        for (String message : messages) {
            player.sendMessage(deserialize(message.replaceAll("%damage%", String.valueOf(damage)), ParseContext.builder().raid(raid).player(eventPlayer).build()));
        }
    }

    public void executeCommands(ServerPlayerEntity player, Integer damage) {
        CommandManager cmdManager = Objects.requireNonNull(player.getServer()).getCommandManager();
        ServerCommandSource source = player.getServer().getCommandSource();
        for (String command : playerCommands) {
            cmdManager.executeWithPrefix(source, command.replaceAll("%damage%", String.valueOf(damage)).replaceAll("%player%", player.getNameForScoreboard()));
        }
    }

    public void executeCommands(Integer damage) {
        CommandManager cmdManager = Objects.requireNonNull(NovaRaids.INSTANCE.server).getCommandManager();
        ServerCommandSource source = NovaRaids.INSTANCE.server.getCommandSource();
        for (String command : globalCommands) {
            cmdManager.executeWithPrefix(source, command.replaceAll("%damage%", String.valueOf(damage)));
        }
    }

    public void playSounds(LocationConfig location) {
        sounds.forEach(soundEvent -> soundEvent.playSound(location));
    }

    public void applyEffects(ServerPlayerEntity player) {
        effects.forEach(effectEvent -> effectEvent.applyEffect(player));
    }

    public void spawnParticles(LocationConfig location, PokemonEntity pokemonEntity) {
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

    public void showTitles(ServerPlayerEntity player, Raid raid, Integer damage) {
        titles.forEach(titleEvent -> titleEvent.showTitle(player, raid, damage));
    }

    public void runMolang(ServerPlayerEntity player, PokemonEntity pokemonEntity, Integer damage) {
        molang.forEach(moLangEvent -> moLangEvent.runMoLang(player, pokemonEntity, damage));
    }

    public static Event getEvent(String key, String id) {
        return ConfigManager.EVENTS.get(Identifier.of(key, id));
    }
}
