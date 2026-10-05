package com.hyfecraft.hyfeutils.text;

import com.hyfecraft.hyfeutils.platform.Dispatcher;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;

import java.util.Objects;

/**
 * Clickable messages. The text accepts MiniMessage and legacy codes; MiniMessage
 * {@code <click>} and {@code <hover>} tags can also be used directly with
 * {@code hyfe.messages().send(...)}.
 */
public final class ClickableTextService {
    private static final TextService DEFAULT_TEXT = new TextService();

    private final Dispatcher dispatcher;
    private final TextService text;

    public ClickableTextService(Dispatcher dispatcher, TextService text) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.text = Objects.requireNonNull(text, "text");
    }

    public static ClickableTextBuilder clickable(String message) {
        return new ClickableTextBuilder(DEFAULT_TEXT, message);
    }

    /** Builder that shares this service's {@link TextService} (and its custom MiniMessage, if any). */
    public ClickableTextBuilder builder(String message) {
        return new ClickableTextBuilder(text, message);
    }

    public void send(CommandSender sender, Component component) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(component, "component");
        dispatcher.message(sender, component);
    }

    /** Joins several parts in one line: {@code send(player, clickable("[Sí]")..., clickable(" [No]")...)}. */
    public void send(CommandSender sender, Component... parts) {
        Objects.requireNonNull(parts, "parts");
        TextComponent.Builder line = Component.text();
        for (Component part : parts) {
            line.append(Objects.requireNonNull(part, "part"));
        }
        send(sender, line.build());
    }

    public static final class ClickableTextBuilder {
        private final TextService text;
        private final String message;
        private ClickEvent clickEvent;
        private HoverEvent<?> hoverEvent;

        private ClickableTextBuilder(TextService text, String message) {
            this.text = text;
            this.message = Objects.requireNonNull(message, "message");
        }

        public ClickableTextBuilder runCommand(String command) {
            this.clickEvent = ClickEvent.runCommand(command);
            return this;
        }

        public ClickableTextBuilder suggestCommand(String command) {
            this.clickEvent = ClickEvent.suggestCommand(command);
            return this;
        }

        public ClickableTextBuilder openUrl(String url) {
            this.clickEvent = ClickEvent.openUrl(url);
            return this;
        }

        public ClickableTextBuilder copyToClipboard(String text) {
            this.clickEvent = ClickEvent.copyToClipboard(text);
            return this;
        }

        public ClickableTextBuilder hoverText(String hoverText) {
            this.hoverEvent = HoverEvent.showText(text.parse(hoverText));
            return this;
        }

        public ClickableTextBuilder hoverText(Component hoverText) {
            this.hoverEvent = HoverEvent.showText(Objects.requireNonNull(hoverText, "hoverText"));
            return this;
        }

        /** Same as {@link #hoverText(String)}. */
        public ClickableTextBuilder showText(String hoverText) {
            return hoverText(hoverText);
        }

        public Component build() {
            Component component = text.parse(message);
            if (clickEvent != null) {
                component = component.clickEvent(clickEvent);
            }
            if (hoverEvent != null) {
                component = component.hoverEvent(hoverEvent);
            }
            return component;
        }

        public String buildLegacy() {
            return text.colorize(message);
        }
    }
}
