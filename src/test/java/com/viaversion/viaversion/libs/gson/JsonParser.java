package com.viaversion.viaversion.libs.gson;

public final class JsonParser {
    public static JsonElement parseString(String json) {
        return new JsonElement(json);
    }
}
