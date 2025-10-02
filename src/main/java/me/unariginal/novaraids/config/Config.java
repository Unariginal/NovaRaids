package me.unariginal.novaraids.config;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.Contraband;
import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.utils.TextUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.UserCache;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

public class Config {
    private final NovaRaids nr = NovaRaids.INSTANCE;

    // Raid Settings
    public boolean useQueueSystem = false;
    public boolean runRaidsWithNoPlayers = false;
    public boolean hideOtherCatchEncounters = true;
    public boolean hideOtherPlayersInRaid = false;
    public boolean hideOtherPokemonInRaid = false;
    public boolean reduceLargePokemonSize = true;
    public boolean disableSpawnsInArena = true;
    public boolean bossesHaveInfinitePP = false;
    public boolean allowExperienceGain = false;
    public boolean automaticBattles = false;
    public int automaticBattleDelay = 2;
    public Contraband globalContraband;

    // Item Settings
    public boolean vouchersEnabled = true;
    public boolean vouchersJoinRaids = false;
    public Voucher defaultVoucher = null;

    public Voucher globalChoiceVoucher;

    public Voucher globalRandomVoucher;

    public boolean passesEnabled = true;
    public Pass defaultPass = null;

    public Pass globalPass;

    public boolean raidBallsEnabled = true;
    public boolean playerLinkedRaidBalls = true;
    public List<RaidBall> raidBalls = new ArrayList<>();

    public Config() {
        try {
            loadConfig();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            NovaRaids.LOADED = false;
            NovaRaids.LOGGER.error("[NovaRaids] Failed to load config file.", e);
        }
    }

    public void loadConfig() throws IOException, NullPointerException, UnsupportedOperationException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        File file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/config.json").toFile();

        JsonObject config = new JsonObject();
        if (file.exists()) config = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

        if (config.has("debug"))
            NovaRaids.INSTANCE.debug = config.get("debug").getAsBoolean();
        config.remove("debug");
        config.addProperty("debug", NovaRaids.INSTANCE.debug);

        JsonObject raidSettingsObject = new JsonObject();
        if (config.has("raid_settings"))
            raidSettingsObject = config.getAsJsonObject("raid_settings");

        if (raidSettingsObject.has("use_queue_system"))
            useQueueSystem = raidSettingsObject.get("use_queue_system").getAsBoolean();
        raidSettingsObject.remove("use_queue_system");
        raidSettingsObject.addProperty("use_queue_system", useQueueSystem);

        if (raidSettingsObject.has("run_raids_with_no_players"))
            runRaidsWithNoPlayers = raidSettingsObject.get("run_raids_with_no_players").getAsBoolean();
        raidSettingsObject.remove("run_raids_with_no_players");
        raidSettingsObject.addProperty("run_raids_with_no_players", runRaidsWithNoPlayers);

        if (raidSettingsObject.has("hide_other_catch_encounters"))
            hideOtherCatchEncounters = raidSettingsObject.get("hide_other_catch_encounters").getAsBoolean();
        raidSettingsObject.remove("hide_other_catch_encounters");
        raidSettingsObject.addProperty("hide_other_catch_encounters", hideOtherCatchEncounters);

        if (raidSettingsObject.has("hide_other_players_in_raid"))
            hideOtherPlayersInRaid = raidSettingsObject.get("hide_other_players_in_raid").getAsBoolean();
        raidSettingsObject.remove("hide_other_players_in_raid");
        raidSettingsObject.addProperty("hide_other_players_in_raid", hideOtherPlayersInRaid);

        if (raidSettingsObject.has("hide_other_pokemon_in_raid"))
            hideOtherPokemonInRaid = raidSettingsObject.get("hide_other_pokemon_in_raid").getAsBoolean();
        raidSettingsObject.remove("hide_other_pokemon_in_raid");
        raidSettingsObject.addProperty("hide_other_pokemon_in_raid", hideOtherPokemonInRaid);

        if (raidSettingsObject.has("reduce_large_pokemon_size"))
            reduceLargePokemonSize = raidSettingsObject.get("reduce_large_pokemon_size").getAsBoolean();
        raidSettingsObject.remove("reduce_large_pokemon_size");
        raidSettingsObject.addProperty("reduce_large_pokemon_size", reduceLargePokemonSize);

        if (raidSettingsObject.has("disable_spawns_in_arena"))
            disableSpawnsInArena = raidSettingsObject.get("disable_spawns_in_arena").getAsBoolean();
        raidSettingsObject.remove("disable_spawns_in_arena");
        raidSettingsObject.addProperty("disable_spawns_in_arena", disableSpawnsInArena);

        if (raidSettingsObject.has("bosses_have_infinite_pp"))
            bossesHaveInfinitePP = raidSettingsObject.get("bosses_have_infinite_pp").getAsBoolean();
        raidSettingsObject.remove("bosses_have_infinite_pp");
        raidSettingsObject.addProperty("bosses_have_infinite_pp", bossesHaveInfinitePP);

        if (raidSettingsObject.has("allow_experience_gain"))
            allowExperienceGain = raidSettingsObject.get("allow_experience_gain").getAsBoolean();
        raidSettingsObject.remove("allow_experience_gain");
        raidSettingsObject.addProperty("allow_experience_gain", allowExperienceGain);

        if (raidSettingsObject.has("automatic_battles"))
            automaticBattles = raidSettingsObject.get("automatic_battles").getAsBoolean();
        raidSettingsObject.remove("automatic_battles");
        raidSettingsObject.addProperty("automatic_battles", automaticBattles);

        if (raidSettingsObject.has("automatic_battle_delay_seconds"))
            automaticBattleDelay = raidSettingsObject.get("automatic_battle_delay_seconds").getAsInt();
        raidSettingsObject.remove("automatic_battle_delay_seconds");
        raidSettingsObject.addProperty("automatic_battle_delay_seconds", automaticBattleDelay);

        JsonObject globalContrabandObject = new JsonObject();
        if (raidSettingsObject.has("global_contraband"))
            globalContrabandObject = raidSettingsObject.getAsJsonObject("global_contraband");

        globalContraband = ConfigHelper.getContraband(globalContrabandObject, file.getName());

        raidSettingsObject.remove("global_contraband");
        raidSettingsObject.add("global_contraband", globalContraband.contrabandObject());

        config.remove("raid_settings");
        config.add("raid_settings", raidSettingsObject);

        JsonObject itemSettingsObject = new JsonObject();
        if (config.has("item_settings"))
            itemSettingsObject = config.getAsJsonObject("item_settings");

        JsonObject voucherSettingsObject = new JsonObject();
        if (itemSettingsObject.has("voucher_settings"))
            voucherSettingsObject = itemSettingsObject.getAsJsonObject("voucher_settings");

        if (voucherSettingsObject.has("vouchers_enabled"))
            vouchersEnabled = voucherSettingsObject.get("vouchers_enabled").getAsBoolean();
        voucherSettingsObject.remove("vouchers_enabled");
        voucherSettingsObject.addProperty("vouchers_enabled", vouchersEnabled);

        if (voucherSettingsObject.has("join_raid_after_voucher_use"))
            vouchersJoinRaids = voucherSettingsObject.get("join_raid_after_voucher_use").getAsBoolean();
        voucherSettingsObject.remove("join_raid_after_voucher_use");
        voucherSettingsObject.addProperty("join_raid_after_voucher_use", vouchersJoinRaids);

        JsonObject defaultVoucherObject = new JsonObject();
        if (voucherSettingsObject.has("default_voucher"))
            defaultVoucherObject = voucherSettingsObject.getAsJsonObject("default_voucher");

        defaultVoucher = ConfigHelper.getVoucher(defaultVoucherObject, null);

        voucherSettingsObject.remove("default_voucher");
        voucherSettingsObject.add("default_voucher", defaultVoucher.voucherObject());

        JsonObject globalChoiceVoucherObject = new JsonObject();
        if (voucherSettingsObject.has("global_choice_voucher"))
            globalChoiceVoucherObject = voucherSettingsObject.getAsJsonObject("global_choice_voucher");

        globalChoiceVoucher = ConfigHelper.getVoucher(globalChoiceVoucherObject, defaultVoucher);

        voucherSettingsObject.remove("global_choice_voucher");
        voucherSettingsObject.add("global_choice_voucher", globalChoiceVoucher.voucherObject());

        JsonObject globalRandomVoucherObject = new JsonObject();
        if (voucherSettingsObject.has("global_random_voucher"))
            globalRandomVoucherObject = voucherSettingsObject.getAsJsonObject("global_random_voucher");

        globalRandomVoucher = ConfigHelper.getVoucher(globalRandomVoucherObject,defaultVoucher);

        voucherSettingsObject.remove("global_random_voucher");
        voucherSettingsObject.add("global_random_voucher", globalRandomVoucher.voucherObject());

        itemSettingsObject.remove("voucher_settings");
        itemSettingsObject.add("voucher_settings", voucherSettingsObject);

        JsonObject passSettingsObject = new JsonObject();
        if (itemSettingsObject.has("pass_settings"))
            passSettingsObject = itemSettingsObject.getAsJsonObject("pass_settings");

        if (passSettingsObject.has("passes_enabled"))
            passesEnabled = passSettingsObject.get("passes_enabled").getAsBoolean();
        passSettingsObject.remove("passes_enabled");
        passSettingsObject.addProperty("passes_enabled", passesEnabled);

        JsonObject defaultPassObject = new JsonObject();
        if (passSettingsObject.has("default_pass"))
            defaultPassObject = passSettingsObject.getAsJsonObject("default_pass");

        defaultPass = ConfigHelper.getPass(defaultPassObject, null);

        passSettingsObject.remove("default_pass");
        passSettingsObject.add("default_pass", defaultPass.passObject());

        JsonObject globalPassObject = new JsonObject();
        if (passSettingsObject.has("global_pass"))
            globalPassObject = passSettingsObject.getAsJsonObject("global_pass");

        globalPass = ConfigHelper.getPass(globalPassObject, globalPass);

        passSettingsObject.remove("global_pass");
        passSettingsObject.add("global_pass", globalPass.passObject());

        itemSettingsObject.remove("pass_settings");
        itemSettingsObject.add("pass_settings", passSettingsObject);

        JsonObject raidBallSettingsObject = new JsonObject();
        if (itemSettingsObject.has("raid_ball_settings"))
            raidBallSettingsObject = itemSettingsObject.getAsJsonObject("raid_ball_settings");

        if (raidBallSettingsObject.has("raid_balls_enabled"))
            raidBallsEnabled = raidBallSettingsObject.get("raid_balls_enabled").getAsBoolean();
        raidBallSettingsObject.remove("raid_balls_enabled");
        raidBallSettingsObject.addProperty("raid_balls_enabled", raidBallsEnabled);

        if (raidBallSettingsObject.has("player_linked_raid_balls"))
            playerLinkedRaidBalls = raidBallSettingsObject.get("player_linked_raid_balls").getAsBoolean();
        raidBallSettingsObject.remove("player_linked_raid_balls");
        raidBallSettingsObject.addProperty("player_linked_raid_balls", playerLinkedRaidBalls);

        JsonObject globalRaidBallsObject = new JsonObject();
        if (raidBallSettingsObject.has("raid_balls"))
            globalRaidBallsObject = raidBallSettingsObject.getAsJsonObject("raid_balls");

        raidBalls = ConfigHelper.getRaidBalls(globalRaidBallsObject);

        for (RaidBall raidBall : raidBalls) {
            globalRaidBallsObject.remove(raidBall.id());
            globalRaidBallsObject.add(raidBall.id(), raidBall.raidBallObject());
        }

        raidBallSettingsObject.remove("raid_balls");
        raidBallSettingsObject.add("raid_balls", globalRaidBallsObject);

        itemSettingsObject.remove("raid_ball_settings");
        itemSettingsObject.add("raid_ball_settings", raidBallSettingsObject);

        config.remove("item_settings");
        config.add("item_settings", itemSettingsObject);

        file.delete();
        file.createNewFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(file);
        gson.toJson(config, writer);
        writer.close();
    }

    public void writeResults(Raid raid) throws IOException, NoSuchElementException {
        File historyFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/history/" + raid.raidBossCategory().id()).toFile();
        if (!historyFolder.exists()) historyFolder.mkdirs();

        File historyFile = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/history/" + raid.raidBossCategory().id() + "/" + raid.bossInfo().bossId() + ".json").toFile();

        JsonObject root = new JsonObject();
        if (historyFile.exists()) root = JsonParser.parseReader(new FileReader(historyFile)).getAsJsonObject();

        JsonObject thisRaid = new JsonObject();
        thisRaid.addProperty("uuid", raid.uuid().toString());
        thisRaid.addProperty("length", TextUtils.hms(raid.raidCompletionTime()));
        thisRaid.addProperty("had_catch_phase", raid.bossInfo().raidDetails().doCatchPhase());
        thisRaid.addProperty("total_players", raid.getDamageLeaderboard().size());

        JsonArray thisRaidLeaderboard = new JsonArray();
        int place = 1;
        for (Map.Entry<String, Integer> entry : raid.getDamageLeaderboard()) {
            JsonObject leaderboardEntry = new JsonObject();
            UserCache cache =  nr.server().getUserCache();
            if (cache != null) {
                Optional<GameProfile> profile = cache.findByName(entry.getKey());
                if (profile.isPresent()) {
                    leaderboardEntry.addProperty("player_uuid", profile.get().getId().toString());
                    leaderboardEntry.addProperty("player_name", entry.getKey());
                }
            } else {
                leaderboardEntry.addProperty("player_name", entry.getKey());
            }
            leaderboardEntry.addProperty("damage", entry.getValue());
            leaderboardEntry.addProperty("place", place++);
            thisRaidLeaderboard.add(leaderboardEntry);
        }
        thisRaid.add("leaderboard", thisRaidLeaderboard);

        root.add(LocalDateTime.now(nr.schedulesConfig().zone).toString(), thisRaid);

        historyFile.createNewFile();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Writer writer = new FileWriter(historyFile);
        gson.toJson(root, writer);
        writer.close();
    }

    public RaidBall getRaidBall(String id) {
        for (RaidBall ball : raidBalls) {
            if (ball.id().equals(id)) {
                return ball;
            }
        }
        return null;
    }
}
