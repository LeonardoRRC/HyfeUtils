package com.hyfecraft.hyfeutils.command;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class CommandService {
    private final JavaPlugin plugin;

    public CommandService(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public PluginCommand bind(String name, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            throw new IllegalArgumentException("Command '" + name + "' is not declared in plugin.yml");
        }
        command.setExecutor(Objects.requireNonNull(executor, "executor"));
        return command;
    }

    public PluginCommand bind(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = bind(name, executor);
        command.setTabCompleter(Objects.requireNonNull(tabCompleter, "tabCompleter"));
        return command;
    }
}
