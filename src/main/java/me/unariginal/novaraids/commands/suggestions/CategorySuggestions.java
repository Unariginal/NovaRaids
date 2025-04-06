package me.unariginal.novaraids.commands.suggestions;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.Category;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class CategorySuggestions implements SuggestionProvider<ServerCommandSource> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        for (Category category : NovaRaids.INSTANCE.config().getCategories()) {
            builder.suggest(category.name());
        }
        return builder.buildFuture();
    }
}
