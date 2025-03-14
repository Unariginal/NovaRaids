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

import net.minecraft.util.UserCache;
import org.json.JSONObject;
import org.json.JSONException;


public class WebhookHandler {

    private static final NovaRaids nr = NovaRaids.INSTANCE;
    private static UserCache cache =  nr.server().getUserCache();
    public static boolean raid_webhook_toggle = false;
    public static String raid_webhookurl = "https://discord.com/api/webhooks/";
    public static String raid_title =  "Raid Alert!";
    public static String raid_avatarurl = "https://cdn.modrinth.com/data/MdwFAVRL/e54083a07bcd9436d1f8d2879b0d821a54588b9e.png";
    public static String raid_role_at = "<@&roldidhere>";

    private static int genRanColor() {
        Random rand = new Random();
        float r = rand.nextFloat();
        float g = rand.nextFloat() / 2f;
        float b = rand.nextFloat() / 2f;
        return new Color(r, g, b).getRGB();
    }
    private static String formatPokemonName(String name) {
        name = name.replace("cobblemon.species.", "").replace('_', ' ');
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

    private  static String getThumbnailUrl(Pokemon pokemon) {
        String baseUrl = "https://play.pokemonshowdown.com/sprites/%rute%/%pokemon%.gif";

        String url = baseUrl.replace("%rute%", pokemon.getShiny() ? "ani-shiny" : "ani")
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

    private  static boolean isUrlAccessible(String url) {
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

    public static void sendStartRaidWebhook(Pokemon pokemon){
        WebhookClient webhook = WebhookClient.withUrl(raid_webhookurl);
        int randColor = genRanColor();
        String thumbnailUrl = getThumbnailUrl(pokemon);
        String formattedPokemonName = formatPokemonName(pokemon.getSpecies().getName());

        WebhookEmbed embed = new WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                        new WebhookEmbed.EmbedAuthor(
                                formattedPokemonName + " Raid Has Started",
                                "",
                                thumbnailUrl
                        )
                )
                .addField(new WebhookEmbed.EmbedField(false, "In-game Command is: ", "/raid join " + nr.fix_raid_ids()))
                .setThumbnailUrl(thumbnailUrl)
                .build();

        WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
                .setContent(raid_role_at)
                .setUsername(raid_title)
                .setAvatarUrl(raid_avatarurl)
                .addEmbeds(embed);

        webhook.send(messageBuilder.build());
    }

    public static void sendEndRaidWebhook(Pokemon pokemon, List<Map.Entry<UUID, Integer>> entries) {

        WebhookClient webhook = WebhookClient.withUrl(raid_webhookurl);
        int randColor = genRanColor();
        String thumbnailUrl = getThumbnailUrl(pokemon);
        String formattedPokemonName = formatPokemonName(pokemon.getSpecies().getName());

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
            embedBuilder.addField(new WebhookEmbed.EmbedField(false, (i + 1) + ". " + cache.getByUuid(entry.getKey()).orElseThrow().getName() + ":", entry.getValue().toString()));
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
