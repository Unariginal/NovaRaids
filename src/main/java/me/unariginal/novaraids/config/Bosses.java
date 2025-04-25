package me.unariginal.novaraids.config;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon.mod.common.pokemon.IVs;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.Category;
import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;
import me.unariginal.novaraids.data.rewards.*;
import me.unariginal.novaraids.utils.TextUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
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
                        places.add(new Place(place, require_damage, allow_other_rewards));
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
    }

    public boolean checkProperty(JsonObject section, String property, String location) {
        if (section.has(property)) {
            return true;
        }
        nr.logError("[RAIDS] Missing " + property + " property in " + location + ".json. Using default value(s) or skipping.");
        return false;
    }
}
