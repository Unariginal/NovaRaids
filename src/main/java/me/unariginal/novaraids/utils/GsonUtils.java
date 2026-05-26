package me.unariginal.novaraids.utils;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon.mod.common.pokemon.IVs;
import com.cobblemon.mod.common.util.adapters.PokemonPropertiesAdapter;
import com.google.gson.*;
import com.google.gson.internal.Streams;
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

import java.io.IOException;

public class GsonUtils {
    public static Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .registerTypeAdapter(ItemStack.class, new ItemStackTypeAdapter())
            .registerTypeAdapter(PokemonProperties.class, new PokemonPropertiesAdapter(true))
            .registerTypeAdapter(IVs.class, new IVsTypeAdapter())
            .registerTypeAdapter(EVs.class, new EVsTypeAdapter())
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

            Streams.write(element, jsonWriter);
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

    public static class IVsTypeAdapter extends TypeAdapter<IVs> {
        @Override
        public void write(JsonWriter jsonWriter, IVs ivs) throws IOException {
            if (ivs == null) {
                jsonWriter.nullValue();
                return;
            }

            DataResult<JsonElement> result = IVs.getCODEC().encodeStart(JsonOps.INSTANCE, ivs);
            JsonElement element = result.getOrThrow();

            Streams.write(element, jsonWriter);
        }

        @Override
        public IVs read(JsonReader jsonReader) {
            JsonElement element = JsonParser.parseReader(jsonReader);

            IVs ivs;
            try {
                DataResult<IVs> result = IVs.getCODEC().parse(JsonOps.INSTANCE, element);

                ivs = result.getOrThrow();
            } catch (IllegalStateException e) {
                ivs = new IVs();
            }

            return ivs;
        }
    }

    public static class EVsTypeAdapter extends TypeAdapter<EVs> {
        @Override
        public void write(JsonWriter jsonWriter, EVs evs) throws IOException {
            if (evs == null) {
                jsonWriter.nullValue();
                return;
            }

            DataResult<JsonElement> result = EVs.getCODEC().encodeStart(JsonOps.INSTANCE, evs);
            JsonElement element = result.getOrThrow();

            Streams.write(element, jsonWriter);
        }

        @Override
        public EVs read(JsonReader jsonReader) {
            JsonElement element = JsonParser.parseReader(jsonReader);

            EVs evs;
            try {
                DataResult<EVs> result = EVs.getCODEC().parse(JsonOps.INSTANCE, element);

                evs = result.getOrThrow();
            } catch (IllegalStateException e) {
                evs = new EVs();
            }

            return evs;
        }
    }
}
