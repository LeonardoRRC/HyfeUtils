package com.hyfecraft.hyfeutils;

import com.hyfecraft.hyfeutils.command.CommandService;
import com.hyfecraft.hyfeutils.config.ConfigurationService;
import com.hyfecraft.hyfeutils.event.EventRegistration;
import com.hyfecraft.hyfeutils.event.EventService;
import com.hyfecraft.hyfeutils.message.AnimatedTitleService;
import com.hyfecraft.hyfeutils.message.ActionBarService;
import com.hyfecraft.hyfeutils.message.BossBarService;
import com.hyfecraft.hyfeutils.message.ChatService;
import com.hyfecraft.hyfeutils.message.MessageService;
import com.hyfecraft.hyfeutils.message.TabListService;
import com.hyfecraft.hyfeutils.message.TitleService;
import com.hyfecraft.hyfeutils.log.LogService;
import com.hyfecraft.hyfeutils.platform.Dispatcher;
import com.hyfecraft.hyfeutils.platform.ViaBridge;
import com.hyfecraft.hyfeutils.scheduler.SchedulerService;
import com.hyfecraft.hyfeutils.text.ClickableTextService;
import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/** Entry point for the library. It does not own a Bukkit plugin lifecycle. */
public final class HyfeUtils implements AutoCloseable {
    private final BukkitAudiences audiences;
    private final ViaBridge via;
    private final Dispatcher dispatcher;
    private final EventRegistration quitListener;
    private final TextService text;
    private final MessageService messages;
    private final ActionBarService actionBar;
    private final TitleService titles;
    private final TabListService tabList;
    private final AnimatedTitleService animatedTitles;
    private final ChatService chat;
    private final ClickableTextService clickableText;
    private final BossBarService bossBar;
    private final SchedulerService scheduler;
    private final EventService events;
    private final ConfigurationService configuration;
    private final CommandService commands;
    private final LogService logger;

    private HyfeUtils(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.audiences = BukkitAudiences.create(plugin);
        this.via = ViaBridge.create(plugin.getLogger());
        this.dispatcher = new Dispatcher(audiences, via);
        this.text = new TextService();
        this.scheduler = new SchedulerService(plugin);
        this.messages = new MessageService(dispatcher, text);
        this.actionBar = new ActionBarService(dispatcher, text);
        this.titles = new TitleService(dispatcher, text);
        this.tabList = new TabListService(dispatcher, text);
        this.animatedTitles = new AnimatedTitleService(dispatcher, text, scheduler);
        this.chat = new ChatService(dispatcher, text);
        this.clickableText = new ClickableTextService(dispatcher, text);
        this.bossBar = new BossBarService(dispatcher, text, scheduler);
        this.events = new EventService(plugin);
        this.quitListener = events.listen(PlayerQuitEvent.class, EventPriority.MONITOR, false,
                event -> dispatcher.forget(event.getPlayer().getUniqueId()));
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
    public TabListService tabList() { return tabList; }
    public AnimatedTitleService animatedTitles() { return animatedTitles; }
    public ChatService chat() { return chat; }
    public ClickableTextService clickableText() { return clickableText; }
    public BossBarService bossBar() { return bossBar; }
    public SchedulerService scheduler() { return scheduler; }
    public EventService events() { return events; }
    public ConfigurationService config() { return configuration; }
    public CommandService commands() { return commands; }
    public LogService logger() { return logger; }
    /** ViaVersion bridge: client protocol, RGB support and whether ViaVersion is installed. */
    public ViaBridge via() { return via; }
    /** Low-level sender used by every service; useful to send your own components. */
    public Dispatcher dispatcher() { return dispatcher; }

    @Override
    public void close() {
        quitListener.close();
        bossBar.cancelAll();
        dispatcher.close();
        audiences.close();
    }
}
