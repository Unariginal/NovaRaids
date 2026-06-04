package me.unariginal.novaraids.handlers;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.authlib.GameProfile;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.events.WebhookEvent;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.utils.TextUtils;
import net.minecraft.util.UserCache;

import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.unariginal.novaraids.NovaRaids.logError;
import static me.unariginal.novaraids.config.ConfigManager.CONFIG;

public class WebhookHandler {
    private static final UserCache cache = NovaRaids.INSTANCE.server.getUserCache();

    public static WebhookClient webhook = null;

    private static int hexToRGB(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        int hexVal = Integer.parseInt(hex, 16);
        int r = (hexVal >> 16) & 0xFF;
        int g = (hexVal >> 8) & 0xFF;
        int b = (hexVal) & 0xFF;
        return new Color(r, g, b).getRGB();
    }

    private static int genTypeColor(Pokemon pokemon) {
        return switch (pokemon.getPrimaryType().getName()) {
            case "bug" -> hexToRGB("91A119");
            case "dark" -> hexToRGB("624D4E");
            case "dragon" -> hexToRGB("5060E1");
            case "electric" -> hexToRGB("FAC000");
            case "fairy" -> hexToRGB("EF70EF");
            case "fighting" -> hexToRGB("FF8000");
            case "fire" -> hexToRGB("E62829");
            case "flying" -> hexToRGB("81B9EF");
            case "ghost" -> hexToRGB("704170");
            case "grass" -> hexToRGB("3FA129");
            case "ground" -> hexToRGB("915121");
            case "ice" -> hexToRGB("3DCEF3");
            case "poison" -> hexToRGB("9141CB");
            case "psychic" -> hexToRGB("EF4179");
            case "rock" -> hexToRGB("AFA981");
            case "steel" -> hexToRGB("60A1B8");
            case "water" -> hexToRGB("2980EF");
            default -> hexToRGB("9FA19F");
        };
    }

    private static String getThumbnailUrl(Pokemon pokemon) {
        String baseUrl = CONFIG.discordWebhook.thumbnailDatabaseUrl;

        if (!pokemon.getForm().formOnlyShowdownId().equalsIgnoreCase("normal")) {
            String url = baseUrl.replace("%rute%", pokemon.getShiny() ? "ani-shiny" : "ani")
                    .replace("%pokemon%", pokemon.getSpecies().getName().toLowerCase())
                    .replace("%form%", "-" + pokemon.getForm().formOnlyShowdownId());

            if (isUrlAccessible(url)) {
                return url;
            }
        }

        String url = baseUrl.replace("%rute%", pokemon.getShiny() ? "ani-shiny" : "ani")
                .replace("%pokemon%", pokemon.getSpecies().getName().toLowerCase())
                .replace("%form%", "");

        if (isUrlAccessible(url)) {
            return url;
        }

        List<String> fallbackUrls = List.of(
                "https://raw.githubusercontent.com/SkyNetCloud/sprites/master/sprites/pokemon/" +
                        pokemon.getSpecies().getNationalPokedexNumber() + ".png"
        );

        for (String fallbackUrl : fallbackUrls) {
            if (isUrlAccessible(fallbackUrl)) {
                return fallbackUrl;
            }
        }

        return "https://play.pokemonshowdown.com/sprites/ani/substitute.gif";
    }

    private static boolean isUrlAccessible(String url) {
        try {
            URI uri = new URI(url);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();
            int responseCode = connection.getResponseCode();

            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            return false;
        }
    }

    public static void connectWebhook() {
        webhook = WebhookClient.withUrl(CONFIG.discordWebhook.url);
    }

    public static void deleteWebhook(long id) {
        webhook.delete(id).exceptionally(e -> {
            logError("Failed to delete webhook: " + e.getMessage());
            return null;
        });
    }

    public static void deleteWebhook(Raid raid) {
        deleteWebhook(raid.webhookID);
    }

    public static CompletableFuture<Long> editWebhookEmbed(WebhookEvent event, Raid raid, Integer damage) {
        return webhook.edit(raid.webhookID, buildWebhookEmbed(event, raid, damage).build())
                .thenApply(ReadonlyMessage::getId)
                .exceptionally(e -> {
                    logError("Failed to edit webhook embed: " + e.getMessage());
                    return null;
                });
    }

    public static CompletableFuture<Long> sendWebhookEmbed(WebhookEvent event, Raid raid, Integer damage) {
        if (raid.webhookID == 0) {
            return webhook.send(buildWebhookEmbed(event, raid, damage).build())
                    .thenApply(ReadonlyMessage::getId)
                    .exceptionally(e -> {
                        logError("Failed to send webhook embed: " + e.getMessage());
                        return 0L;
                    });
        } else {
            return editWebhookEmbed(event, raid, damage);
        }
    }

    public static WebhookMessageBuilder buildWebhookEmbed(WebhookEvent event, Raid raid, Integer damage) {
        raid.webhookDamage = damage;
        Pokemon pokemon = raid.bossPokemon;
        int randColor = genTypeColor(pokemon);
        String thumbnailUrl = getThumbnailUrl(pokemon);

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                        new WebhookEmbed.EmbedAuthor(
                                TextUtils.parse(event.embedTitle.replaceAll("%damage%", String.valueOf(damage)), raid),
                                "",
                                thumbnailUrl
                        )
                );
        for (WebhookEvent.EmbedField field : event.fields) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(field.inline, TextUtils.parse(field.name.replaceAll("%damage%", String.valueOf(damage)), raid), TextUtils.parse(field.value.replaceAll("%damage%", String.valueOf(damage)), raid)));
            if (field.insertLeaderboardAfter != null && field.insertLeaderboardAfter && event.leaderboardFieldLayout != null) {
                Map<UUID, Integer> leaderboard = raid.getDamageLeaderboard();
                for (int i = 0; i < Math.min(leaderboard.size(), 10); i++) {
                    Map.Entry<UUID, Integer> entry = leaderboard.entrySet().stream().toList().get(i);
                    if (cache != null) {
                        GameProfile user = cache.getByUuid(entry.getKey()).orElseThrow();
                        String name = TextUtils.parse(event.leaderboardFieldLayout.name.replaceAll("%damage%", String.valueOf(damage)), raid, user, entry.getValue(), i + 1);
                        String value = TextUtils.parse(event.leaderboardFieldLayout.value.replaceAll("%damage%", String.valueOf(damage)), raid, user, entry.getValue(), i + 1);
                        embedBuilder.addField(new WebhookEmbed.EmbedField(event.leaderboardFieldLayout.inline, name, value));
                    }
                }
            }
        }
        embedBuilder.setThumbnailUrl(thumbnailUrl);
        WebhookEmbed embed = embedBuilder.build();
        return new WebhookMessageBuilder()
                .setContent(event.message.replaceAll("%damage%", String.valueOf(damage)))
                .setUsername(CONFIG.discordWebhook.username)
                .setAvatarUrl(CONFIG.discordWebhook.avatarUrl)
                .addEmbeds(embed);
    }
}
