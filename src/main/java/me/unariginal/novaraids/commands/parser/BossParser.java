package me.unariginal.novaraids.commands.parser;

import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.bosssettings.Boss;
import net.minecraft.server.command.ServerCommandSource;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.caption.CaptionVariable;
import org.incendo.cloud.caption.StandardCaptionKeys;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.exception.parsing.ParserException;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

public class BossParser implements ArgumentParser<ServerCommandSource, Boss> {

    @Override
    public @NonNull ArgumentParseResult<@NonNull Boss> parse(@NonNull CommandContext<@NonNull ServerCommandSource> commandContext, @NonNull CommandInput commandInput) {
        var input = commandInput.peekString();
        var boss = NovaRaids.INSTANCE.bossesConfig().getBoss(input);
        if(boss == null) {
            return ArgumentParseResult.failure(new BossParseException(input, commandContext));
        }
        return ArgumentParseResult.success(boss);
    }

    @Override
    public @NonNull SuggestionProvider<ServerCommandSource> suggestionProvider() {
        return SuggestionProvider.suggesting(
                NovaRaids.INSTANCE.bossesConfig().bosses.stream().map(Boss::bossId)
        );
    }

    public static class BossParseException extends ParserException {

        protected BossParseException(String input, CommandContext<?> context) {
            super(
                    Boss.class,
                    context,
                    StandardCaptionKeys.ARGUMENT_PARSE_FAILURE_STRING,
                    CaptionVariable.of("input", input)
            );
        }
    }
}