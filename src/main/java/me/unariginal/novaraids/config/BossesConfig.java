package me.unariginal.novaraids.config;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.types.tera.TeraType;
import com.cobblemon.mod.common.api.types.tera.TeraTypes;
import com.cobblemon.mod.common.pokemon.*;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.Contraband;
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
            NovaRaids.LOADED = false;
            NovaRaids.LOGGER.error("[NovaRaids] Failed to load bosses folder.", e);
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
        JsonObject root = new JsonObject();
        if (file.exists()) root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

        String categoryName = categoryId;
        if (root.has("category_name"))
            categoryName = root.get("category_name").getAsString();
        root.remove("category_name");
        root.addProperty("category_name", categoryName);

        //------- Raid Details Section -------//

        boolean requirePass = false;
        int minPlayers = 0;
        int maxPlayers = -1;
        String setupBossbar = "setup_phase_example";
        String fightBossbar = "fight_phase_example";
        String preCatchBossbar = "pre_catch_phase_example";
        String catchBossbar = "catch_phase_example";

        JsonObject raidDetails = new JsonObject();
        if (root.has("raid_details"))
            raidDetails = root.get("raid_details").getAsJsonObject();

        if (raidDetails.has("require_pass"))
            requirePass = raidDetails.get("require_pass").getAsBoolean();
        raidDetails.remove("require_pass");
        raidDetails.addProperty("require_pass", requirePass);

        JsonObject playerCount = new JsonObject();
        if (raidDetails.has("player_count"))
            playerCount = raidDetails.get("player_count").getAsJsonObject();

        if (playerCount.has("min"))
            minPlayers = playerCount.get("min").getAsInt();
        playerCount.remove("min");
        playerCount.addProperty("min", minPlayers);

        if (playerCount.has("max"))
            maxPlayers = playerCount.get("max").getAsInt();
        playerCount.remove("max");
        playerCount.addProperty("max", maxPlayers);

        raidDetails.remove("player_count");
        raidDetails.add("player_count", playerCount);

        JsonObject contrabandObject = new JsonObject();
        if (raidDetails.has("contraband"))
            contrabandObject = raidDetails.get("contraband").getAsJsonObject();

        Contraband contraband = ConfigHelper.getContraband(contrabandObject, file.getName());

        raidDetails.remove("contraband");
        raidDetails.add("contraband", contraband.contrabandObject());

        JsonObject bossbars = new JsonObject();
        if (raidDetails.has("bossbars"))
            bossbars = raidDetails.getAsJsonObject("bossbars");

        if (bossbars.has("setup"))
            setupBossbar = bossbars.get("setup").getAsString();
        bossbars.remove("setup");
        bossbars.addProperty("setup", setupBossbar);

        if (bossbars.has("fight"))
            fightBossbar = bossbars.get("fight").getAsString();
        bossbars.remove("fight");
        bossbars.addProperty("fight", fightBossbar);

        if (bossbars.has("pre_catch"))
            preCatchBossbar = bossbars.get("pre_catch").getAsString();
        bossbars.remove("pre_catch");
        bossbars.addProperty("pre_catch", preCatchBossbar);

        if (bossbars.has("catch"))
            catchBossbar = bossbars.get("catch").getAsString();
        bossbars.remove("catch");
        bossbars.addProperty("catch", catchBossbar);

        raidDetails.remove("bossbars");
        raidDetails.add("bossbars", bossbars);

        root.remove("raid_details");
        root.add("raid_details", raidDetails);


        //------- Item Settings Section -------//
        JsonObject itemSettings = new JsonObject();
        if (root.has("item_settings"))
            itemSettings = root.getAsJsonObject("item_settings");

        JsonObject categoryChoiceVoucherObject = new JsonObject();
        if (itemSettings.has("category_choice_voucher"))
            categoryChoiceVoucherObject = itemSettings.getAsJsonObject("category_choice_voucher");

        Voucher categoryChoiceVoucher = ConfigHelper.getVoucher(categoryChoiceVoucherObject, nr.config().defaultVoucher);

        itemSettings.remove("category_choice_voucher");
        itemSettings.add("category_choice_voucher", categoryChoiceVoucher.voucherObject());

        JsonObject categoryRandomVoucherObject = new JsonObject();
        if (itemSettings.has("category_random_voucher"))
            categoryRandomVoucherObject = itemSettings.getAsJsonObject("category_random_voucher");

        Voucher categoryRandomVoucher = ConfigHelper.getVoucher(categoryRandomVoucherObject, nr.config().defaultVoucher);

        itemSettings.remove("category_random_voucher");
        itemSettings.add("category_random_voucher", categoryRandomVoucher.voucherObject());

        JsonObject categoryPassObject = new JsonObject();
        if (itemSettings.has("category_pass"))
            categoryPassObject = itemSettings.getAsJsonObject("category_pass");

        Pass categoryPass = ConfigHelper.getPass(categoryPassObject, nr.config().defaultPass);

        itemSettings.remove("category_pass");
        itemSettings.add("category_pass", categoryPass.passObject());

        JsonObject raidBalls = new JsonObject();
        if (itemSettings.has("raid_balls"))
            raidBalls = itemSettings.getAsJsonObject("raid_balls");

        List<RaidBall> categoryBalls = ConfigHelper.getRaidBalls(raidBalls);

        for (RaidBall raidBall : categoryBalls) {
            raidBalls.remove(raidBall.id());
            raidBalls.add(raidBall.id(), raidBall.raidBallObject());
        }

        itemSettings.remove("raid_balls");
        itemSettings.add("raid_balls", raidBalls);

        root.remove("item_settings");
        root.add("item_settings", itemSettings);

        List<DistributionSection> rewards = new ArrayList<>();
        JsonArray rewardDistributionArray = new JsonArray();
        if (root.has("reward_distribution"))
            rewards = ConfigHelper.getDistributionSections(root, true);

        for (DistributionSection distributionSection : rewards) {
            rewardDistributionArray.add(distributionSection.distributionObject());
        }

        root.remove("reward_distribution");
        root.add("reward_distribution", rewardDistributionArray);

        categories.add(new Category(
                categoryId,
                categoryName,
                requirePass,
                minPlayers,
                maxPlayers,
                contraband,
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
        gson.toJson(root, writer);
        writer.close();
    }

    public void loadBoss(String category_name, File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonObject root = new JsonObject();
        if (file.exists()) root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

        String fileName = file.getName().substring(0, file.getName().indexOf(".json"));

        String bossId = fileName;
        if (root.has("boss_id"))
            bossId = root.get("boss_id").getAsString();
        root.remove(bossId);
        root.addProperty("boss_id", bossId);

        double globalWeight = 1;
        if (root.has("global_weight"))
            globalWeight = root.get("global_weight").getAsDouble();
        root.remove("global_weight");
        root.addProperty("global_weight", globalWeight);

        double categoryWeight = 1;
        if (root.has("category_weight"))
            categoryWeight = root.get("category_weight").getAsDouble();
        root.remove("category_weight");
        root.addProperty("category_weight", categoryWeight);

        // Pokemon Details
        Species species = null;
        int level = 50;
        Map<String, Double> features = new HashMap<>();
        Map<Ability, Double> abilities = new HashMap<>();
        Map<Nature, Double> natures = new HashMap<>();
        Map<Gender, Double> genders = new HashMap<>();
        Map<String, Double> gimmicks = new HashMap<>();
        boolean shiny = false;
        TeraType teraType = null;
        boolean gmaxFactor = false;
        int dynamaxLevel = 0;
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
            species = PokemonSpecies.getByName(pokemonDetails.get("species").getAsString().toLowerCase());

        if (species == null) {
            NovaRaids.LOGGER.warn("[NovaRaids] Invalid species: {}. Using default (bulbasaur)", pokemonDetails.get("species").getAsString().toLowerCase());
            species = PokemonSpecies.getByName("bulbasaur");
        }

        if (species == null) {
            NovaRaids.LOGGER.error("[NovaRaids] Default species was invalid. Using random.");
            species = PokemonSpecies.random();
        }

        pokemonDetails.remove("species");
        pokemonDetails.addProperty("species", species.getResourceIdentifier().getPath());

        if (pokemonDetails.has("level"))
            level = pokemonDetails.get("level").getAsInt();
        pokemonDetails.remove("level");
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

            featureObject.remove("feature");
            featureObject.addProperty("feature", entry.getKey());

            featureObject.remove("weight");
            featureObject.addProperty("weight", entry.getValue());

            featuresArray.add(featureObject);
        }
        pokemonDetails.remove("features");
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

            AbilityTemplate template = Abilities.get(ability);
            if (template == null) {
                template = Abilities.first();
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
        pokemonDetails.remove("abilities");
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

            Nature nature = Natures.getNature(natureStr);
            if (nature == null) {
                nature = Natures.SERIOUS;
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
        pokemonDetails.remove("natures");
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
        pokemonDetails.remove("genders");
        pokemonDetails.add("genders", gendersArray);
        pokemonDetails.remove("gender");

        JsonArray gimmicksArray = new JsonArray();
        if (pokemonDetails.has("gimmicks"))
            gimmicksArray = pokemonDetails.get("gimmicks").getAsJsonArray();

        for (JsonElement gimmickElement : gimmicksArray) {
            JsonObject gimmickObject = gimmickElement.getAsJsonObject();
            if (!gimmickObject.has("gimmick")) continue;
            String gimmickStr = gimmickObject.get("gimmick").getAsString();
            double weight = 1.0;
            if (gimmickObject.has("weight"))
                weight = gimmickObject.get("weight").getAsDouble();

            gimmicks.put(gimmickStr, weight);
        }

        gimmicksArray = new JsonArray();
        for (Map.Entry<String, Double> entry : gimmicks.entrySet()) {
            JsonObject gimmickObject = new JsonObject();
            gimmickObject.addProperty("gimmick", entry.getKey());
            gimmickObject.addProperty("weight", entry.getValue());
            gimmicksArray.add(gimmickObject);
        }
        pokemonDetails.remove("gimmicks");
        pokemonDetails.add("gimmicks", gimmicksArray);

        if (pokemonDetails.has("shiny"))
            shiny = pokemonDetails.get("shiny").getAsBoolean();
        pokemonDetails.remove("shiny");
        pokemonDetails.addProperty("shiny", shiny);

        if (pokemonDetails.has("tera_type"))
            teraType = TeraTypes.get(pokemonDetails.get("tera_type").getAsString().toLowerCase());
        pokemonDetails.remove("tera_type");
        if (teraType == null) pokemonDetails.addProperty("tera_type", "random");
        else pokemonDetails.addProperty("tera_type", teraType.showdownId());

        if (pokemonDetails.has("gmax_factor"))
            gmaxFactor = pokemonDetails.get("gmax_factor").getAsBoolean();
        pokemonDetails.remove("gmax_factor");
        pokemonDetails.addProperty("gmax_factor", gmaxFactor);

        if (pokemonDetails.has("dynamax_level"))
            dynamaxLevel = pokemonDetails.get("dynamax_level").getAsInt();
        pokemonDetails.remove("dynamax_level");
        pokemonDetails.addProperty("dynamax_level", dynamaxLevel);

        if (pokemonDetails.has("scale"))
            scale = pokemonDetails.get("scale").getAsInt();
        pokemonDetails.remove("scale");
        pokemonDetails.addProperty("scale", scale);

        if (pokemonDetails.has("held_item"))
            heldItem = Registries.ITEM.get(Identifier.of(pokemonDetails.get("held_item").getAsString()));
        pokemonDetails.remove("held_item");
        pokemonDetails.addProperty("held_item", heldItem.equals(Items.AIR) ? "" : Registries.ITEM.getId(heldItem).toString());

        if (pokemonDetails.has("held_item_data"))
            heldItemData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, pokemonDetails.get("held_item_data")).getOrThrow().getFirst();
        pokemonDetails.remove("held_item_data");
        pokemonDetails.add("held_item_data", ComponentChanges.CODEC.encode(heldItemData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

        List<String> movesArray = new ArrayList<>();
        if (pokemonDetails.has("moves"))
            movesArray = pokemonDetails.get("moves").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        for (String move : movesArray) {
            MoveTemplate template = Moves.getByNameOrDummy(move);
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
        pokemonDetails.remove("moves");
        pokemonDetails.add("moves", jsonMoveArray);

        if (pokemonDetails.has("friendship"))
            friendship = pokemonDetails.get("friendship").getAsInt();
        pokemonDetails.remove("friendship");
        pokemonDetails.addProperty("friendship", friendship);

        JsonObject ivsObject = new JsonObject();
        if (pokemonDetails.has("ivs"))
            ivsObject = pokemonDetails.get("ivs").getAsJsonObject();

        if (ivsObject.has("hp"))
            ivs.set(Stats.HP, Math.clamp(ivsObject.get("hp").getAsInt(), 0, IVs.MAX_VALUE));
        ivsObject.remove("hp");
        ivsObject.addProperty("hp", ivs.get(Stats.HP));

        if (ivsObject.has("atk"))
            ivs.set(Stats.ATTACK, Math.clamp(ivsObject.get("atk").getAsInt(), 0, IVs.MAX_VALUE));
        ivsObject.remove("atk");
        ivsObject.addProperty("atk", ivs.get(Stats.ATTACK));

        if (ivsObject.has("def"))
            ivs.set(Stats.DEFENCE, Math.clamp(ivsObject.get("def").getAsInt(), 0, IVs.MAX_VALUE));
        ivsObject.remove("def");
        ivsObject.addProperty("def", ivs.get(Stats.DEFENCE));

        if (ivsObject.has("sp_atk"))
            ivs.set(Stats.SPECIAL_ATTACK, Math.clamp(ivsObject.get("sp_atk").getAsInt(), 0, IVs.MAX_VALUE));
        ivsObject.remove("sp_atk");
        ivsObject.addProperty("sp_atk", ivs.get(Stats.SPECIAL_ATTACK));

        if (ivsObject.has("sp_def"))
            ivs.set(Stats.SPECIAL_DEFENCE, Math.clamp(ivsObject.get("sp_def").getAsInt(), 0, IVs.MAX_VALUE));
        ivsObject.remove("sp_def");
        ivsObject.addProperty("sp_def", ivs.get(Stats.SPECIAL_DEFENCE));

        if (ivsObject.has("spd"))
            ivs.set(Stats.SPEED, Math.clamp(ivsObject.get("spd").getAsInt(), 0, IVs.MAX_VALUE));
        ivsObject.remove("spd");
        ivsObject.addProperty("spd", ivs.get(Stats.SPEED));

        pokemonDetails.remove("ivs");
        pokemonDetails.add("ivs", ivsObject);

        JsonObject evsObject = new JsonObject();
        if (pokemonDetails.has("evs"))
            evsObject = pokemonDetails.get("evs").getAsJsonObject();

        if (evsObject.has("hp"))
            evs.set(Stats.HP, Math.clamp(evsObject.get("hp").getAsInt(), 0, EVs.MAX_STAT_VALUE));
        evsObject.remove("hp");
        evsObject.addProperty("hp", evs.get(Stats.HP));

        if (evsObject.has("atk"))
            evs.set(Stats.ATTACK, Math.clamp(evsObject.get("atk").getAsInt(), 0, EVs.MAX_STAT_VALUE));
        evsObject.remove("atk");
        evsObject.addProperty("atk", evs.get(Stats.ATTACK));

        if (evsObject.has("def"))
            evs.set(Stats.DEFENCE, Math.clamp(evsObject.get("def").getAsInt(), 0, EVs.MAX_STAT_VALUE));
        evsObject.remove("def");
        evsObject.addProperty("def", evs.get(Stats.DEFENCE));

        if (evsObject.has("sp_atk"))
            evs.set(Stats.SPECIAL_ATTACK, Math.clamp(evsObject.get("sp_atk").getAsInt(), 0, EVs.MAX_STAT_VALUE));
        evsObject.remove("sp_atk");
        evsObject.addProperty("sp_atk", evs.get(Stats.SPECIAL_ATTACK));

        if (evsObject.has("sp_def"))
            evs.set(Stats.SPECIAL_DEFENCE, Math.clamp(evsObject.get("sp_def").getAsInt(), 0, EVs.MAX_STAT_VALUE));
        evsObject.remove("sp_def");
        evsObject.addProperty("sp_def", evs.get(Stats.SPECIAL_DEFENCE));

        if (evsObject.has("spd"))
            evs.set(Stats.SPEED, Math.clamp(evsObject.get("spd").getAsInt(), 0, EVs.MAX_STAT_VALUE));
        evsObject.remove("spd");
        evsObject.addProperty("spd", evs.get(Stats.SPEED));

        pokemonDetails.remove("evs");
        pokemonDetails.add("evs", evsObject);

        root.remove("pokemon_details");
        root.add("pokemon_details", pokemonDetails);

        PokemonDetails pokemonInfo = new PokemonDetails(
                species,
                level,
                features,
                abilities,
                natures,
                genders,
                gimmicks,
                teraType,
                gmaxFactor,
                dynamaxLevel,
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
        boolean rerollFeaturesEachBattle = false;
        boolean rerollGimmickEachBattle = false;
        Map<String, Double> locations = new HashMap<>();

        JsonObject bossDetails = new JsonObject();
        if (root.has("boss_details"))
            bossDetails = root.get("boss_details").getAsJsonObject();

        if (bossDetails.has("display_name"))
            displayName = bossDetails.get("display_name").getAsString();
        bossDetails.remove("display_name");
        bossDetails.addProperty("display_name", displayName);

        if (bossDetails.has("base_health"))
            baseHealth = bossDetails.get("base_health").getAsInt();
        bossDetails.remove("base_health");
        bossDetails.addProperty("base_health", baseHealth);

        if (bossDetails.has("health_increase_per_player"))
            healthIncreasePerPlayer = bossDetails.get("health_increase_per_player").getAsInt();
        bossDetails.remove("health_increase_per_player");
        bossDetails.addProperty("health_increase_per_player", healthIncreasePerPlayer);

        if (bossDetails.has("apply_glowing"))
            applyGlowing = bossDetails.get("apply_glowing").getAsBoolean();
        bossDetails.remove("apply_glowing");
        bossDetails.addProperty("apply_glowing", applyGlowing);

        if (bossDetails.has("ai_skill_level"))
            aiSkillLevel = bossDetails.get("ai_skill_level").getAsInt();
        bossDetails.remove("ai_skill_level");
        bossDetails.addProperty("ai_skill_level", aiSkillLevel);

        if (bossDetails.has("reroll_features_each_battle"))
            rerollFeaturesEachBattle = bossDetails.get("reroll_features_each_battle").getAsBoolean();
        bossDetails.remove("reroll_features_each_battle");
        bossDetails.addProperty("reroll_features_each_battle", rerollFeaturesEachBattle);

        if (bossDetails.has("reroll_gimmick_each_battle"))
            rerollGimmickEachBattle = bossDetails.get("reroll_gimmick_each_battle").getAsBoolean();
        bossDetails.remove("reroll_gimmick_each_battle");
        bossDetails.addProperty("reroll_gimmick_each_battle", rerollGimmickEachBattle);

        JsonArray locationsArray = new JsonArray();
        if (bossDetails.has("locations"))
            locationsArray = bossDetails.get("locations").getAsJsonArray();

        for (JsonElement locationElement : locationsArray) {
            JsonObject locationObject = locationElement.getAsJsonObject();
            if (!locationObject.has("location")) continue;

            String location = locationObject.get("location").getAsString();
            locationObject.remove("location");
            locationObject.addProperty("location", location);

            double weight = 1.0;
            if (locationObject.has("weight"))
                weight = locationObject.get("weight").getAsDouble();
            locationObject.remove("weight");
            locationObject.addProperty("weight", weight);

            locations.put(location, weight);
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
        bossDetails.remove("locations");
        bossDetails.add("locations", locationsArray);

        root.remove("boss_details");
        root.add("boss_details", bossDetails);

        // Item Settings
        boolean allowGlobalPokeballs = true;
        boolean allowCategoryPokeballs = true;

        JsonObject itemSettingsObject = new JsonObject();
        if (root.has("item_settings"))
            itemSettingsObject = root.get("item_settings").getAsJsonObject();

        if (itemSettingsObject.has("allow_global_pokeballs"))
            allowGlobalPokeballs = itemSettingsObject.get("allow_global_pokeballs").getAsBoolean();
        itemSettingsObject.remove("allow_global_pokeballs");
        itemSettingsObject.addProperty("allow_global_pokeballs", allowGlobalPokeballs);

        if (itemSettingsObject.has("allow_category_pokeballs"))
            allowCategoryPokeballs = itemSettingsObject.get("allow_category_pokeballs").getAsBoolean();
        itemSettingsObject.remove("allow_category_pokeballs");
        itemSettingsObject.addProperty("allow_category_pokeballs", allowCategoryPokeballs);

        JsonObject bossVoucherObject = new JsonObject();
        if (itemSettingsObject.has("boss_voucher"))
            bossVoucherObject = itemSettingsObject.get("boss_voucher").getAsJsonObject();

        Voucher bossVoucher = ConfigHelper.getVoucher(bossVoucherObject, nr.config().defaultVoucher);

        itemSettingsObject.remove("boss_voucher");
        itemSettingsObject.add("boss_voucher", bossVoucher.voucherObject());

        JsonObject bossPassObject = new JsonObject();
        if (itemSettingsObject.has("boss_pass"))
            bossPassObject = itemSettingsObject.get("boss_pass").getAsJsonObject();

        Pass bossPass = ConfigHelper.getPass(bossPassObject, nr.config().defaultPass);

        itemSettingsObject.remove("boss_pass");
        itemSettingsObject.add("boss_pass", bossPass.passObject());

        JsonObject raidBalls = new JsonObject();
        if (itemSettingsObject.has("raid_balls"))
            raidBalls = itemSettingsObject.getAsJsonObject("raid_balls");

        List<RaidBall> bossBalls = ConfigHelper.getRaidBalls(raidBalls);

        for (RaidBall raidBall : bossBalls) {
            raidBalls.remove(raidBall.id());
            raidBalls.add(raidBall.id(), raidBall.raidBallObject());
        }

        itemSettingsObject.remove("raid_balls");
        itemSettingsObject.add("raid_balls", raidBalls);

        root.remove("item_settings");
        root.add("item_settings", itemSettingsObject);

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
        String setupBossbar = "setup_phase_example";
        String fightBossbar = "fight_phase_example";
        String preCatchBossbar = "pre_catch_phase_example";
        String catchBossbar = "catch_phase_example";
        boolean overrideCategoryDistribution = false;
        List<DistributionSection> rewards = List.of();

        JsonObject raidDetailsObject = new JsonObject();
        if (root.has("raid_details"))
            raidDetailsObject = root.getAsJsonObject("raid_details");

        if (raidDetailsObject.has("minimum_level"))
            minimumLevel = raidDetailsObject.get("minimum_level").getAsInt();
        raidDetailsObject.remove("minimum_level");
        raidDetailsObject.addProperty("minimum_level", minimumLevel);

        if (raidDetailsObject.has("maximum_level"))
            maximumLevel = raidDetailsObject.get("maximum_level").getAsInt();
        raidDetailsObject.remove("maximum_level");
        raidDetailsObject.addProperty("maximum_level", maximumLevel);

        if (raidDetailsObject.has("setup_phase_time"))
            setupPhaseTime = raidDetailsObject.get("setup_phase_time").getAsInt();
        raidDetailsObject.remove("setup_phase_time");
        raidDetailsObject.addProperty("setup_phase_time", setupPhaseTime);

        if (raidDetailsObject.has("fight_phase_time"))
            fightPhaseTime = raidDetailsObject.get("fight_phase_time").getAsInt();
        raidDetailsObject.remove("fight_phase_time");
        raidDetailsObject.addProperty("fight_phase_time", fightPhaseTime);

        if (raidDetailsObject.has("do_catch_phase"))
            doCatchPhase = raidDetailsObject.get("do_catch_phase").getAsBoolean();
        raidDetailsObject.remove("do_catch_phase");
        raidDetailsObject.addProperty("do_catch_phase", doCatchPhase);

        if (raidDetailsObject.has("pre_catch_phase_time"))
            preCatchPhaseTime = raidDetailsObject.get("pre_catch_phase_time").getAsInt();
        raidDetailsObject.remove("pre_catch_phase_time");
        raidDetailsObject.addProperty("pre_catch_phase_time", preCatchPhaseTime);

        if (raidDetailsObject.has("catch_phase_time"))
            catchPhaseTime = raidDetailsObject.get("catch_phase_time").getAsInt();
        raidDetailsObject.remove("catch_phase_time");
        raidDetailsObject.addProperty("catch_phase_time", catchPhaseTime);

        if (raidDetailsObject.has("heal_party_on_challenge"))
            healPartyOnChallenge = raidDetailsObject.get("heal_party_on_challenge").getAsBoolean();
        raidDetailsObject.remove("heal_party_on_challenge");
        raidDetailsObject.addProperty("heal_party_on_challenge", healPartyOnChallenge);

        JsonObject contrabandObject = new JsonObject();
        if (raidDetailsObject.has("contraband"))
            contrabandObject = raidDetailsObject.get("contraband").getAsJsonObject();

        Contraband contraband = ConfigHelper.getContraband(contrabandObject, file.getName());

        raidDetailsObject.remove("contraband");
        raidDetailsObject.add("contraband", contraband.contrabandObject());

        JsonObject bossbarsObject = new JsonObject();
        if (raidDetailsObject.has("bossbars"))
            bossbarsObject = raidDetailsObject.getAsJsonObject("bossbars");

        if (bossbarsObject.has("setup"))
            setupBossbar = bossbarsObject.get("setup").getAsString();
        bossbarsObject.remove("setup");
        bossbarsObject.addProperty("setup", setupBossbar);

        if (bossbarsObject.has("fight"))
            fightBossbar = bossbarsObject.get("fight").getAsString();
        bossbarsObject.remove("fight");
        bossbarsObject.addProperty("fight", fightBossbar);

        if (bossbarsObject.has("pre_catch"))
            preCatchBossbar = bossbarsObject.get("pre_catch").getAsString();
        bossbarsObject.remove("pre_catch");
        bossbarsObject.addProperty("pre_catch", preCatchBossbar);

        if (bossbarsObject.has("catch"))
            catchBossbar = bossbarsObject.get("catch").getAsString();
        bossbarsObject.remove("catch");
        bossbarsObject.addProperty("catch", catchBossbar);

        raidDetailsObject.add("bossbars", bossbarsObject);
        raidDetailsObject.add("bossbars", bossbarsObject);

        if (raidDetailsObject.has("override_category_distribution"))
            overrideCategoryDistribution = raidDetailsObject.get("override_category_distribution").getAsBoolean();
        raidDetailsObject.remove("override_category_distribution");
        raidDetailsObject.addProperty("override_category_distribution", overrideCategoryDistribution);

        JsonArray rewardDistributionArray = new JsonArray();
        if (raidDetailsObject.has("reward_distribution"))
            rewards = ConfigHelper.getDistributionSections(raidDetailsObject, false);

        for (DistributionSection distributionSection : rewards) {
            rewardDistributionArray.add(distributionSection.distributionObject());
        }

        raidDetailsObject.remove("reward_distribution");
        raidDetailsObject.add("reward_distribution", rewardDistributionArray);

        root.remove("raid_details");
        root.add("raid_details", raidDetailsObject);

        RaidDetails raidDetails = new RaidDetails(
                minimumLevel,
                maximumLevel,
                setupPhaseTime,
                fightPhaseTime,
                doCatchPhase,
                preCatchPhaseTime,
                catchPhaseTime,
                healPartyOnChallenge,
                contraband,
                setupBossbar,
                fightBossbar,
                preCatchBossbar,
                catchBossbar,
                overrideCategoryDistribution,
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
        boolean randomizeTeraType = true;
        boolean resetGmaxFactor = true;
        int dmaxLevelOverride = 0;
        List<CatchPlacement> catchPlacements = new ArrayList<>();

        JsonObject catchSettingsObject = new JsonObject();
        if (root.has("catch_settings"))
            catchSettingsObject = root.getAsJsonObject("catch_settings");

        String speciesOverrideString = species.getResourceIdentifier().getPath();
        if (catchSettingsObject.has("species_override"))
            speciesOverrideString = catchSettingsObject.get("species_override").getAsString();
        speciesOverride = PokemonSpecies.getByName(speciesOverrideString.toLowerCase());

        if (speciesOverride == null) {
            speciesOverride = species;
            if (!speciesOverrideString.isEmpty())
                NovaRaids.LOGGER.warn("[NovaRaids] Unknown species for catch override: {}. In boss {}. Using boss species ({})", speciesOverrideString, bossId, species.getName());
        }
        catchSettingsObject.remove("species_override");
        catchSettingsObject.addProperty("species_override", speciesOverride.getResourceIdentifier().getPath());

        if (catchSettingsObject.has("level_override"))
            levelOverride = catchSettingsObject.get("level_override").getAsInt();
        catchSettingsObject.remove("level_override");
        catchSettingsObject.addProperty("level_override", levelOverride);

        if (catchSettingsObject.has("form_override"))
            catchSettingsObject.remove("form_override");

        if (catchSettingsObject.has("keep_features"))
            keepFeatures = catchSettingsObject.get("keep_features").getAsBoolean();
        catchSettingsObject.remove("keep_features");
        catchSettingsObject.addProperty("keep_features", keepFeatures);

        if (catchSettingsObject.has("features_override"))
            featuresOverride = catchSettingsObject.get("features_override").getAsString();
        catchSettingsObject.remove("features_override");
        catchSettingsObject.addProperty("features_override", featuresOverride);

        if (catchSettingsObject.has("keep_scale"))
            keepScale = catchSettingsObject.get("keep_scale").getAsBoolean();
        catchSettingsObject.remove("keep_scale");
        catchSettingsObject.addProperty("keep_scale", keepScale);

        if (catchSettingsObject.has("keep_held_item"))
            keepHeldItem = catchSettingsObject.get("keep_held_item").getAsBoolean();
        catchSettingsObject.remove("keep_held_item");
        catchSettingsObject.addProperty("keep_held_item", keepHeldItem);

        if (catchSettingsObject.has("randomize_ivs"))
            randomizeIVs = catchSettingsObject.get("randomize_ivs").getAsBoolean();
        catchSettingsObject.remove("randomize_ivs");
        catchSettingsObject.addProperty("randomize_ivs", randomizeIVs);

        if (catchSettingsObject.has("keep_evs"))
            keepEVs = catchSettingsObject.get("keep_evs").getAsBoolean();
        catchSettingsObject.remove("keep_evs");
        catchSettingsObject.addProperty("keep_evs", keepEVs);

        if (catchSettingsObject.has("randomize_gender"))
            randomizeGender = catchSettingsObject.get("randomize_gender").getAsBoolean();
        catchSettingsObject.remove("randomize_gender");
        catchSettingsObject.addProperty("randomize_gender", randomizeGender);

        if (catchSettingsObject.has("randomize_nature"))
            randomizeNature = catchSettingsObject.get("randomize_nature").getAsBoolean();
        catchSettingsObject.remove("randomize_nature");
        catchSettingsObject.addProperty("randomize_nature", randomizeNature);

        if (catchSettingsObject.has("randomize_ability"))
            randomizeAbility = catchSettingsObject.get("randomize_ability").getAsBoolean();
        catchSettingsObject.remove("randomize_ability");
        catchSettingsObject.addProperty("randomize_ability", randomizeAbility);

        if (catchSettingsObject.has("reset_moves"))
            resetMoves = catchSettingsObject.get("reset_moves").getAsBoolean();
        catchSettingsObject.remove("reset_moves");
        catchSettingsObject.addProperty("reset_moves", resetMoves);

        if (catchSettingsObject.has("friendship_override"))
            friendshipOverride = catchSettingsObject.get("friendship_override").getAsInt();
        catchSettingsObject.remove("friendship_override");
        catchSettingsObject.addProperty("friendship_override", friendshipOverride);

        if (catchSettingsObject.has("randomize_tera_type"))
            randomizeTeraType = catchSettingsObject.get("randomize_tera_type").getAsBoolean();
        catchSettingsObject.remove("randomize_tera_type");
        catchSettingsObject.addProperty("randomize_tera_type", randomizeTeraType);

        if (catchSettingsObject.has("reset_gmax_factor"))
            resetGmaxFactor = catchSettingsObject.get("reset_gmax_factor").getAsBoolean();
        catchSettingsObject.remove("reset_gmax_factor");
        catchSettingsObject.addProperty("reset_gmax_factor", resetGmaxFactor);

        if (catchSettingsObject.has("dynamax_level_override"))
            dmaxLevelOverride = catchSettingsObject.get("dynamax_level_override").getAsInt();
        catchSettingsObject.remove("dynamax_level_override");
        catchSettingsObject.addProperty("dynamax_level_override", dmaxLevelOverride);

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

        placementsArray = new JsonArray();
        for (CatchPlacement placement : catchPlacements) {
            JsonObject placeObject = new JsonObject();
            placeObject.addProperty("place", placement.place());
            placeObject.addProperty("require_damage", placement.requireDamage());
            placeObject.addProperty("shiny_chance", placement.shinyChance());
            placeObject.addProperty("min_perfect_ivs", placement.minPerfectIvs());
            placementsArray.add(placeObject);
        }

        catchSettingsObject.remove("places");
        catchSettingsObject.add("places", placementsArray);

        if (catchPlacements.isEmpty()) catchPlacements.add(new CatchPlacement("participating", true, 8192, 0));

        root.remove("catch_settings");
        root.add("catch_settings", catchSettingsObject);

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
                randomizeTeraType,
                resetGmaxFactor,
                dmaxLevelOverride,
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
                rerollFeaturesEachBattle,
                rerollGimmickEachBattle,
                locations,
                itemSettings,
                raidDetails,
                catchSettings
        ));

        file.delete();
        file.createNewFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(file);
        gson.toJson(root, writer);
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
