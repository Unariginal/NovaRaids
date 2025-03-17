package me.unariginal.novaraids.utils;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.unariginal.novaraids.NovaRaids;

import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;
import java.util.List;

import me.unariginal.novaraids.data.FieldData;
import me.unariginal.novaraids.managers.Raid;
import net.minecraft.util.UserCache;


public class WebhookHandler {

    private static final NovaRaids nr = NovaRaids.INSTANCE;
    private static final UserCache cache =  nr.server().getUserCache();
    public static boolean webhook_toggle = false;
    public static String webhook_url = "https://discord.com/api/webhooks/";
    public static String webhook_username =  "Raid Alert!";
    public static String webhook_avatar_url = "https://cdn.modrinth.com/data/MdwFAVRL/e54083a07bcd9436d1f8d2879b0d821a54588b9e.png";
    public static String role_ping = "<@&roldidhere>";
    public static String start_embed_title = "%boss.form% %boss.species% Raid Has Started";
    public static List<FieldData> start_embed_fields = new ArrayList<>();
    public static String end_embed_title = "%boss.form% %boss.species% Raid Has Ended";
    public static List<FieldData> end_embed_fields = new ArrayList<>();
    public static boolean show_leaderboard = true;

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
        String baseUrl = "https://play.pokemonshowdown.com/sprites/%rute%/%pokemon%%form%.gif";

        if (!pokemon.getForm().formOnlyShowdownId().equalsIgnoreCase("normal")) {
            String url = baseUrl.replace("%rute%", pokemon.getShiny() ? "ani-shiny" : "ani")
                    .replace("%pokemon%", pokemon.getSpecies().getName().toLowerCase())
                    .replace("%form%", "-" + pokemon.getForm().formOnlyShowdownId());
            nr.logInfo("Pokemon URL: " + url);

            if (isUrlAccessible(url)) {
                return url;
            }
        }

        String url = baseUrl.replace("%rute%", pokemon.getShiny() ? "ani-shiny" : "ani")
                .replace("%pokemon%", pokemon.getSpecies().getName().toLowerCase())
                .replace("%form%", "");
        nr.logInfo("Pokemon URL: " + url);

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

    public static void sendStartRaidWebhook(Raid raid){
        WebhookClient webhook = WebhookClient.withUrl(webhook_url);
        Pokemon pokemon = raid.raidBoss_pokemon();
        int randColor = genTypeColor(pokemon);
        String thumbnailUrl = getThumbnailUrl(pokemon);

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                        new WebhookEmbed.EmbedAuthor(
                                nr.config().getMessages().parse(start_embed_title, raid),
                                "",
                                thumbnailUrl
                        )
                );
        for (FieldData field : start_embed_fields) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(field.inline(), nr.config().getMessages().parse(field.name(), raid), nr.config().getMessages().parse(field.value(), raid)));
        }
        embedBuilder.setThumbnailUrl(thumbnailUrl);
        WebhookEmbed embed = embedBuilder.build();

        WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
                .setContent(role_ping)
                .setUsername(webhook_username)
                .setAvatarUrl(webhook_avatar_url)
                .addEmbeds(embed);

        webhook.send(messageBuilder.build());
    }

    public static void sendEndRaidWebhook(Raid raid, List<Map.Entry<UUID, Integer>> entries) {
        WebhookClient webhook = WebhookClient.withUrl(webhook_url);
        Pokemon pokemon = raid.raidBoss_pokemon();
        int randColor = genTypeColor(pokemon);
        String thumbnailUrl = getThumbnailUrl(pokemon);

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                        new WebhookEmbed.EmbedAuthor(
                                nr.config().getMessages().parse(end_embed_title, raid),
                                "",
                                thumbnailUrl
                        )
                );

        for (FieldData field : end_embed_fields) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(field.inline(), nr.config().getMessages().parse(field.name(), raid), nr.config().getMessages().parse(field.value(), raid)));
        }

        if (show_leaderboard) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(false, "Leaderboard:", ""));

            for (int i = 0; i < Math.min(entries.size(), 10); i++) {
                Map.Entry<UUID, Integer> entry = entries.get(i);
                if (cache != null) {
                    embedBuilder.addField(new WebhookEmbed.EmbedField(false, (i + 1) + ". " + cache.getByUuid(entry.getKey()).orElseThrow().getName() + ":", entry.getValue().toString()));
                }
            }
        }

        embedBuilder.setThumbnailUrl(thumbnailUrl);
        WebhookEmbed embed = embedBuilder.build();

        WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
                .setUsername(webhook_username)
                .setAvatarUrl(webhook_avatar_url)
                .addEmbeds(embed);

        webhook.send(messageBuilder.build());
    }
}