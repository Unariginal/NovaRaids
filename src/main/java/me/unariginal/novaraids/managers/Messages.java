package me.unariginal.novaraids.managers;

import java.util.Map;

public record Messages(String prefix, Map<String, String> messages) {
    public String message(String key) {
        return messages.get(key);
    }
}
