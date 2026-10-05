package com.hyfecraft.hyfeutils.message;

import com.hyfecraft.hyfeutils.platform.Dispatcher;
import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Chat messages. Strings accept MiniMessage and legacy codes mixed together. RGB colors reach
 * 1.16+ clients even on old servers when ViaVersion is installed, and are downsampled for others.
 */
public final class MessageService {
    private final Dispatcher dispatcher;
    private final TextService text;

    public MessageService(Dispatcher dispatcher, TextService text) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.text = Objects.requireNonNull(text, "text");
    }

    public void send(CommandSender sender, String message) {
        send(sender, text.parse(message));
    }

    /** Sends a message with MiniMessage placeholders, e.g. {@code Placeholder.unparsed("player", name)}. */
    public void send(CommandSender sender, String message, TagResolver... placeholders) {
        send(sender, text.parse(message, placeholders));
    }

    public void send(CommandSender sender, Component message) {
        dispatcher.message(Objects.requireNonNull(sender, "sender"), Objects.requireNonNull(message, "message"));
    }

    /** Sends several lines, parsing each one only once. */
    public void send(CommandSender sender, Iterable<String> lines) {
        Objects.requireNonNull(lines, "lines");
        for (String line : lines) {
            send(sender, line);
        }
    }

    /** Parses the message once and sends it to every receiver. */
    public void send(Iterable<? extends CommandSender> receivers, String message) {
        send(receivers, text.parse(message));
    }

    public void send(Iterable<? extends CommandSender> receivers, Component message) {
        Objects.requireNonNull(receivers, "receivers");
        for (CommandSender receiver : receivers) {
            send(receiver, message);
        }
    }

    /** Sends the message to every online player and to the console. */
    public void broadcast(String message) {
        broadcast(text.parse(message));
    }

    public void broadcast(String message, TagResolver... placeholders) {
        broadcast(text.parse(message, placeholders));
    }

    public void broadcast(Component message) {
        send(Bukkit.getOnlinePlayers(), message);
        send(Bukkit.getConsoleSender(), message);
    }

    /** Sends the message to online players that have the permission. */
    public void broadcastWithPermission(String permission, String message) {
        Objects.requireNonNull(permission, "permission");
        Component component = text.parse(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                send(player, component);
            }
        }
    }
}
