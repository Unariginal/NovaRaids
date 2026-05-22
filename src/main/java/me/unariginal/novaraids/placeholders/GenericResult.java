package me.unariginal.novaraids.placeholders;

import net.kyori.adventure.text.Component;

public class GenericResult {
    public final String string;
    public final Boolean isSuccessful;

    private GenericResult(String string, Boolean isSuccessful) {
        this.string = string;
        this.isSuccessful = isSuccessful;
    }

    private GenericResult(String string) {
        this(string, true);
    }

    public static GenericResult valid(String result) {
        return new GenericResult(result, true);
    }

    public static GenericResult valid(Integer result) {
        return valid(result.toString());
    }

    public static GenericResult valid(Boolean result) {
        return valid(result.toString());
    }

    public static GenericResult valid(Float result) {
        return valid(result.toString());
    }

    public static GenericResult valid(Double result) {
        return valid(result.toString());
    }

    public static GenericResult invalid(String result) {
        return new GenericResult(result, false);
    }

    public Component asComponent() {
        return Component.text(string);
    }
}
