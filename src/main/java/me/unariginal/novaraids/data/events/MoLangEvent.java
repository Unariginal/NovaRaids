package me.unariginal.novaraids.data.events;

import com.bedrockk.molang.runtime.MoLangRuntime;
import com.bedrockk.molang.runtime.value.MoValue;
import com.cobblemon.mod.common.api.molang.MoLangFunctions;
import com.cobblemon.mod.common.api.scripting.CobblemonScripts;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import static com.cobblemon.mod.common.util.MoLangExtensionsKt.asExpressionLike;
import static com.cobblemon.mod.common.util.MoLangExtensionsKt.withQueryValue;

public class MoLangEvent {
    public String type;
    public String molang;

    public void runMoLang(ServerPlayerEntity player, PokemonEntity pokemonEntity, Integer damage) {
        MoLangRuntime runtime = MoLangFunctions.INSTANCE.setup(new MoLangRuntime());
        withQueryValue(runtime, "pokemon", MoLangFunctions.INSTANCE.asMoLangValue(pokemonEntity));
        withQueryValue(runtime, "player", MoLangFunctions.INSTANCE.asMoLangValue(player));
        if (damage != null) {
            withQueryValue(runtime, "damage", MoValue.of(damage));
        }

        if (type.equals("script")) CobblemonScripts.run(Identifier.of(molang), runtime);
        else asExpressionLike(molang).resolve(runtime, runtime.getEnvironment().context.getMap());
    }
}
