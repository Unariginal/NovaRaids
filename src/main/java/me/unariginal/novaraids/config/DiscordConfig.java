package me.unariginal.novaraids.config;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import me.unariginal.novaraids.utils.WebhookHandler;

//#TODO MERGE THIS INTO CONFIG ONCE HOW Config Class Works For work around
public class DiscordConfig {

    private static final Path CONFIG_PATH = Paths.get("config/NovaRaids/discord.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void loadDiscordInfo() {
        if (Files.exists(CONFIG_PATH)) {
            try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
                DiscordConfig config = gson.fromJson(reader, DiscordConfig.class);
                WebhookHandler.raid_webhook_toggle = config.raid_webhook_toggle;
                WebhookHandler.raid_webhookurl = config.raid_webhookurl;
                WebhookHandler.raid_title = config.raid_title;
                WebhookHandler.raid_avatarurl = config.raid_avatarurl;
                WebhookHandler.raid_role_at = config.raid_role_at;
            } catch (IOException e) {
                System.err.println("Could not load config file: " + e.getMessage());
            }
        } else {
            try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
                DiscordConfig config = new DiscordConfig();
                config.raid_webhook_toggle = WebhookHandler.raid_webhook_toggle;
                config.raid_webhookurl = WebhookHandler.raid_webhookurl;
                config.raid_title = WebhookHandler.raid_title;
                config.raid_avatarurl = WebhookHandler.raid_avatarurl;
                config.raid_role_at = WebhookHandler.raid_role_at;
                gson.toJson(config, writer);
            } catch (IOException e) {
                System.err.println("Could not save config file: " + e.getMessage());
            }
        }
    }

    public boolean raid_webhook_toggle;
    public String raid_webhookurl;
    public String raid_title;
    public String raid_avatarurl;
    public String raid_role_at;
}