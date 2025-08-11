package me.unariginal.novaraids.config;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.utils.TextUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.UserCache;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

public class Config {
    private final NovaRaids nr = NovaRaids.INSTANCE;

    // Raid Settings
    public boolean use_queue_system = false;
    public boolean run_raids_with_no_players = false;
    public boolean hide_other_catch_encounters = true;
    public boolean hide_other_players_in_raid = false;
    public boolean hide_other_pokemon_in_raid = false;
    public List<Species> global_banned_pokemon = new ArrayList<>();
    public List<Move> global_banned_moves = new ArrayList<>();
    public List<Ability> global_banned_abilities = new ArrayList<>();
    public List<Item> global_banned_held_items = new ArrayList<>();
    public List<Item> global_banned_bag_items = new ArrayList<>();

    // Item Settings
    public boolean vouchers_enabled = true;
    public Voucher default_voucher = new Voucher(
            Items.FEATHER,
            "<aqua>Raid Voucher",
            List.of(
                    "<gray>Use this to start a raid!"
            ),
            ComponentChanges.EMPTY
    );

    public Voucher global_choice_voucher = new Voucher(
            Items.FEATHER,
            "<aqua>Choice Raid Voucher",
            List.of(
                    "<gray>Use this to start any raid!"
            ),
            ComponentChanges.EMPTY
    );

    public Voucher global_random_voucher = new Voucher(
            Items.FEATHER,
            "<aqua>Random Raid Voucher",
            List.of(
                    "<gray>Use this to start a random raid!"
            ),
            ComponentChanges.EMPTY
    );

    public boolean passes_enabled = true;
    public Pass default_pass = new Pass(
            Items.PAPER,
            "<light_purple>Raid Pass",
            List.of(
                    "<gray>Use this to join a raid!"
            ),
            ComponentChanges.EMPTY
    );

    public Pass global_pass = new Pass(
            Items.PAPER,
            "<light_purple>Global Raid Pass",
            List.of(
                    "<gray>Use this to join any raid!"
            ),
            ComponentChanges.EMPTY
    );

    public boolean raid_balls_enabled = true;
    public List<RaidBall> raid_balls = new ArrayList<>();

    public Config() {
        try {
            loadConfig();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            nr.loadedProperly = false;
            nr.logError("[RAIDS] Failed to load config file. " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                nr.logError("  " + element.toString());
            }
        }
    }

    public void loadConfig() throws IOException, NullPointerException, UnsupportedOperationException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        File file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/config.json").toFile();
        if (file.createNewFile()) {
            InputStream stream = NovaRaids.class.getResourceAsStream("/raid_config_files/config.json");
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
        
        String location = "config";

        if (ConfigHelper.checkProperty(config, "debug", location)) {
            NovaRaids.INSTANCE.debug = config.get("debug").getAsBoolean();
        }

        if (ConfigHelper.checkProperty(config, "raid_settings", location)) {
            JsonObject raid_settings = config.getAsJsonObject("raid_settings");
            if (ConfigHelper.checkProperty(raid_settings, "use_queue_system", location)) {
                use_queue_system = raid_settings.get("use_queue_system").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(raid_settings, "run_raids_with_no_players", location)) {
                run_raids_with_no_players = raid_settings.get("run_raids_with_no_players").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(raid_settings, "hide_other_catch_encounters", location)) {
                hide_other_catch_encounters = raid_settings.get("hide_other_catch_encounters").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(raid_settings, "hide_other_players_in_raid", location)) {
                hide_other_players_in_raid = raid_settings.get("hide_other_players_in_raid").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(raid_settings, "hide_other_pokemon_in_raid", location)) {
                hide_other_pokemon_in_raid = raid_settings.get("hide_other_pokemon_in_raid").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(raid_settings, "global_contraband", location)) {
                JsonObject global_contraband = raid_settings.getAsJsonObject("global_contraband");
                if (ConfigHelper.checkProperty(global_contraband, "banned_pokemon", location)) {
                    JsonArray banned_pokemon = global_contraband.getAsJsonArray("banned_pokemon");
                    for (JsonElement p : banned_pokemon) {
                        String species_name = p.getAsString();
                        Species species = PokemonSpecies.INSTANCE.getByName(species_name);
                        if (species != null) {
                            global_banned_pokemon.add(species);
                        } else {
                            nr.logError("[RAIDS] Species " + species_name + " not found.");
                        }
                    }
                }
                if (ConfigHelper.checkProperty(global_contraband, "banned_moves", location)) {
                    JsonArray banned_moves = global_contraband.getAsJsonArray("banned_moves");
                    for (JsonElement m : banned_moves) {
                        String move_name = m.getAsString();
                        MoveTemplate move_template = Moves.INSTANCE.getByName(move_name);
                        if (move_template != null) {
                            global_banned_moves.add(move_template.create());
                        } else {
                            nr.logError("[RAIDS] Move " + move_name + " not found.");
                        }
                    }
                }
                if (ConfigHelper.checkProperty(global_contraband, "banned_abilities", location)) {
                    JsonArray banned_abilities = global_contraband.getAsJsonArray("banned_abilities");
                    for (JsonElement a : banned_abilities) {
                        String ability_name = a.getAsString();
                        AbilityTemplate ability_template = Abilities.INSTANCE.get(ability_name);
                        if (ability_template != null) {
                            global_banned_abilities.add(ability_template.create(false, Priority.LOWEST));
                        } else {
                            nr.logError("[RAIDS] Ability " + ability_name + " not found.");
                        }
                    }
                }
                if (ConfigHelper.checkProperty(global_contraband, "banned_held_items", location)) {
                    JsonArray banned_held_items = global_contraband.getAsJsonArray("banned_held_items");
                    for (JsonElement h : banned_held_items) {
                        String held_item_name = h.getAsString();
                        Item held_item = Registries.ITEM.get(Identifier.of(held_item_name));
                        global_banned_held_items.add(held_item);
                    }
                }
                if (ConfigHelper.checkProperty(global_contraband, "banned_bag_items", location)) {
                    JsonArray banned_bag_items = global_contraband.getAsJsonArray("banned_bag_items");
                    for (JsonElement h : banned_bag_items) {
                        String bag_item_name = h.getAsString();
                        Item bag_item = Registries.ITEM.get(Identifier.of(bag_item_name));
                        global_banned_bag_items.add(bag_item);
                    }
                }
            }
        }

        if (ConfigHelper.checkProperty(config, "item_settings", location)) {
            JsonObject item_settings = config.getAsJsonObject("item_settings");
            if (ConfigHelper.checkProperty(item_settings, "voucher_settings", location)) {
                JsonObject voucher_settings = item_settings.getAsJsonObject("voucher_settings");
                if (ConfigHelper.checkProperty(voucher_settings, "vouchers_enabled", location)) {
                    vouchers_enabled = voucher_settings.get("vouchers_enabled").getAsBoolean();
                }
                if (vouchers_enabled) {
                    if (ConfigHelper.checkProperty(voucher_settings, "default_voucher", location)) {
                        JsonObject voucher = voucher_settings.getAsJsonObject("default_voucher");
                        Item default_voucher_item = default_voucher.voucherItem();
                        String default_voucher_name = default_voucher.voucherName();
                        List<String> default_voucher_lore = default_voucher.voucherLore();
                        ComponentChanges default_voucher_data = default_voucher.voucherData();

                        if (ConfigHelper.checkProperty(voucher, "voucher_item", location)) {
                            String voucher_item_name = voucher.get("voucher_item").getAsString();
                            default_voucher_item = Registries.ITEM.get(Identifier.of(voucher_item_name));
                        }
                        if (ConfigHelper.checkProperty(voucher, "voucher_name", location)) {
                            default_voucher_name = voucher.get("voucher_name").getAsString();
                        }
                        if (ConfigHelper.checkProperty(voucher, "voucher_lore", location)) {
                            JsonArray lore_items = voucher.getAsJsonArray("voucher_lore");
                            List<String> lore = new ArrayList<>();
                            for (JsonElement l : lore_items) {
                                String lore_item = l.getAsString();
                                lore.add(lore_item);
                            }
                            default_voucher_lore = lore;
                        }
                        if (ConfigHelper.checkProperty(voucher, "voucher_data", location, false)) {
                            JsonElement data = voucher.getAsJsonObject("voucher_data");
                            if (data != null) {
                                default_voucher_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                            }
                        }

                        default_voucher = new Voucher(
                                default_voucher_item,
                                default_voucher_name,
                                default_voucher_lore,
                                default_voucher_data
                        );
                    }

                    if (ConfigHelper.checkProperty(voucher_settings, "global_choice_voucher", location, false)) {
                        JsonObject voucher = voucher_settings.getAsJsonObject("global_choice_voucher");
                        Item global_choice_voucher_item = global_choice_voucher.voucherItem();
                        String global_choice_voucher_name = global_choice_voucher.voucherName();
                        List<String> global_choice_voucher_lore = global_choice_voucher.voucherLore();
                        ComponentChanges global_choice_voucher_data = global_choice_voucher.voucherData();

                        if (ConfigHelper.checkProperty(voucher, "voucher_item", location)) {
                            String voucher_item_name = voucher.get("voucher_item").getAsString();
                            global_choice_voucher_item = Registries.ITEM.get(Identifier.of(voucher_item_name));
                        }
                        if (ConfigHelper.checkProperty(voucher, "voucher_name", location)) {
                            global_choice_voucher_name = voucher.get("voucher_name").getAsString();
                        }
                        if (ConfigHelper.checkProperty(voucher, "voucher_lore", location)) {
                            JsonArray lore_items = voucher.getAsJsonArray("voucher_lore");
                            List<String> lore = new ArrayList<>();
                            for (JsonElement l : lore_items) {
                                String lore_item = l.getAsString();
                                lore.add(lore_item);
                            }
                            global_choice_voucher_lore = lore;
                        }
                        if (ConfigHelper.checkProperty(voucher, "voucher_data", location, false)) {
                            JsonElement data = voucher.getAsJsonObject("voucher_data");
                            if (data != null) {
                                global_choice_voucher_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                            }
                        }

                        global_choice_voucher = new Voucher(
                                global_choice_voucher_item,
                                global_choice_voucher_name,
                                global_choice_voucher_lore,
                                global_choice_voucher_data
                        );
                    }

                    if (ConfigHelper.checkProperty(voucher_settings, "global_random_voucher", location, false)) {
                        JsonObject voucher = voucher_settings.getAsJsonObject("global_random_voucher");
                        Item global_random_voucher_item = global_random_voucher.voucherItem();
                        String global_random_voucher_name = global_random_voucher.voucherName();
                        List<String> global_random_voucher_lore = global_random_voucher.voucherLore();
                        ComponentChanges global_random_voucher_data = global_random_voucher.voucherData();

                        if (ConfigHelper.checkProperty(voucher, "voucher_item", location)) {
                            String voucher_item_name = voucher.get("voucher_item").getAsString();
                            global_random_voucher_item = Registries.ITEM.get(Identifier.of(voucher_item_name));
                        }
                        if (ConfigHelper.checkProperty(voucher, "voucher_name", location)) {
                            global_random_voucher_name = voucher.get("voucher_name").getAsString();
                        }
                        if (ConfigHelper.checkProperty(voucher, "voucher_lore", location)) {
                            JsonArray lore_items = voucher.getAsJsonArray("voucher_lore");
                            List<String> lore = new ArrayList<>();
                            for (JsonElement l : lore_items) {
                                String lore_item = l.getAsString();
                                lore.add(lore_item);
                            }
                            global_random_voucher_lore = lore;
                        }
                        if (ConfigHelper.checkProperty(voucher, "voucher_data", location, false)) {
                            JsonElement data = voucher.getAsJsonObject("voucher_data");
                            if (data != null) {
                                global_random_voucher_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                            }
                        }

                        global_random_voucher = new Voucher(
                                global_random_voucher_item,
                                global_random_voucher_name,
                                global_random_voucher_lore,
                                global_random_voucher_data
                        );
                    }
                }
            }

            if (ConfigHelper.checkProperty(item_settings, "pass_settings", location)) {
                JsonObject pass_settings = item_settings.getAsJsonObject("pass_settings");
                if (ConfigHelper.checkProperty(pass_settings, "passes_enabled", location)) {
                    passes_enabled = pass_settings.get("passes_enabled").getAsBoolean();
                }
                if (passes_enabled) {
                    if (ConfigHelper.checkProperty(pass_settings, "default_pass", location)) {
                        JsonObject pass = pass_settings.getAsJsonObject("default_pass");
                        Item default_pass_item = default_pass.passItem();
                        String default_pass_name = default_pass.passName();
                        List<String> default_pass_lore = default_pass.passLore();
                        ComponentChanges default_pass_data = default_pass.passData();

                        if (ConfigHelper.checkProperty(pass, "pass_item", location)) {
                            String pass_item_name = pass.get("pass_item").getAsString();
                            default_pass_item = Registries.ITEM.get(Identifier.of(pass_item_name));
                        }
                        if (ConfigHelper.checkProperty(pass, "pass_name", location)) {
                            default_pass_name = pass.get("pass_name").getAsString();
                        }
                        if (ConfigHelper.checkProperty(pass, "pass_lore", location)) {
                            JsonArray lore_items = pass.getAsJsonArray("pass_lore");
                            List<String> lore = new ArrayList<>();
                            for (JsonElement l : lore_items) {
                                String lore_item = l.getAsString();
                                lore.add(lore_item);
                            }
                            default_pass_lore = lore;
                        }
                        if (ConfigHelper.checkProperty(pass, "pass_data", location, false)) {
                            JsonElement data = pass.getAsJsonObject("pass_data");
                            if (data != null) {
                                default_pass_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                            }
                        }

                        default_pass = new Pass(
                                default_pass_item,
                                default_pass_name,
                                default_pass_lore,
                                default_pass_data
                        );
                    }

                    if (ConfigHelper.checkProperty(pass_settings, "global_pass", location, false)) {
                        JsonObject pass = pass_settings.getAsJsonObject("global_pass");
                        Item global_pass_item = global_pass.passItem();
                        String global_pass_name = global_pass.passName();
                        List<String> global_pass_lore = global_pass.passLore();
                        ComponentChanges global_pass_data = global_pass.passData();

                        if (ConfigHelper.checkProperty(pass, "pass_item", location)) {
                            String pass_item_name = pass.get("pass_item").getAsString();
                            global_pass_item = Registries.ITEM.get(Identifier.of(pass_item_name));
                        }
                        if (ConfigHelper.checkProperty(pass, "pass_name", location)) {
                            global_pass_name = pass.get("pass_name").getAsString();
                        }
                        if (ConfigHelper.checkProperty(pass, "pass_lore", location)) {
                            JsonArray lore_items = pass.getAsJsonArray("pass_lore");
                            List<String> lore = new ArrayList<>();
                            for (JsonElement l : lore_items) {
                                String lore_item = l.getAsString();
                                lore.add(lore_item);
                            }
                            global_pass_lore = lore;
                        }
                        if (ConfigHelper.checkProperty(pass, "pass_data", location, false)) {
                            JsonElement data = pass.getAsJsonObject("pass_data");
                            if (data != null) {
                                global_pass_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                            }
                        }

                        global_pass = new Pass(
                                global_pass_item,
                                global_pass_name,
                                global_pass_lore,
                                global_pass_data
                        );
                    }
                }
            }

            if (ConfigHelper.checkProperty(item_settings, "raid_ball_settings", location)) {
                JsonObject raid_ball_settings = item_settings.getAsJsonObject("raid_ball_settings");
                if (ConfigHelper.checkProperty(raid_ball_settings, "raid_balls_enabled", location)) {
                    raid_balls_enabled = raid_ball_settings.get("raid_balls_enabled").getAsBoolean();
                }
                if (raid_balls_enabled) {
                    if (ConfigHelper.checkProperty(raid_ball_settings, "raid_balls", location)) {
                        JsonObject raid_balls = raid_ball_settings.getAsJsonObject("raid_balls");
                        for (String key : raid_balls.keySet()) {
                            JsonObject ball = raid_balls.getAsJsonObject(key);

                            Item item = CobblemonItems.POKE_BALL;
                            String name = "<red>Raid Pokeball";
                            List<String> lore = new ArrayList<>(List.of("<gray>Use this to try and capture raid bosses!"));
                            ComponentChanges data = ComponentChanges.EMPTY;

                            if (ConfigHelper.checkProperty(ball, "pokeball", location)) {
                                String ball_item_name = ball.get("pokeball").getAsString();
                                item = Registries.ITEM.get(Identifier.of(ball_item_name));
                            }
                            if (ConfigHelper.checkProperty(ball, "pokeball_name", location)) {
                                name = ball.get("pokeball_name").getAsString();
                            }
                            if (ConfigHelper.checkProperty(ball, "pokeball_lore", location)) {
                                JsonArray lore_items = ball.getAsJsonArray("pokeball_lore");
                                List<String> newLore = new ArrayList<>();
                                for (JsonElement l : lore_items) {
                                    String lore_item = l.getAsString();
                                    newLore.add(lore_item);
                                }
                                lore = newLore;
                            }
                            if (ConfigHelper.checkProperty(ball, "pokeball_data", location, false)) {
                                JsonElement dataElement = ball.get("pokeball_data");
                                if (dataElement != null) {
                                    data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, dataElement).getOrThrow().getFirst();
                                }
                            }

                            this.raid_balls.add(new RaidBall(key, item, name, lore, data));
                        }
                    }
                }
            }
        }
    }

    public void writeResults(Raid raid) throws IOException, NoSuchElementException {
        File history_folder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/history/" + raid.raidBossCategory().id()).toFile();
        if (!history_folder.exists()) {
            history_folder.mkdirs();
        }

        File history_file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/history/" + raid.raidBossCategory().id() + "/" + raid.bossInfo().bossId() + ".json").toFile();

        JsonObject root;
        if (history_file.createNewFile()) {
            root = new JsonObject();
        } else {
            root = JsonParser.parseReader(new FileReader(history_file)).getAsJsonObject();
        }

        JsonObject this_raid = new JsonObject();
        this_raid.addProperty("uuid", raid.uuid().toString());
        this_raid.addProperty("length", TextUtils.hms(raid.raidCompletionTime()));
        this_raid.addProperty("had_catch_phase", raid.bossInfo().raidDetails().doCatchPhase());
        this_raid.addProperty("total_players", raid.getDamageLeaderboard().size());

        JsonArray this_raid_leaderboard = new JsonArray();
        int place = 1;
        for (Map.Entry<String, Integer> entry : raid.getDamageLeaderboard()) {
            JsonObject leaderboard_entry = new JsonObject();
            UserCache cache =  nr.server().getUserCache();
            if (cache != null) {
                Optional<GameProfile> profile = cache.findByName(entry.getKey());
                if (profile.isPresent()) {
                    leaderboard_entry.addProperty("player_uuid", profile.get().getId().toString());
                    leaderboard_entry.addProperty("player_name", entry.getKey());
                }
            } else {
                leaderboard_entry.addProperty("player_name", entry.getKey());
            }
            leaderboard_entry.addProperty("damage", entry.getValue());
            leaderboard_entry.addProperty("place", place++);
            this_raid_leaderboard.add(leaderboard_entry);
        }
        this_raid.add("leaderboard", this_raid_leaderboard);

        root.add(LocalDateTime.now(nr.schedulesConfig().zone).toString(), this_raid);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Writer writer = new FileWriter(history_file);
        gson.toJson(root, writer);
        writer.close();
    }

    public RaidBall getRaidBall(String id) {
        for (RaidBall ball : raid_balls) {
            if (ball.id().equals(id)) {
                return ball;
            }
        }
        return null;
    }
}
