package me.unariginal.novaraids.handlers.custom;

import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.events.Event;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.handlers.WebhookHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

import static me.unariginal.novaraids.config.ConfigManager.CONFIG;

public class RaidEventHandler {
    public static void runEvent(Event event, Raid raid, Integer damage) {
        List<ServerPlayerEntity> players = new ArrayList<>();
        if (event.global) players.addAll(NovaRaids.INSTANCE.server.getPlayerManager().getPlayerList());
        else {
            raid.participatingPlayers.forEach(uuid -> {
                ServerPlayerEntity player = NovaRaids.INSTANCE.server.getPlayerManager().getPlayer(uuid);
                if (player != null) players.add(player);
            });
        }

        players.forEach(player -> {
            event.sendMessages(player, raid, damage);
            event.executeCommands(player, damage);
            event.applyEffects(player);
            event.showTitles(player, raid, damage);
            event.runMolang(player, raid.bossEntity, damage);
        });

        event.executeCommands(damage);
        event.playSounds(raid.location);
        event.spawnParticles(raid.location, raid.bossEntity);

        if (CONFIG.discordWebhook.enabled &&
                !CONFIG.discordWebhook.blacklistedCategories.contains(raid.category.categoryId) &&
                !CONFIG.discordWebhook.blacklistedBosses.contains(raid.boss.bossId)) {
            if (event.discordWebhook != null) {
                WebhookHandler.sendWebhookEmbed(event.discordWebhook, raid, damage).thenAccept(id -> {
                    raid.currentWebhookEvent = event.discordWebhook;
                    raid.webhookID = id;
                });
            } else {
                raid.currentWebhookEvent = null;
            }
        }
    }
}
