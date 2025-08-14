package me.unariginal.novaraids.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.FieldData;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.utils.TextUtils;
import me.unariginal.novaraids.utils.WebhookHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.io.*;
import java.util.*;

public class MessagesConfig {
    public String prefix = "<dark_gray>[</dark_gray><color:#ffbf00>RAID<dark_gray>]</dark_gray>";
    public String raidStartCommand = "";
    public Map<String, String> messages = new HashMap<>();

    public MessagesConfig() {
        messages.put("start_pre_phase", "%prefix% A raid is starting against %boss.name%!");
        messages.put("start_fight_phase", "%prefix% Time to fight %boss.name% (%boss.maxhp% HP)!");
        messages.put("boss_defeated", "%prefix% %boss.name% has been defeated in %raid.defeat_time%!");
        messages.put("catch_phase_warning", "%prefix% Prepare to catch %boss.name%");
        messages.put("start_catch_phase", "%prefix% Time to catch %boss.name%!");
        messages.put("catch_phase_end", "%prefix% Catch phase completed!");
        messages.put("raid_end", "%prefix% The %boss.name% raid has finished.");
        messages.put("out_of_time", "%prefix% %boss.name% was not defeated in time!");
        messages.put("not_enough_players", "%prefix% There wasn't enough players to start the raid!");
        messages.put("player_damage_report", "%prefix% %raid.player% did %raid.player.damage% to the boss!");
        messages.put("no_active_raids", "%prefix% There are no active raids!");
        messages.put("no_queued_raids", "%prefix% There are no queued raids!");
        messages.put("no_available_locations", "%prefix% Failed to start raid. All possible locations are busy.");
        messages.put("joined_raid", "%prefix% You've successfully joined the %boss.name% raid!");
        messages.put("warning_no_pass", "%prefix% This raid requires a pass to join!");
        messages.put("warning_already_joined_raid", "%prefix% You've already joined a raid!");
        messages.put("warning_no_pass_needed", "%prefix% You don't need a pass for this raid!");
        messages.put("warning_not_joinable", "%prefix% This raid is not joinable!");
        messages.put("warning_no_pokemon", "%prefix% You can't enter a raid with no pokemon!");
        messages.put("warning_banned_pokemon", "%prefix% %banned.pokemon% is banned from raids!");
        messages.put("warning_banned_move", "%prefix% %banned.move% is banned from raids!");
        messages.put("warning_banned_ability", "%prefix% %banned.ability% is banned from raids!");
        messages.put("warning_banned_held_item", "%prefix% %banned.held_item% is banned from raids!");
        messages.put("warning_banned_bag_item", "%prefix% %banned.bag_item% is banned from raids!");
        messages.put("warning_max_players", "%prefix% This raid is full!");
        messages.put("warning_minimum_level", "%prefix% Your pokemon must be above level %boss.minimum_level%!");
        messages.put("warning_maximum_level", "%prefix% Your pokemon must be below level %boss.maximum_level%!");
        messages.put("warning_cooldown", "%prefix% You're on cooldown!");
        messages.put("warning_battle_during_raid", "%prefix% You can't battle wild pokemon during a raid!");
        messages.put("warning_not_your_encounter", "%prefix% This isn't your catch encounter!");
        messages.put("warning_not_your_raid_pokeball", "%prefix% That isn't your raid pokeball!");
        messages.put("warning_raid_pokeball_outside_raid", "%prefix% You can't use raid pokeballs outside of a raid!");
        messages.put("warning_not_catch_phase", "%prefix% You can only use this during the catch phase!");
        messages.put("warning_deny_normal_pokeball", "%prefix% You can only use raid pokeballs in a raid!");
        messages.put("used_voucher", "%prefix% You've successfully started a raid for %boss.name%!");
        messages.put("queue_item_cancelled", "%prefix% You've cancelled the queue for %boss.name%!");
        messages.put("added_to_queue", "%prefix% %boss.name% has been added to the queue!");
        messages.put("reload_command", "%prefix% Reloaded!");
        messages.put("raid_stopped", "%prefix% You've stopped the raid against %boss.name%!");
        messages.put("give_command_invalid_category", "%prefix% Category %category% does not exist!");
        messages.put("give_command_invalid_pokeball", "%prefix% Pokeball %pokeball% does not exist!");
        messages.put("give_command_invalid_boss", "%prefix% Boss %boss% does not exist!");
        messages.put("give_command_failed_to_give", "%prefix% Failed to give the item!");
        messages.put("give_command_received_item", "%prefix% You received a %raid_item%!");
        messages.put("give_command_feedback", "%prefix% Successfully gave %target% a %raid_item%");
        messages.put("checkbanned_command_no_banned_pokemon", "%prefix% There are no banned pokemon!");
        messages.put("checkbanned_command_no_banned_moves", "%prefix% There are no banned moves!");
        messages.put("checkbanned_command_no_banned_abilities", "%prefix% There are no banned abilities!");
        messages.put("checkbanned_command_no_banned_held_items", "%prefix% There are no banned held items!");
        messages.put("checkbanned_command_no_banned_bag_items", "%prefix% There are no banned bag items!");
        messages.put("leaderboard_message_header", "------- Raid Results -------");
        messages.put("leaderboard_message_item", " - [%raid.player.place%] %raid.player% : %raid.player.damage% damage");
        messages.put("leaderboard_individual", "You placed %raid.player.place%%place_suffix% with %raid.player.damage% damage!");

        try {
            loadConfig();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            NovaRaids.LOADED = false;
            NovaRaids.LOGGER.error("[RAIDS] Failed to load messages file.", e);
        }
    }

    public void loadConfig() throws IOException, NullPointerException, UnsupportedOperationException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        File file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/messages.json").toFile();
        if (file.createNewFile()) {
            InputStream stream = NovaRaids.class.getResourceAsStream("/raid_config_files/messages.json");
            assert stream != null;
            OutputStream out = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = stream.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            stream.close();
            out.close();
        }

        JsonElement root = JsonParser.parseReader(new FileReader(file));
        assert root != null;
        JsonObject config = root.getAsJsonObject();
        if (ConfigHelper.checkProperty(config, "prefix", "messages")) {
            prefix = config.get("prefix").getAsString();
        }
        if (ConfigHelper.checkProperty(config, "raid_start_command", "messages")) {
            raidStartCommand = config.get("raid_start_command").getAsString();
        }
        if (ConfigHelper.checkProperty(config, "messages", "messages")) {
            JsonObject messages_object = config.get("messages").getAsJsonObject();
            for (Map.Entry<String, String> message : messages.entrySet()) {
                if (ConfigHelper.checkProperty(messages_object, message.getKey(), "messages")) {
                    messages.put(message.getKey(), messages_object.get(message.getKey()).getAsString());
                }
            }
        }
        if (ConfigHelper.checkProperty(config, "discord", "messages")) {
            JsonObject discord_object = config.get("discord").getAsJsonObject();
            if (ConfigHelper.checkProperty(discord_object, "webhook_toggle", "messages")) {
                WebhookHandler.webhookToggle = discord_object.get("webhook_toggle").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(discord_object, "webhook_url", "messages")) {
                WebhookHandler.webhookUrl = discord_object.get("webhook_url").getAsString();
            }
            if (ConfigHelper.checkProperty(discord_object, "webhook_username", "messages")) {
                WebhookHandler.webhookUsername = discord_object.get("webhook_username").getAsString();
            }
            if (ConfigHelper.checkProperty(discord_object, "webhook_avatar_url", "messages")) {
                WebhookHandler.webhookAvatarUrl = discord_object.get("webhook_avatar_url").getAsString();
            }
            if (ConfigHelper.checkProperty(discord_object, "role_ping", "messages")) {
                WebhookHandler.rolePing = discord_object.get("role_ping").getAsString();
            }

            if (ConfigHelper.checkProperty(discord_object, "webhook_update_rate_seconds", "messages")) {
                WebhookHandler.webhookUpdateRateSeconds = discord_object.get("webhook_update_rate_seconds").getAsInt();
            }

            if (ConfigHelper.checkProperty(discord_object, "delete_if_no_fight_phase", "messages")) {
                WebhookHandler.deleteIfNoFightPhase = discord_object.get("delete_if_no_fight_phase").getAsBoolean();
            }

            if (ConfigHelper.checkProperty(discord_object, "raid_start", "messages")) {
                JsonObject raid_start_object = discord_object.get("raid_start").getAsJsonObject();
                if (ConfigHelper.checkProperty(raid_start_object, "enabled", "messages")) {
                    WebhookHandler.startEmbedEnabled = raid_start_object.get("enabled").getAsBoolean();
                }
                if (WebhookHandler.startEmbedEnabled) {
                    if (ConfigHelper.checkProperty(raid_start_object, "embed_title", "messages")) {
                        WebhookHandler.startEmbedTitle = raid_start_object.get("embed_title").getAsString();
                    }
                    if (ConfigHelper.checkProperty(raid_start_object, "fields", "messages")) {
                        JsonArray raid_start_fields_array = raid_start_object.get("fields").getAsJsonArray();
                        List<FieldData> fields = new ArrayList<>();
                        for (JsonElement field_element : raid_start_fields_array) {
                            JsonObject field_object = field_element.getAsJsonObject();
                            fields.add(getFieldData(field_object));
                        }
                        WebhookHandler.startEmbedFields = fields;
                    }
                }
            }
            if (ConfigHelper.checkProperty(discord_object, "raid_running", "messages")) {
                JsonObject raid_running_object = discord_object.get("raid_running").getAsJsonObject();
                if (ConfigHelper.checkProperty(raid_running_object, "enabled", "messages")) {
                    WebhookHandler.runningEmbedEnabled = raid_running_object.get("enabled").getAsBoolean();
                }
                if (WebhookHandler.runningEmbedEnabled) {
                    boolean has_leaderboard = false;
                    if (ConfigHelper.checkProperty(raid_running_object, "embed_title", "messages")) {
                        WebhookHandler.runningEmbedTitle = raid_running_object.get("embed_title").getAsString();
                    }
                    if (ConfigHelper.checkProperty(raid_running_object, "fields", "messages")) {
                        JsonArray raid_running_fields_array = raid_running_object.get("fields").getAsJsonArray();
                        List<FieldData> fields = new ArrayList<>();
                        for (JsonElement field_element : raid_running_fields_array) {
                            JsonObject field_object = field_element.getAsJsonObject();
                            FieldData fieldData = getFieldData(field_object);
                            fields.add(fieldData);
                            if (fieldData.insertLeaderboardAfter()) {
                                has_leaderboard = true;
                            }
                        }
                        WebhookHandler.runningEmbedFields = fields;
                    }
                    if (has_leaderboard) {
                        if (ConfigHelper.checkProperty(raid_running_object, "leaderboard_field_layout", "messages")) {
                            JsonObject leaderboard_field_object = raid_running_object.get("leaderboard_field_layout").getAsJsonObject();
                            FieldData leaderboard_field = getFieldData(leaderboard_field_object);
                            WebhookHandler.runningEmbedLeaderboardField = new FieldData(leaderboard_field.inline(), leaderboard_field.name(), leaderboard_field.value(), false);
                        }
                    }
                }
            }
            if (ConfigHelper.checkProperty(discord_object, "raid_end", "messages")) {
                JsonObject raid_end_object = discord_object.get("raid_end").getAsJsonObject();
                if (ConfigHelper.checkProperty(raid_end_object, "enabled", "messages")) {
                    WebhookHandler.endEmbedEnabled = raid_end_object.get("enabled").getAsBoolean();
                }
                if (WebhookHandler.endEmbedEnabled) {
                    boolean has_leaderboard = false;
                    if (ConfigHelper.checkProperty(raid_end_object, "embed_title", "messages")) {
                        WebhookHandler.endEmbedTitle = raid_end_object.get("embed_title").getAsString();
                    }
                    if (ConfigHelper.checkProperty(raid_end_object, "fields", "messages")) {
                        JsonArray raid_end_fields_array = raid_end_object.get("fields").getAsJsonArray();
                        List<FieldData> fields = new ArrayList<>();
                        for (JsonElement field_element : raid_end_fields_array) {
                            JsonObject field_object = field_element.getAsJsonObject();
                            FieldData fieldData = getFieldData(field_object);
                            fields.add(fieldData);
                            if (fieldData.insertLeaderboardAfter()) {
                                has_leaderboard = true;
                            }
                        }
                        WebhookHandler.endEmbedFields = fields;
                    }
                    if (has_leaderboard) {
                        if (ConfigHelper.checkProperty(raid_end_object, "leaderboard_field_layout", "messages")) {
                            JsonObject leaderboard_field_object = raid_end_object.get("leaderboard_field_layout").getAsJsonObject();
                            FieldData leaderboard_field = getFieldData(leaderboard_field_object);
                            WebhookHandler.endEmbedLeaderboardField = new FieldData(leaderboard_field.inline(), leaderboard_field.name(), leaderboard_field.value(), false);
                        }
                    }
                }
            }
            if (ConfigHelper.checkProperty(discord_object, "raid_failed", "messages")) {
                JsonObject raid_failed_object = discord_object.get("raid_failed").getAsJsonObject();
                if (ConfigHelper.checkProperty(raid_failed_object, "enabled", "messages")) {
                    WebhookHandler.failedEmbedEnabled = raid_failed_object.get("enabled").getAsBoolean();
                }
                if (WebhookHandler.failedEmbedEnabled) {
                    boolean has_leaderboard = false;
                    if (ConfigHelper.checkProperty(raid_failed_object, "embed_title", "messages")) {
                        WebhookHandler.failedEmbedTitle = raid_failed_object.get("embed_title").getAsString();
                    }
                    if (ConfigHelper.checkProperty(raid_failed_object, "fields", "messages")) {
                        JsonArray raid_end_fields_array = raid_failed_object.get("fields").getAsJsonArray();
                        List<FieldData> fields = new ArrayList<>();
                        for (JsonElement field_element : raid_end_fields_array) {
                            JsonObject field_object = field_element.getAsJsonObject();
                            FieldData fieldData = getFieldData(field_object);
                            fields.add(fieldData);
                            if (fieldData.insertLeaderboardAfter()) {
                                has_leaderboard = true;
                            }
                        }
                        WebhookHandler.failedEmbedFields = fields;
                    }
                    if (has_leaderboard) {
                        if (ConfigHelper.checkProperty(raid_failed_object, "leaderboard_field_layout", "messages")) {
                            JsonObject leaderboard_field_object = raid_failed_object.get("leaderboard_field_layout").getAsJsonObject();
                            FieldData leaderboard_field = getFieldData(leaderboard_field_object);
                            WebhookHandler.failedEmbedLeaderboardField = new FieldData(leaderboard_field.inline(), leaderboard_field.name(), leaderboard_field.value(), false);
                        }
                    }
                }
            }
        }
    }

    public FieldData getFieldData(JsonObject field_object) {
        boolean inline = false;
        String name = "";
        String value = "";
        boolean insert_leaderboard_after = false;
        if (ConfigHelper.checkProperty(field_object, "inline", "messages")) {
            inline = field_object.get("inline").getAsBoolean();
        }
        if (ConfigHelper.checkProperty(field_object, "name", "messages")) {
            name = field_object.get("name").getAsString();
        }
        if (ConfigHelper.checkProperty(field_object, "value", "messages")) {
            value = field_object.get("value").getAsString();
        }
        if (ConfigHelper.checkProperty(field_object, "insert_leaderboard_after", "messages", false)) {
            insert_leaderboard_after = field_object.get("insert_leaderboard_after").getAsBoolean();
        }
        return new FieldData(inline, name, value, insert_leaderboard_after);
    }

    public String getMessage(String key) {
        try {
            if (messages != null) {
                if (messages.containsKey(key)) {
                    return messages.get(key);
                }
                return "<red>Message \"" + key + "\" not found!";
            }
            return "<red>Null!";
        } catch (NullPointerException e) {
            return "<red>Null!";
        }
    }

    public void execute_command(Raid raid) {
        if (!raidStartCommand.isEmpty()) {
            CommandManager cmdManager = Objects.requireNonNull(NovaRaids.INSTANCE.server()).getCommandManager();
            ServerCommandSource source = NovaRaids.INSTANCE.server().getCommandSource();
            cmdManager.executeWithPrefix(source, TextUtils.parse(raidStartCommand, raid));
        }
    }
}
