package com.hyfecraft.hyfeutils.event;

import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.function.Consumer;

public final class EventService {
    private final Plugin plugin;

    public EventService(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public <T extends Event> EventRegistration listen(Class<T> eventType, Consumer<T> handler) {
        return listen(eventType, EventPriority.NORMAL, false, handler);
    }

    public <T extends Event> EventRegistration listen(Class<T> eventType, EventPriority priority,
                                                       boolean ignoreCancelled, Consumer<T> handler) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(handler, "handler");
        Listener listener = new Listener() { };
        EventExecutor executor = (ignored, event) -> {
            if (eventType.isInstance(event)) {
                handler.accept(eventType.cast(event));
            }
        };
        plugin.getServer().getPluginManager().registerEvent(
                eventType, listener, priority, executor, plugin, ignoreCancelled);
        return new EventRegistration(listener);
    }
}
