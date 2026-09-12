package com.hyfecraft.hyfeutils.event;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public final class EventRegistration implements AutoCloseable {
    private final Listener listener;
    private boolean closed;

    EventRegistration(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void close() {
        if (!closed) {
            HandlerList.unregisterAll(listener);
            closed = true;
        }
    }
}
