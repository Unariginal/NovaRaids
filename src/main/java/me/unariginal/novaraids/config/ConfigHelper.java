package me.unariginal.novaraids.config;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon.mod.common.pokemon.IVs;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.rewards.*;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;

public class ConfigHelper {
    public static List<DistributionSection> getDistributionSections(JsonObject config, String location) {
        List<DistributionSection> rewards = new ArrayList<>();
        if (checkProperty(config, "reward_distribution", location)) {
            JsonArray reward_distributions = config.getAsJsonArray("reward_distribution");
            for (JsonElement reward_distribution : reward_distributions) {
                JsonObject reward_distribution_object = reward_distribution.getAsJsonObject();
                List<Place> places = new ArrayList<>();
                if (checkProperty(reward_distribution_object, "places", location)) {
                    JsonArray placesArray = reward_distribution_object.getAsJsonArray("places");
                    for (JsonElement place_element : placesArray) {
                        JsonObject place_object = place_element.getAsJsonObject();
                        String place;
                        if (checkProperty(place_object, "place", location)) {
                            place = place_object.get("place").getAsString();
                        } else {
                            continue;
                        }
                        boolean require_damage;
                        if (checkProperty(place_object, "require_damage", location)) {
                            require_damage = place_object.get("require_damage").getAsBoolean();
                        } else {
                            continue;
                        }
                        boolean allow_other_rewards;
                        if (checkProperty(place_object, "allow_other_rewards", location)) {
                            allow_other_rewards = place_object.get("allow_other_rewards").getAsBoolean();
                        } else {
                            continue;
                        }
                        boolean override_category_rewards = false;
                        if (place_object.has("override_category_rewards")) {
                            override_category_rewards = place_object.get("override_category_rewards").getAsBoolean();
                        }
                        places.add(new Place(place, require_damage, allow_other_rewards, override_category_rewards));
                    }
                } else {
                    continue;
                }
                boolean allow_duplicates;
                int min_rolls;
                int max_rolls;
                Map<RewardPool, Double> reward_pools = new HashMap<>();
                if (checkProperty(reward_distribution_object, "rewards", location)) {
                    JsonObject rewards_object = reward_distribution_object.getAsJsonObject("rewards");
                    if (checkProperty(rewards_object, "allow_duplicates", location)) {
                        allow_duplicates = rewards_object.get("allow_duplicates").getAsBoolean();
                    } else {
                        continue;
                    }
                    if (checkProperty(rewards_object, "rolls", location)) {
                        JsonObject rolls_object = rewards_object.getAsJsonObject("rolls");
                        if (checkProperty(rolls_object, "min", location)) {
                            min_rolls = rolls_object.get("min").getAsInt();
                        } else {
                            continue;
                        }
                        if (checkProperty(rolls_object, "max", location)) {
                            max_rolls = rolls_object.get("max").getAsInt();
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                    if (checkProperty(rewards_object, "reward_pools", location)) {
                        JsonArray reward_pools_array = rewards_object.getAsJsonArray("reward_pools");
                        for (JsonElement reward_pool_element : reward_pools_array) {
                            JsonObject reward_pool_object = reward_pool_element.getAsJsonObject();
                            double weight;
                            if (checkProperty(reward_pool_object, "weight", location)) {
                                weight = reward_pool_object.get("weight").getAsDouble();
                            } else {
                                continue;
                            }

                            RewardPool pool;
                            if (checkProperty(reward_pool_object, "pool_preset", location, false)) {
                                String pool_preset = reward_pool_object.get("pool_preset").getAsString();
                                pool = NovaRaids.INSTANCE.rewardPoolsConfig().getRewardPool(pool_preset);
                            } else if (checkProperty(reward_pool_object, "pool", location, false)) {
                                pool = getRewardPool(reward_pool_object.getAsJsonObject("pool"), "temp", location);
                            } else {
                                NovaRaids.INSTANCE.logError("[RAIDS] Invalid reward pool declaration at: " + location);
                                continue;
                            }
                            if (pool == null) {
                                continue;
                            }
                            reward_pools.put(pool, weight);
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
        return rewards;
    }

    public static RewardPool getRewardPool(JsonObject reward_pool_object, String name, String location) {
        boolean allow_duplicates_pool;
        if (checkProperty(reward_pool_object, "allow_duplicates", location)) {
            allow_duplicates_pool = reward_pool_object.get("allow_duplicates").getAsBoolean();
        } else {
            return null;
        }
        int min_pool_rolls;
        int max_pool_rolls;
        if (checkProperty(reward_pool_object, "rolls", location)) {
            JsonObject rolls_object = reward_pool_object.getAsJsonObject("rolls");
            if (checkProperty(rolls_object, "min", location)) {
                min_pool_rolls = rolls_object.get("min").getAsInt();
            } else {
                return null;
            }
            if (checkProperty(rolls_object, "max", location)) {
                max_pool_rolls = rolls_object.get("max").getAsInt();
            } else {
                return null;
            }
        } else {
            return null;
        }
        Map<Reward, Double> rewardList = new HashMap<>();
        if (checkProperty(reward_pool_object, "rewards", location)) {
            JsonArray rewards_array = reward_pool_object.getAsJsonArray("rewards");
            for (JsonElement reward_element : rewards_array) {
                JsonObject reward_object = reward_element.getAsJsonObject();
                double reward_weight;
                if (checkProperty(reward_object, "weight", location)) {
                    reward_weight = reward_object.get("weight").getAsDouble();
                } else {
                    continue;
                }
                Reward reward;
                if (checkProperty(reward_object, "reward", location, false)) {
                    JsonObject unset_reward_object = reward_object.getAsJsonObject("reward");
                    reward = getReward(unset_reward_object, "temp", location);
                    if (reward == null) {
                        continue;
                    }
                    rewardList.put(reward, reward_weight);
                } else if (checkProperty(reward_object, "reward_preset", location, false)) {
                    String reward_preset = reward_object.get("reward_preset").getAsString();
                    reward = NovaRaids.INSTANCE.rewardPresetsConfig().getReward(reward_preset);
                    if (reward == null) {
                        continue;
                    }
                    rewardList.put(reward, reward_weight);
                } else {
                    NovaRaids.INSTANCE.logError("[RAIDS] Invalid reward declaration at: " + location);
                }
            }
        } else {
            return null;
        }
        return new RewardPool(UUID.randomUUID(), name, allow_duplicates_pool, min_pool_rolls, max_pool_rolls, rewardList);
    }

    public static Reward getReward(JsonObject reward_object, String name, String location) {
        String type;
        if (checkProperty(reward_object, "type", location)) {
            type = reward_object.get("type").getAsString();
        } else {
            return null;
        }
        if (type.equalsIgnoreCase("item")) {
            Item reward_item;
            if (checkProperty(reward_object, "item", location)) {
                reward_item = Registries.ITEM.get(Identifier.of(reward_object.get("item").getAsString()));
            } else {
                return null;
            }
            ComponentChanges item_data = ComponentChanges.EMPTY;
            if (checkProperty(reward_object, "data", location)) {
                JsonElement data = reward_object.getAsJsonObject("data");
                if (data != null) {
                    item_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                }
            }
            int count_min;
            int count_max;
            if (checkProperty(reward_object, "count", location)) {
                JsonObject count_object = reward_object.getAsJsonObject("count");
                if (checkProperty(count_object, "min", location)) {
                    count_min = count_object.get("min").getAsInt();
                } else {
                    return null;
                }
                if (checkProperty(count_object, "max", location)) {
                    count_max = count_object.get("max").getAsInt();
                } else {
                    return null;
                }
            } else {
                return null;
            }
            return new ItemReward(name, reward_item, item_data, count_min, count_max);
        } else if (type.equalsIgnoreCase("command")) {
            List<String> commands;
            if (checkProperty(reward_object, "commands", location)) {
                commands = reward_object.getAsJsonArray("commands").asList().stream().map(JsonElement::getAsString).toList();
            } else {
                return null;
            }
            return new CommandReward(name, commands);
        } else if (type.equalsIgnoreCase("pokemon")) {
            if (checkProperty(reward_object, "pokemon", location)) {
                JsonObject pokemon_object = reward_object.getAsJsonObject("pokemon");
                String species;
                if (checkProperty(pokemon_object, "species", location)) {
                    species = pokemon_object.get("species").getAsString();
                } else {
                    return null;
                }
                int level;
                if (checkProperty(pokemon_object, "level", location)) {
                    level = pokemon_object.get("level").getAsInt();
                } else {
                    return null;
                }
                String ability;
                if (checkProperty(pokemon_object, "ability", location)) {
                    ability = pokemon_object.get("ability").getAsString();
                } else {
                    return null;
                }
                String nature;
                if (checkProperty(pokemon_object, "nature", location)) {
                    nature = pokemon_object.get("nature").getAsString();
                } else {
                    return null;
                }
                String form;
                if (checkProperty(pokemon_object, "form", location)) {
                    form = pokemon_object.get("form").getAsString();
                } else {
                    return null;
                }
                String features;
                if (checkProperty(pokemon_object, "features", location)) {
                    features = pokemon_object.get("features").getAsString();
                } else {
                    return null;
                }
                String gender;
                if (checkProperty(pokemon_object, "gender", location)) {
                    gender = pokemon_object.get("gender").getAsString();
                } else {
                    return null;
                }
                boolean shiny;
                if (checkProperty(pokemon_object, "shiny", location)) {
                    shiny = pokemon_object.get("shiny").getAsBoolean();
                } else {
                    return null;
                }
                float scale;
                if (checkProperty(pokemon_object, "scale", location)) {
                    scale = pokemon_object.get("scale").getAsFloat();
                } else {
                    return null;
                }
                String held_item;
                if (checkProperty(pokemon_object, "held_item", location)) {
                    held_item = pokemon_object.get("held_item").getAsString();
                } else {
                    return null;
                }
                ComponentChanges held_item_data = ComponentChanges.EMPTY;
                if (checkProperty(pokemon_object, "held_item_data", location)) {
                    JsonElement data = pokemon_object.getAsJsonObject("held_item_data");
                    if (data != null) {
                        held_item_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                    }
                }
                List<String> moves = new ArrayList<>();
                if (checkProperty(pokemon_object, "moves", location)) {
                    moves = pokemon_object.getAsJsonArray("moves").asList().stream().map(JsonElement::getAsString).toList();
                }
                if (moves.isEmpty()) {
                    return null;
                }
                IVs ivs = IVs.createRandomIVs(0);
                if (checkProperty(pokemon_object, "ivs", location)) {
                    JsonObject ivs_object = pokemon_object.getAsJsonObject("ivs");
                    if (checkProperty(ivs_object, "hp", location)) {
                        ivs.set(Stats.HP, ivs_object.get("hp").getAsInt());
                    }
                    if (checkProperty(ivs_object, "atk", location)) {
                        ivs.set(Stats.ATTACK, ivs_object.get("atk").getAsInt());
                    }
                    if (checkProperty(ivs_object, "def", location)) {
                        ivs.set(Stats.DEFENCE, ivs_object.get("def").getAsInt());
                    }
                    if (checkProperty(ivs_object, "sp_atk", location)) {
                        ivs.set(Stats.SPECIAL_ATTACK, ivs_object.get("sp_atk").getAsInt());
                    }
                    if (checkProperty(ivs_object, "sp_def", location)) {
                        ivs.set(Stats.SPECIAL_DEFENCE, ivs_object.get("sp_def").getAsInt());
                    }
                    if (checkProperty(ivs_object, "spd", location)) {
                        ivs.set(Stats.SPEED, ivs_object.get("spd").getAsInt());
                    }
                }
                EVs evs = EVs.createEmpty();
                if (checkProperty(pokemon_object, "evs", location)) {
                    JsonObject evs_object = pokemon_object.getAsJsonObject("evs");
                    if (checkProperty(evs_object, "hp", location)) {
                        evs.set(Stats.HP, evs_object.get("hp").getAsInt());
                    }
                    if (checkProperty(evs_object, "atk", location)) {
                        evs.set(Stats.ATTACK, evs_object.get("atk").getAsInt());
                    }
                    if (checkProperty(evs_object, "def", location)) {
                        evs.set(Stats.DEFENCE, evs_object.get("def").getAsInt());
                    }
                    if (checkProperty(evs_object, "sp_atk", location)) {
                        evs.set(Stats.SPECIAL_ATTACK, evs_object.get("sp_atk").getAsInt());
                    }
                    if (checkProperty(evs_object, "sp_def", location)) {
                        evs.set(Stats.SPECIAL_DEFENCE, evs_object.get("sp_def").getAsInt());
                    }
                    if (checkProperty(evs_object, "spd", location)) {
                        evs.set(Stats.SPEED, evs_object.get("spd").getAsInt());
                    }
                }
                return new PokemonReward(name, species, level, ability, nature, form, features, gender, shiny, scale, held_item, held_item_data, moves, ivs, evs);
            } else {
                return null;
            }
        }
        return null;
    }

    public static boolean checkProperty(JsonObject section, String property, String location, boolean report) {
        if (section.has(property)) {
            return true;
        }
        if (report) {
            NovaRaids.INSTANCE.logError("[RAIDS] Missing " + property + " property in " + location + ".json. Using default value(s) or skipping.");
        }
        return false;
    }

    public static boolean checkProperty(JsonObject section, String property, String location) {
        return checkProperty(section, property, location, true);
    }
}
