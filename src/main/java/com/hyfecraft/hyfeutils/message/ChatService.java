package com.hyfecraft.hyfeutils.message;

import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ChatService {
    private static final int CHAT_WIDTH = 176;
    private static final String CENTER_PREFIX = "\u00a7r";
    private static final Map<Character, Integer> CHAR_WIDTHS = new HashMap<>();

    static {
        String normal = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
        int[] widths = {2, 4, 6, 6, 7, 7, 3, 5, 5, 6, 6, 2, 5, 3, 6, 3, 7, 5, 5, 5, 6, 6, 7, 6, 6, 6, 2, 4, 4, 5, 6, 4, 8,
                7, 5, 6, 6, 5, 6, 6, 3, 4, 6, 5, 8, 6, 7, 5, 6, 6, 5, 6, 6, 7, 7, 8, 4, 4, 5, 4, 5, 3,
                5, 5, 4, 5, 5, 4, 5, 5, 3, 3, 5, 3, 8, 5, 5, 5, 5, 4, 4, 4, 5, 5, 7, 5, 5, 4, 4, 2, 4, 5};
        for (int i = 0; i < normal.length() && i < widths.length; i++) {
            CHAR_WIDTHS.put(normal.charAt(i), widths[i]);
        }
        CHAR_WIDTHS.put(' ', 4);
        CHAR_WIDTHS.put('\u00a7', 0);
    }

    private final BukkitAudiences audiences;
    private final TextService text;

    public ChatService(BukkitAudiences audiences, TextService text) {
        this.audiences = Objects.requireNonNull(audiences, "audiences");
        this.text = Objects.requireNonNull(text, "text");
    }

    public void sendCentered(Player player, String message) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(message, "message");
        int protocol = ClientProtocol.get(player);
        String colorized = text.colorize(message, protocol);
        String centered = center(colorized);
        audiences.player(player).sendMessage(
                LegacyComponentSerializer.legacySection().deserialize(centered));
    }

    public void sendCentered(CommandSender sender, String message) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(message, "message");
        if (sender instanceof Player player) {
            sendCentered(player, message);
        } else {
            audiences.sender(sender).sendMessage(
                    LegacyComponentSerializer.legacySection().deserialize(
                            text.colorize(message)));
        }
    }

    public static String center(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        int messageWidth = getStringWidth(message);
        if (messageWidth >= CHAT_WIDTH) {
            return message;
        }
        int padding = (CHAT_WIDTH - messageWidth) / 2;
        StringBuilder centered = new StringBuilder(CENTER_PREFIX);
        while (getStringWidth(centered.toString()) < padding) {
            centered.append(' ');
        }
        centered.append(message);
        return centered.toString();
    }

    private static int getStringWidth(String message) {
        int width = 0;
        boolean isFormatCode = false;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '\u00a7' && i + 1 < message.length()) {
                isFormatCode = true;
                continue;
            }
            if (isFormatCode) {
                isFormatCode = false;
                continue;
            }
            Integer charWidth = CHAR_WIDTHS.get(c);
            width += charWidth != null ? charWidth : 6;
        }
        return width;
    }
}
