package me.unariginal.novaraids.utils;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.authlib.GameProfile;
import me.unariginal.novaraids.NovaRaids;

import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
    public static String role_ping = "<@&role_id_here>";
    public static int webhook_update_rate_seconds = 15;
    public static boolean delete_if_no_fight_phase = true;
    public static boolean start_embed_enabled = false;
    public static String start_embed_title = "%boss.name% Raid Has Started";
    public static List<FieldData> start_embed_fields = new ArrayList<>();
    public static boolean running_embed_enabled = false;
    public static String running_embed_title = "%boss.name% Raid In Progress!";
    public static List<FieldData> running_embed_fields = new ArrayList<>();
    public static FieldData running_embed_leaderboard_field = null;
    public static boolean end_embed_enabled = false;
    public static String end_embed_title = "%boss.name% Raid Has Ended";
    public static List<FieldData> end_embed_fields = new ArrayList<>();
    public static FieldData end_embed_leaderboard_field = null;
    public static boolean failed_embed_enabled = false;
    public static String failed_embed_title = "Failed To Defeat %boss.name%!";
    public static List<FieldData> failed_embed_fields = new ArrayList<>();
    public static FieldData failed_embed_leaderboard_field = null;

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
        String baseUrl = "https://play.pokemonshowdown.com/sprites/%rute%/%pokemon%%form%.gif";

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
        webhook = WebhookClient.withUrl(webhook_url);
    }

    public static void deleteWebhook(long id) throws ExecutionException, InterruptedException {
        webhook.delete(id);
    }

    public static long sendStartRaidWebhook(Raid raid) throws ExecutionException, InterruptedException {
        return webhook.send(buildStartRaidWebhook(raid).build()).get().getId();
    }

    public static void editStartRaidWebhook(long id, Raid raid) throws ExecutionException, InterruptedException {
        webhook.edit(id, buildStartRaidWebhook(raid).build());
    }

    public static WebhookMessageBuilder buildStartRaidWebhook(Raid raid){
        Pokemon pokemon = raid.raidBoss_pokemon();
        int randColor = genTypeColor(pokemon);
        String thumbnailUrl = getThumbnailUrl(pokemon);

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                        new WebhookEmbed.EmbedAuthor(
                                TextUtils.parse(start_embed_title, raid),
                                "",
                                thumbnailUrl
                        )
                );
        for (FieldData field : start_embed_fields) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(field.inline(), TextUtils.parse(field.name(), raid), TextUtils.parse(field.value(), raid)));
        }
        embedBuilder.setThumbnailUrl(thumbnailUrl);
        WebhookEmbed embed = embedBuilder.build();
        return new WebhookMessageBuilder()
                .setContent(role_ping)
                .setUsername(webhook_username)
                .setAvatarUrl(webhook_avatar_url)
                .addEmbeds(embed);
    }

    public static void sendEndRaidWebhook(long id, Raid raid) throws ExecutionException, InterruptedException {
        if (id == 0) {
            webhook.send(buildEndRaidWebhook(raid).build()).get();
        } else {
            editEndRaidWebhook(id, raid);
        }
    }

    public static void editEndRaidWebhook(long id, Raid raid) {
        webhook.edit(id, buildEndRaidWebhook(raid).build());
    }

    public static WebhookMessageBuilder buildEndRaidWebhook(Raid raid) {
        Pokemon pokemon = raid.raidBoss_pokemon();
        int randColor = genTypeColor(pokemon);
        String thumbnailUrl = getThumbnailUrl(pokemon);

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                        new WebhookEmbed.EmbedAuthor(
                                TextUtils.parse(end_embed_title, raid),
                                "",
                                thumbnailUrl
                        )
                );

        for (FieldData field : end_embed_fields) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(field.inline(), TextUtils.parse(field.name(), raid), TextUtils.parse(field.value(), raid)));
            if (field.insert_leaderboard_after()) {
                List<Map.Entry<String, Integer>> entries = raid.get_damage_leaderboard();

                for (int i = 0; i < Math.min(entries.size(), 10); i++) {
                    Map.Entry<String, Integer> entry = entries.get(i);
                    if (cache != null) {
                        GameProfile user = cache.findByName(entry.getKey()).orElseThrow();
                        String name = TextUtils.parse(end_embed_leaderboard_field.name(), raid, user, entry.getValue(), i + 1);
                        String value = TextUtils.parse(end_embed_leaderboard_field.value(), raid, user, entry.getValue(), i + 1);
                        embedBuilder.addField(new WebhookEmbed.EmbedField(end_embed_leaderboard_field.inline(), name, value));
                    }
                }
            }
        }

        embedBuilder.setThumbnailUrl(thumbnailUrl);
        WebhookEmbed embed = embedBuilder.build();

        return new WebhookMessageBuilder()
                .setUsername(webhook_username)
                .setAvatarUrl(webhook_avatar_url)
                .addEmbeds(embed);
    }

    public static long sendRunningWebhook(long id, Raid raid) throws ExecutionException, InterruptedException {
        if (id == 0) {
            return webhook.send(buildRunningWebhook(raid).build()).get().getId();
        } else {
            editRunningWebhook(id, raid);
        }
        return id;
    }

    public static void editRunningWebhook(long id, Raid raid) throws ExecutionException, InterruptedException {
        webhook.edit(id, buildRunningWebhook(raid).build());
    }

    public static WebhookMessageBuilder buildRunningWebhook(Raid raid) {
        Pokemon pokemon = raid.raidBoss_pokemon();
        int randColor = genTypeColor(pokemon);
        String thumbnailUrl = getThumbnailUrl(pokemon);

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                        new WebhookEmbed.EmbedAuthor(
                                TextUtils.parse(running_embed_title, raid),
                                "",
                                thumbnailUrl
                        )
                );

        for (FieldData field : running_embed_fields) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(field.inline(), TextUtils.parse(field.name(), raid), TextUtils.parse(field.value(), raid)));
            if (field.insert_leaderboard_after()) {
                List<Map.Entry<String, Integer>> entries = raid.get_damage_leaderboard();

                for (int i = 0; i < Math.min(entries.size(), 10); i++) {
                    Map.Entry<String, Integer> entry = entries.get(i);
                    if (cache != null) {
                        GameProfile user = cache.findByName(entry.getKey()).orElseThrow();
                        String name = TextUtils.parse(running_embed_leaderboard_field.name(), raid, user, entry.getValue(), i + 1);
                        String value = TextUtils.parse(running_embed_leaderboard_field.value(), raid, user, entry.getValue(), i + 1);
                        embedBuilder.addField(new WebhookEmbed.EmbedField(running_embed_leaderboard_field.inline(), name, value));
                    }
                }
            }
        }

        embedBuilder.setThumbnailUrl(thumbnailUrl);
        WebhookEmbed embed = embedBuilder.build();

        return new WebhookMessageBuilder()
                .setUsername(webhook_username)
                .setAvatarUrl(webhook_avatar_url)
                .addEmbeds(embed);
    }

    public static void sendFailedWebhook(long id, Raid raid) throws ExecutionException, InterruptedException {
        if (id == 0) {
            webhook.send(buildFailedWebhook(raid).build()).get();
        } else {
            editFailedWebhook(id, raid);
        }
    }

    public static void editFailedWebhook(long id, Raid raid) {
        webhook.edit(id, buildFailedWebhook(raid).build());
    }

    public static WebhookMessageBuilder buildFailedWebhook(Raid raid) {
        Pokemon pokemon = raid.raidBoss_pokemon();
        int randColor = genTypeColor(pokemon);
        String thumbnailUrl = getThumbnailUrl(pokemon);

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                        new WebhookEmbed.EmbedAuthor(
                                TextUtils.parse(failed_embed_title, raid),
                                "",
                                thumbnailUrl
                        )
                );

        for (FieldData field : failed_embed_fields) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(field.inline(), TextUtils.parse(field.name(), raid), TextUtils.parse(field.value(), raid)));
            if (field.insert_leaderboard_after()) {
                List<Map.Entry<String, Integer>> entries = raid.get_damage_leaderboard();

                for (int i = 0; i < Math.min(entries.size(), 10); i++) {
                    Map.Entry<String, Integer> entry = entries.get(i);
                    if (cache != null) {
                        GameProfile user = cache.findByName(entry.getKey()).orElseThrow();
                        String name = TextUtils.parse(failed_embed_leaderboard_field.name(), raid, user, entry.getValue(), i + 1);
                        String value = TextUtils.parse(failed_embed_leaderboard_field.value(), raid, user, entry.getValue(), i + 1);
                        embedBuilder.addField(new WebhookEmbed.EmbedField(failed_embed_leaderboard_field.inline(), name, value));
                    }
                }
            }
        }

        embedBuilder.setThumbnailUrl(thumbnailUrl);
        WebhookEmbed embed = embedBuilder.build();

        return new WebhookMessageBuilder()
                .setUsername(webhook_username)
                .setAvatarUrl(webhook_avatar_url)
                .addEmbeds(embed);
    }
}