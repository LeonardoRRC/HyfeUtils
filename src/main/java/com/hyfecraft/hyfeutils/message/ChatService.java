package com.hyfecraft.hyfeutils.message;

import com.hyfecraft.hyfeutils.platform.Dispatcher;
import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public final class ChatService {
    private static final int CHAT_WIDTH = 176;
    private static final int SPACE_WIDTH = 4;
    private static final int DEFAULT_WIDTH = 6;
    private static final String CENTER_PREFIX = "§r";
    private static final byte[] ASCII_WIDTHS = new byte[128];

    static {
        String normal = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
        int[] widths = {2, 4, 6, 6, 7, 7, 3, 5, 5, 6, 6, 2, 5, 3, 6, 3, 7, 5, 5, 5, 6, 6, 7, 6, 6, 6, 2, 4, 4, 5, 6, 4, 8,
                7, 5, 6, 6, 5, 6, 6, 3, 4, 6, 5, 8, 6, 7, 5, 6, 6, 5, 6, 6, 7, 7, 8, 4, 4, 5, 4, 5, 3,
                5, 5, 4, 5, 5, 4, 5, 5, 3, 3, 5, 3, 8, 5, 5, 5, 5, 4, 4, 4, 5, 5, 7, 5, 5, 4, 4, 2, 4, 5};
        java.util.Arrays.fill(ASCII_WIDTHS, (byte) DEFAULT_WIDTH);
        for (int i = 0; i < normal.length() && i < widths.length; i++) {
            ASCII_WIDTHS[normal.charAt(i)] = (byte) widths[i];
        }
        ASCII_WIDTHS[' '] = SPACE_WIDTH;
    }

    private final Dispatcher dispatcher;
    private final TextService text;

    public ChatService(Dispatcher dispatcher, TextService text) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.text = Objects.requireNonNull(text, "text");
    }

    /** Sends a message centered in the chat. Accepts MiniMessage and legacy codes, bold included. */
    public void sendCentered(Player player, String message) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(message, "message");
        dispatcher.message(player, centered(message));
    }

    public void sendCentered(Player player, Component message) {
        Objects.requireNonNull(player, "player");
        dispatcher.message(player, centerComponent(Objects.requireNonNull(message, "message")));
    }

    public void sendCentered(CommandSender sender, String message) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(message, "message");
        if (sender instanceof Player player) {
            sendCentered(player, message);
        } else {
            dispatcher.message(sender, text.parse(message));
        }
    }

    /** Sends every line centered, for example a whole menu from the configuration. */
    public void sendCentered(Player player, Iterable<String> lines) {
        for (String line : lines) {
            sendCentered(player, line);
        }
    }

    /** Parses and centers a message without sending it. */
    public Component centered(String message) {
        return centerComponent(text.parse(message));
    }

    /** Prepends the spaces needed to center a component, taking bold text into account. */
    public static Component centerComponent(Component message) {
        int width = width(message);
        if (width == 0 || width >= CHAT_WIDTH) {
            return message;
        }
        int spaces = spaces((CHAT_WIDTH - width) / 2);
        return spaces == 0 ? message : Component.text(" ".repeat(spaces)).append(message);
    }

    /** Centers a legacy {@code §} string. */
    public static String center(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        int messageWidth = getStringWidth(message);
        if (messageWidth >= CHAT_WIDTH) {
            return message;
        }
        int spaces = spaces((CHAT_WIDTH - messageWidth) / 2);
        return CENTER_PREFIX + " ".repeat(spaces) + message;
    }

    /** Width in pixels of the component as rendered in the default font. */
    public static int width(Component message) {
        int[] width = {0};
        Deque<Boolean> bold = new ArrayDeque<>();
        bold.push(false);
        ComponentFlattener.basic().flatten(message, new FlattenerListener() {
            @Override
            public void pushStyle(Style style) {
                TextDecoration.State state = style.decoration(TextDecoration.BOLD);
                bold.push(state == TextDecoration.State.NOT_SET ? bold.peek() : state == TextDecoration.State.TRUE);
            }

            @Override
            public void component(String text) {
                boolean isBold = Boolean.TRUE.equals(bold.peek());
                for (int i = 0; i < text.length(); i++) {
                    width[0] += charWidth(text.charAt(i)) + (isBold ? 1 : 0);
                }
            }

            @Override
            public void popStyle(Style style) {
                if (bold.size() > 1) {
                    bold.pop();
                }
            }
        });
        return width[0];
    }

    private static int spaces(int padding) {
        return padding <= 0 ? 0 : (padding + SPACE_WIDTH - 1) / SPACE_WIDTH;
    }

    private static int charWidth(char c) {
        return c < ASCII_WIDTHS.length ? ASCII_WIDTHS[c] : DEFAULT_WIDTH;
    }

    private static int getStringWidth(String message) {
        int width = 0;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '§' && i + 1 < message.length()) {
                i++;
                continue;
            }
            if (c != '§') {
                width += charWidth(c);
            }
        }
        return width;
    }
}
