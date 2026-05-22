package me.unariginal.novaraids.utils;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.adapters.PokemonPropertiesAdapter;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import me.unariginal.novaraids.config.RewardPoolsConfig;
import me.unariginal.novaraids.config.RewardPresetsConfig;
import me.unariginal.novaraids.data.events.ParticleEvent;
import me.unariginal.novaraids.data.events.SnowstormEntityParticleEvent;
import me.unariginal.novaraids.data.events.SnowstormParticleEvent;
import me.unariginal.novaraids.data.events.VanillaParticleEvent;
import me.unariginal.novaraids.data.rewards.DistributionSection;
import me.unariginal.novaraids.data.schedule.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static java.lang.System.out;

public class GsonUtils {
    public static Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .registerTypeHierarchyAdapter(ItemStack.class, new ItemStackTypeAdapter())
            .registerTypeHierarchyAdapter(Pokemon.class, new PokemonTypeAdapter())
            .registerTypeHierarchyAdapter(PokemonProperties.class, new PokemonPropertiesAdapter(true))
            .registerTypeAdapterFactory(
                    RuntimeTypeAdapterFactory
                            .of(Schedule.class, "type")
                            .registerSubtype(CronSchedule.class, "cron")
                            .registerSubtype(SpecificSchedule.class, "specific")
                            .registerSubtype(RandomSchedule.class, "random")
            )
            .registerTypeAdapterFactory(
                    RuntimeTypeAdapterFactory
                            .of(ScheduleSection.class, "type")
                            .registerSubtype(ScheduleCategory.class, "category")
                            .registerSubtype(ScheduleBoss.class, "boss")
            )
            .registerTypeAdapterFactory(
                    RuntimeTypeAdapterFactory
                            .of(RewardPresetsConfig.Reward.class, "type")
                            .registerSubtype(RewardPresetsConfig.ItemReward.class, "item")
                            .registerSubtype(RewardPresetsConfig.CommandReward.class, "command")
                            .registerSubtype(RewardPresetsConfig.PokemonReward.class, "pokemon")
            )
            .registerTypeAdapterFactory(
                    RuntimeTypeAdapterFactory
                            .of(RewardPoolsConfig.RewardItem.class, "type")
                            .registerSubtype(RewardPoolsConfig.RewardItemUndefined.class, "unset")
                            .registerSubtype(RewardPoolsConfig.RewardItemPredefined.class, "preset")
            )
            .registerTypeAdapterFactory(
                    RuntimeTypeAdapterFactory
                            .of(DistributionSection.RewardPoolSection.class, "type")
                            .registerSubtype(DistributionSection.UndefinedRewardPoolSection.class, "unset")
                            .registerSubtype(DistributionSection.PredefinedRewardPoolSection.class, "preset")
            )
            .registerTypeAdapterFactory(
                    RuntimeTypeAdapterFactory
                            .of(ParticleEvent.class, "type")
                            .registerSubtype(VanillaParticleEvent.class, "vanilla")
                            .registerSubtype(SnowstormParticleEvent.class, "snowstorm")
                            .registerSubtype(SnowstormEntityParticleEvent.class, "snowstorm_entity")
            )
            .create();

    public static class ItemStackTypeAdapter extends TypeAdapter<ItemStack> {
        @Override
        public void write(JsonWriter jsonWriter, ItemStack itemStack) throws IOException {
            if (itemStack == null) {
                jsonWriter.nullValue();
                return;
            }
            JsonElement element;
            try {
                DataResult<JsonElement> result = ItemStack.OPTIONAL_CODEC.encodeStart(JsonOps.INSTANCE, itemStack);

                element = result.getOrThrow();
            } catch (IllegalStateException e) {
                // This is from minecraft:air, so let's manually set it to air in this case!
                JsonObject object = new JsonObject();
                object.addProperty("id", "minecraft:air");
                element = object;
            }

            new Gson().toJson(element, out);
        }

        @Override
        public ItemStack read(JsonReader jsonReader) {
            JsonElement element = JsonParser.parseReader(jsonReader);

            ItemStack itemStack;
            try {
                DataResult<ItemStack> result = ItemStack.OPTIONAL_CODEC.parse(JsonOps.INSTANCE, element);

                itemStack = result.getOrThrow();
            } catch (IllegalStateException e) {
                itemStack = new ItemStack(Items.AIR);
            }

            return itemStack;
        }
    }

    public static class PokemonTypeAdapter extends TypeAdapter<Pokemon> {
        @Override
        public void write(JsonWriter jsonWriter, Pokemon pokemon) throws IOException {
            if (pokemon == null) {
                jsonWriter.nullValue();
                return;
            }
            JsonElement element;
            try {
                DataResult<JsonElement> result = Pokemon.Companion.getCODEC().encodeStart(JsonOps.INSTANCE, pokemon);

                element = result.getOrThrow();
            } catch (IllegalStateException e) {
                // This is from minecraft:air, so let's manually set it to air in this case!
                JsonObject object = new JsonObject();
                object.addProperty("id", "minecraft:air");
                element = object;
            }

            new Gson().toJson(element, out);
        }

        @Override
        public @Nullable Pokemon read(JsonReader jsonReader) {
            JsonElement element = JsonParser.parseReader(jsonReader);

            Pokemon pokemon;
            try {
                DataResult<Pokemon> result = Pokemon.Companion.getCODEC().parse(JsonOps.INSTANCE, element);

                pokemon = result.getOrThrow();
            } catch (IllegalStateException e) {
                pokemon = null;
            }

            return pokemon;
        }
    }

//    public static class PokemonPropertiesTypeAdapter extends TypeAdapter<PokemonProperties> {
//        @Override
//        public void write(JsonWriter jsonWriter, Pokemon pokemon) throws IOException {
//            if (pokemon == null) {
//                jsonWriter.nullValue();
//                return;
//            }
//            JsonElement element;
//            try {
//                DataResult<JsonElement> result = Pokemon.Companion.getCODEC().encodeStart(JsonOps.INSTANCE, pokemon);
//
//                element = result.getOrThrow();
//            } catch (IllegalStateException e) {
//                // This is from minecraft:air, so let's manually set it to air in this case!
//                JsonObject object = new JsonObject();
//                object.addProperty("id", "minecraft:air");
//                element = object;
//            }
//
//            new Gson().toJson(element, out);
//        }
//
//        @Override
//        public @Nullable Pokemon read(JsonReader jsonReader) {
//            JsonElement element = JsonParser.parseReader(jsonReader);
//
//            Pokemon pokemon;
//            try {
//                DataResult<Pokemon> result = Pokemon.Companion.getCODEC().parse(JsonOps.INSTANCE, element);
//
//                pokemon = result.getOrThrow();
//            } catch (IllegalStateException e) {
//                pokemon = null;
//            }
//
//            return pokemon;
//        }
//    }
}
