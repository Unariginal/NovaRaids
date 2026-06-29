package me.unariginal.novaraids.handlers.custom;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.events.Event;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.handlers.WebhookHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static me.unariginal.novaraids.config.ConfigManager.CONFIG;

public class RaidEventHandler {
    public static void runEvent(Event event, Raid raid, @Nullable Integer damage) {
        runEvent(event, raid, damage, null);
    }

    public static void runEvent(Event event, Raid raid, @Nullable Integer damage, @Nullable ServerPlayerEntity eventPlayer) {
        Event.EventSection eventSection;
        if (raid.modifier != null) eventSection = event.modifier;
        else eventSection = event.noModifier;

        List<ServerPlayerEntity> players = new ArrayList<>();
        if (event.global) players.addAll(NovaRaids.INSTANCE.server.getPlayerManager().getPlayerList());
        else {
            raid.participatingPlayers.forEach(uuid -> {
                ServerPlayerEntity player = NovaRaids.INSTANCE.server.getPlayerManager().getPlayer(uuid);
                if (player != null) players.add(player);
            });
        }

        PokemonEntity bossEntity = raid.getBossEntity();

        players.forEach(player -> {
            eventSection.sendMessages(player, raid, damage, eventPlayer);
            eventSection.executeCommands(player, damage);
            eventSection.applyEffects(player);
            eventSection.showTitles(player, raid, damage);
            eventSection.runMolang(player, bossEntity, damage);
        });

        eventSection.executeCommands(damage);
        eventSection.playSounds(raid.location);
        eventSection.spawnParticles(raid.location, bossEntity);

        if (CONFIG.discordWebhook.enabled &&
                !CONFIG.discordWebhook.blacklistedCategories.contains(raid.category.categoryId) &&
                !CONFIG.discordWebhook.blacklistedBosses.contains(raid.boss.bossId)) {
            if (eventSection.discordWebhook != null) {
                WebhookHandler.sendWebhookEmbed(eventSection.discordWebhook, raid, damage).thenAccept(id -> {
                    raid.currentWebhookEvent = eventSection.discordWebhook;
                    raid.webhookID = id;
                });
            }
        }
    }
}
