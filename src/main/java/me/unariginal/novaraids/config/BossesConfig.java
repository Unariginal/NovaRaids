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
import com.google.gson.*;
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
                String categoryName = file.getName();
                for (File bossFile : Objects.requireNonNull(file.listFiles())) {
                    if (bossFile.isDirectory()) {
                        if (bossFile.getName().equals("bosses")) {
                            for (File boss : Objects.requireNonNull(bossFile.listFiles())) {
                                if (boss.getName().endsWith(".json")) {
                                    loadBoss(categoryName, boss);
                                }
                            }
                        }
                    } else if (bossFile.getName().equalsIgnoreCase("settings.json")) {
                        loadSettings(categoryName, bossFile);
                    }
                }
            }
        }
    }

    public void loadSettings(String categoryId, File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonObject root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
        JsonObject newRoot = new JsonObject();

        String categoryName = categoryId;
        if (root.has("category_name"))
            categoryName = root.get("category_name").getAsString();
        newRoot.addProperty("category_name", categoryName);


        //------- Raid Details Section -------//

        boolean requirePass = false;
        int minPlayers = 0;
        int maxPlayers = -1;
        List<Species> bannedSpecies = new ArrayList<>();
        List<Move> bannedMoves = new ArrayList<>();
        List<Ability> bannedAbilities = new ArrayList<>();
        List<Item> bannedHeldItems = new ArrayList<>();
        List<Item> bannedBagItems = new ArrayList<>();
        String setupBossbar = "setup_phase_example";
        String fightBossbar = "fight_phase_example";
        String preCatchBossbar = "pre_catch_phase_example";
        String catchBossbar = "catch_phase_example";

        JsonObject raidDetails = new JsonObject();
        if (root.has("raid_details"))
            raidDetails = root.get("raid_details").getAsJsonObject();

        if (raidDetails.has("require_pass"))
            requirePass = raidDetails.get("require_pass").getAsBoolean();
        raidDetails.addProperty("require_pass", requirePass);

        JsonObject playerCount = new JsonObject();
        if (raidDetails.has("player_count"))
            playerCount = raidDetails.get("player_count").getAsJsonObject();

        if (playerCount.has("min"))
            minPlayers = playerCount.get("min").getAsInt();
        playerCount.addProperty("min", minPlayers);

        if (playerCount.has("max"))
            maxPlayers = playerCount.get("max").getAsInt();
        playerCount.addProperty("max", maxPlayers);

        raidDetails.add("player_count", playerCount);

        JsonObject contraband = new JsonObject();
        if (raidDetails.has("contraband"))
            contraband = raidDetails.get("contraband").getAsJsonObject();

        JsonArray bannedPokemonJsonArray = new JsonArray();
        List<String> bannedPokemonRaw = new ArrayList<>();
        if (contraband.has("banned_pokemon"))
            bannedPokemonRaw = contraband.getAsJsonArray("banned_pokemon").asList().stream().map(JsonElement::getAsString).toList();
        for (String pokemon : bannedPokemonRaw) {
            Species species = PokemonSpecies.INSTANCE.getByName(pokemon);
            if (species != null) bannedSpecies.add(species);
            else NovaRaids.LOGGER.warn("[NovaRaids] Contraband species {} not found in !{}", pokemon, file.getPath());
            bannedPokemonJsonArray.add(pokemon);
        }
        contraband.add("banned_pokemon", bannedPokemonJsonArray);

        JsonArray bannedMovesJsonArray = new JsonArray();
        List<String> bannedMovesRaw = new ArrayList<>();
        if (contraband.has("banned_moves"))
            bannedMovesRaw = contraband.getAsJsonArray("banned_moves").asList().stream().map(JsonElement::getAsString).toList();
        for (String move : bannedMovesRaw) {
            MoveTemplate template = Moves.INSTANCE.getByName(move);
            if (template != null) bannedMoves.add(template.create());
            else NovaRaids.LOGGER.warn("[NovaRaids] Contraband move {} not found in !{}", move, file.getPath());
            bannedMovesJsonArray.add(move);
        }
        contraband.add("banned_moves", bannedMovesJsonArray);

        JsonArray bannedAbilitiesJsonArray = new JsonArray();
        List<String> bannedAbilitiesRaw = new ArrayList<>();
        if (contraband.has("banned_abilities"))
            bannedAbilitiesRaw = contraband.getAsJsonArray("banned_abilities").asList().stream().map(JsonElement::getAsString).toList();
        for (String ability : bannedAbilitiesRaw) {
            AbilityTemplate abilityTemplate = Abilities.INSTANCE.get(ability);
            if (abilityTemplate != null) bannedAbilities.add(abilityTemplate.create(false, Priority.LOWEST));
            else NovaRaids.LOGGER.warn("[NovaRaids] Contraband ability {} not found in !{}", ability, file.getPath());
            bannedAbilitiesJsonArray.add(ability);
        }
        contraband.add("banned_abilities", bannedAbilitiesJsonArray);

        JsonArray bannedHeldItemsJsonArray = new JsonArray();
        List<String> bannedHeldItemsRaw = new ArrayList<>();
        if (contraband.has("banned_held_items"))
            bannedHeldItemsRaw = contraband.getAsJsonArray("banned_held_items").asList().stream().map(JsonElement::getAsString).toList();
        for (String item : bannedHeldItemsRaw) {
            Item bannedItem = Registries.ITEM.get(Identifier.of(item));
            bannedHeldItems.add(bannedItem);
            bannedHeldItemsJsonArray.add(item);
        }
        contraband.add("banned_held_items", bannedHeldItemsJsonArray);

        JsonArray bannedBagItemsJsonArray = new JsonArray();
        List<String> bannedBagItemsRaw = new ArrayList<>();
        if (contraband.has("banned_bag_items"))
            bannedBagItemsRaw = contraband.getAsJsonArray("banned_bag_items").asList().stream().map(JsonElement::getAsString).toList();
        for (String item : bannedBagItemsRaw) {
            Item bannedItem = Registries.ITEM.get(Identifier.of(item));
            bannedBagItems.add(bannedItem);
            bannedBagItemsJsonArray.add(item);
        }
        contraband.add("banned_bag_items", bannedBagItemsJsonArray);

        raidDetails.add("contraband", contraband);

        JsonObject bossbars = new JsonObject();
        if (raidDetails.has("bossbars"))
            bossbars = raidDetails.getAsJsonObject("bossbars");

        if (bossbars.has("setup"))
            setupBossbar = bossbars.get("setup").getAsString();
        bossbars.addProperty("setup", setupBossbar);

        if (bossbars.has("fight"))
            fightBossbar = bossbars.get("fight").getAsString();
        bossbars.addProperty("fight", fightBossbar);

        if (bossbars.has("pre_catch"))
            preCatchBossbar = bossbars.get("pre_catch").getAsString();
        bossbars.addProperty("pre_catch", preCatchBossbar);

        if (bossbars.has("catch"))
            catchBossbar = bossbars.get("catch").getAsString();
        bossbars.addProperty("catch", catchBossbar);

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
            choiceVoucherLore = categoryChoiceVoucherObject.get("voucher_lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
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
            randomVoucherLore = categoryRandomVoucherObject.get("voucher_lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
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
            passLore = categoryPassObject.get("pass_lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        JsonArray passLoreArray = new JsonArray();
        for (String line : passLore) {
            passLoreArray.add(line);
        }
        categoryPassObject.add("pass_lore", passLoreArray);

        if (categoryPassObject.has("pass_data"))
            passData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, categoryPassObject.get("pass_data")).getOrThrow().getFirst();
        categoryPassObject.add("pass_data", ComponentChanges.CODEC.encode(passData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

        itemSettings.add("category_pass", categoryPassObject);

        categoryPass = new Pass(passItem, passName, passLore, passData);

        JsonObject raidBalls = new JsonObject();
        if (itemSettings.has("raid_balls"))
            raidBalls = itemSettings.getAsJsonObject("raid_balls");

        for (String id : raidBalls.keySet()) {
            JsonObject ballObject = raidBalls.getAsJsonObject(id);
            Item pokeball = CobblemonItems.POKE_BALL;
            String pokeballName = "<red> " + categoryId + " Raid Ball";
            List<String> pokeballLore = List.of("<gray>Use this to try and catch " + categoryId + " bosses!");
            ComponentChanges pokeballData = ComponentChanges.EMPTY;

            if (ballObject.has("pokeball"))
                pokeball = Registries.ITEM.get(Identifier.of(ballObject.get("pokeball").getAsString()));
            ballObject.addProperty("pokeball", Registries.ITEM.getId(pokeball).toString());

            if (ballObject.has("pokeball_name"))
                pokeballName = ballObject.get("pokeball_name").getAsString();
            ballObject.addProperty("pokeball_name", pokeballName);

            if (ballObject.has("pokeball_lore"))
                pokeballLore = ballObject.get("pokeball_lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
            JsonArray pokeballLoreArray = new JsonArray();
            for (String line : pokeballLore) {
                pokeballLoreArray.add(line);
            }
            ballObject.add("pokeball_lore", pokeballLoreArray);

            if (ballObject.has("pokeball_data"))
                pokeballData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, ballObject.get("pokeball_data")).getOrThrow().getFirst();
            ballObject.add("pokeball_data", ComponentChanges.CODEC.encode(pokeballData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

            categoryBalls.add(new RaidBall(id, pokeball, pokeballName, pokeballLore, pokeballData));
        }

        newRoot.add("item_settings", itemSettings);

        // TODO: Fix distribution for new config parsing style thingy
        List<DistributionSection> rewards = List.of();
        JsonArray rewardDistributionArray = new JsonArray();
        if (root.has("reward_distribution"))
            rewards = ConfigHelper.getDistributionSections(root, "", true);
        newRoot.add("reward_distribution", rewardDistributionArray);

        categories.add(new Category(
                categoryId,
                categoryName,
                requirePass,
                minPlayers,
                maxPlayers,
                bannedSpecies,
                bannedMoves,
                bannedAbilities,
                bannedHeldItems,
                bannedBagItems,
                setupBossbar,
                fightBossbar,
                preCatchBossbar,
                catchBossbar,
                categoryChoiceVoucher,
                categoryRandomVoucher,
                categoryPass,
                categoryBalls,
                rewards
        ));

        file.delete();
        file.createNewFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(file);
        gson.toJson(newRoot, writer);
        writer.close();
    }

    public void loadBoss(String category_name, File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonObject root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
        JsonObject newRoot = new JsonObject();
        String fileName = file.getName().substring(0, file.getName().indexOf(".json"));

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
        int friendship = 50;
        IVs ivs = IVs.createRandomIVs(0);
        EVs evs = EVs.createEmpty();

        JsonObject pokemonDetails = new JsonObject();
        if (root.has("pokemon_details"))
            pokemonDetails = root.getAsJsonObject("pokemon_details");

        if (pokemonDetails.has("species"))
            species = PokemonSpecies.INSTANCE.getByName(pokemonDetails.get("species").getAsString().toLowerCase());

        if (species == null) {
            NovaRaids.LOGGER.warn("[NovaRaids] Invalid species: {}. Using default (bulbasaur)", pokemonDetails.get("species").getAsString().toLowerCase());
            species = PokemonSpecies.INSTANCE.getByName("bulbasaur");
        }

        if (species == null) {
            NovaRaids.LOGGER.error("[NovaRaids] Default species was invalid. Using random.");
            species = PokemonSpecies.INSTANCE.random();
        }

        pokemonDetails.addProperty("species", species.getResourceIdentifier().getPath());

        if (pokemonDetails.has("level"))
            level = pokemonDetails.get("level").getAsInt();
        pokemonDetails.addProperty("level", level);

        if (pokemonDetails.has("form"))
            pokemonDetails.remove("form");

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
        pokemonDetails.add("natures", naturesArray);
        pokemonDetails.remove("nature");

        JsonArray gendersArray = new JsonArray();
        if (pokemonDetails.has("genders"))
            gendersArray = pokemonDetails.get("genders").getAsJsonArray();
        else if (pokemonDetails.has("gender"))
            gendersArray = pokemonDetails.get("gender").getAsJsonArray();

        for (JsonElement genderElement : gendersArray) {
            JsonObject genderObject = genderElement.getAsJsonObject();
            if (!genderObject.has("gender")) continue;
            String genderStr = genderObject.get("gender").getAsString();
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

        if (pokemonDetails.has("friendship"))
            friendship = pokemonDetails.get("friendship").getAsInt();
        pokemonDetails.addProperty("friendship", friendship);

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
        ivsObject.addProperty("sp_atk", ivs.get(Stats.SPECIAL_ATTACK));

        if (ivsObject.has("sp_def"))
            ivs.set(Stats.SPECIAL_DEFENCE, Math.clamp(ivsObject.get("sp_def").getAsInt(), 0, IVs.MAX_VALUE));
        ivsObject.addProperty("sp_def", ivs.get(Stats.SPECIAL_DEFENCE));

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
        evsObject.addProperty("sp_atk", evs.get(Stats.SPECIAL_ATTACK));

        if (evsObject.has("sp_def"))
            evs.set(Stats.SPECIAL_DEFENCE, Math.clamp(evsObject.get("sp_def").getAsInt(), 0, EVs.MAX_STAT_VALUE));
        evsObject.addProperty("sp_def", evs.get(Stats.SPECIAL_DEFENCE));

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
                friendship,
                ivs,
                evs
        );

        // Boss Details
        String displayName = bossId;
        int baseHealth = 1000;
        int healthIncreasePerPlayer = 0;
        boolean applyGlowing = false;
        int aiSkillLevel = 3;
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

        if (bossDetails.has("ai_skill_level"))
            aiSkillLevel = bossDetails.get("ai_skill_level").getAsInt();
        bossDetails.addProperty("ai_skill_level", aiSkillLevel);

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

        JsonObject itemSettingsObject = new JsonObject();
        if (root.has("item_settings"))
            itemSettingsObject = root.get("item_settings").getAsJsonObject();

        if (itemSettingsObject.has("allow_global_pokeballs"))
            allowGlobalPokeballs = itemSettingsObject.get("allow_global_pokeballs").getAsBoolean();
        itemSettingsObject.addProperty("allow_global_pokeballs", allowGlobalPokeballs);

        if (itemSettingsObject.has("allow_category_pokeballs"))
            allowCategoryPokeballs = itemSettingsObject.get("allow_category_pokeballs").getAsBoolean();
        itemSettingsObject.addProperty("allow_category_pokeballs", allowCategoryPokeballs);

        Item voucherItem = nr.config().default_voucher.voucherItem();
        String voucherName = nr.config().default_voucher.voucherName();
        List<String> voucherLore = nr.config().default_voucher.voucherLore();
        ComponentChanges voucherData = nr.config().default_voucher.voucherData();

        JsonObject bossVoucherObject = new JsonObject();
        if (itemSettingsObject.has("boss_voucher"))
            bossVoucherObject = itemSettingsObject.get("boss_voucher").getAsJsonObject();

        if (bossVoucherObject.has("voucher_item"))
            voucherItem = Registries.ITEM.get(Identifier.of(bossVoucherObject.get("voucher_item").getAsString()));
        bossVoucherObject.addProperty("voucher_item", Registries.ITEM.getId(voucherItem).toString());

        if (bossVoucherObject.has("voucher_name"))
            voucherName = bossVoucherObject.get("voucher_name").getAsString();
        bossVoucherObject.addProperty("voucher_name", voucherName);

        if (bossVoucherObject.has("voucher_lore"))
            voucherLore = bossVoucherObject.getAsJsonArray("voucher_lore").asList().stream().map(JsonElement::getAsString).toList();
        JsonArray voucherLoreArray = new JsonArray();
        for (String line : voucherLore) {
            voucherLoreArray.add(line);
        }
        bossVoucherObject.add("voucher_lore", voucherLoreArray);

        if (bossVoucherObject.has("voucher_data"))
            voucherData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, bossVoucherObject.get("voucher_data")).getOrThrow().getFirst();
        bossVoucherObject.add("voucher_data", ComponentChanges.CODEC.encode(voucherData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

        itemSettingsObject.add("boss_voucher", bossVoucherObject);

        Item passItem = nr.config().default_pass.passItem();
        String passName = nr.config().default_pass.passName();
        List<String> passLore = nr.config().default_pass.passLore();
        ComponentChanges passData = nr.config().default_pass.passData();

        JsonObject bossPassObject = new JsonObject();
        if (itemSettingsObject.has("boss_pass"))
            bossPassObject = itemSettingsObject.get("boss_pass").getAsJsonObject();

        if (bossPassObject.has("pass_item"))
            passItem = Registries.ITEM.get(Identifier.of(bossPassObject.get("pass_item").getAsString()));
        bossPassObject.addProperty("pass_item", Registries.ITEM.getId(passItem).toString());

        if (bossPassObject.has("pass_name"))
            passName = bossPassObject.get("pass_name").getAsString();
        bossPassObject.addProperty("pass_name", passName);

        if (bossPassObject.has("pass_lore"))
            passLore = bossPassObject.getAsJsonArray("pass_lore").asList().stream().map(JsonElement::getAsString).toList();
        JsonArray passLoreArray = new JsonArray();
        for (String line : passLore) {
            passLoreArray.add(line);
        }
        bossPassObject.add("pass_lore", passLoreArray);

        if (bossPassObject.has("pass_data"))
            passData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, bossPassObject.get("pass_data")).getOrThrow().getFirst();
        bossPassObject.add("pass_data", ComponentChanges.CODEC.encode(passData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

        itemSettingsObject.add("boss_pass", bossPassObject);

        JsonObject raidBalls = new JsonObject();
        if (itemSettingsObject.has("raid_balls"))
            raidBalls = itemSettingsObject.getAsJsonObject("raid_balls");

        for (String id : raidBalls.keySet()) {
            JsonObject ballObject = raidBalls.getAsJsonObject(id);
            Item pokeball = CobblemonItems.POKE_BALL;
            String pokeballName = "<red> " + bossId + " Raid Ball";
            List<String> pokeballLore = List.of("<gray>Use this to try and catch " + bossId);
            ComponentChanges pokeballData = ComponentChanges.EMPTY;

            if (ballObject.has("pokeball"))
                pokeball = Registries.ITEM.get(Identifier.of(ballObject.get("pokeball").getAsString()));
            ballObject.addProperty("pokeball", Registries.ITEM.getId(pokeball).toString());

            if (ballObject.has("pokeball_name"))
                pokeballName = ballObject.get("pokeball_name").getAsString();
            ballObject.addProperty("pokeball_name", pokeballName);

            if (ballObject.has("pokeball_lore"))
                pokeballLore = ballObject.get("pokeball_lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
            JsonArray pokeballLoreArray = new JsonArray();
            for (String line : pokeballLore) {
                pokeballLoreArray.add(line);
            }
            ballObject.add("pokeball_lore", pokeballLoreArray);

            if (ballObject.has("pokeball_data"))
                pokeballData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, ballObject.get("pokeball_data")).getOrThrow().getFirst();
            ballObject.add("pokeball_data", ComponentChanges.CODEC.encode(pokeballData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

            bossBalls.add(new RaidBall(id, pokeball, pokeballName, pokeballLore, pokeballData));
        }

        newRoot.add("item_settings", itemSettingsObject);

        ItemSettings itemSettings = new ItemSettings(
                allowGlobalPokeballs,
                allowCategoryPokeballs,
                bossVoucher,
                bossPass,
                bossBalls
        );

        // Raid Details
        int minimumLevel = 1;
        int maximumLevel = 100;
        int setupPhaseTime = 120;
        int fightPhaseTime = 600;
        boolean doCatchPhase = true;
        int preCatchPhaseTime = -1;
        int catchPhaseTime = -1;
        boolean healPartyOnChallenge = false;
        List<Species> bannedSpecies = new ArrayList<>();
        List<Move> bannedMoves = new ArrayList<>();
        List<Ability> bannedAbilities = new ArrayList<>();
        List<Item> bannedHeldItems = new ArrayList<>();
        List<Item> bannedBagItems = new ArrayList<>();
        String setupBossbar = "setup_phase_example";
        String fightBossbar = "fight_phase_example";
        String preCatchBossbar = "pre_catch_phase_example";
        String catchBossbar = "catch_phase_example";
        List<DistributionSection> rewards = List.of();

        JsonObject raidDetailsObject = new JsonObject();
        if (root.has("raid_details"))
            raidDetailsObject = root.getAsJsonObject("raid_details");

        if (raidDetailsObject.has("minimum_level"))
            minimumLevel = raidDetailsObject.get("minimum_level").getAsInt();
        raidDetailsObject.addProperty("minimum_level", minimumLevel);

        if (raidDetailsObject.has("maximum_level"))
            maximumLevel = raidDetailsObject.get("maximum_level").getAsInt();
        raidDetailsObject.addProperty("maximum_level", maximumLevel);

        if (raidDetailsObject.has("setup_phase_time"))
            setupPhaseTime = raidDetailsObject.get("setup_phase_time").getAsInt();
        raidDetailsObject.addProperty("setup_phase_time", setupPhaseTime);

        if (raidDetailsObject.has("fight_phase_time"))
            fightPhaseTime = raidDetailsObject.get("fight_phase_time").getAsInt();
        raidDetailsObject.addProperty("fight_phase_time", fightPhaseTime);

        if (raidDetailsObject.has("do_catch_phase"))
            doCatchPhase = raidDetailsObject.get("do_catch_phase").getAsBoolean();
        raidDetailsObject.addProperty("do_catch_phase", doCatchPhase);

        if (raidDetailsObject.has("pre_catch_phase_time"))
            preCatchPhaseTime = raidDetailsObject.get("pre_catch_phase_time").getAsInt();
        raidDetailsObject.addProperty("pre_catch_phase_time", preCatchPhaseTime);

        if (raidDetailsObject.has("catch_phase_time"))
            catchPhaseTime = raidDetailsObject.get("catch_phase_time").getAsInt();
        raidDetailsObject.addProperty("catch_phase_time", catchPhaseTime);

        if (raidDetailsObject.has("heal_party_on_challenge"))
            healPartyOnChallenge = raidDetailsObject.get("heal_party_on_challenge").getAsBoolean();
        raidDetailsObject.addProperty("heal_party_on_challenge", healPartyOnChallenge);

        JsonObject contrabandObject = new JsonObject();
        if (raidDetailsObject.has("contraband"))
            contrabandObject = raidDetailsObject.get("contraband").getAsJsonObject();

        JsonArray bannedPokemonJsonArray = new JsonArray();
        List<String> bannedPokemonRaw = new ArrayList<>();
        if (contrabandObject.has("banned_pokemon"))
            bannedPokemonRaw = contrabandObject.getAsJsonArray("banned_pokemon").asList().stream().map(JsonElement::getAsString).toList();
        for (String pokemon : bannedPokemonRaw) {
            Species speciesParsed = PokemonSpecies.INSTANCE.getByName(pokemon);
            if (speciesParsed != null) bannedSpecies.add(speciesParsed);
            else NovaRaids.LOGGER.warn("[NovaRaids] Contraband species {} not found in !{}", pokemon, file.getPath());
            bannedPokemonJsonArray.add(pokemon);
        }
        contrabandObject.add("banned_pokemon", bannedPokemonJsonArray);

        JsonArray bannedMovesJsonArray = new JsonArray();
        List<String> bannedMovesRaw = new ArrayList<>();
        if (contrabandObject.has("banned_moves"))
            bannedMovesRaw = contrabandObject.getAsJsonArray("banned_moves").asList().stream().map(JsonElement::getAsString).toList();
        for (String move : bannedMovesRaw) {
            MoveTemplate template = Moves.INSTANCE.getByName(move);
            if (template != null) bannedMoves.add(template.create());
            else NovaRaids.LOGGER.warn("[NovaRaids] Contraband move {} not found in !{}", move, file.getPath());
            bannedMovesJsonArray.add(move);
        }
        contrabandObject.add("banned_moves", bannedMovesJsonArray);

        JsonArray bannedAbilitiesJsonArray = new JsonArray();
        List<String> bannedAbilitiesRaw = new ArrayList<>();
        if (contrabandObject.has("banned_abilities"))
            bannedAbilitiesRaw = contrabandObject.getAsJsonArray("banned_abilities").asList().stream().map(JsonElement::getAsString).toList();
        for (String ability : bannedAbilitiesRaw) {
            AbilityTemplate abilityTemplate = Abilities.INSTANCE.get(ability);
            if (abilityTemplate != null) bannedAbilities.add(abilityTemplate.create(false, Priority.LOWEST));
            else NovaRaids.LOGGER.warn("[NovaRaids] Contraband ability {} not found in !{}", ability, file.getPath());
            bannedAbilitiesJsonArray.add(ability);
        }
        contrabandObject.add("banned_abilities", bannedAbilitiesJsonArray);

        JsonArray bannedHeldItemsJsonArray = new JsonArray();
        List<String> bannedHeldItemsRaw = new ArrayList<>();
        if (contrabandObject.has("banned_held_items"))
            bannedHeldItemsRaw = contrabandObject.getAsJsonArray("banned_held_items").asList().stream().map(JsonElement::getAsString).toList();
        for (String item : bannedHeldItemsRaw) {
            Item bannedItem = Registries.ITEM.get(Identifier.of(item));
            bannedHeldItems.add(bannedItem);
            bannedHeldItemsJsonArray.add(item);
        }
        contrabandObject.add("banned_held_items", bannedHeldItemsJsonArray);

        JsonArray bannedBagItemsJsonArray = new JsonArray();
        List<String> bannedBagItemsRaw = new ArrayList<>();
        if (contrabandObject.has("banned_bag_items"))
            bannedBagItemsRaw = contrabandObject.getAsJsonArray("banned_bag_items").asList().stream().map(JsonElement::getAsString).toList();
        for (String item : bannedBagItemsRaw) {
            Item bannedItem = Registries.ITEM.get(Identifier.of(item));
            bannedBagItems.add(bannedItem);
            bannedBagItemsJsonArray.add(item);
        }
        contrabandObject.add("banned_bag_items", bannedBagItemsJsonArray);

        raidDetailsObject.add("contraband", contrabandObject);

        JsonObject bossbarsObject = new JsonObject();
        if (raidDetailsObject.has("bossbars"))
            bossbarsObject = raidDetailsObject.getAsJsonObject("bossbars");

        if (bossbarsObject.has("setup"))
            setupBossbar = bossbarsObject.get("setup").getAsString();
        bossbarsObject.addProperty("setup", setupBossbar);

        if (bossbarsObject.has("fight"))
            fightBossbar = bossbarsObject.get("fight").getAsString();
        bossbarsObject.addProperty("fight", fightBossbar);

        if (bossbarsObject.has("pre_catch"))
            preCatchBossbar = bossbarsObject.get("pre_catch").getAsString();
        bossbarsObject.addProperty("pre_catch", preCatchBossbar);

        if (bossbarsObject.has("catch"))
            catchBossbar = bossbarsObject.get("catch").getAsString();
        bossbarsObject.addProperty("catch", catchBossbar);

        raidDetailsObject.add("bossbars", bossbarsObject);

        // TODO: The thingy
        JsonArray rewardDistributionArray = new JsonArray();
        if (raidDetailsObject.has("reward_distribution"))
            rewards = ConfigHelper.getDistributionSections(raidDetailsObject, "", false);
        raidDetailsObject.add("reward_distribution", rewardDistributionArray);

        newRoot.add("raid_details", raidDetailsObject);

        RaidDetails raidDetails = new RaidDetails(
                minimumLevel,
                maximumLevel,
                setupPhaseTime,
                fightPhaseTime,
                doCatchPhase,
                preCatchPhaseTime,
                catchPhaseTime,
                healPartyOnChallenge,
                bannedSpecies,
                bannedMoves,
                bannedAbilities,
                bannedHeldItems,
                bannedBagItems,
                setupBossbar,
                fightBossbar,
                preCatchBossbar,
                catchBossbar,
                rewards
        );

        // Catch Settings
        Species speciesOverride;
        int levelOverride = 1;
        boolean keepFeatures = false;
        String featuresOverride = "";
        boolean keepScale = false;
        boolean keepHeldItem = false;
        boolean randomizeIVs = true;
        boolean keepEVs = false;
        boolean randomizeGender = true;
        boolean randomizeNature = true;
        boolean randomizeAbility = true;
        boolean resetMoves = true;
        int friendshipOverride = 50;
        List<CatchPlacement> catchPlacements = new ArrayList<>();

        JsonObject catchSettingsObject = new JsonObject();
        if (root.has("catch_settings"))
            catchSettingsObject = root.getAsJsonObject("catch_settings");

        String speciesOverrideString = species.getResourceIdentifier().getPath();
        if (catchSettingsObject.has("species_override"))
            speciesOverrideString = catchSettingsObject.get("species_override").getAsString();
        speciesOverride = PokemonSpecies.INSTANCE.getByName(speciesOverrideString.toLowerCase());

        if (speciesOverride == null) {
            speciesOverride = species;
            if (!speciesOverrideString.isEmpty())
                NovaRaids.LOGGER.warn("[NovaRaids] Unknown species for catch override: {}. In boss {}. Using boss species ({})", speciesOverrideString, bossId, species.getName());
        }
        catchSettingsObject.addProperty("species_override", speciesOverride.getResourceIdentifier().getPath());

        if (catchSettingsObject.has("level_override"))
            levelOverride = catchSettingsObject.get("level_override").getAsInt();
        catchSettingsObject.addProperty("level_override", levelOverride);

        if (catchSettingsObject.has("form_override"))
            catchSettingsObject.remove("form_override");

        if (catchSettingsObject.has("keep_features"))
            keepFeatures = catchSettingsObject.get("keep_features").getAsBoolean();
        catchSettingsObject.addProperty("keep_features", keepFeatures);

        if (catchSettingsObject.has("features_override"))
            featuresOverride = catchSettingsObject.get("features_override").getAsString();
        catchSettingsObject.addProperty("features_override", featuresOverride);

        if (catchSettingsObject.has("keep_scale"))
            keepScale = catchSettingsObject.get("keep_scale").getAsBoolean();
        catchSettingsObject.addProperty("keep_scale", keepScale);

        if (catchSettingsObject.has("keep_held_item"))
            keepHeldItem = catchSettingsObject.get("keep_held_item").getAsBoolean();
        catchSettingsObject.addProperty("keep_held_item", keepHeldItem);

        if (catchSettingsObject.has("randomize_ivs"))
            randomizeIVs = catchSettingsObject.get("randomize_ivs").getAsBoolean();
        catchSettingsObject.addProperty("randomize_ivs", randomizeIVs);

        if (catchSettingsObject.has("keep_evs"))
            keepEVs = catchSettingsObject.get("keep_evs").getAsBoolean();
        catchSettingsObject.addProperty("keep_evs", keepEVs);

        if (catchSettingsObject.has("randomize_gender"))
            randomizeGender = catchSettingsObject.get("randomize_gender").getAsBoolean();
        catchSettingsObject.addProperty("randomize_gender", randomizeGender);

        if (catchSettingsObject.has("randomize_nature"))
            randomizeNature = catchSettingsObject.get("randomize_nature").getAsBoolean();
        catchSettingsObject.addProperty("randomize_nature", randomizeNature);

        if (catchSettingsObject.has("randomize_ability"))
            randomizeAbility = catchSettingsObject.get("randomize_ability").getAsBoolean();
        catchSettingsObject.addProperty("randomize_ability", randomizeAbility);

        if (catchSettingsObject.has("reset_moves"))
            resetMoves = catchSettingsObject.get("reset_moves").getAsBoolean();
        catchSettingsObject.addProperty("reset_moves", resetMoves);

        if (catchSettingsObject.has("friendship_override"))
            friendshipOverride = catchSettingsObject.get("friendship_override").getAsInt();
        catchSettingsObject.addProperty("friendship_override", friendshipOverride);

        JsonArray placementsArray = new JsonArray();
        if (catchSettingsObject.has("places"))
            placementsArray = catchSettingsObject.getAsJsonArray("places");

        for (JsonElement place : placementsArray) {
            JsonObject placeObject = place.getAsJsonObject();

            if (!placeObject.has("place")) continue;

            String placeStr = placeObject.get("place").getAsString();
            boolean requireDamage = true;
            double shinyChance = 8192.0;
            int minPerfectIVs = 0;

            if (placeObject.has("require_damage"))
                requireDamage = placeObject.get("require_damage").getAsBoolean();
            placeObject.addProperty("require_damage", requireDamage);

            if (placeObject.has("shiny_chance"))
                shinyChance = placeObject.get("shiny_chance").getAsDouble();
            placeObject.addProperty("shiny_chance", shinyChance);

            if (placeObject.has("min_perfect_ivs"))
                minPerfectIVs = placeObject.get("min_perfect_ivs").getAsInt();
            placeObject.addProperty("min_perfect_ivs", minPerfectIVs);

            catchPlacements.add(new CatchPlacement(placeStr, requireDamage, shinyChance, minPerfectIVs));
        }

        if (catchPlacements.isEmpty()) catchPlacements.add(new CatchPlacement("participating", true, 8192, 0));

        newRoot.add("catch_settings", catchSettingsObject);

        CatchSettings catchSettings = new CatchSettings(
                speciesOverride,
                levelOverride,
                keepFeatures,
                featuresOverride,
                keepScale,
                keepHeldItem,
                randomizeIVs,
                keepEVs,
                randomizeGender,
                randomizeNature,
                randomizeAbility,
                resetMoves,
                friendshipOverride,
                catchPlacements
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
                aiSkillLevel,
                locations,
                itemSettings,
                raidDetails,
                catchSettings
        ));

        file.delete();
        file.createNewFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(file);
        gson.toJson(newRoot, writer);
        writer.close();
    }

    public Boss getRandomBoss(String category) {
        double totalWeight = 0;
        for (Boss boss : bosses) {
            if (boss.categoryId().equalsIgnoreCase(category)) {
                totalWeight += boss.categoryWeight();
            }
        }

        if (totalWeight > 0) {
            double randomWeight = new Random().nextDouble(totalWeight);
            totalWeight = 0;
            for (Boss boss : bosses) {
                if (boss.categoryId().equalsIgnoreCase(category)) {
                    totalWeight += boss.categoryWeight();
                    if (randomWeight < totalWeight) {
                        return boss;
                    }
                }
            }
        }
        return null;
    }

    public Boss getRandomBoss() {
        double totalWeight = 0;
        for (Boss boss : bosses) {
            totalWeight += boss.globalWeight();
        }

        if (totalWeight > 0) {
            double randomWeight = new Random().nextDouble(totalWeight);
            totalWeight = 0;
            for (Boss boss : bosses) {
                totalWeight += boss.globalWeight();
                if (randomWeight < totalWeight) {
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
