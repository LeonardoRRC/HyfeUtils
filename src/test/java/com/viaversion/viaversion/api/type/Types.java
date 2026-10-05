package com.viaversion.viaversion.api.type;

import com.viaversion.viaversion.libs.gson.JsonElement;

import java.util.UUID;

public final class Types {
    public static final Type<JsonElement> COMPONENT = new Type<>(JsonElement.class);
    public static final Type<Byte> BYTE = new Type<>(Byte.class);
    public static final Type<UUID> UUID = new Type<>(UUID.class);
    public static final Type<Integer> VAR_INT = new Type<>(Integer.class);
    public static final Type<Integer> INT = new Type<>(Integer.class);
    public static final Type<Float> FLOAT = new Type<>(Float.class);
}
