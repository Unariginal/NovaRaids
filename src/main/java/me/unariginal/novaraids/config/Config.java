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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.UserCache;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

public class Config {
    private final NovaRaids nr = NovaRaids.INSTANCE;

    public boolean debug = false;

    // Raid Settings
    public boolean use_queue_system = false;
    public boolean run_raids_with_no_players = false;
    public List<Species> global_banned_pokemon = new ArrayList<>();
    public List<Move> global_banned_moves = new ArrayList<>();
    public List<Ability> global_banned_abilities = new ArrayList<>();
    public List<Item> global_banned_held_items = new ArrayList<>();
    public List<Item> global_banned_bag_items = new ArrayList<>();

    // Item Settings
    public boolean vouchers_enabled = true;
    public Voucher default_voucher = new Voucher(
            Items.FEATHER,
            TextUtils.deserialize("<aqua>Raid Voucher"),
            List.of(
                    TextUtils.deserialize("<gray>Use this to start a raid!")
            ),
            ComponentChanges.EMPTY
    );

    public Voucher global_choice_voucher = new Voucher(
            Items.FEATHER,
            TextUtils.deserialize("<aqua>Choice Raid Voucher"),
            List.of(
                    TextUtils.deserialize("<gray>Use this to start any raid!")
            ),
            ComponentChanges.EMPTY
    );

    public Voucher global_random_voucher = new Voucher(
            Items.FEATHER,
            TextUtils.deserialize("<aqua>Random Raid Voucher"),
            List.of(
                    TextUtils.deserialize("<gray>Use this to start a random raid!")
            ),
            ComponentChanges.EMPTY
    );

    public boolean passes_enabled = true;
    public Pass default_pass = new Pass(
            Items.PAPER,
            TextUtils.deserialize("<light_purple>Raid Pass"),
            List.of(
                    TextUtils.deserialize("<gray>Use this to join a raid!")
            ),
            ComponentChanges.EMPTY
    );

    public Pass global_pass = new Pass(
            Items.PAPER,
            TextUtils.deserialize("<light_purple>Global Raid Pass"),
            List.of(
                    TextUtils.deserialize("<gray>Use this to join any raid!")
            ),
            ComponentChanges.EMPTY
    );

    public boolean raid_balls_enabled = true;
    public List<RaidBall> raid_balls = new ArrayList<>();

    public Config() {
        try {
            loadConfig();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            NovaRaids.INSTANCE.loaded_properly = false;
            nr.logError("[RAIDS] Failed to load config file.");
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

        if (checkProperty(config, "debug")) {
            debug = config.get("debug").getAsBoolean();
        }

        if (checkProperty(config, "raid_settings")) {
            JsonObject raid_settings = config.getAsJsonObject("raid_settings");
            if (checkProperty(raid_settings, "use_queue_system")) {
                use_queue_system = raid_settings.get("use_queue_system").getAsBoolean();
            }
            if (checkProperty(raid_settings, "run_raids_with_no_players")) {
                run_raids_with_no_players = raid_settings.get("run_raids_with_no_players").getAsBoolean();
            }
            if (checkProperty(raid_settings, "global_contraband")) {
                JsonObject global_contraband = config.getAsJsonObject("global_contraband");
                if (checkProperty(global_contraband, "banned_pokemon")) {
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
                if (checkProperty(global_contraband, "banned_moves")) {
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
                if (checkProperty(global_contraband, "banned_abilities")) {
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
                if (checkProperty(global_contraband, "banned_held_items")) {
                    JsonArray banned_held_items = global_contraband.getAsJsonArray("banned_held_items");
                    for (JsonElement h : banned_held_items) {
                        String held_item_name = h.getAsString();
                        Item held_item = Registries.ITEM.get(Identifier.of(held_item_name));
                        global_banned_held_items.add(held_item);
                    }
                }
                if (checkProperty(global_contraband, "banned_bag_items")) {
                    JsonArray banned_bag_items = global_contraband.getAsJsonArray("banned_bag_items");
                    for (JsonElement h : banned_bag_items) {
                        String bag_item_name = h.getAsString();
                        Item bag_item = Registries.ITEM.get(Identifier.of(bag_item_name));
                        global_banned_bag_items.add(bag_item);
                    }
                }
            }
        }

        if (checkProperty(config, "item_settings")) {
            JsonObject item_settings = config.getAsJsonObject("item_settings");
            if (checkProperty(item_settings, "voucher_settings")) {
                JsonObject voucher_settings = item_settings.getAsJsonObject("voucher_settings");
                if (checkProperty(voucher_settings, "vouchers_enabled")) {
                    vouchers_enabled = voucher_settings.get("vouchers_enabled").getAsBoolean();
                }
                if (vouchers_enabled) {
                    if (checkProperty(voucher_settings, "default_voucher")) {
                        JsonObject voucher = item_settings.getAsJsonObject("default_voucher");
                        Item default_voucher_item = default_voucher.voucher_item();
                        Text default_voucher_name = default_voucher.voucher_name();
                        List<Text> default_voucher_lore = default_voucher.voucher_lore();
                        ComponentChanges default_voucher_data = default_voucher.voucher_data();

                        if (checkProperty(voucher, "voucher_item")) {
                            String voucher_item_name = voucher.get("voucher_item").getAsString();
                            default_voucher_item = Registries.ITEM.get(Identifier.of(voucher_item_name));
                        }
                        if (checkProperty(voucher, "voucher_name")) {
                            String voucher_name = voucher.get("voucher_name").getAsString();
                            default_voucher_name = TextUtils.deserialize(voucher_name);
                        }
                        if (checkProperty(voucher, "voucher_lore")) {
                            JsonArray lore_items = voucher.getAsJsonArray("voucher_lore");
                            List<Text> lore = new ArrayList<>();
                            for (JsonElement l : lore_items) {
                                String lore_item = l.getAsString();
                                lore.add(TextUtils.deserialize(lore_item));
                            }
                            default_voucher_lore = lore;
                        }
                        if (checkProperty(voucher, "voucher_data")) {
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

                    if (voucher_settings.has("global_choice_voucher")) {
                        JsonObject voucher = item_settings.getAsJsonObject("global_choice_voucher");
                        Item global_choice_voucher_item = global_choice_voucher.voucher_item();
                        Text global_choice_voucher_name = global_choice_voucher.voucher_name();
                        List<Text> global_choice_voucher_lore = global_choice_voucher.voucher_lore();
                        ComponentChanges global_choice_voucher_data = global_choice_voucher.voucher_data();

                        if (checkProperty(voucher, "voucher_item")) {
                            String voucher_item_name = voucher.get("voucher_item").getAsString();
                            global_choice_voucher_item = Registries.ITEM.get(Identifier.of(voucher_item_name));
                        }
                        if (checkProperty(voucher, "voucher_name")) {
                            String voucher_name = voucher.get("voucher_name").getAsString();
                            global_choice_voucher_name = TextUtils.deserialize(voucher_name);
                        }
                        if (checkProperty(voucher, "voucher_lore")) {
                            JsonArray lore_items = voucher.getAsJsonArray("voucher_lore");
                            List<Text> lore = new ArrayList<>();
                            for (JsonElement l : lore_items) {
                                String lore_item = l.getAsString();
                                lore.add(TextUtils.deserialize(lore_item));
                            }
                            global_choice_voucher_lore = lore;
                        }
                        if (checkProperty(voucher, "voucher_data")) {
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

                    if (voucher_settings.has("global_random_voucher")) {
                        JsonObject voucher = item_settings.getAsJsonObject("global_random_voucher");
                        Item global_random_voucher_item = global_random_voucher.voucher_item();
                        Text global_random_voucher_name = global_random_voucher.voucher_name();
                        List<Text> global_random_voucher_lore = global_random_voucher.voucher_lore();
                        ComponentChanges global_random_voucher_data = global_random_voucher.voucher_data();

                        if (checkProperty(voucher, "voucher_item")) {
                            String voucher_item_name = voucher.get("voucher_item").getAsString();
                            global_random_voucher_item = Registries.ITEM.get(Identifier.of(voucher_item_name));
                        }
                        if (checkProperty(voucher, "voucher_name")) {
                            String voucher_name = voucher.get("voucher_name").getAsString();
                            global_random_voucher_name = TextUtils.deserialize(voucher_name);
                        }
                        if (checkProperty(voucher, "voucher_lore")) {
                            JsonArray lore_items = voucher.getAsJsonArray("voucher_lore");
                            List<Text> lore = new ArrayList<>();
                            for (JsonElement l : lore_items) {
                                String lore_item = l.getAsString();
                                lore.add(TextUtils.deserialize(lore_item));
                            }
                            global_random_voucher_lore = lore;
                        }
                        if (checkProperty(voucher, "voucher_data")) {
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

            if (checkProperty(item_settings, "pass_settings")) {
                JsonObject pass_settings = item_settings.getAsJsonObject("pass_settings");
                if (checkProperty(pass_settings, "passes_enabled")) {
                    passes_enabled = pass_settings.get("passes_enabled").getAsBoolean();
                }
                if (passes_enabled) {
                    if (checkProperty(pass_settings, "default_pass")) {
                        JsonObject pass = item_settings.getAsJsonObject("default_pass");
                        Item default_pass_item = default_pass.pass_item();
                        Text default_pass_name = default_pass.pass_name();
                        List<Text> default_pass_lore = default_pass.pass_lore();
                        ComponentChanges default_pass_data = default_pass.pass_data();

                        if (checkProperty(pass, "pass_item")) {
                            String pass_item_name = pass.get("pass_item").getAsString();
                            default_pass_item = Registries.ITEM.get(Identifier.of(pass_item_name));
                        }
                        if (checkProperty(pass, "pass_name")) {
                            String pass_name = pass.get("pass_name").getAsString();
                            default_pass_name = TextUtils.deserialize(pass_name);
                        }
                        if (checkProperty(pass, "pass_lore")) {
                            JsonArray lore_items = pass.getAsJsonArray("pass_lore");
                            List<Text> lore = new ArrayList<>();
                            for (JsonElement l : lore_items) {
                                String lore_item = l.getAsString();
                                lore.add(TextUtils.deserialize(lore_item));
                            }
                            default_pass_lore = lore;
                        }
                        if (checkProperty(pass, "pass_data")) {
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

                    if (pass_settings.has("global_pass")) {
                        JsonObject pass = item_settings.getAsJsonObject("global_pass");
                        Item global_pass_item = global_pass.pass_item();
                        Text global_pass_name = global_pass.pass_name();
                        List<Text> global_pass_lore = global_pass.pass_lore();
                        ComponentChanges global_pass_data = global_pass.pass_data();

                        if (checkProperty(pass, "pass_item")) {
                            String pass_item_name = pass.get("pass_item").getAsString();
                            global_pass_item = Registries.ITEM.get(Identifier.of(pass_item_name));
                        }
                        if (checkProperty(pass, "pass_name")) {
                            String pass_name = pass.get("pass_name").getAsString();
                            global_pass_name = TextUtils.deserialize(pass_name);
                        }
                        if (checkProperty(pass, "pass_lore")) {
                            JsonArray lore_items = pass.getAsJsonArray("pass_lore");
                            List<Text> lore = new ArrayList<>();
                            for (JsonElement l : lore_items) {
                                String lore_item = l.getAsString();
                                lore.add(TextUtils.deserialize(lore_item));
                            }
                            global_pass_lore = lore;
                        }
                        if (checkProperty(pass, "pass_data")) {
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

            if (checkProperty(item_settings, "raid_ball_settings")) {
                JsonObject raid_ball_settings = item_settings.getAsJsonObject("raid_ball_settings");
                if (checkProperty(raid_ball_settings, "raid_balls_enabled")) {
                    raid_balls_enabled = raid_ball_settings.get("raid_balls_enabled").getAsBoolean();
                }
                if (raid_balls_enabled) {
                    if (checkProperty(raid_ball_settings, "raid_balls")) {
                        JsonObject raid_balls = raid_ball_settings.getAsJsonObject("raid_balls");
                        for (String key : raid_balls.keySet()) {
                            JsonObject ball = raid_balls.getAsJsonObject(key);

                            Item item = CobblemonItems.POKE_BALL;
                            Text name = TextUtils.deserialize("<red>Raid Pokeball");
                            List<Text> lore = new ArrayList<>(List.of(TextUtils.deserialize("<gray>Use this to try and capture raid bosses!")));
                            ComponentChanges data = ComponentChanges.EMPTY;

                            if (checkProperty(ball, "pokeball")) {
                                String ball_item_name = ball.get("pokeball").getAsString();
                                item = Registries.ITEM.get(Identifier.of(ball_item_name));
                            }
                            if (checkProperty(ball, "pokeball_name")) {
                                String ball_name = ball.get("pokeball_name").getAsString();
                                name = TextUtils.deserialize(ball_name);
                            }
                            if (checkProperty(ball, "pokeball_lore")) {
                                JsonArray lore_items = ball.getAsJsonArray("pokeball_lore");
                                List<Text> newLore = new ArrayList<>();
                                for (JsonElement l : lore_items) {
                                    String lore_item = l.getAsString();
                                    newLore.add(TextUtils.deserialize(lore_item));
                                }
                                lore = newLore;
                            }
                            if (checkProperty(ball, "pokeball_data")) {
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

    public boolean checkProperty(JsonObject section, String property) {
        if (section.has(property)) {
            return true;
        }
        nr.logError("[RAIDS] Missing " + property + " property in config.json. Using default value(s) or skipping.");
        return false;
    }

    public void writeResults(Raid raid) throws IOException, NoSuchElementException {
        File history_file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/history/" + raid.raidBoss_category().name() + "/" + raid.boss_info().boss_id() + ".json").toFile();
        if (!history_file.exists()) {
            history_file.mkdirs();
        }

        JsonObject root;
        if (history_file.createNewFile()) {
            root = new JsonObject();
        } else {
            root = JsonParser.parseReader(new FileReader(history_file)).getAsJsonObject();
        }

        JsonObject this_raid = new JsonObject();
        this_raid.addProperty("uuid", raid.uuid().toString());
        this_raid.addProperty("length", TextUtils.hms(raid.raid_completion_time()));
        this_raid.addProperty("had_catch_phase", raid.boss_info().raid_details().do_catch_phase());
        this_raid.addProperty("total_players", raid.get_damage_leaderboard().size());

        JsonArray this_raid_leaderboard = new JsonArray();
        int place = 1;
        for (Map.Entry<UUID, Integer> entry : raid.get_damage_leaderboard()) {
            JsonObject leaderboard_entry = new JsonObject();
            leaderboard_entry.addProperty("player_uuid", entry.getKey().toString());
            UserCache cache =  nr.server().getUserCache();
            if (cache != null) {
                leaderboard_entry.addProperty("player_name", cache.getByUuid(entry.getKey()).orElseThrow().getName());
            } else {
                leaderboard_entry.addProperty("player_name", entry.getKey().toString());
            }
            leaderboard_entry.addProperty("damage", entry.getValue());
            leaderboard_entry.addProperty("place", place++);
            this_raid_leaderboard.add(leaderboard_entry);
        }
        this_raid.add("leaderboard", this_raid_leaderboard);

        root.add(LocalDateTime.now(TimeZone.getDefault().toZoneId()).toString(), this_raid);

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
