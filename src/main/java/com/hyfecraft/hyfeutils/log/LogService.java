package com.hyfecraft.hyfeutils.log;

import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.logging.Level;

public final class LogService {
    private final Plugin plugin;

    public LogService(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void info(String message) {
        plugin.getLogger().info(message);
    }

    public void warning(String message) {
        plugin.getLogger().warning(message);
    }

    public void severe(String message) {
        plugin.getLogger().severe(message);
    }

    public void log(Level level, String message, Throwable throwable) {
        plugin.getLogger().log(level, message, throwable);
    }
}
