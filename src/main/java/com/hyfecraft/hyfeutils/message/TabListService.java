package com.hyfecraft.hyfeutils.message;

import com.hyfecraft.hyfeutils.platform.Dispatcher;
import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

/** Header and footer of the player list (TAB). */
public final class TabListService {
    private final Dispatcher dispatcher;
    private final TextService text;

    public TabListService(Dispatcher dispatcher, TextService text) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.text = Objects.requireNonNull(text, "text");
    }

    public void send(Player player, String header, String footer) {
        send(player, text.parse(header), text.parse(footer));
    }

    /** Each element of the lists is one line. */
    public void send(Player player, List<String> header, List<String> footer) {
        send(player, String.join("\n", header), String.join("\n", footer));
    }

    public void send(Player player, Component header, Component footer) {
        dispatcher.tabList(Objects.requireNonNull(player, "player"),
                header == null ? Component.empty() : header,
                footer == null ? Component.empty() : footer);
    }

    public void broadcast(String header, String footer) {
        Component main = text.parse(header);
        Component sub = text.parse(footer);
        for (Player player : Bukkit.getOnlinePlayers()) {
            send(player, main, sub);
        }
    }

    public void clear(Player player) {
        send(player, Component.empty(), Component.empty());
    }
}
