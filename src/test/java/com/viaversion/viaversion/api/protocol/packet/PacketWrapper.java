package com.viaversion.viaversion.api.protocol.packet;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.type.Type;

import java.util.ArrayList;
import java.util.List;

public interface PacketWrapper {
    /** Packets "sent" by the tests. */
    List<Recorded> SENT = new ArrayList<>();

    static PacketWrapper create(PacketType packetType, UserConnection connection) {
        Recorded recorded = new Recorded(packetType, connection, new ArrayList<>(), new Class<?>[1]);
        return new PacketWrapper() {
            @Override
            public <T> void write(Type<T> type, T value) {
                recorded.values().add(value);
            }

            @Override
            public void scheduleSend(Class<? extends Protocol> protocol) {
                recorded.protocol()[0] = protocol;
                SENT.add(recorded);
            }
        };
    }

    <T> void write(Type<T> type, T value);

    void scheduleSend(Class<? extends Protocol> protocol);

    record Recorded(PacketType type, UserConnection connection, List<Object> values, Class<?>[] protocol) { }
}
