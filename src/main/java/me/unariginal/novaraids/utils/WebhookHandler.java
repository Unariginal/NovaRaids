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

import me.unariginal.novaraids.managers.Raid;
import net.minecraft.util.UserCache;


public class WebhookHandler {

    private static final NovaRaids nr = NovaRaids.INSTANCE;
    private static final UserCache cache =  nr.server().getUserCache();
    public static boolean raid_webhook_toggle = false;
    public static String raid_webhookurl = "https://discord.com/api/webhooks/";
    public static String raid_title =  "Raid Alert!";
    public static String raid_avatarurl = "https://cdn.modrinth.com/data/MdwFAVRL/e54083a07bcd9436d1f8d2879b0d821a54588b9e.png";
    public static String raid_role_at = "<@&roldidhere>";

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
    private static String formatPokemonName(Raid raid) {
        String name = raid.boss_info().display_form() + " " + raid.boss_info().species().getName().replace("cobblemon.species.", "").replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder formattedName = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                formattedName.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return formattedName.toString().trim();
    }

    private static String getThumbnailUrl(Pokemon pokemon) {
        String baseUrl = "https://play.pokemonshowdown.com/sprites/%rute%/%pokemon%%form%.gif";

        String url = baseUrl.replace("%rute%", pokemon.getShiny() ? "ani-shiny" : "ani")
                .replace("%pokemon%", pokemon.getSpecies().getName().toLowerCase())
                .replace("%form%", "-" + pokemon.getForm().formOnlyShowdownId());
        nr.logInfo("Pokemon URL: " + url);

        if (isUrlAccessible(url)) {
            return url;
        }

        url = baseUrl.replace("%rute%", pokemon.getShiny() ? "ani-shiny" : "ani")
                .replace("%pokemon%", pokemon.getSpecies().getName().toLowerCase())
                .replace("%form%", "-" + pokemon.getForm().getName().toLowerCase());
        nr.logInfo("Pokemon URL: " + url);

        if (isUrlAccessible(url)) {
            return url;
        }

        url = baseUrl.replace("%rute%", pokemon.getShiny() ? "ani-shiny" : "ani")
                .replace("%pokemon%", pokemon.getSpecies().getName().toLowerCase());

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
        WebhookClient webhook = WebhookClient.withUrl(raid_webhookurl);
        Pokemon pokemon = raid.raidBoss_pokemon();
        int randColor = genTypeColor(pokemon);
        String thumbnailUrl = getThumbnailUrl(pokemon);
        String formattedPokemonName = formatPokemonName(raid);
        String fieldName = "Players: " + raid.participating_players().size() + "/" + (raid.max_players() == -1 ? "∞" : raid.max_players()) + " ";
        String fieldValue = "";
        if (raid.raidBoss_category().require_pass()) {
            fieldValue = "Join Now Using A Raid Pass!";
        } else {
            fieldValue = "Join Now Using `/raid list`!";
        }

        WebhookEmbed embed = new WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                        new WebhookEmbed.EmbedAuthor(
                                formattedPokemonName + " Raid Has Started",
                                "",
                                thumbnailUrl
                        )
                )
                .addField(new WebhookEmbed.EmbedField(false, fieldName, fieldValue))
                .setThumbnailUrl(thumbnailUrl)
                .build();

        WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
                .setContent(raid_role_at)
                .setUsername(raid_title)
                .setAvatarUrl(raid_avatarurl)
                .addEmbeds(embed);

        webhook.send(messageBuilder.build());
    }

    public static void sendEndRaidWebhook(Raid raid, List<Map.Entry<UUID, Integer>> entries) {
        WebhookClient webhook = WebhookClient.withUrl(raid_webhookurl);
        Pokemon pokemon = raid.raidBoss_pokemon();
        int randColor = genTypeColor(pokemon);
        String thumbnailUrl = getThumbnailUrl(pokemon);
        String formattedPokemonName = formatPokemonName(raid);

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                        new WebhookEmbed.EmbedAuthor(
                                formattedPokemonName + " Raid Has Ended",
                                "",
                                thumbnailUrl
                        )
                )
                .addField(new WebhookEmbed.EmbedField(false, "Leaderboard:", ""));

        for (int i = 0; i < Math.min(entries.size(), 10); i++) {
            Map.Entry<UUID, Integer> entry = entries.get(i);
            if (cache != null) {
                embedBuilder.addField(new WebhookEmbed.EmbedField(false, (i + 1) + ". " + cache.getByUuid(entry.getKey()).orElseThrow().getName() + ":", entry.getValue().toString()));
            }
        }

        embedBuilder.setThumbnailUrl(thumbnailUrl);

        WebhookEmbed embed = embedBuilder.build();

        WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
                .setUsername(raid_title)
                .setAvatarUrl(raid_avatarurl)
                .addEmbeds(embed);

        webhook.send(messageBuilder.build());
    }

}
