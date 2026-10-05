package com.viaversion.viaversion.api;

/** Test double with the same signatures as ViaVersion 5. */
public final class Via {
    public static ViaAPI<?> api;

    public static ViaAPI<?> getAPI() {
        return api;
    }
}
