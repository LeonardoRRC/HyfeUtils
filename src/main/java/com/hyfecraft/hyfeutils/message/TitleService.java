package com.hyfecraft.hyfeutils.message;

import com.hyfecraft.hyfeutils.platform.Dispatcher;
import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

/** Titles. Times are in ticks (20 ticks = 1 second). */
public final class TitleService {
    private final Dispatcher dispatcher;
    private final TextService text;

    public TitleService(Dispatcher dispatcher, TextService text) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.text = Objects.requireNonNull(text, "text");
    }

    public void send(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        send(player, text.parse(title), text.parse(subtitle), fadeIn, stay, fadeOut);
    }

    /** Uses the vanilla times: 10 ticks fade in, 70 ticks stay and 20 ticks fade out. */
    public void send(Player player, String title, String subtitle) {
        send(player, title, subtitle, 10, 70, 20);
    }

    public void send(Player player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        Objects.requireNonNull(player, "player");
        dispatcher.title(player,
                title == null ? Component.empty() : title,
                subtitle == null ? Component.empty() : subtitle,
                fadeIn, stay, fadeOut);
    }

    /** Parses the title once and shows it to every player. */
    public void send(Iterable<? extends Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component main = text.parse(title);
        Component sub = text.parse(subtitle);
        for (Player player : players) {
            send(player, main, sub, fadeIn, stay, fadeOut);
        }
    }

    public void broadcast(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        send(Bukkit.getOnlinePlayers(), title, subtitle, fadeIn, stay, fadeOut);
    }

    /** Hides the current title. */
    public void clear(Player player) {
        dispatcher.clearTitle(player);
    }

    /** Hides the current title and restores the default times. */
    public void reset(Player player) {
        dispatcher.resetTitle(player);
    }
}
