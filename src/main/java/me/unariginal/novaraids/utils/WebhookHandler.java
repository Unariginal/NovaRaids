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
    public static boolean webhookToggle = false;
    public static List<String> blacklistedCategories = new ArrayList<>();
    public static List<String> blacklistedBosses = new ArrayList<>();
    public static String webhookUrl = "https://discord.com/api/webhooks/";
    public static String webhookUsername =  "Raid Alert!";
    public static String webhookAvatarUrl = "https://cdn.modrinth.com/data/MdwFAVRL/e54083a07bcd9436d1f8d2879b0d821a54588b9e.png";
    public static String rolePing = "<@&role_id_here>";
    public static int webhookUpdateRateSeconds = 15;
    public static boolean deleteIfNoFightPhase = true;
    public static boolean startEmbedEnabled = false;
    public static String startEmbedTitle = "%boss.id% Raid Has Started";
    public static List<FieldData> startEmbedFields = new ArrayList<>();
    public static boolean runningEmbedEnabled = false;
    public static String runningEmbedTitle = "%boss.id% Raid In Progress!";
    public static List<FieldData> runningEmbedFields = new ArrayList<>();
    public static FieldData runningEmbedLeaderboardField = null;
    public static boolean endEmbedEnabled = false;
    public static String endEmbedTitle = "%boss.id% Raid Has Ended";
    public static List<FieldData> endEmbedFields = new ArrayList<>();
    public static FieldData endEmbedLeaderboardField = null;
    public static boolean failedEmbedEnabled = false;
    public static String failedEmbedTitle = "Failed To Defeat %boss.id%!";
    public static List<FieldData> failedEmbedFields = new ArrayList<>();
    public static FieldData failedEmbedLeaderboardField = null;

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
        webhook = WebhookClient.withUrl(webhookUrl);
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
        Pokemon pokemon = raid.raidBossPokemon();
        int randColor = genTypeColor(pokemon);
        String thumbnailUrl = getThumbnailUrl(pokemon);

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                        new WebhookEmbed.EmbedAuthor(
                                TextUtils.parse(startEmbedTitle, raid),
                                "",
                                thumbnailUrl
                        )
                );
        for (FieldData field : startEmbedFields) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(field.inline(), TextUtils.parse(field.name(), raid), TextUtils.parse(field.value(), raid)));
        }
        embedBuilder.setThumbnailUrl(thumbnailUrl);
        WebhookEmbed embed = embedBuilder.build();
        return new WebhookMessageBuilder()
                .setContent(rolePing)
                .setUsername(webhookUsername)
                .setAvatarUrl(webhookAvatarUrl)
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
        Pokemon pokemon = raid.raidBossPokemon();
        int randColor = genTypeColor(pokemon);
        String thumbnailUrl = getThumbnailUrl(pokemon);

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                        new WebhookEmbed.EmbedAuthor(
                                TextUtils.parse(endEmbedTitle, raid),
                                "",
                                thumbnailUrl
                        )
                );

        for (FieldData field : endEmbedFields) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(field.inline(), TextUtils.parse(field.name(), raid), TextUtils.parse(field.value(), raid)));
            if (field.insertLeaderboardAfter()) {
                List<Map.Entry<String, Integer>> entries = raid.getDamageLeaderboard();

                for (int i = 0; i < Math.min(entries.size(), 10); i++) {
                    Map.Entry<String, Integer> entry = entries.get(i);
                    if (cache != null) {
                        GameProfile user = cache.findByName(entry.getKey()).orElseThrow();
                        String name = TextUtils.parse(endEmbedLeaderboardField.name(), raid, user, entry.getValue(), i + 1);
                        String value = TextUtils.parse(endEmbedLeaderboardField.value(), raid, user, entry.getValue(), i + 1);
                        embedBuilder.addField(new WebhookEmbed.EmbedField(endEmbedLeaderboardField.inline(), name, value));
                    }
                }
            }
        }

        embedBuilder.setThumbnailUrl(thumbnailUrl);
        WebhookEmbed embed = embedBuilder.build();

        return new WebhookMessageBuilder()
                .setUsername(webhookUsername)
                .setAvatarUrl(webhookAvatarUrl)
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
        Pokemon pokemon = raid.raidBossPokemon();
        int randColor = genTypeColor(pokemon);
        String thumbnailUrl = getThumbnailUrl(pokemon);

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                        new WebhookEmbed.EmbedAuthor(
                                TextUtils.parse(runningEmbedTitle, raid),
                                "",
                                thumbnailUrl
                        )
                );

        for (FieldData field : runningEmbedFields) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(field.inline(), TextUtils.parse(field.name(), raid), TextUtils.parse(field.value(), raid)));
            if (field.insertLeaderboardAfter()) {
                List<Map.Entry<String, Integer>> entries = raid.getDamageLeaderboard();

                for (int i = 0; i < Math.min(entries.size(), 10); i++) {
                    Map.Entry<String, Integer> entry = entries.get(i);
                    if (cache != null) {
                        GameProfile user = cache.findByName(entry.getKey()).orElseThrow();
                        String name = TextUtils.parse(runningEmbedLeaderboardField.name(), raid, user, entry.getValue(), i + 1);
                        String value = TextUtils.parse(runningEmbedLeaderboardField.value(), raid, user, entry.getValue(), i + 1);
                        embedBuilder.addField(new WebhookEmbed.EmbedField(runningEmbedLeaderboardField.inline(), name, value));
                    }
                }
            }
        }

        embedBuilder.setThumbnailUrl(thumbnailUrl);
        WebhookEmbed embed = embedBuilder.build();

        return new WebhookMessageBuilder()
                .setUsername(webhookUsername)
                .setAvatarUrl(webhookAvatarUrl)
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
        Pokemon pokemon = raid.raidBossPokemon();
        int randColor = genTypeColor(pokemon);
        String thumbnailUrl = getThumbnailUrl(pokemon);

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                        new WebhookEmbed.EmbedAuthor(
                                TextUtils.parse(failedEmbedTitle, raid),
                                "",
                                thumbnailUrl
                        )
                );

        for (FieldData field : failedEmbedFields) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(field.inline(), TextUtils.parse(field.name(), raid), TextUtils.parse(field.value(), raid)));
            if (field.insertLeaderboardAfter()) {
                List<Map.Entry<String, Integer>> entries = raid.getDamageLeaderboard();

                for (int i = 0; i < Math.min(entries.size(), 10); i++) {
                    Map.Entry<String, Integer> entry = entries.get(i);
                    if (cache != null) {
                        GameProfile user = cache.findByName(entry.getKey()).orElseThrow();
                        String name = TextUtils.parse(failedEmbedLeaderboardField.name(), raid, user, entry.getValue(), i + 1);
                        String value = TextUtils.parse(failedEmbedLeaderboardField.value(), raid, user, entry.getValue(), i + 1);
                        embedBuilder.addField(new WebhookEmbed.EmbedField(failedEmbedLeaderboardField.inline(), name, value));
                    }
                }
            }
        }

        embedBuilder.setThumbnailUrl(thumbnailUrl);
        WebhookEmbed embed = embedBuilder.build();

        return new WebhookMessageBuilder()
                .setUsername(webhookUsername)
                .setAvatarUrl(webhookAvatarUrl)
                .addEmbeds(embed);
    }
}