package me.unariginal.novaraids.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.utils.TextUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MessagesConfig {
    public String prefix = "<dark_gray>[</dark_gray><color:#ffbf00>RAID<dark_gray>]</dark_gray>";
    public String raid_start_command = "";
    public Map<String, String> messages = new HashMap<>();

    public MessagesConfig() {
        // Shut up I know there are better ways to do this *but I don't wanna*
        messages.put("start_pre_phase", "%prefix% A raid is starting against %boss.form% %boss.species%!");
        messages.put("start_fight_phase", "%prefix% Time to fight %boss.form% %boss.species% (%boss.maxhp% HP)!");
        messages.put("boss_defeated", "%prefix% %boss.form% %boss.species% has been defeated in %raid.defeat_time%!");
        messages.put("catch_phase_warning", "%prefix% Prepare to catch %boss.form% %boss.species%");
        messages.put("start_catch_phase", "%prefix% Time to catch %boss.form% %boss.species%!");
        messages.put("catch_phase_end", "%prefix% Catch phase completed!");
        messages.put("raid_end", "%prefix% The %boss.form% %boss.species% raid has finished.");
        messages.put("out_of_time", "%prefix% %boss.form% %boss.species% was not defeated in time!");
        messages.put("not_enough_players", "%prefix% There wasn't enough players to start the raid!");
        messages.put("player_damage_report", "%prefix% %raid.player% did %raid.player.damage% to the boss!");
        messages.put("no_active_raids", "%prefix% There are no active raids!");
        messages.put("no_queued_raids", "%prefix% There are no queued raids!");
        messages.put("no_available_locations", "%prefix% Failed to start raid. All possible locations are busy.");
        messages.put("joined_raid", "%prefix% You've successfully joined the %boss.form% %boss.species% raid!");
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
        messages.put("warning_cooldown", "%prefix% You're on cooldown!");
        messages.put("warning_battle_during_raid", "%prefix% You can't battle wild pokemon during a raid!");
        messages.put("warning_not_your_encounter", "%prefix% This isn't your catch encounter!");
        messages.put("warning_not_your_raid_pokeball", "%prefix% That isn't your raid pokeball!");
        messages.put("warning_raid_pokeball_outside_raid", "%prefix% You can't use raid pokeballs outside of a raid!");
        messages.put("warning_not_catch_phase", "%prefix% You can only use this during the catch phase!");
        messages.put("warning_deny_normal_pokeball", "%prefix% You can only use raid pokeballs in a raid!");
        messages.put("used_voucher", "%prefix% You've successfully started a raid for %boss.form% %boss.species%!");
        messages.put("queue_item_cancelled", "%prefix% You've cancelled the queue for %boss.form% %boss.species%!");
        messages.put("added_to_queue", "%prefix% %boss.form% %boss.species% has been added to the queue!");
        messages.put("reload_command", "%prefix% Reloaded!");
        messages.put("raid_stopped", "%prefix% You've stopped the raid against %boss.form% %boss.species%!");
        messages.put("give_command_invalid_category", "%prefix% Category %category% does not exist!");
        messages.put("give_command_invalid_pokeball", "%prefix% Pokeball %pokeball% does not exist!");
        messages.put("give_command_invalid_boss", "%prefix% Boss %boss% does not exist!");
        messages.put("give_command_failed_to_give", "%prefix% Failed to give the item!");
        messages.put("give_command_received_item", "%prefix% You received a %raid_item%!");
        messages.put("give_command_feedback", "%prefix% Successfully gave %target% a %raid_item%");
        messages.put("raid_list_gui_title", "Active Raids");
        messages.put("raid_queue_gui_title", "Queued Raids");
        messages.put("contraband_gui_title", "Raid Contraband");
        messages.put("leaderboard_message_header", "------- Raid Results -------");
        messages.put("leaderboard_message_item", " - [%raid.player.place%] %raid.player% : %raid.player.damage% damage");
        messages.put("leaderboard_individual", "You placed %raid.player.place%%place_suffix% with %raid.player.damage% damage!");

        try {
            loadConfig();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            NovaRaids.INSTANCE.loaded_properly = false;
            NovaRaids.INSTANCE.logError("[RAIDS] Failed to load messages file. " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                NovaRaids.INSTANCE.logError("  " + element.toString());
            }
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
            raid_start_command = config.get("raid_start_command").getAsString();
        }
        if (ConfigHelper.checkProperty(config, "messages", "messages")) {
            JsonObject messages_object = config.get("messages").getAsJsonObject();
            for (Map.Entry<String, String> message : messages.entrySet()) {
                if (ConfigHelper.checkProperty(messages_object, message.getKey(), "messages")) {
                    messages.put(message.getKey(), messages_object.get(message.getKey()).getAsString());
                }
            }
        }
        // TODO: Webhook
    }

    public String getMessage(String key) {
        if (messages.containsKey(key)) {
            return messages.get(key);
        }
        return "null";
    }

    public void execute_command(Raid raid) {
        if (!raid_start_command.isEmpty()) {
            CommandManager cmdManager = Objects.requireNonNull(NovaRaids.INSTANCE.server()).getCommandManager();
            ServerCommandSource source = NovaRaids.INSTANCE.server().getCommandSource();
            cmdManager.executeWithPrefix(source, TextUtils.parse(raid_start_command, raid));
        }
    }
}
