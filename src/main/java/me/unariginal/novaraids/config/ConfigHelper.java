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
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon.mod.common.pokemon.IVs;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.Contraband;
import me.unariginal.novaraids.data.items.Pass;
import me.unariginal.novaraids.data.items.RaidBall;
import me.unariginal.novaraids.data.items.Voucher;
import me.unariginal.novaraids.data.rewards.*;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;

public class ConfigHelper {
    public static List<DistributionSection> getDistributionSections(JsonObject config, boolean isCategorySection) {
        List<DistributionSection> rewards = new ArrayList<>();

        JsonArray rewardDistributionArray = new JsonArray();
        if (config.has("reward_distribution"))
            rewardDistributionArray = config.getAsJsonArray("reward_distribution");

        for (JsonElement distributionElement : rewardDistributionArray) {
            JsonObject distributionObject = distributionElement.getAsJsonObject();

            if (!distributionObject.has("places")) continue;

            List<Place> places = new ArrayList<>();
            JsonArray placesArray = new JsonArray();
            for (JsonElement placeElement : distributionObject.get("places").getAsJsonArray()) {
                JsonObject placeObject = placeElement.getAsJsonObject();

                String place = "participating";
                if (placeObject.has("place"))
                    place = placeObject.get("place").getAsString();
                placeObject.remove("place");
                placeObject.addProperty("place", place);

                boolean requireDamage = true;
                if (placeObject.has("require_damage"))
                    requireDamage = placeObject.get("require_damage").getAsBoolean();
                placeObject.remove("require_damage");
                placeObject.addProperty("require_damage", requireDamage);

                boolean allowOtherRewards = true;
                if (placeObject.has("allow_other_rewards"))
                    allowOtherRewards = placeObject.get("allow_other_rewards").getAsBoolean();
                placeObject.remove("allow_other_rewards");
                placeObject.addProperty("allow_other_rewards", allowOtherRewards);

                boolean overrideCategoryPlacement = false;
                if (!isCategorySection) {
                    if (placeObject.has("override_category_rewards"))
                        overrideCategoryPlacement = placeObject.get("override_category_rewards").getAsBoolean();
                    if (placeObject.has("override_category_placement"))
                        overrideCategoryPlacement = placeObject.get("override_category_placement").getAsBoolean();
                    placeObject.remove("override_category_rewards");
                    placeObject.addProperty("override_category_placement", overrideCategoryPlacement);
                }

                placesArray.add(placeObject);
                places.add(new Place(place, requireDamage, allowOtherRewards, overrideCategoryPlacement));
            }

            distributionObject.remove("places");
            distributionObject.add("places", placesArray);

            JsonObject rewardsObject = new JsonObject();
            if (distributionObject.has("rewards"))
                rewardsObject = distributionObject.getAsJsonObject("rewards");

            boolean allowDuplicates = true;
            if (rewardsObject.has("allow_duplicates"))
                allowDuplicates = rewardsObject.get("allow_duplicates").getAsBoolean();
            rewardsObject.remove("allow_duplicates");
            rewardsObject.addProperty("allow_duplicates", allowDuplicates);

            JsonObject rollsObject = new JsonObject();
            if (rewardsObject.has("rolls"))
                rollsObject = rewardsObject.getAsJsonObject("rolls");

            int minRolls = 1;
            int maxRolls = 1;

            if (rollsObject.has("min"))
                minRolls = rollsObject.get("min").getAsInt();
            rollsObject.remove("min");
            rollsObject.addProperty("min", minRolls);
            if (rollsObject.has("max"))
                maxRolls = rollsObject.get("max").getAsInt();
            rollsObject.remove("max");
            rollsObject.addProperty("max", maxRolls);

            rewardsObject.remove("rolls");
            rewardsObject.add("rolls", rollsObject);

            Map<RewardPool, Double> rewardPools = new HashMap<>();
            JsonArray rewardPoolsArray = new JsonArray();
            if (rewardsObject.has("reward_pools"))
                rewardPoolsArray = rewardsObject.getAsJsonArray("reward_pools");

            JsonArray newRewardPoolsArray = new JsonArray();
            for (JsonElement rewardPoolElement : rewardPoolsArray) {
                JsonObject rewardPoolObject = rewardPoolElement.getAsJsonObject();

                double weight = 1;
                if (rewardPoolObject.has("weight"))
                    weight = rewardPoolObject.get("weight").getAsDouble();
                rewardPoolObject.remove("weight");
                rewardPoolObject.addProperty("weight", weight);

                if (rewardPoolObject.has("pool_preset")) {
                    String poolPreset = rewardPoolObject.get("pool_preset").getAsString();
                    RewardPool pool = NovaRaids.INSTANCE.rewardPoolsConfig().getRewardPool(poolPreset);
                    rewardPoolObject.remove("pool_preset");
                    rewardPoolObject.addProperty("pool_preset", poolPreset);
                    if (pool == null) continue;
                    rewardPools.put(pool, weight);
                } else if (rewardPoolObject.has("pool")) {
                    JsonObject poolObject = rewardPoolObject.getAsJsonObject("pool");
                    RewardPool pool = getRewardPool(poolObject, "temp");
                    rewardPools.put(pool, weight);
                    rewardPoolObject.remove("pool");
                    rewardPoolObject.add("pool", pool.poolObject());
                }

                newRewardPoolsArray.add(rewardPoolObject);
            }

            rewardsObject.remove("reward_pools");
            rewardsObject.add("reward_pools", newRewardPoolsArray);

            distributionObject.remove("rewards");
            distributionObject.add("rewards", rewardsObject);

            rewards.add(new DistributionSection(distributionObject, isCategorySection, places, allowDuplicates, minRolls, maxRolls, rewardPools));
        }
        return rewards;
    }

    public static RewardPool getRewardPool(JsonObject rewardPoolObject, String name) {
        boolean allowDuplicatesPool = true;
        if (rewardPoolObject.has("allow_duplicates"))
            allowDuplicatesPool = rewardPoolObject.get("allow_duplicates").getAsBoolean();
        rewardPoolObject.remove("allow_duplicates");
        rewardPoolObject.addProperty("allow_duplicates", allowDuplicatesPool);

        int minPoolRolls = 1;
        int maxPoolRolls = 1;
        JsonObject rollsObject = new JsonObject();
        if (rewardPoolObject.has("rolls"))
            rollsObject = rewardPoolObject.getAsJsonObject("rolls");

        if (rollsObject.has("min"))
            minPoolRolls = rollsObject.get("min").getAsInt();
        rollsObject.remove("min");
        rollsObject.addProperty("min", minPoolRolls);

        if (rollsObject.has("max"))
            maxPoolRolls = rollsObject.get("max").getAsInt();
        rollsObject.remove("max");
        rollsObject.addProperty("max", maxPoolRolls);

        rewardPoolObject.remove("rolls");
        rewardPoolObject.add("rolls", rollsObject);

        Map<Reward, Double> rewardList = new HashMap<>();
        JsonArray rewardsArray = new JsonArray();
        if (rewardPoolObject.has("rewards"))
            rewardsArray = rewardPoolObject.getAsJsonArray("rewards");

        JsonArray newRewardsArray = new JsonArray();
        for (JsonElement rewardElement : rewardsArray) {
            JsonObject rewardsObject = rewardElement.getAsJsonObject();

            double rewardWeight = 1;
            if (rewardsObject.has("weight"))
                rewardWeight = rewardsObject.get("weight").getAsDouble();
            rewardsObject.remove("weight");
            rewardsObject.addProperty("weight", rewardWeight);

            if (rewardsObject.has("reward")) {
                JsonObject rewardObject = rewardsObject.getAsJsonObject("reward");
                Reward reward = getReward(rewardObject, "temp");
                if (reward == null) continue;
                rewardsObject.remove("reward");
                rewardsObject.add("reward", reward.rewardObject());
                rewardList.put(reward, rewardWeight);
            } else if (rewardsObject.has("reward_preset")) {
                String rewardPreset = rewardsObject.get("reward_preset").getAsString();
                Reward reward = NovaRaids.INSTANCE.rewardPresetsConfig().getReward(rewardPreset);
                rewardsObject.remove("reward_preset");
                rewardsObject.addProperty("reward_preset", rewardPreset);
                if (reward == null) continue;
                rewardList.put(reward, rewardWeight);
            }

            newRewardsArray.add(rewardsObject);
        }
        rewardPoolObject.remove("rewards");
        rewardPoolObject.add("rewards", newRewardsArray);

        return new RewardPool(rewardPoolObject, UUID.randomUUID(), name, allowDuplicatesPool, minPoolRolls, maxPoolRolls, rewardList);
    }

    public static Reward getReward(JsonObject rewardObject, String name) {
        if (!rewardObject.has("type")) return null;
        String type = rewardObject.get("type").getAsString();

        if (type.equalsIgnoreCase("item")) {
            if (!rewardObject.has("item")) return null;
            Item rewardItem = Registries.ITEM.get(Identifier.of(rewardObject.get("item").getAsString()));

            ComponentChanges item_data = ComponentChanges.EMPTY;
            if (rewardObject.has("data"))
                item_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, rewardObject.get("data")).getOrThrow().getFirst();
            rewardObject.remove("data");
            rewardObject.add("data", ComponentChanges.CODEC.encode(item_data, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

            int countMin = 1;
            int countMax = 1;
            JsonObject countObject = new JsonObject();
            if (rewardObject.has("count"))
                countObject = rewardObject.getAsJsonObject("count");

            if (countObject.has("min"))
                countMin = countObject.get("min").getAsInt();
            countObject.remove("min");
            countObject.addProperty("min", countMin);
            if (countObject.has("max"))
                countMax = countObject.get("max").getAsInt();
            countObject.remove("max");
            countObject.addProperty("max", countMax);

            rewardObject.remove("count");
            rewardObject.add("count", countObject);

            return new ItemReward(rewardObject, name, rewardItem, item_data, countMin, countMax);
        } else if (type.equalsIgnoreCase("command")) {
            if (!rewardObject.has("commands")) return null;
            List<String> commands = rewardObject.getAsJsonArray("commands").asList().stream().map(JsonElement::getAsString).toList();

            JsonArray commandsArray = new JsonArray();
            for (String command : commands) {
                commandsArray.add(command);
            }
            rewardObject.remove("commands");
            rewardObject.add("commands", commandsArray);

            return new CommandReward(rewardObject, name, commands);
        } else if (type.equalsIgnoreCase("pokemon")) {
            if (!rewardObject.has("pokemon")) return null;
            JsonObject pokemonObject = rewardObject.getAsJsonObject("pokemon");

            String species = "weedle";
            if (pokemonObject.has("species"))
                species = pokemonObject.get("species").getAsString();
            pokemonObject.remove("species");
            pokemonObject.addProperty("species", species);

            int level = 1;
            if (pokemonObject.has("level"))
                level = pokemonObject.get("level").getAsInt();
            pokemonObject.remove("level");
            pokemonObject.addProperty("level", level);

            String ability = "noability";
            if (pokemonObject.has("ability"))
                ability = pokemonObject.get("ability").getAsString();
            pokemonObject.remove("ability");
            pokemonObject.addProperty("ability", ability);

            String nature = "serious";
            if (pokemonObject.has("nature"))
                nature = pokemonObject.get("nature").getAsString();
            pokemonObject.remove("nature");
            pokemonObject.addProperty("nature", nature);

            pokemonObject.remove("form");

            String features = "";
            if (pokemonObject.has("features"))
                features = pokemonObject.get("features").getAsString();
            pokemonObject.remove("features");
            pokemonObject.addProperty("features", features);

            String gender = "male";
            if (pokemonObject.has("gender"))
                gender = pokemonObject.get("gender").getAsString();
            pokemonObject.remove("gender");
            pokemonObject.addProperty("gender", gender);

            boolean shiny = false;
            if (pokemonObject.has("shiny"))
                shiny = pokemonObject.get("shiny").getAsBoolean();
            pokemonObject.remove("shiny");
            pokemonObject.addProperty("shiny", shiny);

            float scale = 1.0F;
            if (pokemonObject.has("scale"))
                scale = pokemonObject.get("scale").getAsFloat();
            pokemonObject.remove("scale");
            pokemonObject.addProperty("scale", scale);

            String heldItem = "";
            if (pokemonObject.has("held_item"))
                heldItem = pokemonObject.get("held_item").getAsString();
            pokemonObject.remove("held_item");
            pokemonObject.addProperty("held_item", heldItem);

            ComponentChanges heldItemData = ComponentChanges.EMPTY;
            if (pokemonObject.has("held_item_data"))
                heldItemData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, pokemonObject.get("held_item_data")).getOrThrow().getFirst();
            pokemonObject.remove("held_item_data");
            pokemonObject.add("held_item_data", ComponentChanges.CODEC.encode(heldItemData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

            List<String> moves = List.of("tackle");
            if (pokemonObject.has("moves"))
                moves = pokemonObject.getAsJsonArray("moves").asList().stream().map(JsonElement::getAsString).toList();

            JsonArray movesArray = new JsonArray();
            for (String move : moves)
                movesArray.add(move);
            pokemonObject.remove("moves");
            pokemonObject.add("moves", movesArray);

            IVs ivs = IVs.createRandomIVs(0);
            JsonObject ivsObject = new JsonObject();
            if (pokemonObject.has("ivs"))
                ivsObject = pokemonObject.getAsJsonObject("ivs");

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

            pokemonObject.remove("ivs");
            pokemonObject.add("ivs", ivsObject);

            EVs evs = EVs.createEmpty();
            JsonObject evsObject = new JsonObject();
            if (pokemonObject.has("evs"))
                evsObject = pokemonObject.getAsJsonObject("evs");

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

            pokemonObject.remove("evs");
            pokemonObject.add("evs", evsObject);

            rewardObject.remove("pokemon");
            rewardObject.add("pokemon", pokemonObject);

            return new PokemonReward(rewardObject, name, species, level, ability, nature, features, gender, shiny, scale, heldItem, heldItemData, moves, ivs, evs);
        }
        return null;
    }

    public static Contraband getContraband(JsonObject contrabandObject, String fileName) {
        List<Species> bannedPokemon = new ArrayList<>();
        List<Move> bannedMoves = new ArrayList<>();
        List<Ability> bannedAbilities = new ArrayList<>();
        List<Item> bannedHeldItems = new ArrayList<>();
        List<Item> bannedBagItems = new ArrayList<>();

        JsonArray bannedPokemonArray = new JsonArray();
        if (contrabandObject.has("banned_pokemon"))
            bannedPokemonArray = contrabandObject.getAsJsonArray("banned_pokemon");

        for (JsonElement pokemon : bannedPokemonArray) {
            String speciesName = pokemon.getAsString();
            Species species = PokemonSpecies.getByName(speciesName);
            if (species == null) {
                NovaRaids.LOGGER.error("[NovaRaids] Unknown species in {} contraband: {}", fileName, speciesName);
                continue;
            }
            bannedPokemon.add(species);
        }

        bannedPokemonArray = new JsonArray();
        for (Species species : bannedPokemon) {
            bannedPokemonArray.add(species.resourceIdentifier.getPath());
        }
        contrabandObject.remove("banned_pokemon");
        contrabandObject.add("banned_pokemon", bannedPokemonArray);

        JsonArray bannedMovesArray = new JsonArray();
        if (contrabandObject.has("banned_moves"))
            bannedMovesArray = contrabandObject.getAsJsonArray("banned_moves");

        for (JsonElement move : bannedMovesArray) {
            String moveName = move.getAsString();
            MoveTemplate moveTemplate = Moves.getByName(moveName);
            if (moveTemplate == null) {
                NovaRaids.LOGGER.error("[NovaRaids] Unknown move in {} contraband: {}", fileName, moveName);
                continue;
            }
            bannedMoves.add(moveTemplate.create());
        }

        bannedMovesArray = new JsonArray();
        for (Move move : bannedMoves) {
            bannedMovesArray.add(move.getTemplate().getName().toLowerCase());
        }
        contrabandObject.remove("banned_moves");
        contrabandObject.add("banned_moves", bannedMovesArray);

        JsonArray bannedAbilitiesArray = new JsonArray();
        if (contrabandObject.has("banned_abilities"))
            bannedAbilitiesArray = contrabandObject.getAsJsonArray("banned_abilities");

        for (JsonElement ability : bannedAbilitiesArray) {
            String abilityName = ability.getAsString();
            AbilityTemplate abilityTemplate = Abilities.get(abilityName);
            if (abilityTemplate == null) {
                NovaRaids.LOGGER.error("[NovaRaids] Unknown ability in {} contraband: {}", fileName, abilityName);
                continue;
            }
            bannedAbilities.add(abilityTemplate.create(false, Priority.LOWEST));
        }

        bannedAbilitiesArray = new JsonArray();
        for (Ability ability : bannedAbilities) {
            bannedAbilitiesArray.add(ability.getTemplate().getName().toLowerCase());
        }
        contrabandObject.remove("banned_abilities");
        contrabandObject.add("banned_abilities", bannedAbilitiesArray);

        JsonArray bannedHeldItemsArray = new JsonArray();
        if (contrabandObject.has("banned_held_items"))
            bannedHeldItemsArray = contrabandObject.getAsJsonArray("banned_held_items");

        for (JsonElement heldItemElement : bannedHeldItemsArray) {
            String heldItemID = heldItemElement.getAsString();
            Optional<Item> heldItem = Registries.ITEM.getOrEmpty(Identifier.of(heldItemID));
            if (heldItem.isEmpty()) {
                NovaRaids.LOGGER.error("[NovaRaids] Unknown held item in {} contraband: {}", fileName, heldItemID);
                continue;
            }
            bannedHeldItems.add(heldItem.get());
        }

        bannedHeldItemsArray = new JsonArray();
        for (Item heldItem : bannedHeldItems) {
            bannedHeldItemsArray.add(Registries.ITEM.getId(heldItem).toString());
        }
        contrabandObject.remove("banned_held_items");
        contrabandObject.add("banned_held_items", bannedHeldItemsArray);

        JsonArray bannedBagItemsArray = new JsonArray();
        if (contrabandObject.has("banned_bag_items"))
            bannedBagItemsArray = contrabandObject.getAsJsonArray("banned_bag_items");

        for (JsonElement bagItemElement : bannedBagItemsArray) {
            String bagItemID = bagItemElement.getAsString();
            Optional<Item> bagItem = Registries.ITEM.getOrEmpty(Identifier.of(bagItemID));
            if (bagItem.isEmpty()) {
                NovaRaids.LOGGER.error("[NovaRaids] Unknown bag item in {} contraband: {}", fileName, bagItemID);
                continue;
            }
            bannedBagItems.add(bagItem.get());
        }

        bannedBagItemsArray = new JsonArray();
        for (Item heldItem : bannedBagItems) {
            bannedBagItemsArray.add(Registries.ITEM.getId(heldItem).toString());
        }
        contrabandObject.remove("banned_bag_items");
        contrabandObject.add("banned_bag_items", bannedBagItemsArray);

        return new Contraband(contrabandObject, bannedPokemon, bannedMoves, bannedAbilities, bannedHeldItems, bannedBagItems);
    }

    public static Voucher getVoucher(JsonObject voucherObject, Voucher defaultVoucher) {
        Item voucherItem = defaultVoucher == null ? Items.FEATHER : defaultVoucher.voucherItem();
        String voucherName = defaultVoucher == null ? "<aqua>Raid Voucher" : defaultVoucher.voucherName();
        List<String> voucherLore = defaultVoucher == null ? List.of("<gray>Use this to start a raid!") : defaultVoucher.voucherLore();
        ComponentChanges voucherData = defaultVoucher == null ? ComponentChanges.EMPTY : defaultVoucher.voucherData();

        if (voucherObject.has("voucher_item"))
            voucherItem = Registries.ITEM.get(Identifier.of(voucherObject.get("voucher_item").getAsString()));
        voucherObject.remove("voucher_item");
        voucherObject.addProperty("voucher_item", Registries.ITEM.getId(voucherItem).toString());

        if (voucherObject.has("voucher_name"))
            voucherName = voucherObject.get("voucher_name").getAsString();
        voucherObject.remove("voucher_name");
        voucherObject.addProperty("voucher_name", voucherName);

        if (voucherObject.has("voucher_lore"))
            voucherLore = voucherObject.get("voucher_lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        JsonArray loreArray = new JsonArray();
        for (String line : voucherLore) {
            loreArray.add(line);
        }
        voucherObject.remove("voucher_lore");
        voucherObject.add("voucher_lore", loreArray);

        if (voucherObject.has("voucher_data"))
            voucherData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, voucherObject.get("voucher_data")).getOrThrow().getFirst();
        voucherObject.remove("voucher_data");
        voucherObject.add("voucher_data", ComponentChanges.CODEC.encode(voucherData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

        return new Voucher(voucherObject, voucherItem, voucherName, voucherLore, voucherData);
    }

    public static Pass getPass(JsonObject passObject, Pass defaultPass) {
        Item passItem = defaultPass == null ? Items.PAPER : defaultPass.passItem();
        String passName = defaultPass == null ? "<light_purple>Raid Pass" : defaultPass.passName();
        List<String> passLore = defaultPass == null ? List.of("<gray>Use this to join a raid!") : defaultPass.passLore();
        ComponentChanges passData = defaultPass == null ? ComponentChanges.EMPTY : defaultPass.passData();

        if (passObject.has("pass_item"))
            passItem = Registries.ITEM.get(Identifier.of(passObject.get("pass_item").getAsString()));
        passObject.remove("pass_item");
        passObject.addProperty("pass_item", Registries.ITEM.getId(passItem).toString());

        if (passObject.has("pass_name"))
            passName = passObject.get("pass_name").getAsString();
        passObject.remove("pass_name");
        passObject.addProperty("pass_name", passName);

        if (passObject.has("pass_lore"))
            passLore = passObject.get("pass_lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        JsonArray passLoreArray = new JsonArray();
        for (String line : passLore) {
            passLoreArray.add(line);
        }
        passObject.remove("pass_lore");
        passObject.add("pass_lore", passLoreArray);

        if (passObject.has("pass_data"))
            passData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, passObject.get("pass_data")).getOrThrow().getFirst();
        passObject.remove("pass_data");
        passObject.add("pass_data", ComponentChanges.CODEC.encode(passData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

        return new Pass(passObject, passItem, passName, passLore, passData);
    }

    public static List<RaidBall> getRaidBalls(JsonObject raidBallsObject) {
        List<RaidBall> raidBalls = new ArrayList<>();
        if (raidBallsObject.isEmpty())
            raidBallsObject.add("default", new JsonObject());
        for (String raidBallID : raidBallsObject.keySet()) {
            JsonObject ballObject = raidBallsObject.getAsJsonObject(raidBallID);
            Item pokeball = CobblemonItems.POKE_BALL;
            String pokeballName = "<red>Raid Ball";
            List<String> pokeballLore = List.of("<gray>Use this to try and catch raid bosses!");
            ComponentChanges pokeballData = ComponentChanges.EMPTY;

            if (ballObject.has("pokeball"))
                pokeball = Registries.ITEM.get(Identifier.of(ballObject.get("pokeball").getAsString()));
            ballObject.remove("pokeball");
            ballObject.addProperty("pokeball", Registries.ITEM.getId(pokeball).toString());

            if (ballObject.has("pokeball_name"))
                pokeballName = ballObject.get("pokeball_name").getAsString();
            ballObject.remove("pokeball_name");
            ballObject.addProperty("pokeball_name", pokeballName);

            if (ballObject.has("pokeball_lore"))
                pokeballLore = ballObject.get("pokeball_lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
            JsonArray pokeballLoreArray = new JsonArray();
            for (String line : pokeballLore) {
                pokeballLoreArray.add(line);
            }
            ballObject.remove("pokeball_lore");
            ballObject.add("pokeball_lore", pokeballLoreArray);

            if (ballObject.has("pokeball_data"))
                pokeballData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, ballObject.get("pokeball_data")).getOrThrow().getFirst();
            ballObject.remove("pokeball_data");
            ballObject.add("pokeball_data", ComponentChanges.CODEC.encode(pokeballData, JsonOps.INSTANCE, new JsonObject()).getOrThrow());

            raidBalls.add(new RaidBall(ballObject, raidBallID, pokeball, pokeballName, pokeballLore, pokeballData));
        }
        return raidBalls;
    }
}
