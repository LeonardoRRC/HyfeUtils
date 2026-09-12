package com.hyfecraft.hyfeutils.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class ConfigurationService {
    private final Plugin plugin;

    public ConfigurationService(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public FileConfiguration load(String fileName) {
        return load(fileName, fileName);
    }

    public FileConfiguration load(String fileName, String resourceName) {
        File file = file(fileName);
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void save(FileConfiguration configuration, String fileName) {
        Objects.requireNonNull(configuration, "configuration");
        try {
            configuration.save(file(fileName));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save " + fileName, exception);
        }
    }

    public File file(String fileName) {
        return new File(plugin.getDataFolder(), fileName);
    }
}
