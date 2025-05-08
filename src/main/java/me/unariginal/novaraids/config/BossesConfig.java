package me.unariginal.novaraids.config;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.bosssettings.*;
import me.unariginal.novaraids.data.Category;
import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;
import me.unariginal.novaraids.data.rewards.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.*;
import java.util.*;

public class BossesConfig {
    private final NovaRaids nr = NovaRaids.INSTANCE;
    public List<Category> categories = new ArrayList<>();
    public List<Boss> bosses = new ArrayList<>();

    public BossesConfig() {
        try {
            loadBosses();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            nr.loaded_properly = false;
            nr.logError("[RAIDS] Failed to load bosses folder. " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                nr.logError("  " + element.toString());
            }
        }
    }

    public void loadBosses() throws IOException, NullPointerException, UnsupportedOperationException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        File bossesFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/bosses").toFile();
        if (!bossesFolder.exists()) {
            bossesFolder.mkdirs();
        }

        File[] files = bossesFolder.listFiles();
        if (files == null || files.length == 0) {
            File exampleCategoryFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/bosses/common/bosses").toFile();
            if (!exampleCategoryFolder.exists()) {
                exampleCategoryFolder.mkdirs();
            }

            File settingsFile = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/bosses/common/settings.json").toFile();
            if (settingsFile.createNewFile()) {
                InputStream stream = NovaRaids.class.getResourceAsStream("/raid_config_files/bosses/common/settings.json");
                assert stream != null;
                OutputStream out = new FileOutputStream(settingsFile);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = stream.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

                stream.close();
                out.close();
            }

            File exampleBoss = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/bosses/common/bosses/example_eevee.json").toFile();
            if (exampleBoss.createNewFile()) {
                InputStream stream = NovaRaids.class.getResourceAsStream("/raid_config_files/bosses/common/bosses/example_eevee.json");
                assert stream != null;
                OutputStream out = new FileOutputStream(exampleBoss);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = stream.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

                stream.close();
                out.close();
            }
        }


        for (File file : Objects.requireNonNull(bossesFolder.listFiles())) {
            if (file.isDirectory()) {
                String category_name = file.getName();
                for (File bossFile : Objects.requireNonNull(file.listFiles())) {
                    if (bossFile.isDirectory()) {
                        if (bossFile.getName().equals("bosses")) {
                            for (File boss : Objects.requireNonNull(bossFile.listFiles())) {
                                if (boss.getName().endsWith(".json")) {
                                    loadBoss(category_name, boss);
                                }
                            }
                        }
                    } else if (bossFile.getName().equalsIgnoreCase("settings.json")) {
                        loadSettings(category_name, bossFile);
                    }
                }
            }
        }
    }

    public void loadSettings(String category_name, File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonElement root = JsonParser.parseReader(new FileReader(file));
        assert root != null;
        JsonObject config = root.getAsJsonObject();
        
        String location = category_name + "/settings";
        
        boolean require_pass = false;
        int min_players = 0;
        int max_players = -1;
        List<Species> banned_species = new ArrayList<>();
        List<Move> banned_moves = new ArrayList<>();
        List<Ability> banned_abilities = new ArrayList<>();
        List<Item> banned_held_items = new ArrayList<>();
        List<Item> banned_bag_items = new ArrayList<>();
        String setup_bossbar = "setup_phase_example";
        String fight_bossbar = "fight_phase_example";
        String pre_catch_bossbar = "pre_catch_phase_example";
        String catch_bossbar = "catch_phase_example";
        if (ConfigHelper.checkProperty(config, "raid_details", location)) {
            JsonObject raid_details = config.getAsJsonObject("raid_details");
            if (ConfigHelper.checkProperty(raid_details, "require_pass", location)) {
                require_pass = raid_details.get("require_pass").getAsBoolean();
            }
            
            if (ConfigHelper.checkProperty(raid_details, "player_count", location)) {
                JsonObject player_count = raid_details.getAsJsonObject("player_count");
                if (ConfigHelper.checkProperty(player_count, "min", location)) {
                    min_players = player_count.get("min").getAsInt();
                }
                if (ConfigHelper.checkProperty(player_count, "max", location)) {
                    max_players = player_count.get("max").getAsInt();
                }
            }
            
            if (ConfigHelper.checkProperty(raid_details, "contraband", location)) {
                JsonObject contraband = raid_details.get("contraband").getAsJsonObject();
                if (ConfigHelper.checkProperty(contraband, "banned_pokemon", location)) {
                    List<String> banned_species_names = contraband.getAsJsonArray("banned_pokemon").asList().stream().map(JsonElement::getAsString).toList();
                    for (String species_name : banned_species_names) {
                        Species toAdd = PokemonSpecies.INSTANCE.getByName(species_name);
                        if (toAdd == null) {
                            continue;
                        }
                        banned_species.add(toAdd);
                    }
                }
                if (ConfigHelper.checkProperty(contraband, "banned_moves", location)) {
                    List<String> banned_move_names = contraband.getAsJsonArray("banned_moves").asList().stream().map(JsonElement::getAsString).toList();
                    for (String move_name : banned_move_names) {
                        MoveTemplate template = Moves.INSTANCE.getByName(move_name);
                        if (template == null) {
                            continue;
                        }
                        banned_moves.add(template.create());
                    }
                }
                if (ConfigHelper.checkProperty(contraband, "banned_abilities", location)) {
                    List<String> banned_ability_names = contraband.getAsJsonArray("banned_abilities").asList().stream().map(JsonElement::getAsString).toList();
                    for (String ability_name : banned_ability_names) {
                        AbilityTemplate abilityTemplate = Abilities.INSTANCE.get(ability_name);
                        if (abilityTemplate == null) {
                            continue;
                        }
                        banned_abilities.add(abilityTemplate.create(false, Priority.LOWEST));
                    }
                }
                if (ConfigHelper.checkProperty(contraband, "banned_held_items", location)) {
                    List<String> banned_held_item_names = contraband.getAsJsonArray("banned_held_items").asList().stream().map(JsonElement::getAsString).toList();
                    for (String held_item_name : banned_held_item_names) {
                        Item banned_item = Registries.ITEM.get(Identifier.of(held_item_name));
                        banned_held_items.add(banned_item);
                    }
                }
                if (ConfigHelper.checkProperty(contraband, "banned_bag_items", location)) {
                    List<String> banned_bag_item_names = contraband.getAsJsonArray("banned_bag_items").asList().stream().map(JsonElement::getAsString).toList();
                    for (String bag_item_name : banned_bag_item_names) {
                        Item bag_item = Registries.ITEM.get(Identifier.of(bag_item_name));
                        banned_bag_items.add(bag_item);
                    }
                }
            }
            
            if (ConfigHelper.checkProperty(raid_details, "bossbars", location)) {
                JsonObject bossbars = raid_details.getAsJsonObject("bossbars");
                if (ConfigHelper.checkProperty(bossbars, "setup", location)) {
                    setup_bossbar = bossbars.get("setup").getAsString();
                } else {
                    throw new NullPointerException("Bossbars must have a 'setup' property.");
                }
                if (ConfigHelper.checkProperty(bossbars, "fight", location)) {
                    fight_bossbar = bossbars.get("fight").getAsString();
                } else {
                    throw new NullPointerException("Bossbars must have a 'fight' property.");
                }
                if (ConfigHelper.checkProperty(bossbars, "pre_catch", location)) {
                    pre_catch_bossbar = bossbars.get("pre_catch").getAsString();
                } else {
                    throw new NullPointerException("Bossbars must have a 'pre_catch' property.");
                }
                if (ConfigHelper.checkProperty(bossbars, "catch", location)) {
                    catch_bossbar = bossbars.get("catch").getAsString();
                } else {
                    throw new NullPointerException("Bossbars must have a 'catch' property.");
                }
            }
        }

        Voucher category_choice_voucher = nr.config().global_choice_voucher;
        Voucher category_random_voucher = nr.config().global_random_voucher;
        Pass category_pass = nr.config().global_pass;
        List<RaidBall> category_balls = new ArrayList<>();
        if (ConfigHelper.checkProperty(config, "item_settings", location)) {
            JsonObject item_settings = config.getAsJsonObject("item_settings");
            if (ConfigHelper.checkProperty(item_settings, "category_choice_voucher", location)) {
                JsonObject voucher = item_settings.getAsJsonObject("category_choice_voucher");
                Item voucher_item = category_choice_voucher.voucher_item();
                String voucher_name = category_choice_voucher.voucher_name();
                List<String> voucher_lore = category_choice_voucher.voucher_lore();
                ComponentChanges voucher_data = category_choice_voucher.voucher_data();

                if (ConfigHelper.checkProperty(voucher, "voucher_item", location)) {
                    String voucher_item_name = voucher.get("voucher_item").getAsString();
                    voucher_item = Registries.ITEM.get(Identifier.of(voucher_item_name));
                }
                if (ConfigHelper.checkProperty(voucher, "voucher_name", location)) {
                    voucher_name = voucher.get("voucher_name").getAsString();
                }
                if (ConfigHelper.checkProperty(voucher, "voucher_lore", location)) {
                    JsonArray lore_items = voucher.getAsJsonArray("voucher_lore");
                    List<String> lore = new ArrayList<>();
                    for (JsonElement l : lore_items) {
                        String lore_item = l.getAsString();
                        lore.add(lore_item);
                    }
                    voucher_lore = lore;
                }
                if (ConfigHelper.checkProperty(voucher, "voucher_data", location)) {
                    JsonElement data = voucher.getAsJsonObject("voucher_data");
                    if (data != null) {
                        voucher_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                    }
                }

                category_choice_voucher = new Voucher(voucher_item, voucher_name, voucher_lore, voucher_data);
            }

            if (ConfigHelper.checkProperty(item_settings, "category_random_voucher", location)) {
                JsonObject voucher = item_settings.getAsJsonObject("category_random_voucher");
                Item voucher_item = category_random_voucher.voucher_item();
                String voucher_name = category_random_voucher.voucher_name();
                List<String> voucher_lore = category_random_voucher.voucher_lore();
                ComponentChanges voucher_data = category_random_voucher.voucher_data();

                if (ConfigHelper.checkProperty(voucher, "voucher_item", location)) {
                    String voucher_item_name = voucher.get("voucher_item").getAsString();
                    voucher_item = Registries.ITEM.get(Identifier.of(voucher_item_name));
                }
                if (ConfigHelper.checkProperty(voucher, "voucher_name", location)) {
                    voucher_name = voucher.get("voucher_name").getAsString();
                }
                if (ConfigHelper.checkProperty(voucher, "voucher_lore", location)) {
                    JsonArray lore_items = voucher.getAsJsonArray("voucher_lore");
                    List<String> lore = new ArrayList<>();
                    for (JsonElement l : lore_items) {
                        String lore_item = l.getAsString();
                        lore.add(lore_item);
                    }
                    voucher_lore = lore;
                }
                if (ConfigHelper.checkProperty(voucher, "voucher_data", location)) {
                    JsonElement data = voucher.getAsJsonObject("voucher_data");
                    if (data != null) {
                        voucher_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                    }
                }

                category_random_voucher = new Voucher(voucher_item, voucher_name, voucher_lore, voucher_data);
            }

            if (ConfigHelper.checkProperty(item_settings, "category_pass", location)) {
                JsonObject pass = item_settings.getAsJsonObject("category_pass");
                Item pass_item = category_pass.pass_item();
                String pass_name = category_pass.pass_name();
                List<String> pass_lore = category_pass.pass_lore();
                ComponentChanges pass_data = category_pass.pass_data();

                if (ConfigHelper.checkProperty(pass, "pass_item", location)) {
                    String pass_item_name = pass.get("pass_item").getAsString();
                    pass_item = Registries.ITEM.get(Identifier.of(pass_item_name));
                }
                if (ConfigHelper.checkProperty(pass, "pass_name", location)) {
                    pass_name = pass.get("pass_name").getAsString();
                }
                if (ConfigHelper.checkProperty(pass, "pass_lore", location)) {
                    JsonArray lore_items = pass.getAsJsonArray("pass_lore");
                    List<String> lore = new ArrayList<>();
                    for (JsonElement l : lore_items) {
                        String lore_item = l.getAsString();
                        lore.add(lore_item);
                    }
                    pass_lore = lore;
                }
                if (ConfigHelper.checkProperty(pass, "pass_data", location)) {
                    JsonElement data = pass.getAsJsonObject("pass_data");
                    if (data != null) {
                        pass_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                    }
                }

                category_pass = new Pass(pass_item, pass_name, pass_lore, pass_data);
            }

            if (ConfigHelper.checkProperty(item_settings, "raid_balls", location)) {
                JsonObject raidBalls = item_settings.getAsJsonObject("raid_balls");
                for (String id : raidBalls.keySet()) {
                    JsonObject ballObject = raidBalls.getAsJsonObject(id);
                    Item pokeball = CobblemonItems.POKE_BALL;
                    String pokeball_name = "<red> " + category_name + " Raid Ball";
                    List<String> pokeball_lore = List.of("<gray>Use this to try and catch " + category_name + " bosses!");
                    ComponentChanges pokeball_data = ComponentChanges.EMPTY;

                    if (ConfigHelper.checkProperty(ballObject, "pokeball", location)) {
                        String pokeball_item_name = ballObject.get("pokeball").getAsString();
                        pokeball = Registries.ITEM.get(Identifier.of(pokeball_item_name));
                    }
                    if (ConfigHelper.checkProperty(ballObject, "pokeball_name", location)) {
                        pokeball_name = ballObject.get("pokeball_name").getAsString();
                    }
                    if (ConfigHelper.checkProperty(ballObject, "pokeball_lore", location)) {
                        JsonArray lore_items = ballObject.getAsJsonArray("pokeball_lore");
                        List<String> lore = new ArrayList<>();
                        for (JsonElement l : lore_items) {
                            String lore_item = l.getAsString();
                            lore.add(lore_item);
                        }
                        pokeball_lore = lore;
                    }
                    if (ConfigHelper.checkProperty(ballObject, "pokeball_data", location)) {
                        JsonElement data = ballObject.getAsJsonObject("pokeball_data");
                        if (data != null) {
                            pokeball_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                        }
                    }

                    category_balls.add(new RaidBall(id, pokeball, pokeball_name, pokeball_lore, pokeball_data));
                }
            }
        }

        List<DistributionSection> rewards = ConfigHelper.getDistributionSections(config, location);
        categories.add(new Category(
                category_name, 
                require_pass,
                min_players, 
                max_players,
                banned_species,
                banned_moves,
                banned_abilities,
                banned_held_items,
                banned_bag_items,
                setup_bossbar, 
                fight_bossbar, 
                pre_catch_bossbar, 
                catch_bossbar, 
                category_choice_voucher, 
                category_random_voucher, 
                category_pass, 
                category_balls, 
                rewards
        ));
    }

    public void loadBoss(String category_name, File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonElement root = JsonParser.parseReader(new FileReader(file));
        assert root != null;
        JsonObject config = root.getAsJsonObject();
        String file_name = file.getName().substring(0, file.getName().indexOf(".json"));

        String location = category_name + "/bosses/" + file_name;

        String boss_id;
        if (ConfigHelper.checkProperty(config, "boss_id", location)) {
            boss_id = config.get("boss_id").getAsString();
        } else {
            throw new NullPointerException("Boss must have a Boss ID!");
        }

        double global_weight;
        if (ConfigHelper.checkProperty(config, "global_weight", location)) {
            global_weight = config.get("global_weight").getAsDouble();
        } else {
            throw new NullPointerException("Boss must have a global weight!");
        }

        double category_weight;
        if (ConfigHelper.checkProperty(config, "category_weight", location)) {
            category_weight = config.get("category_weight").getAsDouble();
        } else {
            throw new NullPointerException("Boss must have a category weight!");
        }

        // Pokemon Details
        Species species;
        int level = 50;
        FormData form;
        String features = "";
        Map<Ability, Double> abilities = new HashMap<>();
        Map<Nature, Double> natures = new HashMap<>();
        Map<Gender, Double> genders = new HashMap<>();
        boolean shiny = false;
        float scale = 1.0f;
        Item held_item = Items.AIR;
        ComponentChanges held_item_data = ComponentChanges.EMPTY;
        List<MoveTemplate> moves = new ArrayList<>();
        IVs ivs = IVs.createRandomIVs(0);
        EVs evs = EVs.createEmpty();

        if (ConfigHelper.checkProperty(config, "pokemon_details", location)) {
            JsonObject pokemon_details = config.getAsJsonObject("pokemon_details");
            if (ConfigHelper.checkProperty(pokemon_details, "species", location)) {
                species = PokemonSpecies.INSTANCE.getByName(pokemon_details.get("species").getAsString());
                if (species == null) {
                    throw new NullPointerException("Species not found!");
                }
            } else {
                throw new NullPointerException("Pokemon details must have a species!");
            }
            if (ConfigHelper.checkProperty(pokemon_details, "level", location)) {
                level = pokemon_details.get("level").getAsInt();
            }
            if (ConfigHelper.checkProperty(pokemon_details, "form", location)) {
                form = species.getFormByName(pokemon_details.get("form").getAsString());
            } else {
                form = species.getStandardForm();
            }
            if (ConfigHelper.checkProperty(pokemon_details, "features", location)) {
                features = pokemon_details.get("features").getAsString();
            }
            if (ConfigHelper.checkProperty(pokemon_details, "ability", location)) {
                JsonArray ability_array = pokemon_details.getAsJsonArray("ability");
                for (JsonElement ability_element : ability_array) {
                    JsonObject ability = ability_element.getAsJsonObject();
                    String ability_id;
                    if (ConfigHelper.checkProperty(ability, "ability", location)) {
                        ability_id = ability.get("ability").getAsString();
                    } else {
                        continue;
                    }
                    double weight;
                    if (ConfigHelper.checkProperty(ability, "weight", location)) {
                        weight = ability.get("weight").getAsDouble();
                    } else {
                        continue;
                    }
                    AbilityTemplate template = Abilities.INSTANCE.get(ability_id);
                    if (template == null) {
                        nr.logError("[RAIDS] Skipping unknown ability: " + ability_id + ".");
                        continue;
                    }
                    Ability possible_ability = template.create(false, Priority.LOWEST);
                    abilities.put(possible_ability, weight);
                }
                if (abilities.isEmpty()) {
                    throw new NullPointerException("No abilities found!");
                }
            } else {
                throw new NullPointerException("Pokemon details must have an ability!");
            }
            if (ConfigHelper.checkProperty(pokemon_details, "nature", location)) {
                JsonArray nature_array = pokemon_details.getAsJsonArray("nature");
                for (JsonElement nature_element : nature_array) {
                    JsonObject nature = nature_element.getAsJsonObject();
                    String nature_id;
                    if (ConfigHelper.checkProperty(nature, "nature", location)) {
                        nature_id = nature.get("nature").getAsString();
                    } else {
                        continue;
                    }
                    double weight;
                    if (ConfigHelper.checkProperty(nature, "weight", location)) {
                        weight = nature.get("weight").getAsDouble();
                    } else {
                        continue;
                    }
                    Nature possible_nature = Natures.INSTANCE.getNature(nature_id);
                    if (possible_nature == null) {
                        nr.logError("[RAIDS] Skipping unknown nature: " + nature_id + ".");
                    }
                    natures.put(possible_nature, weight);
                }
                if (natures.isEmpty()) {
                    throw new NullPointerException("No natures found!");
                }
            } else {
                throw new NullPointerException("Pokemon details must have a nature!");
            }
            if (ConfigHelper.checkProperty(pokemon_details, "gender", location)) {
                JsonArray gender_array = pokemon_details.getAsJsonArray("gender");
                for (JsonElement gender_element : gender_array) {
                    JsonObject gender = gender_element.getAsJsonObject();
                    String gender_id;
                    if (ConfigHelper.checkProperty(gender, "gender", location)) {
                        gender_id = gender.get("gender").getAsString();
                    } else {
                        continue;
                    }
                    double weight;
                    if (ConfigHelper.checkProperty(gender, "weight", location)) {
                        weight = gender.get("weight").getAsDouble();
                    } else {
                        continue;
                    }
                    Gender possible_gender = Gender.valueOf(gender_id.toUpperCase());
                    genders.put(possible_gender, weight);
                }
                if (genders.isEmpty()) {
                    throw new NullPointerException("No genders found!");
                }
            } else {
                throw new NullPointerException("Pokemon details must have a gender!");
            }
            if (ConfigHelper.checkProperty(pokemon_details, "shiny", location)) {
                shiny = pokemon_details.get("shiny").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(pokemon_details, "scale", location)) {
                scale = pokemon_details.get("scale").getAsFloat();
            }
            if (ConfigHelper.checkProperty(pokemon_details, "held_item", location)) {
                held_item = Registries.ITEM.get(Identifier.of(pokemon_details.get("held_item").getAsString()));
            }
            if (ConfigHelper.checkProperty(pokemon_details, "held_item_data", location)) {
                JsonElement data_element = pokemon_details.getAsJsonObject("held_item_data");
                if (data_element != null) {
                    held_item_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data_element).getOrThrow().getFirst();
                }
            }
            if (ConfigHelper.checkProperty(pokemon_details, "moves", location)) {
                List<String> moves_array = pokemon_details.getAsJsonArray("moves").asList().stream().map(JsonElement::getAsString).toList();
                if (moves_array.isEmpty()) {
                    throw new NullPointerException("Boss must have at least one move!");
                }
                for (String move : moves_array) {
                    MoveTemplate template = Moves.INSTANCE.getByName(move);
                    if (template == null) {
                        nr.logError("[RAIDS] Skipping unknown move " + move + ".");
                        continue;
                    }
                    moves.add(template);
                }
                if (moves.isEmpty()) {
                    throw new NullPointerException("No moves found!");
                }
            }
            if (ConfigHelper.checkProperty(pokemon_details, "ivs", location)) {
                JsonObject ivs_object = pokemon_details.getAsJsonObject("ivs");
                if (ConfigHelper.checkProperty(ivs_object, "hp", location)) {
                    ivs.set(Stats.HP, ivs_object.get("hp").getAsInt());
                }
                if (ConfigHelper.checkProperty(ivs_object, "atk", location)) {
                    ivs.set(Stats.ATTACK, ivs_object.get("atk").getAsInt());
                }
                if (ConfigHelper.checkProperty(ivs_object, "def", location)) {
                    ivs.set(Stats.DEFENCE, ivs_object.get("def").getAsInt());
                }
                if (ConfigHelper.checkProperty(ivs_object, "sp_atk", location)) {
                    ivs.set(Stats.SPECIAL_ATTACK, ivs_object.get("sp_atk").getAsInt());
                }
                if (ConfigHelper.checkProperty(ivs_object, "sp_def", location)) {
                    ivs.set(Stats.SPECIAL_DEFENCE, ivs_object.get("sp_def").getAsInt());
                }
                if (ConfigHelper.checkProperty(ivs_object, "spd", location)) {
                    ivs.set(Stats.SPEED, ivs_object.get("spd").getAsInt());
                }
            }
            if (ConfigHelper.checkProperty(pokemon_details, "evs", location)) {
                JsonObject evs_object = pokemon_details.getAsJsonObject("ivs");
                if (ConfigHelper.checkProperty(evs_object, "hp", location)) {
                    evs.set(Stats.HP, evs_object.get("hp").getAsInt());
                }
                if (ConfigHelper.checkProperty(evs_object, "atk", location)) {
                    evs.set(Stats.ATTACK, evs_object.get("atk").getAsInt());
                }
                if (ConfigHelper.checkProperty(evs_object, "def", location)) {
                    evs.set(Stats.DEFENCE, evs_object.get("def").getAsInt());
                }
                if (ConfigHelper.checkProperty(evs_object, "sp_atk", location)) {
                    evs.set(Stats.SPECIAL_ATTACK, evs_object.get("sp_atk").getAsInt());
                }
                if (ConfigHelper.checkProperty(evs_object, "sp_def", location)) {
                    evs.set(Stats.SPECIAL_DEFENCE, evs_object.get("sp_def").getAsInt());
                }
                if (ConfigHelper.checkProperty(evs_object, "spd", location)) {
                    evs.set(Stats.SPEED, evs_object.get("spd").getAsInt());
                }
            }
        } else {
            throw new NullPointerException("Boss must have pokemon details!");
        }

        PokemonDetails pokemonDetails = new PokemonDetails(
                species,
                level,
                form,
                features,
                abilities,
                natures,
                genders,
                shiny,
                scale,
                held_item,
                held_item_data,
                moves,
                ivs,
                evs
        );

        // Boss Details
        String display_name = boss_id;
        int base_health;
        int health_increase_per_player = 0;
        boolean apply_glowing = false;
        Map<String, Double> locations = new HashMap<>();
        if (ConfigHelper.checkProperty(config, "boss_details", location)) {
            JsonObject boss_details = config.get("boss_details").getAsJsonObject();
            if (ConfigHelper.checkProperty(boss_details, "display_name", location)) {
                display_name = boss_details.get("display_name").getAsString();
            }
            if (ConfigHelper.checkProperty(boss_details, "base_health", location)) {
                base_health = boss_details.get("base_health").getAsInt();
            } else {
                throw new NullPointerException("Boss details must have base health!");
            }
            if (ConfigHelper.checkProperty(boss_details, "health_increase_per_player", location)) {
                health_increase_per_player = boss_details.get("health_increase_per_player").getAsInt();
            }
            if (ConfigHelper.checkProperty(boss_details, "apply_glowing", location)) {
                apply_glowing = boss_details.get("apply_glowing").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(boss_details, "locations", location)) {
                JsonArray locations_array = boss_details.get("locations").getAsJsonArray();
                for (JsonElement location_element : locations_array) {
                    JsonObject location_object = location_element.getAsJsonObject();
                    String location_name;
                    if (ConfigHelper.checkProperty(location_object, "location", location)) {
                        location_name = location_object.get("location").getAsString();
                    } else {
                        continue;
                    }
                    double weight;
                    if (ConfigHelper.checkProperty(location_object, "weight", location)) {
                        weight = location_object.get("weight").getAsDouble();
                    } else {
                        continue;
                    }
                    locations.put(location_name, weight);
                }
                if (locations.isEmpty()) {
                    throw new NullPointerException("No locations found!");
                }
            } else {
                throw new NullPointerException("Boss details must have locations!");
            }
        } else {
            throw new NullPointerException("Boss must have boss details!");
        }

        // Item Settings
        boolean allow_global_pokeballs = true;
        boolean allow_category_pokeballs = true;
        Voucher boss_voucher = nr.config().default_voucher;
        Pass boss_pass = nr.config().default_pass;
        List<RaidBall> boss_balls = new ArrayList<>();
        if (ConfigHelper.checkProperty(config, "item_settings", location)) {
            JsonObject item_settings = config.get("item_settings").getAsJsonObject();
            if (ConfigHelper.checkProperty(item_settings, "allow_global_pokeballs", location)) {
                allow_global_pokeballs = item_settings.get("allow_global_pokeballs").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(item_settings, "allow_category_pokeballs", location)) {
                allow_category_pokeballs = item_settings.get("allow_category_pokeballs").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(item_settings, "boss_voucher", location)) {
                Item voucher_item = nr.config().default_voucher.voucher_item();
                String voucher_name = nr.config().default_voucher.voucher_name();
                List<String> voucher_lore = nr.config().default_voucher.voucher_lore();
                ComponentChanges voucher_data = nr.config().default_voucher.voucher_data();
                JsonObject boss_voucher_object = item_settings.getAsJsonObject("boss_voucher");
                if (ConfigHelper.checkProperty(boss_voucher_object, "voucher_item", location)) {
                    voucher_item = Registries.ITEM.get(Identifier.of(boss_voucher_object.get("voucher_item").getAsString()));
                }
                if (ConfigHelper.checkProperty(boss_voucher_object, "voucher_name", location)) {
                    voucher_name = boss_voucher_object.get("voucher_name").getAsString();
                }
                if (ConfigHelper.checkProperty(boss_voucher_object, "voucher_lore", location)) {
                    JsonArray lore_array = boss_voucher_object.get("voucher_lore").getAsJsonArray();
                    List<String> lore = new ArrayList<>();
                    for (JsonElement lore_element : lore_array) {
                        lore.add(lore_element.getAsString());
                    }
                    voucher_lore = lore;
                }
                if (ConfigHelper.checkProperty(boss_voucher_object, "voucher_data", location)) {
                    JsonElement data_object = boss_voucher_object.get("voucher_data");
                    if (data_object != null) {
                        voucher_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data_object).getOrThrow().getFirst();
                    }
                }
                boss_voucher = new Voucher(voucher_item, voucher_name, voucher_lore, voucher_data);
            }
            if (ConfigHelper.checkProperty(item_settings, "boss_pass", location)) {
                Item pass_item = nr.config().default_pass.pass_item();
                String pass_name = nr.config().default_pass.pass_name();
                List<String> pass_lore = nr.config().default_pass.pass_lore();
                ComponentChanges pass_data = nr.config().default_pass.pass_data();
                JsonObject boss_pass_object = item_settings.getAsJsonObject("boss_pass");
                if (ConfigHelper.checkProperty(boss_pass_object, "pass_item", location)) {
                    pass_item = Registries.ITEM.get(Identifier.of(boss_pass_object.get("pass_item").getAsString()));
                }
                if (ConfigHelper.checkProperty(boss_pass_object, "pass_name", location)) {
                    pass_name = boss_pass_object.get("pass_name").getAsString();
                }
                if (ConfigHelper.checkProperty(boss_pass_object, "pass_lore", location)) {
                    JsonArray lore_array = boss_pass_object.get("pass_lore").getAsJsonArray();
                    List<String> lore = new ArrayList<>();
                    for (JsonElement lore_element : lore_array) {
                        lore.add(lore_element.getAsString());
                    }
                    pass_lore = lore;
                }
                if (ConfigHelper.checkProperty(boss_pass_object, "pass_data", location)) {
                    JsonElement data_object = boss_pass_object.get("pass_data");
                    if (data_object != null) {
                        pass_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data_object).getOrThrow().getFirst();
                    }
                }
                boss_pass = new Pass(pass_item, pass_name, pass_lore, pass_data);
            }
            if (ConfigHelper.checkProperty(item_settings, "raid_balls", location)) {
                JsonObject raid_balls = item_settings.getAsJsonObject("raid_balls");
                for (String ball_id : raid_balls.keySet()) {
                    JsonObject ball_info = raid_balls.getAsJsonObject(ball_id);
                    Item item = CobblemonItems.POKE_BALL;
                    String name = "<red>Raid Pokeball";
                    List<String> lore = new ArrayList<>(List.of("<gray>Use this to try and capture raid bosses!"));
                    ComponentChanges data = ComponentChanges.EMPTY;
                    if (ConfigHelper.checkProperty(ball_info, "pokeball", location)) {
                        item = Registries.ITEM.get(Identifier.of(ball_info.get("pokeball").getAsString()));
                    }
                    if (ConfigHelper.checkProperty(ball_info, "pokeball_name", location)) {
                        name = ball_info.get("pokeball_name").getAsString();
                    }
                    if (ConfigHelper.checkProperty(ball_info, "pokeball_lore", location)) {
                        JsonArray lore_items = ball_info.getAsJsonArray("pokeball_lore");
                        List<String> newLore = new ArrayList<>();
                        for (JsonElement l : lore_items) {
                            String lore_item = l.getAsString();
                            newLore.add(lore_item);
                        }
                        lore = newLore;
                    }
                    if (ConfigHelper.checkProperty(ball_info, "pokeball_data", location)) {
                        JsonElement dataElement = ball_info.get("pokeball_data");
                        if (dataElement != null) {
                            data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, dataElement).getOrThrow().getFirst();
                        }
                    }
                    boss_balls.add(new RaidBall(ball_id, item, name, lore, data));
                }
            }
        }

        ItemSettings itemSettings = new ItemSettings(
                allow_global_pokeballs,
                allow_category_pokeballs,
                boss_voucher,
                boss_pass,
                boss_balls
        );

        // Raid Details
        int minimum_level = 1;
        int setup_phase_time;
        int fight_phase_time;
        boolean do_catch_phase = true;
        int pre_catch_phase_time = -1;
        int catch_phase_time = -1;
        boolean heal_party_on_challenge = false;
        List<Species> banned_species = new ArrayList<>();
        List<Move> banned_moves = new ArrayList<>();
        List<Ability> banned_abilities = new ArrayList<>();
        List<Item> banned_held_items = new ArrayList<>();
        List<Item> banned_bag_items = new ArrayList<>();
        String setup_bossbar = "";
        String fight_bossbar = "";
        String pre_catch_bossbar = "";
        String catch_bossbar = "";
        List<DistributionSection> rewards;
        if (ConfigHelper.checkProperty(config, "raid_details", location)) {
            JsonObject raid_details = config.get("raid_details").getAsJsonObject();
            if (ConfigHelper.checkProperty(raid_details, "minimum_level", location)) {
                minimum_level = raid_details.get("minimum_level").getAsInt();
            }
            if (ConfigHelper.checkProperty(raid_details, "setup_phase_time", location)) {
                setup_phase_time = raid_details.get("setup_phase_time").getAsInt();
            } else {
                throw new NullPointerException("Raid details must have setup phase time!");
            }
            if (ConfigHelper.checkProperty(raid_details, "fight_phase_time", location)) {
                fight_phase_time = raid_details.get("fight_phase_time").getAsInt();
            } else {
                throw new NullPointerException("Raid details must have fight phase time!");
            }
            if (ConfigHelper.checkProperty(raid_details, "do_catch_phase", location)) {
                do_catch_phase = raid_details.get("do_catch_phase").getAsBoolean();
            }
            if (do_catch_phase) {
                if (ConfigHelper.checkProperty(raid_details, "pre_catch_phase_time", location)) {
                    pre_catch_phase_time = raid_details.get("pre_catch_phase_time").getAsInt();
                } else {
                    throw new NullPointerException("Raid details must have pre catch phase time!");
                }
                if (ConfigHelper.checkProperty(raid_details, "catch_phase_time", location)) {
                    catch_phase_time = raid_details.get("catch_phase_time").getAsInt();
                } else {
                    throw new NullPointerException("Raid details must have catch phase time!");
                }
            }
            if (ConfigHelper.checkProperty(raid_details, "heal_party_on_challenge", location)) {
                heal_party_on_challenge = raid_details.get("heal_party_on_challenge").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(raid_details, "contraband", location)) {
                JsonObject contraband = raid_details.get("contraband").getAsJsonObject();
                if (ConfigHelper.checkProperty(contraband, "banned_pokemon", location)) {
                    List<String> banned_species_names = contraband.getAsJsonArray("banned_pokemon").asList().stream().map(JsonElement::getAsString).toList();
                    for (String species_name : banned_species_names) {
                        Species toAdd = PokemonSpecies.INSTANCE.getByName(species_name);
                        if (toAdd == null) {
                            continue;
                        }
                        banned_species.add(toAdd);
                    }
                }
                if (ConfigHelper.checkProperty(contraband, "banned_moves", location)) {
                    List<String> banned_move_names = contraband.getAsJsonArray("banned_moves").asList().stream().map(JsonElement::getAsString).toList();
                    for (String move_name : banned_move_names) {
                        MoveTemplate template = Moves.INSTANCE.getByName(move_name);
                        if (template == null) {
                            continue;
                        }
                        banned_moves.add(template.create());
                    }
                }
                if (ConfigHelper.checkProperty(contraband, "banned_abilities", location)) {
                    List<String> banned_ability_names = contraband.getAsJsonArray("banned_abilities").asList().stream().map(JsonElement::getAsString).toList();
                    for (String ability_name : banned_ability_names) {
                        AbilityTemplate abilityTemplate = Abilities.INSTANCE.get(ability_name);
                        if (abilityTemplate == null) {
                            continue;
                        }
                        banned_abilities.add(abilityTemplate.create(false, Priority.LOWEST));
                    }
                }
                if (ConfigHelper.checkProperty(contraband, "banned_held_items", location)) {
                    List<String> banned_held_item_names = contraband.getAsJsonArray("banned_held_items").asList().stream().map(JsonElement::getAsString).toList();
                    for (String held_item_name : banned_held_item_names) {
                        Item banned_item = Registries.ITEM.get(Identifier.of(held_item_name));
                        banned_held_items.add(banned_item);
                    }
                }
                if (ConfigHelper.checkProperty(contraband, "banned_bag_items", location)) {
                    List<String> banned_bag_item_names = contraband.getAsJsonArray("banned_bag_items").asList().stream().map(JsonElement::getAsString).toList();
                    for (String bag_item_name : banned_bag_item_names) {
                        Item bag_item = Registries.ITEM.get(Identifier.of(bag_item_name));
                        banned_bag_items.add(bag_item);
                    }
                }
            }
            if (ConfigHelper.checkProperty(raid_details, "bossbars", location)) {
                JsonObject bossbars = raid_details.get("bossbars").getAsJsonObject();
                if (ConfigHelper.checkProperty(bossbars, "setup", location)) {
                    setup_bossbar = bossbars.get("setup").getAsString();
                }
                if (ConfigHelper.checkProperty(bossbars, "fight", location)) {
                    fight_bossbar = bossbars.get("fight").getAsString();
                }
                if (ConfigHelper.checkProperty(bossbars, "pre_catch", location)) {
                    pre_catch_bossbar = bossbars.get("pre_catch").getAsString();
                }
                if (ConfigHelper.checkProperty(bossbars, "catch", location)) {
                    catch_bossbar = bossbars.get("catch").getAsString();
                }
            }
            rewards = ConfigHelper.getDistributionSections(raid_details, location);
        } else {
            throw new NullPointerException("Boss must have raid details!");
        }

        RaidDetails raidDetails = new RaidDetails(
                minimum_level,
                setup_phase_time,
                fight_phase_time,
                do_catch_phase,
                pre_catch_phase_time,
                catch_phase_time,
                heal_party_on_challenge,
                banned_species,
                banned_moves,
                banned_abilities,
                banned_held_items,
                banned_bag_items,
                setup_bossbar,
                fight_bossbar,
                pre_catch_bossbar,
                catch_bossbar,
                rewards
        );

        // Catch Settings
        Species species_override = species;
        int level_override = 1;
        FormData form_override = species.getStandardForm();
        String features_override = "";
        boolean keep_scale = false;
        boolean keep_held_item = false;
        boolean randomize_ivs = true;
        boolean keep_evs = false;
        boolean randomize_gender = true;
        boolean randomize_nature = true;
        boolean randomize_ability = true;
        boolean reset_moves = true;
        List<CatchPlacement> catch_places = new ArrayList<>();

        if (ConfigHelper.checkProperty(config, "catch_settings", location)) {
            JsonObject catch_settings = config.get("catch_settings").getAsJsonObject();
            if (ConfigHelper.checkProperty(catch_settings, "species_override", location)) {
                String species_string = catch_settings.get("species_override").getAsString();
                if (!species_string.isEmpty()) {
                    Species s = PokemonSpecies.INSTANCE.getByName(species_string);
                    if (s != null) {
                        species_override = s;
                    } else {
                        nr.logError("[RAIDS] Skipping unknown species override: " + catch_settings.get("species_override").getAsString());
                    }
                }
            }
            if (ConfigHelper.checkProperty(catch_settings, "level_override", location)) {
                level_override = catch_settings.get("level_override").getAsInt();
            }
            if (ConfigHelper.checkProperty(catch_settings, "form_override", location)) {
                String form_ov = catch_settings.get("form_override").getAsString();
                form_override = species_override.getFormByName(form_ov);
            }
            if (ConfigHelper.checkProperty(catch_settings, "features_override", location)) {
                features_override = catch_settings.get("features_override").getAsString();
            }
            if (ConfigHelper.checkProperty(catch_settings, "keep_scale", location)) {
                keep_scale = catch_settings.get("keep_scale").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(catch_settings, "keep_held_item", location)) {
                keep_held_item = catch_settings.get("keep_held_item").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(catch_settings, "randomize_ivs", location)) {
                randomize_ivs = catch_settings.get("randomize_ivs").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(catch_settings, "keep_evs", location)) {
                keep_evs = catch_settings.get("keep_evs").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(catch_settings, "randomize_gender", location)) {
                randomize_gender = catch_settings.get("randomize_gender").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(catch_settings, "randomize_nature", location)) {
                randomize_nature = catch_settings.get("randomize_nature").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(catch_settings, "randomize_ability", location)) {
                randomize_ability = catch_settings.get("randomize_ability").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(catch_settings, "reset_moves", location)) {
                reset_moves = catch_settings.get("reset_moves").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(catch_settings, "places", location)) {
                JsonArray places = catch_settings.get("places").getAsJsonArray();
                for (JsonElement place_element : places) {
                    JsonObject place_object = place_element.getAsJsonObject();
                    String place;
                    if (ConfigHelper.checkProperty(place_object, "place", location)) {
                        place = place_object.get("place").getAsString();
                    } else {
                        continue;
                    }
                    boolean require_damage;
                    if (ConfigHelper.checkProperty(place_object, "require_damage", location)) {
                        require_damage = place_object.get("require_damage").getAsBoolean();
                    } else {
                        continue;
                    }
                    double shiny_chance;
                    if (ConfigHelper.checkProperty(place_object, "shiny_chance", location)) {
                        shiny_chance = place_object.get("shiny_chance").getAsDouble();
                    } else {
                        continue;
                    }
                    int min_perfect_ivs;
                    if (ConfigHelper.checkProperty(place_object, "min_perfect_ivs", location)) {
                        min_perfect_ivs = place_object.get("min_perfect_ivs").getAsInt();
                    } else {
                        continue;
                    }
                    catch_places.add(new CatchPlacement(place, require_damage, shiny_chance, min_perfect_ivs));
                }
            }
            if (catch_places.isEmpty()) {
                catch_places.add(new CatchPlacement("participating", true, 8192, 0));
            }
        }

        CatchSettings catch_settings = new CatchSettings(
                species_override,
                level_override,
                form_override,
                features_override,
                keep_scale,
                keep_held_item,
                randomize_ivs,
                keep_evs,
                randomize_gender,
                randomize_nature,
                randomize_ability,
                reset_moves,
                catch_places
        );

        bosses.add(new Boss(
                boss_id,
                category_name,
                global_weight,
                category_weight,
                pokemonDetails,
                display_name,
                base_health,
                health_increase_per_player,
                apply_glowing,
                locations,
                itemSettings,
                raidDetails,
                catch_settings
        ));
    }

    public Boss getRandomBoss(String category) {
        double total_weight = 0;
        for (Boss boss : bosses) {
            if (boss.category_id().equalsIgnoreCase(category)) {
                total_weight += boss.category_weight();
            }
        }

        if (total_weight > 0) {
            double random_weight = new Random().nextDouble(total_weight);
            total_weight = 0;
            for (Boss boss : bosses) {
                if (boss.category_id().equalsIgnoreCase(category)) {
                    total_weight += boss.category_weight();
                    if (random_weight < total_weight) {
                        return boss;
                    }
                }
            }
        }
        return null;
    }

    public Boss getRandomBoss() {
        double total_weight = 0;
        for (Boss boss : bosses) {
            total_weight += boss.global_weight();
        }

        if (total_weight > 0) {
            double random_weight = new Random().nextDouble(total_weight);
            total_weight = 0;
            for (Boss boss : bosses) {
                total_weight += boss.global_weight();
                if (random_weight < total_weight) {
                    return boss;
                }
            }
        }
        return null;
    }

    public Boss getBoss(String id) {
        for (Boss boss : bosses) {
            if (boss.boss_id().equalsIgnoreCase(id)) {
                return boss;
            }
        }
        return null;
    }

    public Category getCategory(String id) {
        for (Category category : categories) {
            if (category.name().equalsIgnoreCase(id)) {
                return category;
            }
        }
        return null;
    }
}
