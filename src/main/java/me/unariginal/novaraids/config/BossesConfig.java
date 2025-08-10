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
            nr.loadedProperly = false;
            nr.logError("[RAIDS] Failed to load bosses folder. " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                nr.logError("  " + element.toString());
            }
        }
    }

    public void loadBosses() throws IOException, NullPointerException, UnsupportedOperationException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids").toFile();
        if (!rootFolder.exists()) rootFolder.mkdirs();

        File bossesFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/bosses").toFile();
        if (!bossesFolder.exists()) {
            bossesFolder.mkdirs();

            File exampleCategoryFolder = FabricLoader.getInstance().getConfigDir().resolve("NovaRaids/bosses/common/bosses").toFile();
            if (!exampleCategoryFolder.exists()) exampleCategoryFolder.mkdirs();

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

    public void loadSettings(String category_id, File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonObject root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
        JsonObject newRoot = new JsonObject();

        String category_name = category_id;
        if (root.has("category_name"))
            category_name = root.get("category_name").getAsString();
        newRoot.addProperty("category_name", category_name);


        //------- Raid Details Section -------//

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

        JsonObject raidDetails = new JsonObject();
        if (root.has("raid_details"))
            raidDetails = root.get("raid_details").getAsJsonObject();

        if (raidDetails.has("require_pass"))
            require_pass = raidDetails.get("require_pass").getAsBoolean();
        raidDetails.addProperty("require_pass", require_pass);

        JsonObject playerCount = new JsonObject();
        if (raidDetails.has("player_count"))
            playerCount = raidDetails.get("player_count").getAsJsonObject();

        if (playerCount.has("min"))
            min_players = playerCount.get("min").getAsInt();
        playerCount.addProperty("min", min_players);

        if (playerCount.has("max"))
            max_players = playerCount.get("max").getAsInt();
        playerCount.addProperty("max", max_players);

        raidDetails.add("player_count", playerCount);

        JsonObject contraband = new JsonObject();
        if (raidDetails.has("contraband"))
            contraband = raidDetails.get("contraband").getAsJsonObject();

        JsonArray bannedPokemonJsonArray = new JsonArray();
        List<String> bannedPokemon = new ArrayList<>();
        if (contraband.has("banned_pokemon"))
            bannedPokemon = contraband.getAsJsonArray("banned_pokemon").asList().stream().map(JsonElement::getAsString).toList();
        for (String pokemon : bannedPokemon) {
            Species species = PokemonSpecies.INSTANCE.getByName(pokemon);
            if (species != null) banned_species.add(species);
            else NovaRaids.LOGGER.warn("[NovaRaids] Contraband species {} not found in !{}", pokemon, file.getPath());
            bannedPokemonJsonArray.add(pokemon);
        }
        contraband.add("banned_pokemon", bannedPokemonJsonArray);

        JsonArray bannedMovesJsonArray = new JsonArray();
        List<String> bannedMoves = new ArrayList<>();
        if (contraband.has("banned_moves"))
            bannedMoves = contraband.getAsJsonArray("banned_moves").asList().stream().map(JsonElement::getAsString).toList();
        for (String move : bannedMoves) {
            MoveTemplate template = Moves.INSTANCE.getByName(move);
            if (template != null) banned_moves.add(template.create());
            else NovaRaids.LOGGER.warn("[NovaRaids] Contraband move {} not found in !{}", move, file.getPath());
            bannedMovesJsonArray.add(move);
        }
        contraband.add("banned_moves", bannedMovesJsonArray);

        JsonArray bannedAbilitiesJsonArray = new JsonArray();
        List<String> bannedAbilities = new ArrayList<>();
        if (contraband.has("banned_abilities"))
            bannedAbilities = contraband.getAsJsonArray("banned_abilities").asList().stream().map(JsonElement::getAsString).toList();
        for (String ability : bannedAbilities) {
            AbilityTemplate abilityTemplate = Abilities.INSTANCE.get(ability);
            if (abilityTemplate != null) banned_abilities.add(abilityTemplate.create(false, Priority.LOWEST));
            else NovaRaids.LOGGER.warn("[NovaRaids] Contraband ability {} not found in !{}", ability, file.getPath());
            bannedAbilitiesJsonArray.add(ability);
        }
        contraband.add("banned_abilities", bannedAbilitiesJsonArray);

        JsonArray bannedHeldItemsJsonArray = new JsonArray();
        List<String> bannedHeldItems = new ArrayList<>();
        if (contraband.has("banned_held_items"))
            bannedHeldItems = contraband.getAsJsonArray("banned_held_items").asList().stream().map(JsonElement::getAsString).toList();
        for (String item : bannedHeldItems) {
            Item bannedItem = Registries.ITEM.get(Identifier.of(item));
            banned_held_items.add(bannedItem);
            bannedHeldItemsJsonArray.add(item);
        }
        contraband.add("banned_held_items", bannedHeldItemsJsonArray);

        JsonArray bannedBagItemsJsonArray = new JsonArray();
        List<String> bannedBagItems = new ArrayList<>();
        if (contraband.has("banned_bag_items"))
            bannedBagItems = contraband.getAsJsonArray("banned_bag_items").asList().stream().map(JsonElement::getAsString).toList();
        for (String item : bannedBagItems) {
            Item bannedItem = Registries.ITEM.get(Identifier.of(item));
            banned_bag_items.add(bannedItem);
            bannedBagItemsJsonArray.add(item);
        }
        contraband.add("banned_bag_items", bannedBagItemsJsonArray);

        raidDetails.add("contraband", contraband);

        JsonObject bossbars = new JsonObject();
        if (raidDetails.has("bossbars"))
            bossbars = raidDetails.getAsJsonObject("bossbars");

        if (bossbars.has("setup"))
            setup_bossbar = bossbars.get("setup").getAsString();
        bossbars.addProperty("setup", setup_bossbar);

        if (bossbars.has("fight"))
            fight_bossbar = bossbars.get("fight").getAsString();
        bossbars.addProperty("fight", fight_bossbar);

        if (bossbars.has("pre_catch"))
            pre_catch_bossbar = bossbars.get("pre_catch").getAsString();
        bossbars.addProperty("pre_catch", pre_catch_bossbar);

        if (bossbars.has("catch"))
            catch_bossbar = bossbars.get("catch").getAsString();
        bossbars.addProperty("catch", catch_bossbar);

        raidDetails.add("bossbars", bossbars);

        newRoot.add("raid_details", raidDetails);


        //------- Item Settings Section -------//

        Voucher categoryChoiceVoucher = nr.config().global_choice_voucher;
        Voucher categoryRandomVoucher = nr.config().global_random_voucher;
        Pass categoryPass = nr.config().global_pass;
        List<RaidBall> categoryBalls = new ArrayList<>();

        JsonObject itemSettings = new JsonObject();
        if (root.has("item_settings"))
            itemSettings = root.getAsJsonObject("item_settings");

        JsonObject categoryChoiceVoucherObject = new JsonObject();
        if (itemSettings.has("category_choice_voucher"))
            categoryChoiceVoucherObject = itemSettings.getAsJsonObject("category_choice_voucher");

        Item choiceVoucherItem = categoryChoiceVoucher.voucherItem();
        String choiceVoucherName = categoryChoiceVoucher.voucherName();
        List<String> choiceVoucherLore = categoryChoiceVoucher.voucherLore();
        ComponentChanges choiceVoucherData = categoryChoiceVoucher.voucherData();

        if (categoryChoiceVoucherObject.has("voucher_item"))
            choiceVoucherItem = Registries.ITEM.get(Identifier.of(categoryChoiceVoucherObject.get("voucher_item").getAsString()));
        categoryChoiceVoucherObject.addProperty("voucher_item", Registries.ITEM.getId(choiceVoucherItem).toString());

        if (categoryChoiceVoucherObject.has("voucher_name"))
            choiceVoucherName = categoryChoiceVoucherObject.get("voucher_name").getAsString();
        categoryChoiceVoucherObject.addProperty("voucher_name", choiceVoucherName);

        if (categoryChoiceVoucherObject.has("voucher_lore"))
            choiceVoucherLore = categoryChoiceVoucherObject.get("voucher_lore").getAsJsonArray().asList().stream().map(JsonElement::toString).toList();
        JsonArray choiceLoreArray = new JsonArray();
        for (String line : choiceVoucherLore) {
            choiceLoreArray.add(line);
        }
        categoryChoiceVoucherObject.add("voucher_lore", choiceLoreArray);

        if (categoryChoiceVoucherObject.has("voucher_data"))
            choiceVoucherData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, categoryChoiceVoucherObject.get("voucher_data")).getOrThrow().getFirst();
        categoryChoiceVoucherObject.add("voucher_data", ComponentChanges.CODEC.encode(choiceVoucherData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

        itemSettings.add("category_choice_voucher", categoryChoiceVoucherObject);

        categoryChoiceVoucher = new Voucher(choiceVoucherItem, choiceVoucherName, choiceVoucherLore, choiceVoucherData);

        JsonObject categoryRandomVoucherObject = new JsonObject();
        if (itemSettings.has("category_random_voucher"))
            categoryRandomVoucherObject = itemSettings.getAsJsonObject("category_random_voucher");

        Item randomVoucherItem = categoryRandomVoucher.voucherItem();
        String randomVoucherName = categoryRandomVoucher.voucherName();
        List<String> randomVoucherLore = categoryRandomVoucher.voucherLore();
        ComponentChanges randomVoucherData = categoryRandomVoucher.voucherData();

        if (categoryRandomVoucherObject.has("voucher_item"))
            randomVoucherItem = Registries.ITEM.get(Identifier.of(categoryRandomVoucherObject.get("voucher_item").getAsString()));
        categoryRandomVoucherObject.addProperty("voucher_item", Registries.ITEM.getId(randomVoucherItem).toString());

        if (categoryRandomVoucherObject.has("voucher_name"))
            randomVoucherName = categoryRandomVoucherObject.get("voucher_name").getAsString();
        categoryRandomVoucherObject.addProperty("voucher_name", randomVoucherName);

        if (categoryRandomVoucherObject.has("voucher_lore"))
            randomVoucherLore = categoryRandomVoucherObject.get("voucher_lore").getAsJsonArray().asList().stream().map(JsonElement::toString).toList();
        JsonArray randomLoreArray = new JsonArray();
        for (String line : randomVoucherLore) {
            randomLoreArray.add(line);
        }
        categoryRandomVoucherObject.add("voucher_lore", randomLoreArray);

        if (categoryRandomVoucherObject.has("voucher_data"))
            randomVoucherData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, categoryRandomVoucherObject.get("voucher_data")).getOrThrow().getFirst();
        categoryRandomVoucherObject.add("voucher_data", ComponentChanges.CODEC.encode(randomVoucherData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

        itemSettings.add("category_random_voucher", categoryRandomVoucherObject);

        categoryRandomVoucher = new Voucher(randomVoucherItem, randomVoucherName, randomVoucherLore, randomVoucherData);

        JsonObject categoryPassObject = new JsonObject();
        if (itemSettings.has("category_pass"))
            categoryPassObject = itemSettings.getAsJsonObject("category_pass");

        Item passItem = categoryPass.passItem();
        String passName = categoryPass.passName();
        List<String> passLore = categoryPass.passLore();
        ComponentChanges passData = categoryPass.passData();

        if (categoryPassObject.has("pass_item"))
            passItem = Registries.ITEM.get(Identifier.of(categoryPassObject.get("pass_item").getAsString()));
        categoryPassObject.addProperty("pass_item", Registries.ITEM.getId(passItem).toString());

        if (categoryPassObject.has("pass_name"))
            passName = categoryPassObject.get("pass_name").getAsString();
        categoryPassObject.addProperty("pass_name", passName);

        if (categoryPassObject.has("pass_lore"))
            passLore = categoryPassObject.get("pass_lore").getAsJsonArray().asList().stream().map(JsonElement::toString).toList();
        JsonArray passLoreArray = new JsonArray();
        for (String line : passLore) {
            passLoreArray.add(line);
        }
        categoryPassObject.add("pass_lore", passLoreArray);

        if (categoryPassObject.has("pass_data"))
            passData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, categoryPassObject.get("voucher_data")).getOrThrow().getFirst();
        categoryPassObject.add("pass_data", ComponentChanges.CODEC.encode(passData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

        itemSettings.add("category_pass", categoryPassObject);

        categoryPass = new Pass(passItem, passName, passLore, passData);

        JsonObject raidBalls = new JsonObject();
        if (itemSettings.has("raid_balls"))
            raidBalls = itemSettings.getAsJsonObject("raid_balls");

        for (String id : raidBalls.keySet()) {
            JsonObject ballObject = raidBalls.getAsJsonObject(id);
            Item pokeball = CobblemonItems.POKE_BALL;
            String pokeball_name = "<red> " + category_id + " Raid Ball";
            List<String> pokeball_lore = List.of("<gray>Use this to try and catch " + category_id + " bosses!");
            ComponentChanges pokeball_data = ComponentChanges.EMPTY;

            if (ballObject.has("pokeball"))
                pokeball = Registries.ITEM.get(Identifier.of(ballObject.get("pokeball").getAsString()));
            ballObject.addProperty("pokeball", Registries.ITEM.getId(pokeball).toString());

            if (ballObject.has("pokeball_name"))
                pokeball_name = ballObject.get("pokeball_name").getAsString();
            ballObject.addProperty("pokeball_name", pokeball_name);

            if (ballObject.has("pokeball_lore"))
                pokeball_lore = ballObject.get("pokeball_lore").getAsJsonArray().asList().stream().map(JsonElement::toString).toList();
            JsonArray pokeballLoreArray = new JsonArray();
            for (String line : pokeball_lore) {
                pokeballLoreArray.add(line);
            }
            ballObject.add("pokeball_lore", pokeballLoreArray);

            if (ballObject.has("pokeball_data"))
                pokeball_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, ballObject.get("pokeball_data")).getOrThrow().getFirst();
            ballObject.add("pokeball_data", ComponentChanges.CODEC.encode(pokeball_data, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

            categoryBalls.add(new RaidBall(id, pokeball, pokeball_name, pokeball_lore, pokeball_data));
        }

        List<DistributionSection> rewards = ConfigHelper.getDistributionSections(root, "", true);
        categories.add(new Category(
                category_id,
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
                categoryChoiceVoucher,
                categoryRandomVoucher,
                categoryPass,
                categoryBalls,
                rewards
        ));
    }

    public void loadBoss(String category_name, File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonObject root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
        JsonObject newRoot = new JsonObject();
        String fileName = file.getName().substring(0, file.getName().indexOf(".json"));

        String location = category_name + "/bosses/" + fileName;

        String bossId = fileName;
        if (root.has("boss_id"))
            bossId = root.get("boss_id").getAsString();
        newRoot.addProperty("boss_id", bossId);

        double globalWeight = 1;
        if (root.has("global_weight"))
            globalWeight = root.get("global_weight").getAsDouble();
        newRoot.addProperty("global_weight", globalWeight);

        double categoryWeight = 1;
        if (root.has("category_weight"))
            categoryWeight = root.get("category_weight").getAsDouble();
        newRoot.addProperty("category_weight", categoryWeight);

        // Pokemon Details

        Species species = null;
        int level = 50;
        Map<String, Double> features = new HashMap<>();
        Map<Ability, Double> abilities = new HashMap<>();
        Map<Nature, Double> natures = new HashMap<>();
        Map<Gender, Double> genders = new HashMap<>();
        boolean shiny = false;
        float scale = 1.0f;
        Item heldItem = Items.AIR;
        ComponentChanges heldItemData = ComponentChanges.EMPTY;
        List<MoveTemplate> moves = new ArrayList<>();
        IVs ivs = IVs.createRandomIVs(0);
        EVs evs = EVs.createEmpty();

        JsonObject pokemonDetails = new JsonObject();
        if (root.has("pokemon_details"))
            pokemonDetails = root.getAsJsonObject("pokemon_details");

        if (pokemonDetails.has("species"))
            species = PokemonSpecies.INSTANCE.getByName(pokemonDetails.get("species").getAsString());

        if (species == null) {
            NovaRaids.LOGGER.warn("[NovaRaids] Invalid species: {}. Using default (bulbasaur)", pokemonDetails.get("species").getAsString());
            species = PokemonSpecies.INSTANCE.getByName("bulbasaur");
        }

        if (species == null) {
            NovaRaids.LOGGER.error("[NovaRaids] Default species was invalid. Using random.");
            species = PokemonSpecies.INSTANCE.random();
        }

        pokemonDetails.addProperty("species", species.getName());

        if (pokemonDetails.has("level"))
            level = pokemonDetails.get("level").getAsInt();
        pokemonDetails.addProperty("level", level);

        JsonArray featuresArray = new JsonArray();
        if (pokemonDetails.has("features")) {
            if (pokemonDetails.get("features").isJsonPrimitive()) {
                String oldFeatures = pokemonDetails.get("features").getAsString();
                features.put(oldFeatures, 1.0);
            } else {
                featuresArray = pokemonDetails.get("features").getAsJsonArray();
            }
        }

        for (JsonElement featureElement : featuresArray) {
            JsonObject featureObject = featureElement.getAsJsonObject();
            if (!featureObject.has("feature")) continue;

            String feature = featureObject.get("feature").getAsString();

            double weight = 1.0;
            if (featureObject.has("weight"))
                weight = featureObject.get("weight").getAsDouble();

            features.put(feature, weight);
        }

        featuresArray = new JsonArray();
        for (Map.Entry<String, Double> entry : features.entrySet()) {
            JsonObject featureObject = new JsonObject();
            featureObject.addProperty("feature", entry.getKey());
            featureObject.addProperty("weight", entry.getValue());
            featuresArray.add(featureObject);
        }
        pokemonDetails.add("features", featuresArray);

        JsonArray abilitiesArray = new JsonArray();
        if (pokemonDetails.has("abilities"))
            abilitiesArray = pokemonDetails.get("abilities").getAsJsonArray();
        else if (pokemonDetails.has("ability"))
            abilitiesArray = pokemonDetails.get("ability").getAsJsonArray();

        for (JsonElement abilityElement : abilitiesArray) {
            JsonObject abilityObject = abilityElement.getAsJsonObject();
            if (!abilityObject.has("ability")) continue;
            String ability = abilityObject.get("ability").getAsString();
            double weight = 1.0;
            if (abilityObject.has("weight"))
                weight = abilityObject.get("weight").getAsDouble();

            AbilityTemplate template = Abilities.INSTANCE.get(ability);
            if (template == null) {
                template = Abilities.INSTANCE.first();
                NovaRaids.LOGGER.warn("[NovaRaids] Invalid ability: {}. Using default ({}).", ability, template.getName());
            }

            abilities.put(template.create(false, Priority.LOWEST), weight);
        }

        abilitiesArray = new JsonArray();
        for (Map.Entry<Ability, Double> entry : abilities.entrySet()) {
            JsonObject abilityObject = new JsonObject();
            abilityObject.addProperty("ability", entry.getKey().getTemplate().getName());
            abilityObject.addProperty("weight", entry.getValue());
            abilitiesArray.add(abilityObject);
        }
        pokemonDetails.add("abilities", abilitiesArray);
        pokemonDetails.remove("ability");

        JsonArray naturesArray = new JsonArray();
        if (pokemonDetails.has("natures"))
            naturesArray = pokemonDetails.get("natures").getAsJsonArray();
        else if (pokemonDetails.has("nature"))
            naturesArray = pokemonDetails.get("nature").getAsJsonArray();


        for (JsonElement natureElement : naturesArray) {
            JsonObject natureObject = natureElement.getAsJsonObject();
            if (!natureObject.has("nature")) continue;
            String natureStr = natureObject.get("nature").getAsString();
            double weight = 1.0;
            if (natureObject.has("weight"))
                weight = natureObject.get("weight").getAsDouble();

            Nature nature = Natures.INSTANCE.getNature(natureStr);
            if (nature == null) {
                nature = Natures.INSTANCE.getSERIOUS();
                NovaRaids.LOGGER.warn("[NovaRaids] Invalid nature: {}. Using default ({}).", natureStr, nature.getName());
            }

            natures.put(nature, weight);
        }

        naturesArray = new JsonArray();
        for (Map.Entry<Nature, Double> entry : natures.entrySet()) {
            JsonObject natureObject = new JsonObject();
            natureObject.addProperty("nature", entry.getKey().getName().getPath());
            natureObject.addProperty("weight", entry.getValue());
            naturesArray.add(natureObject);
        }
        pokemonDetails.add("natures", abilitiesArray);
        pokemonDetails.remove("nature");

        JsonArray gendersArray = new JsonArray();
        if (pokemonDetails.has("genders"))
            gendersArray = pokemonDetails.get("genders").getAsJsonArray();
        else if (pokemonDetails.has("gender"))
            gendersArray = pokemonDetails.get("gender").getAsJsonArray();


        for (JsonElement genderElement : gendersArray) {
            JsonObject genderObject = genderElement.getAsJsonObject();
            if (!genderObject.has("nature")) continue;
            String genderStr = genderObject.get("nature").getAsString();
            double weight = 1.0;
            if (genderObject.has("weight"))
                weight = genderObject.get("weight").getAsDouble();

            Gender gender = Gender.GENDERLESS;
            try {
                gender = Gender.valueOf(genderStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                NovaRaids.LOGGER.warn("[NovaRaids] Invalid gender: {}. Using default (Genderless).", genderStr);
            }

            genders.put(gender, weight);
        }

        gendersArray = new JsonArray();
        for (Map.Entry<Gender, Double> entry : genders.entrySet()) {
            JsonObject genderObject = new JsonObject();
            genderObject.addProperty("gender", entry.getKey().asString());
            genderObject.addProperty("weight", entry.getValue());
            gendersArray.add(genderObject);
        }
        pokemonDetails.add("genders", gendersArray);
        pokemonDetails.remove("gender");

        if (pokemonDetails.has("shiny"))
            shiny = pokemonDetails.get("shiny").getAsBoolean();
        pokemonDetails.addProperty("shiny", shiny);

        if (pokemonDetails.has("scale"))
            scale = pokemonDetails.get("scale").getAsInt();
        pokemonDetails.addProperty("scale", scale);

        if (pokemonDetails.has("held_item"))
            heldItem = Registries.ITEM.get(Identifier.of(pokemonDetails.get("held_item").getAsString()));
        pokemonDetails.addProperty("held_item", heldItem.equals(Items.AIR) ? "" : Registries.ITEM.getId(heldItem).toString());

        if (pokemonDetails.has("held_item_data"))
            heldItemData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, pokemonDetails.get("held_item_data")).getOrThrow().getFirst();
        pokemonDetails.add("held_item_data", ComponentChanges.CODEC.encode(heldItemData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

        List<String> movesArray = new ArrayList<>();
        if (pokemonDetails.has("moves"))
            movesArray = pokemonDetails.get("moves").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        for (String move : movesArray) {
            MoveTemplate template = Moves.INSTANCE.getByNameOrDummy(move);
            if (template.getNum() == -1) {
                NovaRaids.LOGGER.warn("[NovaRaids] Invalid move: {}. Creating dummy move", move);
                continue;
            }
            moves.add(template);
        }

        JsonArray jsonMoveArray = new JsonArray();
        for (MoveTemplate move : moves) {
            jsonMoveArray.add(move.getName());
        }
        pokemonDetails.add("moves", jsonMoveArray);

        JsonObject ivsObject = new JsonObject();
        if (pokemonDetails.has("ivs"))
            ivsObject = pokemonDetails.get("ivs").getAsJsonObject();

        if (ivsObject.has("hp"))
            ivs.set(Stats.HP, Math.clamp(ivsObject.get("hp").getAsInt(), 0, IVs.MAX_VALUE));
        ivsObject.addProperty("hp", ivs.get(Stats.HP));

        if (ivsObject.has("atk"))
            ivs.set(Stats.ATTACK, Math.clamp(ivsObject.get("atk").getAsInt(), 0, IVs.MAX_VALUE));
        ivsObject.addProperty("atk", ivs.get(Stats.ATTACK));

        if (ivsObject.has("def"))
            ivs.set(Stats.DEFENCE, Math.clamp(ivsObject.get("def").getAsInt(), 0, IVs.MAX_VALUE));
        ivsObject.addProperty("def", ivs.get(Stats.DEFENCE));

        if (ivsObject.has("sp_atk"))
            ivs.set(Stats.SPECIAL_ATTACK, Math.clamp(ivsObject.get("sp_atk").getAsInt(), 0, IVs.MAX_VALUE));
        ivsObject.addProperty("sp_atk", ivs.get(Stats.ATTACK));

        if (ivsObject.has("sp_def"))
            ivs.set(Stats.SPECIAL_DEFENCE, Math.clamp(ivsObject.get("sp_def").getAsInt(), 0, IVs.MAX_VALUE));
        ivsObject.addProperty("sp_def", ivs.get(Stats.SPEED));

        if (ivsObject.has("spd"))
            ivs.set(Stats.SPEED, Math.clamp(ivsObject.get("spd").getAsInt(), 0, IVs.MAX_VALUE));
        ivsObject.addProperty("spd", ivs.get(Stats.SPEED));

        pokemonDetails.add("ivs", ivsObject);

        JsonObject evsObject = new JsonObject();
        if (pokemonDetails.has("evs"))
            evsObject = pokemonDetails.get("evs").getAsJsonObject();

        if (evsObject.has("hp"))
            evs.set(Stats.HP, Math.clamp(evsObject.get("hp").getAsInt(), 0, EVs.MAX_STAT_VALUE));
        evsObject.addProperty("hp", evs.get(Stats.HP));

        if (evsObject.has("atk"))
            evs.set(Stats.ATTACK, Math.clamp(evsObject.get("atk").getAsInt(), 0, EVs.MAX_STAT_VALUE));
        evsObject.addProperty("atk", evs.get(Stats.ATTACK));

        if (evsObject.has("def"))
            evs.set(Stats.DEFENCE, Math.clamp(evsObject.get("def").getAsInt(), 0, EVs.MAX_STAT_VALUE));
        evsObject.addProperty("def", evs.get(Stats.DEFENCE));

        if (evsObject.has("sp_atk"))
            evs.set(Stats.SPECIAL_ATTACK, Math.clamp(evsObject.get("sp_atk").getAsInt(), 0, EVs.MAX_STAT_VALUE));
        evsObject.addProperty("sp_atk", evs.get(Stats.SPEED));

        if (evsObject.has("sp_def"))
            evs.set(Stats.SPECIAL_DEFENCE, Math.clamp(evsObject.get("sp_def").getAsInt(), 0, EVs.MAX_STAT_VALUE));
        evsObject.addProperty("sp_def", evs.get(Stats.SPEED));

        if (evsObject.has("spd"))
            evs.set(Stats.SPEED, Math.clamp(evsObject.get("spd").getAsInt(), 0, EVs.MAX_STAT_VALUE));
        evsObject.addProperty("spd", evs.get(Stats.SPEED));

        pokemonDetails.add("evs", evsObject);

        newRoot.add("pokemon_details", pokemonDetails);


        PokemonDetails pokemonInfo = new PokemonDetails(
                species,
                level,
                features,
                abilities,
                natures,
                genders,
                shiny,
                scale,
                heldItem,
                heldItemData,
                moves,
                ivs,
                evs
        );

        // Boss Details
        String displayName = bossId;
        int baseHealth = 1000;
        int healthIncreasePerPlayer = 0;
        boolean applyGlowing = false;
        Map<String, Double> locations = new HashMap<>();

        JsonObject bossDetails = new JsonObject();
        if (root.has("boss_details"))
            bossDetails = root.get("boss_details").getAsJsonObject();

        if (bossDetails.has("display_name"))
            displayName = bossDetails.get("display_name").getAsString();
        bossDetails.addProperty("display_name", displayName);

        if (bossDetails.has("base_health"))
            baseHealth = bossDetails.get("base_health").getAsInt();
        bossDetails.addProperty("base_health", baseHealth);

        if (bossDetails.has("health_increase_per_player"))
            healthIncreasePerPlayer = bossDetails.get("health_increase_per_player").getAsInt();
        bossDetails.addProperty("health_increase_per_player", healthIncreasePerPlayer);

        if (bossDetails.has("apply_glowing"))
            applyGlowing = bossDetails.get("apply_glowing").getAsBoolean();
        bossDetails.addProperty("apply_glowing", applyGlowing);

        JsonArray locationsArray = new JsonArray();
        if (bossDetails.has("locations"))
            locationsArray = bossDetails.get("locations").getAsJsonArray();

        for (JsonElement locationElement : locationsArray) {
            JsonObject locationObject = locationElement.getAsJsonObject();
            if (!locationObject.has("location")) continue;
            double weight = 1.0;
            if (locationObject.has("weight"))
                weight = locationObject.get("weight").getAsDouble();
            locations.put(locationObject.get("location").getAsString(), weight);
        }

        if (locations.isEmpty()) {
            try {
                String defaultLocation = NovaRaids.INSTANCE.locationsConfig().locations.getFirst().id();
                locations.put(defaultLocation, 1.0);
                NovaRaids.LOGGER.error("[NovaRaids] No locations in boss details of boss, {}. Setting to default ({}). This will .", bossId, defaultLocation);
            } catch (NoSuchElementException e) {
                NovaRaids.LOGGER.error("[NovaRaids] No locations to default to.. this should stop your server from crashing but please fix your locations and {} boss config!", bossId);
            }
        }

        locationsArray = new JsonArray();
        for (Map.Entry<String, Double> entry : locations.entrySet()) {
            JsonObject locationObject = new JsonObject();
            locationObject.addProperty("location", entry.getKey());
            locationObject.addProperty("weight", entry.getValue());
            locationsArray.add(locationObject);
        }
        bossDetails.add("locations", locationsArray);

        newRoot.add("boss_details", bossDetails);

        // Item Settings
        boolean allowGlobalPokeballs = true;
        boolean allowCategoryPokeballs = true;
        Voucher bossVoucher = nr.config().default_voucher;
        Pass bossPass = nr.config().default_pass;
        List<RaidBall> bossBalls = new ArrayList<>();
        if (ConfigHelper.checkProperty(root, "item_settings", location)) {
            JsonObject item_settings = root.get("item_settings").getAsJsonObject();
            if (ConfigHelper.checkProperty(item_settings, "allow_global_pokeballs", location)) {
                allowGlobalPokeballs = item_settings.get("allow_global_pokeballs").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(item_settings, "allow_category_pokeballs", location)) {
                allowCategoryPokeballs = item_settings.get("allow_category_pokeballs").getAsBoolean();
            }
            if (ConfigHelper.checkProperty(item_settings, "boss_voucher", location)) {
                Item voucher_item = nr.config().default_voucher.voucherItem();
                String voucher_name = nr.config().default_voucher.voucherName();
                List<String> voucher_lore = nr.config().default_voucher.voucherLore();
                ComponentChanges voucher_data = nr.config().default_voucher.voucherData();
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
                bossVoucher = new Voucher(voucher_item, voucher_name, voucher_lore, voucher_data);
            }
            if (ConfigHelper.checkProperty(item_settings, "boss_pass", location)) {
                Item pass_item = nr.config().default_pass.passItem();
                String pass_name = nr.config().default_pass.passName();
                List<String> pass_lore = nr.config().default_pass.passLore();
                ComponentChanges pass_data = nr.config().default_pass.passData();
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
                bossPass = new Pass(pass_item, pass_name, pass_lore, pass_data);
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
                    bossBalls.add(new RaidBall(ball_id, item, name, lore, data));
                }
            }
        }

        ItemSettings itemSettings = new ItemSettings(
                allowGlobalPokeballs,
                allowCategoryPokeballs,
                bossVoucher,
                bossPass,
                bossBalls
        );

        // Raid Details
        int minimum_level = 1;
        int maximum_level = 100;
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
        if (ConfigHelper.checkProperty(root, "raid_details", location)) {
            JsonObject raid_details = root.get("raid_details").getAsJsonObject();
            if (ConfigHelper.checkProperty(raid_details, "minimum_level", location)) {
                minimum_level = raid_details.get("minimum_level").getAsInt();
            }
            if (ConfigHelper.checkProperty(raid_details, "maximum_level", location)) {
                maximum_level = raid_details.get("maximum_level").getAsInt();
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
            rewards = ConfigHelper.getDistributionSections(raid_details, location, false);
        } else {
            throw new NullPointerException("Boss must have raid details!");
        }

        RaidDetails raidDetails = new RaidDetails(
                minimum_level,
                maximum_level,
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

        if (ConfigHelper.checkProperty(root, "catch_settings", location)) {
            JsonObject catch_settings = root.get("catch_settings").getAsJsonObject();
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
                bossId,
                category_name,
                globalWeight,
                categoryWeight,
                pokemonInfo,
                displayName,
                baseHealth,
                healthIncreasePerPlayer,
                applyGlowing,
                locations,
                itemSettings,
                raidDetails,
                catch_settings
        ));
    }

    public Boss getRandomBoss(String category) {
        double total_weight = 0;
        for (Boss boss : bosses) {
            if (boss.categoryId().equalsIgnoreCase(category)) {
                total_weight += boss.categoryWeight();
            }
        }

        if (total_weight > 0) {
            double random_weight = new Random().nextDouble(total_weight);
            total_weight = 0;
            for (Boss boss : bosses) {
                if (boss.categoryId().equalsIgnoreCase(category)) {
                    total_weight += boss.categoryWeight();
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
            total_weight += boss.globalWeight();
        }

        if (total_weight > 0) {
            double random_weight = new Random().nextDouble(total_weight);
            total_weight = 0;
            for (Boss boss : bosses) {
                total_weight += boss.globalWeight();
                if (random_weight < total_weight) {
                    return boss;
                }
            }
        }
        return null;
    }

    public Boss getBoss(String id) {
        for (Boss boss : bosses) {
            if (boss.bossId().equalsIgnoreCase(id)) {
                return boss;
            }
        }
        return null;
    }

    public Category getCategory(String id) {
        for (Category category : categories) {
            if (category.id().equalsIgnoreCase(id)) {
                return category;
            }
        }
        return null;
    }
}
