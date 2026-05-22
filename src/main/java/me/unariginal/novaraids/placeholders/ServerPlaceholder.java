package me.unariginal.novaraids.placeholders;

import java.util.List;

public interface ServerPlaceholder {
    GenericResult handle(List<String> args);
    List<String> id();
}
