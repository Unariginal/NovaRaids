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
import me.unariginal.novaraids.data.CatchPlacement;
import me.unariginal.novaraids.data.Category;
import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;
import me.unariginal.novaraids.data.rewards.*;
import me.unariginal.novaraids.utils.TextUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Bosses {
    private final NovaRaids nr = NovaRaids.INSTANCE;
    public List<Category> categories = new ArrayList<>();

    public Bosses() {
        try {
            loadBosses();
        } catch (IOException | NullPointerException | UnsupportedOperationException e) {
            nr.logError("[RAIDS] Failed to load bosses folder. " + e.getMessage());
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

        boolean require_pass = false;
        if (checkProperty(config, "require_pass", category_name + "/settings")) {
            require_pass = config.get("require_pass").getAsBoolean();
        }

        int min_players = 0;
        int max_players = -1;
        if (checkProperty(config, "player_count", category_name + "/settings")) {
            JsonObject player_count = config.getAsJsonObject("player_count");
            if (checkProperty(player_count, "min", category_name + "/settings")) {
                min_players = player_count.get("min").getAsInt();
            }
            if (checkProperty(player_count, "max", category_name + "/settings")) {
                max_players = player_count.get("max").getAsInt();
            }
        }

        String setup_bossbar = "setup_phase_example";
        String fight_bossbar = "fight_phase_example";
        String pre_catch_bossbar = "pre_catch_phase_example";
        String catch_bossbar = "catch_phase_example";
        if (checkProperty(config, "bossbars", category_name + "/settings")) {
            JsonObject bossbars = config.getAsJsonObject("bossbars");
            if (checkProperty(bossbars, "setup", category_name + "/settings")) {
                setup_bossbar = bossbars.get("setup").getAsString();
            } else {
                throw new NullPointerException("Bossbars must have a 'setup' property.");
            }
            if (checkProperty(bossbars, "fight", category_name + "/settings")) {
                fight_bossbar = bossbars.get("fight").getAsString();
            } else {
                throw new NullPointerException("Bossbars must have a 'fight' property.");
            }
            if (checkProperty(bossbars, "pre_catch", category_name + "/settings")) {
                pre_catch_bossbar = bossbars.get("pre_catch").getAsString();
            } else {
                throw new NullPointerException("Bossbars must have a 'pre_catch' property.");
            }
            if (checkProperty(bossbars, "catch", category_name + "/settings")) {
                catch_bossbar = bossbars.get("catch").getAsString();
            } else {
                throw new NullPointerException("Bossbars must have a 'catch' property.");
            }
        }

        Voucher category_voucher = nr.config().default_voucher;
        Pass category_pass = nr.config().default_pass;
        List<RaidBall> category_balls = new ArrayList<>();
        if (checkProperty(config, "item_settings", category_name + "/settings")) {
            JsonObject item_settings = config.getAsJsonObject("item_settings");
            if (checkProperty(item_settings, "category_voucher", category_name + "/settings")) {
                JsonObject voucher = item_settings.getAsJsonObject("category_voucher");
                Item voucher_item = category_voucher.voucher_item();
                Text voucher_name = category_voucher.voucher_name();
                List<Text> voucher_lore = category_voucher.voucher_lore();
                ComponentChanges voucher_data = category_voucher.voucher_data();

                if (checkProperty(voucher, "pass_item", category_name + "/settings")) {
                    String voucher_item_name = voucher.get("pass_item").getAsString();
                    voucher_item = Registries.ITEM.get(Identifier.of(voucher_item_name));
                }
                if (checkProperty(voucher, "voucher_name", category_name + "/settings")) {
                    voucher_name = TextUtil.deserialize(voucher.get("voucher_name").getAsString());
                }
                if (checkProperty(voucher, "voucher_lore", category_name + "/settings")) {
                    JsonArray lore_items = voucher.getAsJsonArray("voucher_lore");
                    List<Text> lore = new ArrayList<>();
                    for (JsonElement l : lore_items) {
                        String lore_item = l.getAsString();
                        lore.add(TextUtil.deserialize(lore_item));
                    }
                    voucher_lore = lore;
                }
                if (checkProperty(voucher, "voucher_data", category_name + "/settings")) {
                    JsonElement data = voucher.getAsJsonObject("voucher_data");
                    if (data != null) {
                        voucher_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                    }
                }

                category_voucher = new Voucher(voucher_item, voucher_name, voucher_lore, voucher_data);
            }

            if (checkProperty(item_settings, "category_pass", category_name + "/settings")) {
                JsonObject pass = item_settings.getAsJsonObject("category_pass");
                Item pass_item = category_pass.pass_item();
                Text pass_name = category_pass.pass_name();
                List<Text> pass_lore = category_pass.pass_lore();
                ComponentChanges pass_data = category_pass.pass_data();

                if (checkProperty(pass, "pass_item", category_name + "/settings")) {
                    String pass_item_name = pass.get("pass_item").getAsString();
                    pass_item = Registries.ITEM.get(Identifier.of(pass_item_name));
                }
                if (checkProperty(pass, "pass_name", category_name + "/settings")) {
                    pass_name = TextUtil.deserialize(pass.get("pass_name").getAsString());
                }
                if (checkProperty(pass, "pass_lore", category_name + "/settings")) {
                    JsonArray lore_items = pass.getAsJsonArray("pass_lore");
                    List<Text> lore = new ArrayList<>();
                    for (JsonElement l : lore_items) {
                        String lore_item = l.getAsString();
                        lore.add(TextUtil.deserialize(lore_item));
                    }
                    pass_lore = lore;
                }
                if (checkProperty(pass, "pass_data", category_name + "/settings")) {
                    JsonElement data = pass.getAsJsonObject("pass_data");
                    if (data != null) {
                        pass_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                    }
                }

                category_pass = new Pass(pass_item, pass_name, pass_lore, pass_data);
            }

            if (checkProperty(item_settings, "raid_balls", category_name + "/settings")) {
                JsonObject raidBalls = item_settings.getAsJsonObject("raid_balls");
                for (String id : raidBalls.keySet()) {
                    JsonObject ballObject = raidBalls.getAsJsonObject(id);
                    Item pokeball = CobblemonItems.POKE_BALL;
                    Text pokeball_name = TextUtil.deserialize("<red> " + category_name + " Raid Ball");
                    List<Text> pokeball_lore = List.of(TextUtil.deserialize("<gray>Use this to try and catch " + category_name + " bosses!"));
                    ComponentChanges pokeball_data = ComponentChanges.EMPTY;

                    if (checkProperty(ballObject, "pokeball", category_name + "/settings")) {
                        String pokeball_item_name = ballObject.get("pokeball").getAsString();
                        pokeball = Registries.ITEM.get(Identifier.of(pokeball_item_name));
                    }
                    if (checkProperty(ballObject, "pokeball_name", category_name + "/settings")) {
                        pokeball_name = TextUtil.deserialize(ballObject.get("pokeball_name").getAsString());
                    }
                    if (checkProperty(ballObject, "pokeball_lore", category_name + "/settings")) {
                        JsonArray lore_items = ballObject.getAsJsonArray("pokeball_lore");
                        List<Text> lore = new ArrayList<>();
                        for (JsonElement l : lore_items) {
                            String lore_item = l.getAsString();
                            lore.add(TextUtil.deserialize(lore_item));
                        }
                        pokeball_lore = lore;
                    }
                    if (checkProperty(ballObject, "pokeball_data", category_name + "/settings")) {
                        JsonElement data = ballObject.getAsJsonObject("pokeball_data");
                        if (data != null) {
                            pokeball_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                        }
                    }

                    category_balls.add(new RaidBall(id, pokeball, pokeball_name, pokeball_lore, pokeball_data));
                }
            }
        }

        List<DistributionSection> rewards = new ArrayList<>();
        if (checkProperty(config, "reward_distribution", category_name + "/settings")) {
            JsonArray reward_distributions = config.getAsJsonArray("reward_distribution");
            for (JsonElement reward_distribution : reward_distributions) {
                JsonObject reward_distribution_object = reward_distribution.getAsJsonObject();
                List<Place> places = new ArrayList<>();
                if (checkProperty(reward_distribution_object, "places", category_name + "/settings")) {
                    JsonArray placesArray = reward_distribution_object.getAsJsonArray("places");
                    for (JsonElement place_element : placesArray) {
                        JsonObject place_object = place_element.getAsJsonObject();
                        String place;
                        if (checkProperty(place_object, "place", category_name + "/settings")) {
                            place = place_object.get("place").getAsString();
                        } else {
                            continue;
                        }
                        boolean require_damage;
                        if (checkProperty(place_object, "require_damage", category_name + "/settings")) {
                            require_damage = place_object.get("require_damage").getAsBoolean();
                        } else {
                            continue;
                        }
                        boolean allow_other_rewards;
                        if (checkProperty(place_object, "allow_other_rewards", category_name + "/settings")) {
                            allow_other_rewards = place_object.get("allow_other_rewards").getAsBoolean();
                        } else {
                            continue;
                        }
                        places.add(new Place(place, require_damage, allow_other_rewards, false));
                    }
                } else {
                    continue;
                }
                boolean allow_duplicates;
                int min_rolls;
                int max_rolls;
                List<RewardPool> reward_pools = new ArrayList<>();
                if (checkProperty(reward_distribution_object, "rewards", category_name + "/settings")) {
                    JsonObject rewards_object = reward_distribution_object.getAsJsonObject("rewards");
                    if (checkProperty(rewards_object, "allow_duplicates", category_name + "/settings")) {
                        allow_duplicates = rewards_object.get("allow_duplicates").getAsBoolean();
                    } else {
                        continue;
                    }
                    if (checkProperty(rewards_object, "rolls", category_name + "/settings")) {
                        JsonObject rolls_object = rewards_object.getAsJsonObject("rolls");
                        if (checkProperty(rolls_object, "min", category_name + "/settings")) {
                            min_rolls = rolls_object.get("min").getAsInt();
                        } else {
                            continue;
                        }
                        if (checkProperty(rolls_object, "max", category_name + "/settings")) {
                            max_rolls = rolls_object.get("max").getAsInt();
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                    if (checkProperty(rewards_object, "reward_pools", category_name + "/settings")) {
                        JsonArray reward_pools_array = rewards_object.getAsJsonArray("reward_pools");
                        for (JsonElement reward_pool_element : reward_pools_array) {
                            JsonObject reward_pool_object = reward_pool_element.getAsJsonObject();
                            double weight;
                            if (checkProperty(reward_pool_object, "weight", category_name + "/settings")) {
                                weight = reward_pool_object.get("weight").getAsDouble();
                            } else {
                                continue;
                            }

                            RewardPool pool;
                            if (checkProperty(reward_pool_object, "pool_preset", category_name + "/settings")) {
                                String pool_preset = reward_pool_object.get("pool_preset").getAsString();
                                //TODO: GET REWARD POOL BY NAME
                                continue;
                            } else if (checkProperty(reward_pool_object, "pool", category_name + "/settings")) {
                                boolean allow_duplicates_pool;
                                if (checkProperty(reward_pool_object, "allow_duplicates", category_name + "/settings")) {
                                    allow_duplicates_pool = reward_pool_object.get("allow_duplicates").getAsBoolean();
                                } else {
                                    continue;
                                }
                                int min_pool_rolls;
                                int max_pool_rolls;
                                if (checkProperty(reward_pool_object, "rolls", category_name + "/settings")) {
                                    JsonObject rolls_object = reward_pool_object.getAsJsonObject("rolls");
                                    if (checkProperty(rolls_object, "min", category_name + "/settings")) {
                                        min_pool_rolls = rolls_object.get("min").getAsInt();
                                    } else {
                                        continue;
                                    }
                                    if (checkProperty(rolls_object, "max", category_name + "/settings")) {
                                        max_pool_rolls = rolls_object.get("max").getAsInt();
                                    } else {
                                        continue;
                                    }
                                } else {
                                    continue;
                                }
                                Map<Reward, Double> rewardList = new HashMap<>();
                                if (checkProperty(reward_pool_object, "rewards", category_name + "/settings")) {
                                    JsonArray rewards_array = reward_pool_object.getAsJsonArray("rewards");
                                    for (JsonElement reward_element : rewards_array) {
                                        JsonObject reward_object = reward_element.getAsJsonObject();
                                        double reward_weight = 1;
                                        if (checkProperty(reward_object, "weight", category_name + "/settings")) {
                                            reward_weight = reward_object.get("weight").getAsDouble();
                                        } else {
                                            continue;
                                        }
                                        Reward reward;
                                        if (checkProperty(reward_object, "reward", category_name + "/settings")) {
                                            String type;
                                            if (checkProperty(reward_object, "type", category_name + "/settings")) {
                                                type = reward_object.get("type").getAsString();
                                            } else {
                                                continue;
                                            }
                                            if (type.equalsIgnoreCase("item")) {
                                                Item reward_item;
                                                if (checkProperty(reward_object, "item", category_name + "/settings")) {
                                                    reward_item = Registries.ITEM.get(Identifier.of(reward_object.get("item").getAsString()));
                                                } else {
                                                    continue;
                                                }
                                                ComponentChanges item_data = ComponentChanges.EMPTY;
                                                if (checkProperty(reward_object, "data", category_name + "/settings")) {
                                                    JsonElement data = reward_object.getAsJsonObject("data");
                                                    if (data != null) {
                                                        item_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                                                    }
                                                }
                                                int count_min;
                                                int count_max;
                                                if (checkProperty(rewards_object, "count", category_name + "/settings")) {
                                                    JsonObject count_object = rewards_object.getAsJsonObject("count");
                                                    if (checkProperty(count_object, "min", category_name + "/settings")) {
                                                        count_min = count_object.get("min").getAsInt();
                                                    } else {
                                                        continue;
                                                    }
                                                    if (checkProperty(count_object, "max", category_name + "/settings")) {
                                                        count_max = count_object.get("max").getAsInt();
                                                    } else {
                                                        continue;
                                                    }
                                                } else {
                                                    continue;
                                                }
                                                reward = new ItemReward("temp", reward_item, item_data, count_min, count_max);
                                            } else if (type.equalsIgnoreCase("command")) {
                                                List<String> commands;
                                                if (checkProperty(reward_object, "commands", category_name + "/settings")) {
                                                    commands = reward_object.getAsJsonArray("commands").asList().stream().map(JsonElement::getAsString).toList();
                                                } else {
                                                    continue;
                                                }
                                                reward = new CommandReward("temp", commands);
                                            } else if (type.equalsIgnoreCase("pokemon")) {
                                                String species;
                                                if (checkProperty(reward_object, "species", category_name + "/settings")) {
                                                    species = reward_object.get("species").getAsString();
                                                } else {
                                                    continue;
                                                }
                                                int level;
                                                if (checkProperty(reward_object, "level", category_name + "/settings")) {
                                                    level = reward_object.get("level").getAsInt();
                                                } else {
                                                    continue;
                                                }
                                                String ability;
                                                if (checkProperty(reward_object, "ability", category_name + "/settings")) {
                                                    ability = reward_object.get("ability").getAsString();
                                                } else {
                                                    continue;
                                                }
                                                String nature;
                                                if (checkProperty(reward_object, "nature", category_name + "/settings")) {
                                                    nature = reward_object.get("nature").getAsString();
                                                } else {
                                                    continue;
                                                }
                                                String form;
                                                if (checkProperty(reward_object, "form", category_name + "/settings")) {
                                                    form = reward_object.get("form").getAsString();
                                                } else {
                                                    continue;
                                                }
                                                String features;
                                                if (checkProperty(reward_object, "features", category_name + "/settings")) {
                                                    features = reward_object.get("features").getAsString();
                                                } else {
                                                    continue;
                                                }
                                                String gender;
                                                if (checkProperty(reward_object, "gender", category_name + "/settings")) {
                                                    gender = reward_object.get("gender").getAsString();
                                                } else {
                                                    continue;
                                                }
                                                boolean shiny;
                                                if (checkProperty(reward_object, "shiny", category_name + "/settings")) {
                                                    shiny = reward_object.get("shiny").getAsBoolean();
                                                } else {
                                                    continue;
                                                }
                                                float scale;
                                                if (checkProperty(reward_object, "scale", category_name + "/settings")) {
                                                    scale = reward_object.get("scale").getAsFloat();
                                                } else {
                                                    continue;
                                                }
                                                String held_item;
                                                if (checkProperty(reward_object, "held_item", category_name + "/settings")) {
                                                    held_item = reward_object.get("held_item").getAsString();
                                                } else {
                                                    continue;
                                                }
                                                ComponentChanges held_item_data = ComponentChanges.EMPTY;
                                                if (checkProperty(reward_object, "data", category_name + "/settings")) {
                                                    JsonElement data = reward_object.getAsJsonObject("data");
                                                    if (data != null) {
                                                        held_item_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                                                    }
                                                }
                                                List<String> moves = new ArrayList<>();
                                                if (checkProperty(reward_object, "moves", category_name + "/settings")) {
                                                    moves = reward_object.getAsJsonArray("moves").asList().stream().map(JsonElement::getAsString).toList();
                                                }
                                                if (moves.isEmpty()) {
                                                    continue;
                                                }
                                                IVs ivs = IVs.createRandomIVs(0);
                                                if (checkProperty(reward_object, "ivs", category_name + "/settings")) {
                                                    JsonObject ivs_object = reward_object.getAsJsonObject("ivs");
                                                    if (checkProperty(ivs_object, "hp", category_name + "/settings")) {
                                                        ivs.set(Stats.HP, ivs_object.get("hp").getAsInt());
                                                    }
                                                    if (checkProperty(ivs_object, "atk", category_name + "/settings")) {
                                                        ivs.set(Stats.ATTACK, ivs_object.get("atk").getAsInt());
                                                    }
                                                    if (checkProperty(ivs_object, "def", category_name + "/settings")) {
                                                        ivs.set(Stats.DEFENCE, ivs_object.get("def").getAsInt());
                                                    }
                                                    if (checkProperty(ivs_object, "sp_atk", category_name + "/settings")) {
                                                        ivs.set(Stats.SPECIAL_ATTACK, ivs_object.get("sp_atk").getAsInt());
                                                    }
                                                    if (checkProperty(ivs_object, "sp_def", category_name + "/settings")) {
                                                        ivs.set(Stats.SPECIAL_DEFENCE, ivs_object.get("sp_def").getAsInt());
                                                    }
                                                    if (checkProperty(ivs_object, "spd", category_name + "/settings")) {
                                                        ivs.set(Stats.SPEED, ivs_object.get("spd").getAsInt());
                                                    }
                                                }
                                                EVs evs = EVs.createEmpty();
                                                if (checkProperty(reward_object, "evs", category_name + "/settings")) {
                                                    JsonObject evs_object = reward_object.getAsJsonObject("evs");
                                                    if (checkProperty(evs_object, "hp", category_name + "/settings")) {
                                                        evs.set(Stats.HP, evs_object.get("hp").getAsInt());
                                                    }
                                                    if (checkProperty(evs_object, "atk", category_name + "/settings")) {
                                                        evs.set(Stats.ATTACK, evs_object.get("atk").getAsInt());
                                                    }
                                                    if (checkProperty(evs_object, "def", category_name + "/settings")) {
                                                        evs.set(Stats.DEFENCE, evs_object.get("def").getAsInt());
                                                    }
                                                    if (checkProperty(evs_object, "sp_atk", category_name + "/settings")) {
                                                        evs.set(Stats.SPECIAL_ATTACK, evs_object.get("sp_atk").getAsInt());
                                                    }
                                                    if (checkProperty(evs_object, "sp_def", category_name + "/settings")) {
                                                        evs.set(Stats.SPECIAL_DEFENCE, evs_object.get("sp_def").getAsInt());
                                                    }
                                                    if (checkProperty(evs_object, "spd", category_name + "/settings")) {
                                                        evs.set(Stats.SPEED, evs_object.get("spd").getAsInt());
                                                    }
                                                }
                                                reward = new PokemonReward("temp", species, level, ability, nature, form, features, gender, shiny, scale, held_item, held_item_data, moves, ivs, evs);
                                            } else {
                                                continue;
                                            }
                                            rewardList.put(reward, weight);
                                        } else if (checkProperty(reward_object, "reward_preset", category_name + "/settings")) {
                                            String reward_preset = reward_object.get("reward_preset").getAsString();
                                            // TODO: Get reward preset
                                            continue;
                                        } else {
                                            continue;
                                        }
                                    }
                                } else {
                                    continue;
                                }
                                pool = new RewardPool("temp", allow_duplicates_pool, min_pool_rolls, max_pool_rolls, rewardList);
                            } else {
                                continue;
                            }
                            reward_pools.add(pool);
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
                rewards.add(new DistributionSection(places, allow_duplicates, min_rolls, max_rolls, reward_pools));
            }
        }

        categories.add(new Category(category_name, require_pass, min_players, max_players, setup_bossbar, fight_bossbar, pre_catch_bossbar, catch_bossbar, category_voucher, category_pass, category_balls, rewards));
    }

    public void loadBoss(String category_name, File file) throws IOException, NullPointerException, UnsupportedOperationException {
        JsonElement root = JsonParser.parseReader(new FileReader(file));
        assert root != null;
        JsonObject config = root.getAsJsonObject();
        String file_name = file.getName().substring(0, file.getName().indexOf(".json"));

        String boss_id;
        if (checkProperty(config, "boss_id", category_name + "/bosses/" + file_name)) {
            boss_id = config.get("boss_id").getAsString();
        } else {
            throw new NullPointerException("Boss must have a Boss ID!");
        }

        double global_weight;
        if (checkProperty(config, "global_weight", category_name + "/bosses/" + file_name)) {
            global_weight = config.get("global_weight").getAsDouble();
        } else {
            throw new NullPointerException("Boss must have a global weight!");
        }

        double category_weight;
        if (checkProperty(config, "category_weight", category_name + "/bosses/" + file_name)) {
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

        if (checkProperty(config, "pokemon_details", category_name + "/bosses/" + file_name)) {
            JsonObject pokemon_details = config.getAsJsonObject("pokemon_details");
            if (checkProperty(pokemon_details, "species", category_name + "/bosses/" + file_name)) {
                species = PokemonSpecies.INSTANCE.getByName(pokemon_details.get("species").getAsString());
                if (species == null) {
                    throw new NullPointerException("Species not found!");
                }
            } else {
                throw new NullPointerException("Pokemon details must have a species!");
            }
            if (checkProperty(pokemon_details, "level", category_name + "/bosses/" + file_name)) {
                level = pokemon_details.get("level").getAsInt();
            }
            if (checkProperty(pokemon_details, "form", category_name + "/bosses/" + file_name)) {
                form = species.getFormByName(pokemon_details.get("form").getAsString());
            } else {
                form = species.getStandardForm();
            }
            if (checkProperty(pokemon_details, "features", category_name + "/bosses/" + file_name)) {
                features = pokemon_details.get("features").getAsString();
            }
            if (checkProperty(pokemon_details, "ability", category_name + "/bosses/" + file_name)) {
                JsonArray ability_array = pokemon_details.getAsJsonArray("ability");
                for (JsonElement ability_element : ability_array) {
                    JsonObject ability = ability_element.getAsJsonObject();
                    String ability_id;
                    if (checkProperty(ability, "ability", category_name + "/bosses/" + file_name)) {
                        ability_id = ability.get("ability").getAsString();
                    } else {
                        continue;
                    }
                    double weight;
                    if (checkProperty(ability, "weight", category_name + "/bosses/" + file_name)) {
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
            if (checkProperty(pokemon_details, "nature", category_name + "/bosses/" + file_name)) {
                JsonArray nature_array = pokemon_details.getAsJsonArray("nature");
                for (JsonElement nature_element : nature_array) {
                    JsonObject nature = nature_element.getAsJsonObject();
                    String nature_id;
                    if (checkProperty(nature, "nature", category_name + "/bosses/" + file_name)) {
                        nature_id = nature.get("nature").getAsString();
                    } else {
                        continue;
                    }
                    double weight;
                    if (checkProperty(nature, "weight", category_name + "/bosses/" + file_name)) {
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
            if (checkProperty(pokemon_details, "gender", category_name + "/bosses/" + file_name)) {
                JsonArray gender_array = pokemon_details.getAsJsonArray("gender");
                for (JsonElement gender_element : gender_array) {
                    JsonObject gender = gender_element.getAsJsonObject();
                    String gender_id;
                    if (checkProperty(gender, "gender", category_name + "/bosses/" + file_name)) {
                        gender_id = gender.get("gender").getAsString();
                    } else {
                        continue;
                    }
                    double weight;
                    if (checkProperty(gender, "weight", category_name + "/bosses/" + file_name)) {
                        weight = gender.get("weight").getAsDouble();
                    } else {
                        continue;
                    }
                    Gender possible_gender = Gender.valueOf(gender_id);
                    genders.put(possible_gender, weight);
                }
                if (genders.isEmpty()) {
                    throw new NullPointerException("No genders found!");
                }
            } else {
                throw new NullPointerException("Pokemon details must have a gender!");
            }
            if (checkProperty(pokemon_details, "shiny", category_name + "/bosses/" + file_name)) {
                shiny = pokemon_details.get("shiny").getAsBoolean();
            }
            if (checkProperty(pokemon_details, "scale", category_name + "/bosses/" + file_name)) {
                scale = pokemon_details.get("scale").getAsFloat();
            }
            if (checkProperty(pokemon_details, "held_item", category_name + "/bosses/" + file_name)) {
                held_item = Registries.ITEM.get(Identifier.of(pokemon_details.get("held_item").getAsString()));
            }
            if (checkProperty(pokemon_details, "held_item_data", category_name + "/bosses/" + file_name)) {
                JsonElement data_element = pokemon_details.getAsJsonObject("held_item_data");
                if (data_element != null) {
                    held_item_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data_element).getOrThrow().getFirst();
                }
            }
            if (checkProperty(pokemon_details, "moves", category_name + "/bosses/" + file_name)) {
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
            if (checkProperty(pokemon_details, "ivs", category_name + "/bosses/" + file_name)) {
                JsonObject ivs_object = pokemon_details.getAsJsonObject("ivs");
                if (checkProperty(ivs_object, "hp", category_name + "/bosses/" + file_name)) {
                    ivs.set(Stats.HP, ivs_object.get("hp").getAsInt());
                }
                if (checkProperty(ivs_object, "atk", category_name + "/bosses/" + file_name)) {
                    ivs.set(Stats.ATTACK, ivs_object.get("atk").getAsInt());
                }
                if (checkProperty(ivs_object, "def", category_name + "/bosses/" + file_name)) {
                    ivs.set(Stats.DEFENCE, ivs_object.get("def").getAsInt());
                }
                if (checkProperty(ivs_object, "sp_atk", category_name + "/bosses/" + file_name)) {
                    ivs.set(Stats.SPECIAL_ATTACK, ivs_object.get("sp_atk").getAsInt());
                }
                if (checkProperty(ivs_object, "sp_def", category_name + "/bosses/" + file_name)) {
                    ivs.set(Stats.SPECIAL_DEFENCE, ivs_object.get("sp_def").getAsInt());
                }
                if (checkProperty(ivs_object, "spd", category_name + "/bosses/" + file_name)) {
                    ivs.set(Stats.SPEED, ivs_object.get("spd").getAsInt());
                }
            }
            if (checkProperty(pokemon_details, "evs", category_name + "/bosses/" + file_name)) {
                JsonObject evs_object = pokemon_details.getAsJsonObject("ivs");
                if (checkProperty(evs_object, "hp", category_name + "/bosses/" + file_name)) {
                    evs.set(Stats.HP, evs_object.get("hp").getAsInt());
                }
                if (checkProperty(evs_object, "atk", category_name + "/bosses/" + file_name)) {
                    evs.set(Stats.ATTACK, evs_object.get("atk").getAsInt());
                }
                if (checkProperty(evs_object, "def", category_name + "/bosses/" + file_name)) {
                    evs.set(Stats.DEFENCE, evs_object.get("def").getAsInt());
                }
                if (checkProperty(evs_object, "sp_atk", category_name + "/bosses/" + file_name)) {
                    evs.set(Stats.SPECIAL_ATTACK, evs_object.get("sp_atk").getAsInt());
                }
                if (checkProperty(evs_object, "sp_def", category_name + "/bosses/" + file_name)) {
                    evs.set(Stats.SPECIAL_DEFENCE, evs_object.get("sp_def").getAsInt());
                }
                if (checkProperty(evs_object, "spd", category_name + "/bosses/" + file_name)) {
                    evs.set(Stats.SPEED, evs_object.get("spd").getAsInt());
                }
            }
        } else {
            throw new NullPointerException("Boss must have pokemon details!");
        }

        // Boss Details
        String display_name = boss_id;
        int base_health;
        int health_increase_per_player = 0;
        boolean apply_glowing = false;
        Map<String, Double> locations = new HashMap<>();
        if (checkProperty(config, "boss_details", category_name + "/bosses/" + file_name)) {
            JsonObject boss_details = config.get("boss_details").getAsJsonObject();
            if (checkProperty(boss_details, "display_name", category_name + "/bosses/" + file_name)) {
                display_name = boss_details.get("display_name").getAsString();
            }
            if (checkProperty(boss_details, "base_health", category_name + "/bosses/" + file_name)) {
                base_health = boss_details.get("base_health").getAsInt();
            } else {
                throw new NullPointerException("Boss details must have base health!");
            }
            if (checkProperty(boss_details, "health_increase_per_player", category_name + "/bosses/" + file_name)) {
                health_increase_per_player = boss_details.get("health_increase_per_player").getAsInt();
            }
            if (checkProperty(boss_details, "apply_glowing", category_name + "/bosses/" + file_name)) {
                apply_glowing = boss_details.get("apply_glowing").getAsBoolean();
            }
            if (checkProperty(boss_details, "locations", category_name + "/bosses/" + file_name)) {
                JsonArray locations_array = boss_details.get("locations").getAsJsonArray();
                for (JsonElement location_element : locations_array) {
                    JsonObject location_object = location_element.getAsJsonObject();
                    String location_name;
                    if (checkProperty(location_object, "location", category_name + "/bosses/" + file_name)) {
                        location_name = location_object.get("location").getAsString();
                    } else {
                        continue;
                    }
                    double weight;
                    if (checkProperty(location_object, "weight", category_name + "/bosses/" + file_name)) {
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
        if (checkProperty(config, "item_settings", category_name + "/bosses/" + file_name)) {
            JsonObject item_settings = config.get("item_settings").getAsJsonObject();
            if (checkProperty(item_settings, "allow_global_pokeballs", category_name + "/bosses/" + file_name)) {
                allow_global_pokeballs = item_settings.get("allow_global_pokeballs").getAsBoolean();
            }
            if (checkProperty(item_settings, "allow_category_pokeballs", category_name + "/bosses/" + file_name)) {
                allow_category_pokeballs = item_settings.get("allow_category_pokeballs").getAsBoolean();
            }
            if (checkProperty(item_settings, "boss_voucher", category_name + "/bosses/" + file_name)) {
                Item voucher_item = nr.config().default_voucher.voucher_item();
                Text voucher_name = nr.config().default_voucher.voucher_name();
                List<Text> voucher_lore = nr.config().default_voucher.voucher_lore();
                ComponentChanges voucher_data = nr.config().default_voucher.voucher_data();
                JsonObject boss_voucher_object = item_settings.getAsJsonObject("boss_voucher");
                if (checkProperty(boss_voucher_object, "voucher_item", category_name + "/bosses/" + file_name)) {
                    voucher_item = Registries.ITEM.get(Identifier.of(item_settings.get("voucher_item").getAsString()));
                }
                if (checkProperty(boss_voucher_object, "voucher_name", category_name + "/bosses/" + file_name)) {
                    voucher_name = TextUtil.deserialize(boss_voucher_object.get("voucher_name").getAsString());
                }
                if (checkProperty(boss_voucher_object, "voucher_lore", category_name + "/bosses/" + file_name)) {
                    JsonArray lore_array = boss_voucher_object.get("voucher_lore").getAsJsonArray();
                    List<Text> lore = new ArrayList<>();
                    for (JsonElement lore_element : lore_array) {
                        lore.add(TextUtil.deserialize(lore_element.getAsString()));
                    }
                    voucher_lore = lore;
                }
                if (checkProperty(boss_voucher_object, "voucher_data", category_name + "/bosses/" + file_name)) {
                    JsonElement data_object = boss_voucher_object.get("voucher_data");
                    if (data_object != null) {
                        voucher_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data_object).getOrThrow().getFirst();
                    }
                }
                boss_voucher = new Voucher(voucher_item, voucher_name, voucher_lore, voucher_data);
            }
            if (checkProperty(item_settings, "boss_pass", category_name + "/bosses/" + file_name)) {
                Item pass_item = nr.config().default_pass.pass_item();
                Text pass_name = nr.config().default_pass.pass_name();
                List<Text> pass_lore = nr.config().default_pass.pass_lore();
                ComponentChanges pass_data = nr.config().default_pass.pass_data();
                JsonObject boss_pass_object = item_settings.getAsJsonObject("boss_pass");
                if (checkProperty(boss_pass_object, "pass_item", category_name + "/bosses/" + file_name)) {
                    pass_item = Registries.ITEM.get(Identifier.of(item_settings.get("pass_item").getAsString()));
                }
                if (checkProperty(boss_pass_object, "pass_name", category_name + "/bosses/" + file_name)) {
                    pass_name = TextUtil.deserialize(boss_pass_object.get("pass_name").getAsString());
                }
                if (checkProperty(boss_pass_object, "pass_lore", category_name + "/bosses/" + file_name)) {
                    JsonArray lore_array = boss_pass_object.get("pass_lore").getAsJsonArray();
                    List<Text> lore = new ArrayList<>();
                    for (JsonElement lore_element : lore_array) {
                        lore.add(TextUtil.deserialize(lore_element.getAsString()));
                    }
                    pass_lore = lore;
                }
                if (checkProperty(boss_pass_object, "pass_data", category_name + "/bosses/" + file_name)) {
                    JsonElement data_object = boss_pass_object.get("pass_data");
                    if (data_object != null) {
                        pass_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data_object).getOrThrow().getFirst();
                    }
                }
                boss_pass = new Pass(pass_item, pass_name, pass_lore, pass_data);
            }
            if (checkProperty(item_settings, "raid_balls", category_name + "/bosses/" + file_name)) {
                JsonObject raid_balls = item_settings.getAsJsonObject("raid_balls");
                for (String ball_id : raid_balls.keySet()) {
                    JsonObject ball_info = raid_balls.getAsJsonObject(ball_id);
                    Item item = CobblemonItems.POKE_BALL;
                    Text name = TextUtil.deserialize("<red>Raid Pokeball");
                    List<Text> lore = new ArrayList<>(List.of(TextUtil.deserialize("<gray>Use this to try and capture raid bosses!")));
                    ComponentChanges data = ComponentChanges.EMPTY;
                    if (checkProperty(ball_info, "pokeball", category_name + "/bosses/" + file_name)) {
                        item = Registries.ITEM.get(Identifier.of(ball_info.get("pokeball").getAsString()));
                    }
                    if (checkProperty(ball_info, "pokeball_name", category_name + "/bosses/" + file_name)) {
                        String ball_name = ball_info.get("pokeball_name").getAsString();
                        name = TextUtil.deserialize(ball_name);
                    }
                    if (checkProperty(ball_info, "pokeball_lore", category_name + "/bosses/" + file_name)) {
                        JsonArray lore_items = ball_info.getAsJsonArray("pokeball_lore");
                        List<Text> newLore = new ArrayList<>();
                        for (JsonElement l : lore_items) {
                            String lore_item = l.getAsString();
                            newLore.add(TextUtil.deserialize(lore_item));
                        }
                        lore = newLore;
                    }
                    if (checkProperty(ball_info, "pokeball_data", category_name + "/bosses/" + file_name)) {
                        JsonElement dataElement = ball_info.get("pokeball_data");
                        if (dataElement != null) {
                            data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, dataElement).getOrThrow().getFirst();
                        }
                    }
                    boss_balls.add(new RaidBall(boss_id, item, name, lore, data));
                }
            }
        }

        // Raid Details
        int minimum_level;
        int setup_phase_time;
        int fight_phase_time;
        int pre_catch_phase_time;
        boolean do_catch_phase;
        int catch_phase_time;
        boolean heal_party_on_challenge;
        List<Species> banned_species = new ArrayList<>();
        List<Move> banned_moves = new ArrayList<>();
        List<Ability> banned_abilities = new ArrayList<>();
        List<Item> banned_held_items = new ArrayList<>();
        List<Item> banned_bag_items = new ArrayList<>();
        String setup_bossbar;
        String fight_bossbar;
        String pre_catch_bossbar;
        String catch_bossbar;
        List<DistributionSection> rewards;

        // Catch Settings
        Species species_override;
        int level_override;
        FormData form_override;
        String features_override;
        boolean keep_scale;
        boolean keep_held_item;
        boolean randomize_ivs;
        boolean keep_evs;
        boolean randomize_gender;
        boolean randomize_nature;
        boolean randomize_ability;
        boolean reset_moves;
        List<CatchPlacement> catch_places = new ArrayList<>();
    }

    public boolean checkProperty(JsonObject section, String property, String location) {
        if (section.has(property)) {
            return true;
        }
        nr.logError("[RAIDS] Missing " + property + " property in " + location + ".json. Using default value(s) or skipping.");
        return false;
    }
}
