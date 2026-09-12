package com.hyfecraft.hyfeutils.message;

import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class MessageService {
    private final BukkitAudiences audiences;
    private final TextService text;

    public MessageService(BukkitAudiences audiences, TextService text) {
        this.audiences = audiences;
        this.text = text;
    }

    public void send(CommandSender sender, String message) {
        Objects.requireNonNull(sender, "sender");
        int protocol = sender instanceof Player ? ClientProtocol.get((Player) sender) : -1;
        audiences.sender(sender).sendMessage(component(text.colorize(message, protocol)));
    }

    private net.kyori.adventure.text.Component component(String value) {
        return LegacyComponentSerializer.legacySection().deserialize(value);
    }
}
