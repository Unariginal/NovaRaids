package me.unariginal.novaraids.placeholders.interfaces;

import me.unariginal.novaraids.placeholders.GenericResult;

import java.util.List;

public interface ServerPlaceholder {
    GenericResult handle(List<String> args);
    List<String> id();
}
