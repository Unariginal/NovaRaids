package me.unariginal.novaraids.data;

import com.google.gson.JsonObject;

public record FieldData(JsonObject fieldObject, boolean inline, String name, String value, boolean insertLeaderboardAfter) {
}
