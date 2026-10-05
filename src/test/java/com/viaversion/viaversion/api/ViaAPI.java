package com.viaversion.viaversion.api;

import com.viaversion.viaversion.api.connection.UserConnection;

import java.util.UUID;

public interface ViaAPI<T> {
    int getPlayerVersion(UUID uuid);

    UserConnection getConnection(UUID uuid);
}
