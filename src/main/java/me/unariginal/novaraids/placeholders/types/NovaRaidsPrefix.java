package me.unariginal.novaraids.placeholders.types;

import me.unariginal.novaraids.placeholders.GenericResult;
import me.unariginal.novaraids.placeholders.interfaces.ServerPlaceholder;

import java.util.List;

import static me.unariginal.novaraids.config.ConfigManager.MESSAGES;

public class NovaRaidsPrefix implements ServerPlaceholder {
    @Override
    public GenericResult handle(List<String> args) {
        return GenericResult.valid(MESSAGES.prefix);
    }

    @Override
    public List<String> id() {
        return List.of("novaraids_prefix", "prefix");
    }
}
