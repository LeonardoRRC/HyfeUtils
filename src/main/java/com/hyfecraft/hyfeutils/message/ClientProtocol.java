package com.hyfecraft.hyfeutils.message;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/** Optional ViaVersion bridge. The API remains usable without ViaVersion. */
final class ClientProtocol {
    private static final String VIA = "com.viaversion.viaversion.api.Via";

    private ClientProtocol() { }

    static int get(Player player) {
        try {
            Class<?> via = Class.forName(VIA);
            Object api = via.getMethod("getAPI").invoke(null);
            Class<?> apiType = Class.forName("com.viaversion.viaversion.api.ViaAPI");
            Method method = apiType.getMethod("getPlayerVersion", UUID.class);
            Object version = method.invoke(api, player.getUniqueId());
            return version instanceof Number ? ((Number) version).intValue() : -1;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return -1;
        }
    }
}
