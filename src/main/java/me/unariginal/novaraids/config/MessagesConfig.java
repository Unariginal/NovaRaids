package me.unariginal.novaraids.config;

import com.google.gson.*;
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
    public LinkedHashMap<String, String> messages = new LinkedHashMap<>();

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
            NovaRaids.LOGGER.error("[NovaRaids] Failed to load messages file.", e);
        }
    }

    public void loadConfig() throws IOException, NullPointerException, UnsupportedOperationException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        File file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/messages.json").toFile();
        JsonObject config = new JsonObject();
        if (file.exists()) config = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

        if (config.has("prefix"))
            prefix = config.get("prefix").getAsString();
        config.remove("prefix");
        config.addProperty("prefix", prefix);

        if (config.has("raid_start_command"))
            raidStartCommand = config.get("raid_start_command").getAsString();
        config.remove("raid_start_command");
        config.addProperty("raid_start_command", raidStartCommand);

        JsonObject messagesObject = new JsonObject();
        if (config.has("messages"))
            messagesObject = config.getAsJsonObject("messages");

        for (String messageKey : messages.keySet()) {
            if (messagesObject.has(messageKey))
                messages.put(messageKey, messagesObject.get(messageKey).getAsString());
            messagesObject.remove(messageKey);
            messagesObject.addProperty(messageKey, messages.get(messageKey));
        }

        config.remove("messages");
        config.add("messages", messagesObject);

        JsonObject webhookObject = new JsonObject();
        if (config.has("discord"))
            webhookObject = config.getAsJsonObject("discord");

        if (webhookObject.has("webhook_toggle"))
            WebhookHandler.webhookToggle = webhookObject.get("webhook_toggle").getAsBoolean();
        webhookObject.remove("webhook_toggle");
        webhookObject.addProperty("webhook_toggle", WebhookHandler.webhookToggle);

        JsonArray blacklistedCategories = new JsonArray();
        if (webhookObject.has("blacklisted_categories"))
            WebhookHandler.blacklistedCategories = webhookObject.getAsJsonArray("blacklisted_categories").asList().stream().map(JsonElement::getAsString).toList();

        for (String category : WebhookHandler.blacklistedCategories) {
            blacklistedCategories.add(category);
        }

        webhookObject.remove("blacklisted_categories");
        webhookObject.add("blacklisted_categories", blacklistedCategories);

        JsonArray blacklistedBosses = new JsonArray();
        if (webhookObject.has("blacklisted_bosses"))
            WebhookHandler.blacklistedBosses = webhookObject.getAsJsonArray("blacklisted_bosses").asList().stream().map(JsonElement::getAsString).toList();

        for (String category : WebhookHandler.blacklistedBosses) {
            blacklistedBosses.add(category);
        }

        webhookObject.remove("blacklisted_bosses");
        webhookObject.add("blacklisted_bosses", blacklistedBosses);

        if (webhookObject.has("webhook_url"))
            WebhookHandler.webhookUrl = webhookObject.get("webhook_url").getAsString();
        webhookObject.remove("webhook_url");
        webhookObject.addProperty("webhook_url", WebhookHandler.webhookUrl);

        if (webhookObject.has("webhook_username"))
            WebhookHandler.webhookUsername = webhookObject.get("webhook_username").getAsString();
        webhookObject.remove("webhook_username");
        webhookObject.addProperty("webhook_username", WebhookHandler.webhookUsername);

        if (webhookObject.has("webhook_avatar_url"))
            WebhookHandler.webhookAvatarUrl = webhookObject.get("webhook_avatar_url").getAsString();
        webhookObject.remove("webhook_avatar_url");
        webhookObject.addProperty("webhook_avatar_url", WebhookHandler.webhookAvatarUrl);

        if (webhookObject.has("role_ping"))
            WebhookHandler.rolePing = webhookObject.get("role_ping").getAsString();
        webhookObject.remove("role_ping");
        webhookObject.addProperty("role_ping", WebhookHandler.rolePing);

        if (webhookObject.has("webhook_update_rate_seconds"))
            WebhookHandler.webhookUpdateRateSeconds = webhookObject.get("webhook_update_rate_seconds").getAsInt();
        webhookObject.remove("webhook_update_rate_seconds");
        webhookObject.addProperty("webhook_update_rate_seconds", WebhookHandler.webhookUpdateRateSeconds);

        if (webhookObject.has("delete_if_no_fight_phase"))
            WebhookHandler.deleteIfNoFightPhase = webhookObject.get("delete_if_no_fight_phase").getAsBoolean();
        webhookObject.remove("delete_if_no_fight_phase");
        webhookObject.addProperty("delete_if_no_fight_phase", WebhookHandler.deleteIfNoFightPhase);

        // Raid Start Embed
        JsonObject raidStartObject = new JsonObject();
        if (webhookObject.has("raid_start"))
            raidStartObject = webhookObject.get("raid_start").getAsJsonObject();

        if (raidStartObject.has("enabled"))
            WebhookHandler.startEmbedEnabled = raidStartObject.get("enabled").getAsBoolean();
        raidStartObject.remove("enabled");
        raidStartObject.addProperty("enabled", WebhookHandler.startEmbedEnabled);

        if (raidStartObject.has("embed_title"))
            WebhookHandler.startEmbedTitle = raidStartObject.get("embed_title").getAsString();
        raidStartObject.remove("embed_title");
        raidStartObject.addProperty("embed_title", WebhookHandler.startEmbedTitle);

        JsonArray raidStartFieldsArray = new JsonArray();
        if (raidStartObject.has("fields"))
            raidStartFieldsArray = raidStartObject.get("fields").getAsJsonArray();

        List<FieldData> raidStartFields = new ArrayList<>();
        for (JsonElement fieldElement : raidStartFieldsArray) {
            JsonObject fieldObject = fieldElement.getAsJsonObject();
            raidStartFields.add(getFieldData(fieldObject, false));
        }

        if (raidStartFields.isEmpty()) {
            JsonObject fieldObject = new JsonObject();
            fieldObject.addProperty("inline", false);
            fieldObject.addProperty("name", "Players: %raid.player_count%/%raid.max_players%");
            fieldObject.addProperty("value", "Join Now Using `%raid.join_method%`!");
            raidStartFields.add(getFieldData(fieldObject, false));
        }

        WebhookHandler.startEmbedFields = raidStartFields;

        raidStartFieldsArray = new JsonArray();
        for (FieldData fieldData : raidStartFields) {
            raidStartFieldsArray.add(fieldData.fieldObject());
        }

        raidStartObject.remove("fields");
        raidStartObject.add("fields", raidStartFieldsArray);

        webhookObject.remove("raid_start");
        webhookObject.add("raid_start", raidStartObject);

        // Raid Running Embed
        JsonObject raidRunningObject = new JsonObject();
        if (webhookObject.has("raid_running"))
            raidRunningObject = webhookObject.get("raid_running").getAsJsonObject();

        if (raidRunningObject.has("enabled"))
            WebhookHandler.runningEmbedEnabled = raidRunningObject.get("enabled").getAsBoolean();
        raidRunningObject.remove("enabled");
        raidRunningObject.addProperty("enabled", WebhookHandler.runningEmbedEnabled);

        if (raidRunningObject.has("embed_title"))
            WebhookHandler.runningEmbedTitle = raidRunningObject.get("embed_title").getAsString();
        raidRunningObject.remove("embed_title");
        raidRunningObject.addProperty("embed_title", WebhookHandler.runningEmbedTitle);

        JsonArray raidRunningFieldsArray = new JsonArray();
        if (raidRunningObject.has("fields"))
            raidRunningFieldsArray = raidRunningObject.get("fields").getAsJsonArray();

        List<FieldData> raidRunningFields = new ArrayList<>();
        for (JsonElement fieldElement : raidRunningFieldsArray) {
            JsonObject fieldObject = fieldElement.getAsJsonObject();
            raidRunningFields.add(getFieldData(fieldObject, true));
        }

        if (raidRunningFields.isEmpty()) {
            JsonObject fieldObject = new JsonObject();
            fieldObject.addProperty("inline", false);
            fieldObject.addProperty("name", "Current Health");
            fieldObject.addProperty("value", "%boss.currenthp%/%boss.maxhp% HP");
            raidRunningFields.add(getFieldData(fieldObject, true));

            fieldObject = new JsonObject();
            fieldObject.addProperty("inline", false);
            fieldObject.addProperty("name", "Current Leaderboard");
            fieldObject.addProperty("value", "");
            fieldObject.addProperty("insert_leaderboard_after", true);
            raidRunningFields.add(getFieldData(fieldObject, true));
        }

        WebhookHandler.runningEmbedFields = raidRunningFields;

        raidRunningFieldsArray = new JsonArray();
        for (FieldData fieldData : raidRunningFields) {
            raidRunningFieldsArray.add(fieldData.fieldObject());
        }

        raidRunningObject.remove("fields");
        raidRunningObject.add("fields", raidRunningFieldsArray);

        JsonObject leaderboardFieldObject = new JsonObject();
        if (raidRunningObject.has("leaderboard_field_layout"))
            leaderboardFieldObject = raidRunningObject.get("leaderboard_field_layout").getAsJsonObject();

        WebhookHandler.runningEmbedLeaderboardField = getFieldData(leaderboardFieldObject, false);
        leaderboardFieldObject = WebhookHandler.runningEmbedLeaderboardField.fieldObject();

        raidRunningObject.remove("leaderboard_field_layout");
        raidRunningObject.add("leaderboard_field_layout", leaderboardFieldObject);

        webhookObject.remove("raid_running");
        webhookObject.add("raid_running", raidRunningObject);

        // Raid End Embed
        JsonObject raidEndObject = new JsonObject();
        if (webhookObject.has("raid_end"))
            raidEndObject = webhookObject.get("raid_end").getAsJsonObject();

        if (raidEndObject.has("enabled"))
            WebhookHandler.endEmbedEnabled = raidEndObject.get("enabled").getAsBoolean();
        raidEndObject.remove("enabled");
        raidEndObject.addProperty("enabled", WebhookHandler.endEmbedEnabled);

        if (raidEndObject.has("embed_title"))
            WebhookHandler.endEmbedTitle = raidEndObject.get("embed_title").getAsString();
        raidEndObject.remove("embed_title");
        raidEndObject.addProperty("embed_title", WebhookHandler.endEmbedTitle);

        JsonArray raidEndFieldsArray = new JsonArray();
        if (raidEndObject.has("fields"))
            raidEndFieldsArray = raidEndObject.get("fields").getAsJsonArray();

        List<FieldData> raidEndFields = new ArrayList<>();
        for (JsonElement fieldElement : raidEndFieldsArray) {
            JsonObject fieldObject = fieldElement.getAsJsonObject();
            raidEndFields.add(getFieldData(fieldObject, true));
        }

        if (raidEndFields.isEmpty()) {
            JsonObject fieldObject = new JsonObject();
            fieldObject.addProperty("inline", false);
            fieldObject.addProperty("name", "------- Raid Results -------");
            fieldObject.addProperty("value", "");
            fieldObject.addProperty("insert_leaderboard_after", true);
            raidEndFields.add(getFieldData(fieldObject, true));
        }

        WebhookHandler.endEmbedFields = raidEndFields;

        raidEndFieldsArray = new JsonArray();
        for (FieldData fieldData : raidEndFields) {
            raidEndFieldsArray.add(fieldData.fieldObject());
        }

        raidEndObject.remove("fields");
        raidEndObject.add("fields", raidEndFieldsArray);

        leaderboardFieldObject = new JsonObject();
        if (raidEndObject.has("leaderboard_field_layout"))
            leaderboardFieldObject = raidEndObject.get("leaderboard_field_layout").getAsJsonObject();

        WebhookHandler.endEmbedLeaderboardField = getFieldData(leaderboardFieldObject, false);
        leaderboardFieldObject = WebhookHandler.endEmbedLeaderboardField.fieldObject();

        raidEndObject.remove("leaderboard_field_layout");
        raidEndObject.add("leaderboard_field_layout", leaderboardFieldObject);

        webhookObject.remove("raid_end");
        webhookObject.add("raid_end", raidEndObject);

        // Raid Failed Embed
        JsonObject raidFailedObject = new JsonObject();
        if (webhookObject.has("raid_failed"))
            raidFailedObject = webhookObject.get("raid_failed").getAsJsonObject();

        if (raidFailedObject.has("enabled"))
            WebhookHandler.failedEmbedEnabled = raidFailedObject.get("enabled").getAsBoolean();
        raidFailedObject.remove("enabled");
        raidFailedObject.addProperty("enabled", WebhookHandler.failedEmbedEnabled);

        if (raidFailedObject.has("embed_title"))
            WebhookHandler.failedEmbedTitle = raidFailedObject.get("embed_title").getAsString();
        raidFailedObject.remove("embed_title");
        raidFailedObject.addProperty("embed_title", WebhookHandler.failedEmbedTitle);

        JsonArray raidFailedFieldsArray = new JsonArray();
        if (raidFailedObject.has("fields"))
            raidFailedFieldsArray = raidFailedObject.get("fields").getAsJsonArray();

        List<FieldData> raidFailedFields = new ArrayList<>();
        for (JsonElement fieldElement : raidFailedFieldsArray) {
            JsonObject fieldObject = fieldElement.getAsJsonObject();
            raidFailedFields.add(getFieldData(fieldObject, true));
        }

        if (raidFailedFields.isEmpty()) {
            JsonObject fieldObject = new JsonObject();
            fieldObject.addProperty("inline", false);
            fieldObject.addProperty("name", "Dealt %raid.total_damage%/%boss.maxhp% damage!");
            fieldObject.addProperty("value", "");
            fieldObject.addProperty("insert_leaderboard_after", false);
            raidFailedFields.add(getFieldData(fieldObject, true));
        }

        WebhookHandler.failedEmbedFields = raidFailedFields;

        raidFailedFieldsArray = new JsonArray();
        for (FieldData fieldData : raidFailedFields) {
            raidFailedFieldsArray.add(fieldData.fieldObject());
        }

        raidFailedObject.remove("fields");
        raidFailedObject.add("fields", raidFailedFieldsArray);

        leaderboardFieldObject = new JsonObject();
        if (raidFailedObject.has("leaderboard_field_layout"))
            leaderboardFieldObject = raidFailedObject.get("leaderboard_field_layout").getAsJsonObject();

        WebhookHandler.failedEmbedLeaderboardField = getFieldData(leaderboardFieldObject, false);
        leaderboardFieldObject = WebhookHandler.failedEmbedLeaderboardField.fieldObject();

        raidFailedObject.remove("leaderboard_field_layout");
        raidFailedObject.add("leaderboard_field_layout", leaderboardFieldObject);

        webhookObject.remove("raid_failed");
        webhookObject.add("raid_failed", raidFailedObject);

        config.remove("discord");
        config.add("discord", webhookObject);

        file.delete();
        file.createNewFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(file);
        gson.toJson(config, writer);
        writer.close();
    }

    public FieldData getFieldData(JsonObject fieldObject, boolean hasLeaderboard) {
        boolean inline = false;
        String name = "";
        String value = "";
        boolean insertLeaderboardAfter = false;

        if (fieldObject.has("inline"))
            inline = fieldObject.get("inline").getAsBoolean();
        fieldObject.remove("inline");
        fieldObject.addProperty("inline", inline);

        if (fieldObject.has("name"))
            name = fieldObject.get("name").getAsString();
        fieldObject.remove("name");
        fieldObject.addProperty("name", name);

        if (fieldObject.has("value"))
            value = fieldObject.get("value").getAsString();
        fieldObject.remove("value");
        fieldObject.addProperty("value", value);

        if (hasLeaderboard) {
            if (fieldObject.has("insert_leaderboard_after"))
                insertLeaderboardAfter = fieldObject.get("insert_leaderboard_after").getAsBoolean();
            fieldObject.remove("insert_leaderboard_after");
            fieldObject.addProperty("insert_leaderboard_after", insertLeaderboardAfter);
        }

        return new FieldData(fieldObject, inline, name, value, insertLeaderboardAfter);
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

    public void executeCommand(Raid raid) {
        if (!raidStartCommand.isEmpty()) {
            CommandManager cmdManager = Objects.requireNonNull(NovaRaids.INSTANCE.server()).getCommandManager();
            ServerCommandSource source = NovaRaids.INSTANCE.server().getCommandSource();
            cmdManager.executeWithPrefix(source, TextUtils.parse(raidStartCommand, raid));
        }
    }
}
