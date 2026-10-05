package com.hyfecraft.hyfeutils.message;

import com.hyfecraft.hyfeutils.platform.Dispatcher;
import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class ActionBarService {
    private final Dispatcher dispatcher;
    private final TextService text;

    public ActionBarService(Dispatcher dispatcher, TextService text) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.text = Objects.requireNonNull(text, "text");
    }

    public void send(Player player, String message) {
        send(player, text.parse(message));
    }

    public void send(Player player, String message, TagResolver... placeholders) {
        send(player, text.parse(message, placeholders));
    }

    public void send(Player player, Component message) {
        dispatcher.actionBar(Objects.requireNonNull(player, "player"), Objects.requireNonNull(message, "message"));
    }

    public void send(Iterable<? extends Player> players, String message) {
        Component component = text.parse(message);
        for (Player player : players) {
            send(player, component);
        }
    }

    public void broadcast(String message) {
        send(Bukkit.getOnlinePlayers(), message);
    }
}
