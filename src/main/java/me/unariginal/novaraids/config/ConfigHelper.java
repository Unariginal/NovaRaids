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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                            if (checkProperty(reward_pool_object, "pool_preset", location)) {
                                String pool_preset = reward_pool_object.get("pool_preset").getAsString();
                                //TODO: GET REWARD POOL BY NAME
                                continue;
                            } else if (checkProperty(reward_pool_object, "pool", location)) {
                                boolean allow_duplicates_pool;
                                if (checkProperty(reward_pool_object, "allow_duplicates", location)) {
                                    allow_duplicates_pool = reward_pool_object.get("allow_duplicates").getAsBoolean();
                                } else {
                                    continue;
                                }
                                int min_pool_rolls;
                                int max_pool_rolls;
                                if (checkProperty(reward_pool_object, "rolls", location)) {
                                    JsonObject rolls_object = reward_pool_object.getAsJsonObject("rolls");
                                    if (checkProperty(rolls_object, "min", location)) {
                                        min_pool_rolls = rolls_object.get("min").getAsInt();
                                    } else {
                                        continue;
                                    }
                                    if (checkProperty(rolls_object, "max", location)) {
                                        max_pool_rolls = rolls_object.get("max").getAsInt();
                                    } else {
                                        continue;
                                    }
                                } else {
                                    continue;
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
                                        if (checkProperty(reward_object, "reward", location)) {
                                            String type;
                                            if (checkProperty(reward_object, "type", location)) {
                                                type = reward_object.get("type").getAsString();
                                            } else {
                                                continue;
                                            }
                                            if (type.equalsIgnoreCase("item")) {
                                                Item reward_item;
                                                if (checkProperty(reward_object, "item", location)) {
                                                    reward_item = Registries.ITEM.get(Identifier.of(reward_object.get("item").getAsString()));
                                                } else {
                                                    continue;
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
                                                if (checkProperty(rewards_object, "count", location)) {
                                                    JsonObject count_object = rewards_object.getAsJsonObject("count");
                                                    if (checkProperty(count_object, "min", location)) {
                                                        count_min = count_object.get("min").getAsInt();
                                                    } else {
                                                        continue;
                                                    }
                                                    if (checkProperty(count_object, "max", location)) {
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
                                                if (checkProperty(reward_object, "commands", location)) {
                                                    commands = reward_object.getAsJsonArray("commands").asList().stream().map(JsonElement::getAsString).toList();
                                                } else {
                                                    continue;
                                                }
                                                reward = new CommandReward("temp", commands);
                                            } else if (type.equalsIgnoreCase("pokemon")) {
                                                String species;
                                                if (checkProperty(reward_object, "species", location)) {
                                                    species = reward_object.get("species").getAsString();
                                                } else {
                                                    continue;
                                                }
                                                int level;
                                                if (checkProperty(reward_object, "level", location)) {
                                                    level = reward_object.get("level").getAsInt();
                                                } else {
                                                    continue;
                                                }
                                                String ability;
                                                if (checkProperty(reward_object, "ability", location)) {
                                                    ability = reward_object.get("ability").getAsString();
                                                } else {
                                                    continue;
                                                }
                                                String nature;
                                                if (checkProperty(reward_object, "nature", location)) {
                                                    nature = reward_object.get("nature").getAsString();
                                                } else {
                                                    continue;
                                                }
                                                String form;
                                                if (checkProperty(reward_object, "form", location)) {
                                                    form = reward_object.get("form").getAsString();
                                                } else {
                                                    continue;
                                                }
                                                String features;
                                                if (checkProperty(reward_object, "features", location)) {
                                                    features = reward_object.get("features").getAsString();
                                                } else {
                                                    continue;
                                                }
                                                String gender;
                                                if (checkProperty(reward_object, "gender", location)) {
                                                    gender = reward_object.get("gender").getAsString();
                                                } else {
                                                    continue;
                                                }
                                                boolean shiny;
                                                if (checkProperty(reward_object, "shiny", location)) {
                                                    shiny = reward_object.get("shiny").getAsBoolean();
                                                } else {
                                                    continue;
                                                }
                                                float scale;
                                                if (checkProperty(reward_object, "scale", location)) {
                                                    scale = reward_object.get("scale").getAsFloat();
                                                } else {
                                                    continue;
                                                }
                                                String held_item;
                                                if (checkProperty(reward_object, "held_item", location)) {
                                                    held_item = reward_object.get("held_item").getAsString();
                                                } else {
                                                    continue;
                                                }
                                                ComponentChanges held_item_data = ComponentChanges.EMPTY;
                                                if (checkProperty(reward_object, "data", location)) {
                                                    JsonElement data = reward_object.getAsJsonObject("data");
                                                    if (data != null) {
                                                        held_item_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, data).getOrThrow().getFirst();
                                                    }
                                                }
                                                List<String> moves = new ArrayList<>();
                                                if (checkProperty(reward_object, "moves", location)) {
                                                    moves = reward_object.getAsJsonArray("moves").asList().stream().map(JsonElement::getAsString).toList();
                                                }
                                                if (moves.isEmpty()) {
                                                    continue;
                                                }
                                                IVs ivs = IVs.createRandomIVs(0);
                                                if (checkProperty(reward_object, "ivs", location)) {
                                                    JsonObject ivs_object = reward_object.getAsJsonObject("ivs");
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
                                                if (checkProperty(reward_object, "evs", location)) {
                                                    JsonObject evs_object = reward_object.getAsJsonObject("evs");
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
                                                reward = new PokemonReward("temp", species, level, ability, nature, form, features, gender, shiny, scale, held_item, held_item_data, moves, ivs, evs);
                                            } else {
                                                continue;
                                            }
                                            rewardList.put(reward, reward_weight);
                                        } else if (checkProperty(reward_object, "reward_preset", location)) {
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

    public static boolean checkProperty(JsonObject section, String property, String location) {
        if (section.has(property)) {
            return true;
        }
        NovaRaids.INSTANCE.logError("[RAIDS] Missing " + property + " property in " + location + ".json. Using default value(s) or skipping.");
        return false;
    }
}
