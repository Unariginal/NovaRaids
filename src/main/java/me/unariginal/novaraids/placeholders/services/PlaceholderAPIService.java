package me.unariginal.novaraids.placeholders.services;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.placeholders.PlayerPlaceholder;
import me.unariginal.novaraids.placeholders.ServerPlaceholder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderAPIService {
    public void registerPlayer(PlayerPlaceholder placeholder) {
        placeholder.id().forEach(id -> Placeholders.register(Identifier.of(NovaRaids.MOD_ID, id), (ctx, arg) -> {
            ServerPlayerEntity player = ctx.player();
            if (player == null) return PlaceholderResult.invalid("NO PLAYER");

            List<String> args = arg != null ? List.of(parse(arg, player).split(":")) : new ArrayList<>();
            GenericResult result = placeholder.handle(player, args);
            if (result.isSuccessful) {
                return PlaceholderResult.value(NovaRaids.INSTANCE.audience.toNative(result.asComponent()));
            } else {
                return PlaceholderResult.invalid(result.string);
            }
        }));
    }

    public void registerServer(ServerPlaceholder placeholder) {
        placeholder.id().forEach(id -> Placeholders.register(Identifier.of(NovaRaids.MOD_ID, id), (ctx, arg) -> {
            List<String> args = arg != null ? List.of(parse(arg, ctx.player()).split(":")) : new ArrayList<>();
            GenericResult result = placeholder.handle(args);
            if (result.isSuccessful) {
                return PlaceholderResult.value(NovaRaids.INSTANCE.audience.toNative(result.asComponent()));
            } else {
                return PlaceholderResult.invalid(result.string);
            }
        }));
    }

    public String parse(String input, @Nullable ServerPlayerEntity player) {
        return Placeholders.parseText(Text.literal(input), player != null ? PlaceholderContext.of(player) : PlaceholderContext.of(NovaRaids.INSTANCE.server)).getString();
    }
}
