package com.hyfecraft.hyfeutils;

import com.hyfecraft.hyfeutils.command.CommandService;
import com.hyfecraft.hyfeutils.config.ConfigurationService;
import com.hyfecraft.hyfeutils.event.EventService;
import com.hyfecraft.hyfeutils.message.ActionBarService;
import com.hyfecraft.hyfeutils.message.MessageService;
import com.hyfecraft.hyfeutils.message.TitleService;
import com.hyfecraft.hyfeutils.log.LogService;
import com.hyfecraft.hyfeutils.scheduler.SchedulerService;
import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/** Entry point for the library. It does not own a Bukkit plugin lifecycle. */
public final class HyfeUtils implements AutoCloseable {
    private final BukkitAudiences audiences;
    private final TextService text;
    private final MessageService messages;
    private final ActionBarService actionBar;
    private final TitleService titles;
    private final SchedulerService scheduler;
    private final EventService events;
    private final ConfigurationService configuration;
    private final CommandService commands;
    private final LogService logger;

    private HyfeUtils(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.audiences = BukkitAudiences.create(plugin);
        this.text = new TextService();
        this.messages = new MessageService(audiences, text);
        this.actionBar = new ActionBarService(audiences, text);
        this.titles = new TitleService(audiences, text);
        this.scheduler = new SchedulerService(plugin);
        this.events = new EventService(plugin);
        this.configuration = new ConfigurationService(plugin);
        this.commands = new CommandService(plugin);
        this.logger = new LogService(plugin);
    }

    public static HyfeUtils create(JavaPlugin plugin) {
        return new HyfeUtils(plugin);
    }

    public TextService text() { return text; }
    public MessageService messages() { return messages; }
    public ActionBarService actionBar() { return actionBar; }
    public TitleService titles() { return titles; }
    public SchedulerService scheduler() { return scheduler; }
    public EventService events() { return events; }
    public ConfigurationService config() { return configuration; }
    public CommandService commands() { return commands; }
    public LogService logger() { return logger; }

    @Override
    public void close() {
        audiences.close();
    }
}
