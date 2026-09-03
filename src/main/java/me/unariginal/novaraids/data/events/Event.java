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
import static me.unariginal.novaraids.utils.TextUtils.parse;

public class Event {
    public transient String eventId;
    public boolean global;
    public EventSection noModifier;
    public EventSection modifier;

    public static class EventSection {
        @Nullable
        public List<String> messages;
        @Nullable
        public List<String> playerCommands;
        @Nullable
        public List<String> globalCommands;
        @Nullable
        public List<SoundEvent> sounds;
        @Nullable
        public List<EffectEvent> effects;
        @Nullable
        public List<ParticleEvent> particles;
        @Nullable
        public List<TitleEvent> titles;
        @Nullable
        public List<MoLangEvent> molang;
        @Nullable
        public WebhookEvent discordWebhook;

        public void sendMessages(ServerPlayerEntity player, Raid raid, @Nullable Integer damage, @Nullable ServerPlayerEntity eventPlayer) {
            if (messages == null) return;
            for (String message : messages) {
                player.sendMessage(deserialize(message.replaceAll("%damage%", String.valueOf(damage)), ParseContext.builder().raid(raid).player(eventPlayer).build()));
            }
        }

        public void executeCommands(Raid raid, ServerPlayerEntity player, @Nullable Integer damage) {
            if (playerCommands == null) return;
            CommandManager cmdManager = Objects.requireNonNull(player.getServer()).getCommandManager();
            ServerCommandSource source = player.getServer().getCommandSource();
            for (String command : playerCommands) {
                cmdManager.executeWithPrefix(source, parse(command.replaceAll("%damage%", String.valueOf(damage)), ParseContext.builder().raid(raid).player(player).build()));
            }
        }

        public void executeCommands(Raid raid, @Nullable Integer damage) {
            if (globalCommands == null) return;
            CommandManager cmdManager = Objects.requireNonNull(NovaRaids.INSTANCE.server).getCommandManager();
            ServerCommandSource source = NovaRaids.INSTANCE.server.getCommandSource();
            for (String command : globalCommands) {
                cmdManager.executeWithPrefix(source, parse(command.replaceAll("%damage%", String.valueOf(damage)), ParseContext.builder().raid(raid).build()));
            }
        }

        public void playSounds(LocationConfig location) {
            if (sounds == null) return;
            sounds.forEach(soundEvent -> soundEvent.playSound(location));
        }

        public void applyEffects(ServerPlayerEntity player) {
            if (effects == null) return;
            effects.forEach(effectEvent -> effectEvent.applyEffect(player));
        }

        public void spawnParticles(LocationConfig location, @Nullable PokemonEntity pokemonEntity) {
            if (particles == null) return;
            particles.forEach(particleEvent -> {
                if (particleEvent instanceof VanillaParticleEvent vanillaParticleEvent) {
                    vanillaParticleEvent.spawnParticle(location);
                } else if (particleEvent instanceof SnowstormParticleEvent snowstormParticleEvent) {
                    snowstormParticleEvent.spawnParticle(location);
                } else if (particleEvent instanceof SnowstormEntityParticleEvent snowstormEntityParticleEvent && pokemonEntity != null) {
                    snowstormEntityParticleEvent.spawnParticle(location, pokemonEntity);
                }
            });
        }

        public void showTitles(ServerPlayerEntity player, Raid raid, @Nullable Integer damage) {
            if (titles == null) return;
            titles.forEach(titleEvent -> titleEvent.showTitle(player, raid, damage));
        }

        public void runMolang(ServerPlayerEntity player, @Nullable PokemonEntity pokemonEntity, @Nullable Integer damage) {
            if (molang == null) return;
            molang.forEach(moLangEvent -> moLangEvent.runMoLang(player, pokemonEntity, damage));
        }
    }

    public static Event getEvent(String key, String id) {
        return ConfigManager.EVENTS.get(Identifier.of(key, id));
    }
}
