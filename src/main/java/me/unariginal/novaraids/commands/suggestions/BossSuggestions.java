package me.unariginal.novaraids.commands.suggestions;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.BossSettings.Boss;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class BossSuggestions implements SuggestionProvider<ServerCommandSource> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        for (Boss boss : NovaRaids.INSTANCE.config().getBosses()) {
            builder.suggest(boss.name());
        }
        return builder.buildFuture();
    }
}
