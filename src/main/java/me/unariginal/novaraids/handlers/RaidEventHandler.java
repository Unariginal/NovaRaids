package me.unariginal.novaraids.handlers;

import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.events.Event;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.managers.WebhookHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

import static me.unariginal.novaraids.config.ConfigManager.CONFIG;

public class RaidEventHandler {
    public static void runEvent(Event event, Raid raid, Integer damage, boolean global) {
        List<ServerPlayerEntity> players = new ArrayList<>();
        if (global) players.addAll(NovaRaids.INSTANCE.server().getPlayerManager().getPlayerList());
        else {
            raid.participatingPlayers.forEach(uuid -> {
                ServerPlayerEntity player = NovaRaids.INSTANCE.server().getPlayerManager().getPlayer(uuid);
                if (player != null) players.add(player);
            });
        }

        players.forEach(player -> {
            event.sendMessages(player, raid);
            event.executeCommands(player);
            event.showTitles(player, raid);
            event.runMolang(player, raid.bossEntity, damage);
        });

        event.playSounds(raid.location);
        event.spawnParticles(raid.location, raid.bossEntity);

        if (CONFIG.discordWebhook.enabled &&
                !CONFIG.discordWebhook.blacklistedCategories.contains(raid.category.categoryId) &&
                !CONFIG.discordWebhook.blacklistedBosses.contains(raid.bossInfo.bossId)) {
            if (event.discordWebhook != null) {
                WebhookHandler.sendWebhookEmbed(event.discordWebhook, raid).thenAccept(id -> {
                    raid.currentWebhookEvent = event.discordWebhook;
                    raid.webhookID = id;
                });
            } else {
                raid.currentWebhookEvent = null;
            }
        }
    }
}
