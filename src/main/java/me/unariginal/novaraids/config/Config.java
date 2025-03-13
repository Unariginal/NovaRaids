package me.unariginal.novaraids.config;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.*;
import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.*;
import me.unariginal.novaraids.data.rewards.*;
import me.unariginal.novaraids.managers.Messages;
import me.unariginal.novaraids.managers.Raid;
import me.unariginal.novaraids.utils.TextUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.UserCache;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class Config {
    private final NovaRaids nr = NovaRaids.INSTANCE;
    private Settings settings;
    private Messages messages;
    private List<Category> categories;
    private List<Boss> bosses;
    private List<Location> locations;
    private List<BossbarData> bossbars;
    private List<Reward> rewards;
    private List<RewardPool> reward_pools;

    public Config() {
        try {
            checkFiles();
        } catch (IOException e) {
            nr.logError("[RAIDS] Failed to generate default configuration files. Error: " + e.getMessage());
        }

        try {
            loadConfig();
            loadMessages();
            loadBossBars();
            loadLocations();
            loadRewards();
            loadRewardPools();
            loadCategories();
            loadBosses();
        } catch (NullPointerException e) {
            nr.logError("[RAIDS] Failed to load config files. Error: " + e.getMessage());
        } catch (UnsupportedOperationException u) {
            nr.logError("[RAIDS] Failed to parse json element. Error: " + u.getMessage());
        }
    }

    private void checkFiles() throws IOException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) {
            if (!rootFolder.mkdirs()) {
                return;
            }
        }

        File bossesFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/bosses").toFile();
        if (!bossesFolder.exists()) {
            if (!bossesFolder.mkdirs()) {
                return;
            }
        }

        File historyFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/history").toFile();
        if (!historyFolder.exists()) {
            if (!historyFolder.mkdirs()) {
                return;
            }
        }

        String[] fileNames = {
                "config",
                "locations",
                "messages",
                "bossbars",
                "categories",
                "rewards",
                "reward_pools"
        };

        for (String fileName : fileNames) {
            File file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/" + fileName + ".json").toFile();
            if (file.createNewFile()) {
                InputStream stream = NovaRaids.class.getResourceAsStream("/nr_config_files/" + fileName + ".json");
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
        }

        if (bossesFolder.listFiles() == null || Objects.requireNonNull(bossesFolder.listFiles()).length == 0) {
            File boss_example = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/bosses/example_boss.json").toFile();
            if (boss_example.createNewFile()) {
                InputStream stream = NovaRaids.class.getResourceAsStream("/nr_config_files/bosses/example_boss.json");
                assert stream != null;
                OutputStream out = new FileOutputStream(boss_example);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = stream.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                stream.close();
                out.close();
            }
        }
    }

    public void writeResults(Raid raid) throws IOException, NoSuchElementException {
        File history_file = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/history/" + raid.boss_info().name() + ".json").toFile();

        JsonObject root;
        if (history_file.createNewFile()) {
            root = new JsonObject();
        } else {
            root = getRoot(history_file).getAsJsonObject();
        }

        JsonObject this_raid = new JsonObject();
        this_raid.addProperty("uuid", raid.uuid().toString());
        this_raid.addProperty("length", TextUtil.hms(raid.raid_completion_time()));
        this_raid.addProperty("had_catch_phase", raid.boss_info().do_catch_phase());
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

    private JsonElement getRoot(File file) {
        try {
            return JsonParser.parseReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            nr.logError("[RAIDS] Failed to load file " + file.getName() + " at root object.");
            throw new NullPointerException();
        }
    }

    private JsonElement getSafe(JsonObject obj, String key, String type, String context) {
        if (obj.get(key) != null) {
            return obj.get(key);
        } else {
            nr.logError("[RAIDS] Failed to find " + type + " '" + key + "' at " + context + ".");
            throw new NullPointerException();
        }
    }

    private void loadConfig() throws NullPointerException, UnsupportedOperationException {
        File configFile = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/config.json").toFile();

        JsonElement root = getRoot(configFile);
        assert root != null;
        JsonObject config = root.getAsJsonObject();
        nr.logInfo("[RAIDS] Loading config..");

        nr.debug = getSafe(config, "debug", "boolean", "config.json").getAsBoolean();

        TimeZone timezone = TimeZone.getTimeZone(getSafe(config, "timezone", "String", "config.json").getAsString().toUpperCase());
        TimeZone.setDefault(timezone);

        JsonObject settingsObject = getSafe(config, "raid_settings", "Json Object", "config.json").getAsJsonObject();
        int raid_radius = getSafe(settingsObject, "raid_radius", "Integer", "config.json/raid_settings").getAsInt();
        int raid_pushback_radius = getSafe(settingsObject, "raid_pushback_radius", "Integer", "config.json/raid_settings").getAsInt();
        boolean bosses_glow = getSafe(settingsObject, "bosses_glow", "Boolean", "config.json/raid_settings").getAsBoolean();
        String raidHookURL = getSafe(settingsObject, "raidHookURL", "String", "config.json/raid_settings").getAsString();
        String raidTtitle = getSafe(settingsObject, "raidTtitle", "String", "config.json/raid_settings").getAsString();
        String avatarUrl = getSafe(settingsObject, "avatarUrl", "String", "config.json/raid_settings").getAsString();
        String raidHookName = getSafe(settingsObject, "raidHookName", "String", "config.json/raid_settings").getAsString();
        boolean heal_party_on_challenge = getSafe(settingsObject, "heal_party_on_challenge", "Boolean", "config.json/raid_settings").getAsBoolean();
        boolean use_queue_system = getSafe(settingsObject, "use_queue_system", "Boolean", "config.json/raid_settings").getAsBoolean();
        int setup_phase_time = getSafe(settingsObject, "setup_phase_time", "Boolean", "config.json/raid_settings").getAsInt();
        int fight_phase_time = getSafe(settingsObject, "fight_phase_time", "Integer", "config.json/raid_settings").getAsInt();
        int pre_catch_phase_time = getSafe(settingsObject, "pre_catch_phase_time", "Integer", "config.json/raid_settings").getAsInt();
        int catch_phase_time = getSafe(settingsObject, "catch_phase_time", "Integer", "config.json/raid_settings").getAsInt();

        JsonObject banned_section = getSafe(settingsObject, "banned_section", "Json Object", "config.json/raid_settings").getAsJsonObject();
        JsonArray banned_pokemon = getSafe(banned_section, "banned_pokemon", "Json String Array", "config.json/raid_settings/banned_section").getAsJsonArray();
        JsonArray banned_moves = getSafe(banned_section, "banned_moves", "Json String Array", "config.json/raid_settings/banned_section").getAsJsonArray();
        JsonArray banned_abilities = getSafe(banned_section, "banned_abilities", "Json String Array", "config.json/raid_settings/banned_section").getAsJsonArray();
        JsonArray banned_held_items = getSafe(banned_section, "banned_held_items", "Json String Array", "config.json/raid_settings/banned_section").getAsJsonArray();
        JsonArray banned_bag_items = getSafe(banned_section, "banned_bag_items", "Json String Array", "config.json/raid_settings/banned_section").getAsJsonArray();

        List<Species> banned_pokemon_list = new ArrayList<>();
        for (JsonElement element : banned_pokemon) {
            String speciesString = element.getAsString();
            Species species = PokemonSpecies.INSTANCE.getByName(speciesString);
            if (species != null) {
                banned_pokemon_list.add(species);
            }
        }

        List<Move> banned_move_list = new ArrayList<>();
        for (JsonElement element : banned_moves) {
            String moveString = element.getAsString();
            MoveTemplate moveTemplate = Moves.INSTANCE.getByName(moveString);
            if (moveTemplate != null) {
                banned_move_list.add(moveTemplate.create());
            }
        }

        List<Ability> banned_abilities_list = new ArrayList<>();
        for (JsonElement element : banned_abilities) {
            String abilityString = element.getAsString();
            AbilityTemplate ability = Abilities.INSTANCE.get(abilityString);
            if (ability != null) {
                banned_abilities_list.add(ability.create(false, Priority.LOWEST));
            }
        }

        List<Item> banned_held_item_list = new ArrayList<>();
        for (JsonElement element : banned_held_items) {
            String itemString = element.getAsString();
            Item item = Registries.ITEM.get(Identifier.of(itemString));
            banned_held_item_list.add(item);
        }

        List<Item> banned_bag_item_list = new ArrayList<>();
        for (JsonElement element : banned_bag_items) {
            String itemString = element.getAsString();
            Item item = Registries.ITEM.get(Identifier.of(itemString));
            banned_bag_item_list.add(item);
        }

        JsonObject items = getSafe(settingsObject, "items", "Json Object", "config.json/raid_settings").getAsJsonObject();
        Item voucher_item = Registries.ITEM.get(Identifier.of(getSafe(items, "voucher_item", "String", "config.json/raid_settings/items").getAsString()));
        ComponentChanges voucher_item_data = null;
        if (items.get("voucher_item_data") != null) {
            DataResult<Pair<ComponentChanges, JsonElement>> data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, items.get("voucher_item_data"));
            voucher_item_data = data.getOrThrow().getFirst();
        }
        Item pass_item = Registries.ITEM.get(Identifier.of(getSafe(items, "pass_item", "String", "config.json/raid_settings/items").getAsString()));
        ComponentChanges pass_item_data = null;
        if (items.get("pass_item_data") != null) {
            DataResult<Pair<ComponentChanges, JsonElement>> data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, items.get("pass_item_data"));
            pass_item_data = data.getOrThrow().getFirst();
        }

        boolean use_raid_pokeballs = getSafe(items, "use_raid_pokeballs", "Boolean", "config.json/raid_settings/items").getAsBoolean();
        Map<String, Item> pokeball_items = new HashMap<>();
        Map<String, ComponentChanges> pokeball_item_data = new HashMap<>();
        if (use_raid_pokeballs) {
            JsonObject balls = getSafe(items, "raid_pokeballs", "Json Object", "config.json/raid_settings/items").getAsJsonObject();
            for (String key : balls.keySet()) {
                JsonObject ball = getSafe(balls, key, "Json Object", "config.json/raid_settings/items/raid_pokeballs").getAsJsonObject();
                Item ball_item = Registries.ITEM.get(Identifier.of(getSafe(ball, "pokeball", "String", "config.json/raid_settings/items/raid_pokeballs/" + key).getAsString()));
                ComponentChanges ball_data = null;
                if (ball.get("data") != null) {
                    ball_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, ball.get("data")).getOrThrow().getFirst();
                }
                pokeball_items.put(key, ball_item);
                pokeball_item_data.put(key, ball_data);
            }
        }

        this.settings = new Settings(
                raid_radius,
                raid_pushback_radius,
                bosses_glow,
                heal_party_on_challenge,
                use_queue_system,
                setup_phase_time,
                fight_phase_time,
                pre_catch_phase_time,
                catch_phase_time,
                banned_pokemon_list,
                banned_move_list,
                banned_abilities_list,
                banned_held_item_list,
                banned_bag_item_list,
                voucher_item,
                voucher_item_data,
                pass_item,
                pass_item_data,
                use_raid_pokeballs,
                pokeball_items,
                pokeball_item_data
        );
    }

    private void loadCategories() throws NullPointerException, UnsupportedOperationException {
        File categoriesFile = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/categories.json").toFile();

        JsonElement root = getRoot(categoriesFile);
        assert root != null;
        JsonObject categories = root.getAsJsonObject();
        nr.logInfo("[RAIDS] Loading categories...");

        List<Category> categoriesList = new ArrayList<>();
        for (String category : categories.keySet()) {
            JsonObject categoryObject = getSafe(categories, category, "Json Object", "categories.json").getAsJsonObject();

            boolean require_pass = getSafe(categoryObject, "require_pass", "Boolean", "categories.json/" + category).getAsBoolean();

            JsonObject player_count = getSafe(categoryObject, "player_count", "Json Object", "categories.json/" + category).getAsJsonObject();
            int min_players = getSafe(player_count, "min", "Integer", "categories.json/" + category + "/player_count").getAsInt();
            int max_players = getSafe(player_count, "max", "Integer", "categories.json/" + category + "/player_count").getAsInt();

            JsonObject random_wait_time = getSafe(categoryObject, "random_wait_time", "Json Object", "categories.json/" + category).getAsJsonObject();
            int min_wait_time = getSafe(random_wait_time, "min", "Integer", "categories.json/" + category + "/random_wait_time").getAsInt();
            int max_wait_time = getSafe(random_wait_time, "max", "Integer", "categories.json/" + category + "/random_wait_time").getAsInt();

            JsonArray set_times = getSafe(categoryObject, "set_times", "Json String Array", "categories.json/" + category).getAsJsonArray();
            List<LocalTime> set_times_list = new ArrayList<>();
            for (JsonElement element : set_times) {
                String time = element.getAsString();
                set_times_list.add(LocalTime.parse(time));
            }

            JsonArray rewards = getSafe(categoryObject, "rewards", "Json Object Array", "categories.json/" + category).getAsJsonArray();
            List<DistributionSection> rewards_list = new ArrayList<>();
            for (JsonElement element : rewards) {
                JsonObject sectionObj = element.getAsJsonObject();
                List<Place> places = new ArrayList<>();
                JsonArray placesArr = getSafe(sectionObj, "places", "Json Object Array", "categories.json/" + category + "/rewards").getAsJsonArray();
                for (JsonElement placeElement : placesArr) {
                    JsonObject placeObj = placeElement.getAsJsonObject();
                    places.add(new Place(getSafe(placeObj, "place", "String", "categories.json/" + category + "/rewards/places").getAsString(), placeObj.get("allow_other_rewards").getAsBoolean()));
                }
                List<RewardPool> pools = new ArrayList<>();
                JsonArray poolsArr = getSafe(sectionObj, "reward_pools", "Json String Array", "categories.json/" + category + "/rewards").getAsJsonArray();
                for (JsonElement poolElement : poolsArr) {
                    String poolObj = poolElement.getAsString();
                    for (RewardPool rewardPool : reward_pools) {
                        if (rewardPool.name().equals(poolObj)) {
                            pools.add(rewardPool);
                        }
                    }
                }
                rewards_list.add(new DistributionSection(places, pools));
            }

            categoriesList.add(new Category(category, require_pass, min_players, max_players, min_wait_time, max_wait_time, set_times_list, rewards_list));
        }
        this.categories = categoriesList;
    }

    private void loadBosses() throws NullPointerException, UnsupportedOperationException {
        File bossesFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/bosses").toFile();
        if (!bossesFolder.exists()) {
            if (!bossesFolder.mkdirs()) {
                return;
            }
        }

        List<Boss> bossesList = new ArrayList<>();
        for (File bossFile : Objects.requireNonNull(bossesFolder.listFiles())) {
            if (bossFile.getName().endsWith(".json")) {
                JsonElement root = getRoot(bossFile);
                assert root != null;
                JsonObject bossObject = root.getAsJsonObject();
                JsonObject pokemon_details = getSafe(bossObject, "pokemon_details", "Json Object", bossFile.getName()).getAsJsonObject();
                Species species = PokemonSpecies.INSTANCE.getByName(getSafe(pokemon_details, "species", "String", bossFile.getName() + "/pokemon_details").getAsString());
                if (species != null) {
                    int level = getSafe(pokemon_details, "level", "Integer", bossFile.getName() + "/pokemon_details").getAsInt();

                    Map<Ability, Double> possible_abilities = new HashMap<>();
                    JsonArray abilities = getSafe(pokemon_details, "ability", "Json Object Array", bossFile.getName() + "/pokemon_details").getAsJsonArray();
                    for (JsonElement element : abilities) {
                        JsonObject abilityObj = element.getAsJsonObject();
                        String ability = getSafe(abilityObj, "ability", "String", bossFile.getName() + "/pokemon_details/ability").getAsString();
                        AbilityTemplate abilityTemplate = Abilities.INSTANCE.get(ability);
                        if (abilityTemplate != null) {
                            possible_abilities.put(abilityTemplate.create(false, Priority.LOWEST), getSafe(abilityObj, "weight", "Double", bossFile.getName() + "/pokemon_details/ability").getAsDouble());
                        }
                    }

                    Map<Nature, Double> possible_natures = new HashMap<>();
                    JsonArray natures = getSafe(pokemon_details, "nature", "Json Object Array", bossFile.getName() + "/pokemon_details").getAsJsonArray();
                    for (JsonElement element : natures) {
                        JsonObject natureObj = element.getAsJsonObject();
                        String nature_str = getSafe(natureObj, "nature", "String", bossFile.getName() + "/pokemon_details/nature").getAsString();
                        Nature nature = Natures.INSTANCE.getNature(nature_str);
                        if (nature != null) {
                            possible_natures.put(nature, getSafe(natureObj, "weight", "Double", bossFile.getName() + "/pokemon_details/nature").getAsDouble());
                        }
                    }

                    String form_str = getSafe(pokemon_details, "form", "String", bossFile.getName() + "/pokemon_details").getAsString();
                    FormData form = species.getFormByName(form_str);

                    String features = getSafe(pokemon_details, "features", "String", bossFile.getName() + "/pokemon_details").getAsString();

                    Map<Gender, Double> possible_genders = new HashMap<>();
                    JsonArray genders = getSafe(pokemon_details, "gender", "Json Object Array", bossFile.getName() + "/pokemon_details").getAsJsonArray();
                    for (JsonElement element : genders) {
                        JsonObject genderObj = element.getAsJsonObject();
                        String gender_str = getSafe(genderObj, "gender", "String", bossFile.getName() + "/pokemon_details/gender").getAsString();
                        Gender gender = Gender.valueOf(gender_str.toUpperCase());
                        possible_genders.put(gender, getSafe(genderObj, "weight", "Double", bossFile.getName() + "/pokemon_details/gender").getAsDouble());
                    }

                    boolean shiny = getSafe(pokemon_details, "shiny", "Boolean", bossFile.getName() + "/pokemon_details").getAsBoolean();
                    float scale = getSafe(pokemon_details, "scale", "Float", bossFile.getName() + "/pokemon_details").getAsFloat();

                    String held_item_string = getSafe(pokemon_details, "held_item", "String", bossFile.getName() + "/pokemon_details").getAsString();
                    Item held_item = null;
                    if (!held_item_string.isEmpty()) {
                        held_item = Registries.ITEM.get(Identifier.of(held_item_string));
                    }

                    JsonElement held_item_data = null;
                    if (pokemon_details.get("held_item_data") != null) {
                        held_item_data = pokemon_details.get("held_item_data");
                    }

                    MoveSet moves = new MoveSet();
                    JsonArray moves_list = getSafe(pokemon_details, "moves", "Json String Array", bossFile.getName() + "/pokemon_details").getAsJsonArray();
                    for (int i = 0; (i < moves_list.size() && i < 4); i++) {
                        MoveTemplate moveTemplate = Moves.INSTANCE.getByName(moves_list.get(i).getAsString());
                        if (moveTemplate != null) {
                            moves.setMove(i, moveTemplate.create());
                        }
                    }

                    JsonObject ivObject = getSafe(pokemon_details, "ivs", "Json Object", bossFile.getName() + "/pokemon_details").getAsJsonObject();
                    IVs ivs = new IVs();
                    ivs.set(Stats.HP, getSafe(ivObject, "hp", "Integer", bossFile.getName() + "/pokemon_details/ivs").getAsInt());
                    ivs.set(Stats.ATTACK, getSafe(ivObject, "atk", "Integer", bossFile.getName() + "/pokemon_details/ivs").getAsInt());
                    ivs.set(Stats.DEFENCE, getSafe(ivObject, "def", "Integer", bossFile.getName() + "/pokemon_details/ivs").getAsInt());
                    ivs.set(Stats.SPECIAL_ATTACK, getSafe(ivObject, "sp_atk", "Integer", bossFile.getName() + "/pokemon_details/ivs").getAsInt());
                    ivs.set(Stats.SPECIAL_DEFENCE, getSafe(ivObject, "sp_def", "Integer", bossFile.getName() + "/pokemon_details/ivs").getAsInt());
                    ivs.set(Stats.SPEED, getSafe(ivObject, "spd", "Integer", bossFile.getName() + "/pokemon_details/ivs").getAsInt());

                    JsonObject evObject = getSafe(pokemon_details, "evs", "Json Object", bossFile.getName() + "/pokemon_details").getAsJsonObject();
                    EVs evs = new EVs();
                    evs.set(Stats.HP, getSafe(evObject, "hp", "Integer", bossFile.getName() + "/pokemon_details/evs").getAsInt());
                    evs.set(Stats.ATTACK, getSafe(evObject, "atk", "Integer", bossFile.getName() + "/pokemon_details/evs").getAsInt());
                    evs.set(Stats.DEFENCE, getSafe(evObject, "def", "Integer", bossFile.getName() + "/pokemon_details/evs").getAsInt());
                    evs.set(Stats.SPECIAL_ATTACK, getSafe(evObject, "sp_atk", "Integer", bossFile.getName() + "/pokemon_details/evs").getAsInt());
                    evs.set(Stats.SPECIAL_DEFENCE, getSafe(evObject, "sp_def", "Integer", bossFile.getName() + "/pokemon_details/evs").getAsInt());
                    evs.set(Stats.SPEED, getSafe(evObject, "spd", "Integer", bossFile.getName() + "/pokemon_details/evs").getAsInt());

                    JsonObject boss_details = getSafe(bossObject, "boss_details", "Json Object", bossFile.getName()).getAsJsonObject();
                    String display_form = getSafe(boss_details, "display_form", "String", bossFile.getName() + "/boss_details").getAsString();
                    int base_health = getSafe(boss_details, "base_health", "Integer", bossFile.getName() + "/boss_details").getAsInt();
                    int health_increase_per_player = getSafe(boss_details, "health_increase_per_player", "Integer", bossFile.getName() + "/boss_details").getAsInt();
                    String category = getSafe(boss_details, "category", "String", bossFile.getName() + "/boss_details").getAsString();
                    double random_weight = getSafe(boss_details, "random_weight", "Double", bossFile.getName() + "/boss_details").getAsDouble();
                    Float facing = getSafe(boss_details, "body_direction", "Float", bossFile.getName() + "/boss_details").getAsFloat();
                    boolean do_catch_phase = getSafe(boss_details, "do_catch_phase", "Boolean", bossFile.getName() + "/boss_details").getAsBoolean();

                    Map<String, Double> spawn_locations = new HashMap<>();
                    JsonArray locations = getSafe(boss_details, "locations", "Json Object Array", bossFile.getName() + "/boss_details").getAsJsonArray();
                    for (JsonElement element : locations) {
                        JsonObject locationObject = element.getAsJsonObject();
                        String location = getSafe(locationObject, "location", "String", bossFile.getName() + "/boss_details/locations").getAsString();
                        double weight = getSafe(locationObject, "weight", "Double", bossFile.getName() + "/boss_details/locations").getAsDouble();
                        spawn_locations.put(location, weight);
                    }

                    JsonArray rewards_override = getSafe(boss_details, "rewards_override", "Json Object Array", bossFile.getName() + "/boss_details").getAsJsonArray();
                    List<DistributionSection> rewards_list = new ArrayList<>();
                    for (JsonElement element : rewards_override) {
                        JsonObject sectionObj = element.getAsJsonObject();
                        List<Place> places = new ArrayList<>();
                        JsonArray placesArr = getSafe(sectionObj, "places", "Json Object Array", bossFile.getName() + "/boss_details/rewards_override").getAsJsonArray();
                        for (JsonElement placeElement : placesArr) {
                            JsonObject placeObj = placeElement.getAsJsonObject();
                            places.add(new Place(getSafe(placeObj, "place", "String", bossFile.getName() + "/boss_details/rewards_override/places").getAsString(), getSafe(placeObj, "allow_other_rewards", "Boolean", bossFile.getName() + "/boss_details/rewards_override/places").getAsBoolean()));
                        }
                        List<RewardPool> pools = new ArrayList<>();
                        JsonArray poolsArr = getSafe(sectionObj, "reward_pools", "Json String Array", bossFile.getName() + "/boss_details/rewards_override").getAsJsonArray();
                        for (JsonElement poolElement : poolsArr) {
                            String poolObj = poolElement.getAsString();
                            for (RewardPool rewardPool : reward_pools) {
                                if (rewardPool.name().equals(poolObj)) {
                                    pools.add(rewardPool);
                                }
                            }
                        }
                        rewards_list.add(new DistributionSection(places, pools));
                    }

                    JsonObject catch_settings = getSafe(bossObject, "catch_settings", "Json Object", bossFile.getName()).getAsJsonObject();
                    FormData form_override = null;
                    if (!getSafe(catch_settings, "form_override", "String", bossFile.getName() + "/catch_settings").getAsString().isEmpty()) {
                        form_override = species.getFormByName(catch_settings.get("form_override").getAsString());
                    }
                    String features_override = getSafe(catch_settings, "features_override", "String", bossFile.getName() + "/catch_settings").getAsString();
                    boolean keep_scale = getSafe(catch_settings, "keep_scale", "Boolean", bossFile.getName() + "/catch_settings").getAsBoolean();
                    boolean keep_held_item = getSafe(catch_settings, "keep_held_item", "Boolean", bossFile.getName() + "/catch_settings").getAsBoolean();
                    boolean randomize_ivs = getSafe(catch_settings, "randomize_ivs", "Boolean", bossFile.getName() + "/catch_settings").getAsBoolean();
                    int min_ivs = getSafe(catch_settings, "min_perfect_ivs", "Integer", bossFile.getName() + "/catch_settings").getAsInt();
                    boolean keep_evs = getSafe(catch_settings, "keep_evs", "Boolean", bossFile.getName() + "/catch_settings").getAsBoolean();
                    boolean randomize_gender = getSafe(catch_settings, "randomize_gender", "Boolean", bossFile.getName() + "/catch_settings").getAsBoolean();
                    boolean randomize_nature = getSafe(catch_settings, "randomize_nature", "Boolean", bossFile.getName() + "/catch_settings").getAsBoolean();
                    boolean randomize_ability = getSafe(catch_settings, "randomize_ability", "Boolean", bossFile.getName() + "/catch_settings").getAsBoolean();
                    boolean reset_moves = getSafe(catch_settings, "reset_moves", "Boolean", bossFile.getName() + "/catch_settings").getAsBoolean();
                    int level_override = getSafe(catch_settings, "level_override", "Integer", bossFile.getName() + "/catch_settings").getAsInt();
                    int shiny_chance = getSafe(catch_settings, "shiny_chance", "Integer", bossFile.getName() + "/catch_settings").getAsInt();

                    CatchSettings catchSettings = new CatchSettings(form_override, features_override, keep_scale, keep_held_item, randomize_ivs, min_ivs, keep_evs, randomize_gender, randomize_nature, randomize_ability, reset_moves, level_override, shiny_chance);

                    bossesList.add(new Boss(
                            bossFile.getName().replace(".json", ""),
                            species,
                            level,
                            form,
                            features,
                            possible_abilities,
                            possible_natures,
                            possible_genders,
                            shiny,
                            scale,
                            held_item,
                            held_item_data,
                            moves,
                            ivs,
                            evs,
                            display_form,
                            base_health,
                            health_increase_per_player,
                            category,
                            random_weight,
                            facing,
                            do_catch_phase,
                            spawn_locations,
                            rewards_list,
                            catchSettings)
                    );
                } else {
                    nr.logError("[RAIDS] Invalid Boss Species: " + pokemon_details.get("species").getAsString());
                }
                this.bosses = bossesList;
                nr.logInfo("[RAIDS] Loaded " + bosses.size() + " bosses.");
            }
        }
    }

    private void loadLocations() throws NullPointerException, UnsupportedOperationException {
        File locationsFile = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/locations.json").toFile();

        JsonElement root = getRoot(locationsFile);
        assert root != null;

        JsonObject locationsObject = root.getAsJsonObject();
        List<Location> locationsList = new ArrayList<>();
        for (String location : locationsObject.keySet()) {
            JsonObject locationObject = getSafe(locationsObject, location, "Json Object", "locations.json").getAsJsonObject();

            double x = getSafe(locationObject, "x_pos", "Double", "locations.json/" + location).getAsDouble();
            double y = getSafe(locationObject, "y_pos", "Double", "locations.json/" + location).getAsDouble();
            double z = getSafe(locationObject, "z_pos", "Double", "locations.json/" + location).getAsDouble();
            Vec3d pos = new Vec3d(x, y, z);

            String world_path = getSafe(locationObject, "world", "String", "locations.json/" + location).getAsString();
            ServerWorld world = nr.server().getOverworld();
            for (ServerWorld world_loop : nr.server().getWorlds()) {
                if ((world_loop.getRegistryKey().getValue().getNamespace() + ":" + world_loop.getRegistryKey().getValue().getPath()).equalsIgnoreCase(world_path)) {
                    world = world_loop;
                    break;
                }
            }

            Location loc = new Location(location, pos, world);
            locationsList.add(loc);
        }
        this.locations = locationsList;
        nr.logInfo("[RAIDS] Loaded " + locations.size() + " locations");
    }

    private void loadBossBars() throws NullPointerException, UnsupportedOperationException {
        File bossbarsFile = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/bossbars.json").toFile();

        JsonElement root = getRoot(bossbarsFile);
        assert root != null;

        JsonObject bossbarsObject = root.getAsJsonObject();
        List<BossbarData> bossbarDataList = new ArrayList<>();
        for (String bossbar : bossbarsObject.keySet()) {
            JsonObject bossbarObject = getSafe(bossbarsObject, bossbar, "Json Object", "bossbars.json").getAsJsonObject();
            String phase = getSafe(bossbarObject, "phase", "String", "bossbars.json/" + bossbar).getAsString();
            boolean use_overlay = getSafe(bossbarObject, "use_overlay", "Boolean", "bossbars.json/" + bossbar).getAsBoolean();
            String overlay_text = getSafe(bossbarObject, "overlay_text", "String", "bossbars.json/" + bossbar).getAsString();
            String bar_color = getSafe(bossbarObject, "bar_color", "String", "bossbars.json/" + bossbar).getAsString();
            String bar_style = getSafe(bossbarObject, "bar_style", "String", "bossbars.json/" + bossbar).getAsString();
            String bar_text = getSafe(bossbarObject, "bar_text", "String", "bossbars.json/" + bossbar).getAsString();
            List<String> bosses = getSafe(bossbarObject, "bosses", "Json String Array", "bossbars.json/" + bossbar).getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
            List<String> categories = getSafe(bossbarObject, "categories", "Json String Array", "bossbars.json/" + bossbar).getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
            bossbarDataList.add(new BossbarData(bossbar, phase, use_overlay, overlay_text, bar_color, bar_style, bar_text, bosses, categories));
        }
        this.bossbars = bossbarDataList;
        nr.logInfo("[RAIDS] Loaded " + bossbars.size() + " bossbars");
    }

    private void loadMessages() throws NullPointerException, UnsupportedOperationException {
        File messagesFile = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/messages.json").toFile();
        JsonElement root = getRoot(messagesFile);
        assert root != null;
        JsonObject messagesObject = root.getAsJsonObject();
        Map<String, String> messages_map = new HashMap<>();
        String prefix = getSafe(messagesObject, "prefix", "String", "messages.json").getAsString();
        String command = getSafe(messagesObject, "raid_start_command", "String", "messages.json").getAsString();
        JsonObject messages = getSafe(messagesObject, "messages", "Json Object", "messages.json").getAsJsonObject();
        for (String key : messages.keySet()) {
            messages_map.put(key, messages.get(key).getAsString());
        }
        this.messages = new Messages(prefix, command, messages_map);
        nr.logInfo("[RAIDS] Loaded messages.");
    }

    private void loadRewards() throws NullPointerException, UnsupportedOperationException {
        File rewardsFile = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/rewards.json").toFile();
        JsonElement root = getRoot(rewardsFile);
        assert root != null;
        JsonObject rewardsObject = root.getAsJsonObject();
        List<Reward> rewardsList = new ArrayList<>();
        for (String key : rewardsObject.keySet()) {
            JsonObject rewardObject = getSafe(rewardsObject, key, "Json Object", "rewards.json").getAsJsonObject();
            String type = getSafe(rewardObject, "type", "String", "rewards.json/" + key).getAsString();
            Reward reward = null;
            if (type.equalsIgnoreCase("item")) {
                nr.logInfo("[RAIDS] Loading new item reward.");
                String item = getSafe(rewardObject, "item", "String", "rewards.json/" + key).getAsString();
                JsonElement data = null;
                if (rewardObject.get("data") != null) {
                    data = rewardObject.get("data");
                }
                JsonObject count = getSafe(rewardObject, "count", "Json Object", "rewards.json/" + key).getAsJsonObject();
                int min_count = getSafe(count, "min", "String", "rewards.json/" + key + "/count").getAsInt();
                int max_count = getSafe(count, "max", "String", "rewards.json/" + key + "/count").getAsInt();
                reward = new ItemReward(key, item, data, min_count, max_count);
            } else if (type.equalsIgnoreCase("command")) {
                nr.logInfo("[RAIDS] Loading new command reward.");
                List<String> commands = getSafe(rewardObject, "commands", "Json String Array", "rewards.json/" + key).getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
                reward = new CommandReward(key, commands);
            } else if (type.equalsIgnoreCase("pokemon")) {
                nr.logInfo("[RAIDS] Loading new pokemon reward.");
                JsonObject pokemon_info = getSafe(rewardObject, "pokemon", "Json Object", "rewards.json/" + key).getAsJsonObject();
                String species = getSafe(pokemon_info, "species", "String", "rewards.json/" + key + "/pokemon").getAsString();
                int level = getSafe(pokemon_info, "level", "Integer", "rewards.json/" + key + "/pokemon").getAsInt();
                String ability = getSafe(pokemon_info, "ability", "String", "rewards.json/" + key + "/pokemon").getAsString();
                String nature = getSafe(pokemon_info, "nature", "String", "rewards.json/" + key + "/pokemon").getAsString();
                String form = getSafe(pokemon_info, "form", "String", "rewards.json/" + key + "/pokemon").getAsString();
                String features = getSafe(pokemon_info, "features", "String", "rewards.json/" + key + "/pokemon").getAsString();
                String gender = getSafe(pokemon_info, "gender", "String", "rewards.json/" + key + "/pokemon").getAsString();
                boolean shiny = getSafe(pokemon_info, "shiny", "Boolean", "rewards.json/" + key + "/pokemon").getAsBoolean();
                float scale = getSafe(pokemon_info, "scale", "Float", "rewards.json/" + key + "/pokemon").getAsFloat();
                String held_item = getSafe(pokemon_info, "held_item", "String", "rewards.json/" + key + "/pokemon").getAsString();
                JsonElement held_item_data = null;
                if (pokemon_info.get("held_item_data") != null) {
                    held_item_data = pokemon_info.get("held_item_data");
                }
                List<String> move_set = getSafe(pokemon_info, "moves", "Json String Array", "rewards.json/" + key + "/pokemon").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
                JsonObject ivObject = getSafe(pokemon_info, "ivs", "Json Object", "rewards.json/" + key + "/pokemon").getAsJsonObject();
                IVs ivs = new IVs();
                ivs.set(Stats.HP, getSafe(ivObject, "hp", "Integer", "rewards.json/" + key + "/pokemon/ivs").getAsInt());
                ivs.set(Stats.ATTACK, getSafe(ivObject, "atk", "Integer", "rewards.json/" + key + "/pokemon/ivs").getAsInt());
                ivs.set(Stats.DEFENCE, getSafe(ivObject, "def", "Integer", "rewards.json/" + key + "/pokemon/ivs").getAsInt());
                ivs.set(Stats.SPECIAL_ATTACK, getSafe(ivObject, "sp_atk", "Integer", "rewards.json/" + key + "/pokemon/ivs").getAsInt());
                ivs.set(Stats.SPECIAL_DEFENCE, getSafe(ivObject, "sp_def", "Integer", "rewards.json/" + key + "/pokemon/ivs").getAsInt());
                ivs.set(Stats.SPEED, getSafe(ivObject, "spd", "Integer", "rewards.json/" + key + "/pokemon/ivs").getAsInt());

                JsonObject evObject = getSafe(pokemon_info, "evs", "Json Object", "rewards.json/" + key + "/pokemon").getAsJsonObject();
                EVs evs = new EVs();
                evs.set(Stats.HP, getSafe(evObject, "hp", "Integer", "rewards.json/" + key + "/pokemon/evs").getAsInt());
                evs.set(Stats.ATTACK, getSafe(evObject, "atk", "Integer", "rewards.json/" + key + "/pokemon/evs").getAsInt());
                evs.set(Stats.DEFENCE, getSafe(evObject, "def", "Integer", "rewards.json/" + key + "/pokemon/evs").getAsInt());
                evs.set(Stats.SPECIAL_ATTACK, getSafe(evObject, "sp_atk", "Integer", "rewards.json/" + key + "/pokemon/evs").getAsInt());
                evs.set(Stats.SPECIAL_DEFENCE, getSafe(evObject, "sp_def", "Integer", "rewards.json/" + key + "/pokemon/evs").getAsInt());
                evs.set(Stats.SPEED, getSafe(evObject, "spd", "Integer", "rewards.json/" + key + "/pokemon/evs").getAsInt());

                reward = new PokemonReward(key, species, level, ability, nature, form, features, gender, shiny, scale, held_item, held_item_data, move_set, ivs, evs);
            } else {
                nr.logError("Unknown reward type: " + type);
            }
            rewardsList.add(reward);
        }
        rewards = rewardsList;
        nr.logInfo("[RAIDS] Loaded " + rewardsList.size() + " rewards.");
    }

    private void loadRewardPools() throws NullPointerException, UnsupportedOperationException {
        File rewardPoolsFile = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/reward_pools.json").toFile();
        JsonElement root = getRoot(rewardPoolsFile);
        assert root != null;
        JsonObject rewardPoolsObject = root.getAsJsonObject();
        List<RewardPool> rewardPools = new ArrayList<>();
        for (String key : rewardPoolsObject.keySet()) {
            JsonObject rewardPoolObject = getSafe(rewardPoolsObject, key, "Json Object", "reward_pools.json").getAsJsonObject();
            boolean allow_duplicates = getSafe(rewardPoolObject, "allow_duplicates", "Boolean", "reward_pools.json/" + key).getAsBoolean();
            JsonObject rolls = getSafe(rewardPoolObject, "rolls", "Json Object", "reward_pools.json/" + key).getAsJsonObject();
            int min_rolls = getSafe(rolls, "min", "Integer", "reward_pools.json/" + key + "/rolls").getAsInt();
            int max_rolls = getSafe(rolls, "max", "Integer", "reward_pools.json/" + key + "/rolls").getAsInt();

            JsonArray rewards = getSafe(rewardPoolObject, "rewards", "Json Object Array", "reward_pools.json/" + key).getAsJsonArray();
            Map<String, Double> rewards_map = new HashMap<>();
            for (JsonElement reward : rewards) {
                JsonObject rewardObject = reward.getAsJsonObject();
                String reward_name = getSafe(rewardObject, "reward", "Json Object", "reward_pools.json/" + key + "/rewards").getAsString();
                double weight = getSafe(rewardObject, "weight", "Double", "reward_pools.json/" + key + "/rewards").getAsDouble();
                rewards_map.put(reward_name, weight);
            }
            rewardPools.add(new RewardPool(key, allow_duplicates, min_rolls, max_rolls, rewards_map));
        }
        reward_pools = rewardPools;
        nr.logInfo("[RAIDS] Loaded " + rewardPools.size() + " reward pools.");
    }

    public Settings getSettings() {
        return settings;
    }

    public Messages getMessages() {
        return messages;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public Category getCategory(String name) {
        for (Category category : categories) {
            if (category.name().equalsIgnoreCase(name)) {
                return category;
            }
        }
        return null;
    }

    public List<Boss> getBosses() {
        return bosses;
    }

    public Boss getBoss(String name) {
        for (Boss boss : bosses) {
            if (boss.name().equalsIgnoreCase(name)) {
                return boss;
            }
        }
        return null;
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    public Reward getReward(String name) {
        for (Reward reward : rewards) {
            if (reward.name().equalsIgnoreCase(name)) {
                return reward;
            }
        }
        return null;
    }

    public List<RewardPool> getRewardPools() {
        return reward_pools;
    }

    public RewardPool getRewardPool(String name) {
        for (RewardPool rewardPool : reward_pools) {
            if (rewardPool.name().equalsIgnoreCase(name)) {
                return rewardPool;
            }
        }
        return null;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public Location getLocation(String name) {
        for (Location loc : locations) {
            if (loc.name().equalsIgnoreCase(name)) {
                return loc;
            }
        }
        return null;
    }

    public List<BossbarData> getBossbars() {
        return bossbars;
    }

    public BossbarData getBossbar(String name) {
        for (BossbarData boss : bossbars) {
            if (boss.name().equalsIgnoreCase(name)) {
                return boss;
            }
        }
        return null;
    }

    public BossbarData getBossbar(String phase, String category, String boss) {
        for (BossbarData bossbarData : bossbars) {
            if (bossbarData.phase().equalsIgnoreCase(phase)) {
                if (bossbarData.categories().contains(category) || bossbarData.bosses().contains(boss)) {
                    return bossbarData;
                }
            }
        }
        return null;
    }

    public boolean loadedProperly() {
        return settings != null && !bossbars.isEmpty() && !categories.isEmpty() && !bosses.isEmpty() && messages != null && !rewards.isEmpty() && !reward_pools.isEmpty() && !locations.isEmpty();
    }
}
