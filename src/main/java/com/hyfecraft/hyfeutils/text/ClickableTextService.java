package com.hyfecraft.hyfeutils.text;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class ClickableTextService {
    private final BukkitAudiences audiences;
    private final TextService text;

    public ClickableTextService(BukkitAudiences audiences, TextService text) {
        this.audiences = Objects.requireNonNull(audiences, "audiences");
        this.text = Objects.requireNonNull(text, "text");
    }

    public static ClickableTextBuilder clickable(String message) {
        return new ClickableTextBuilder(new TextService(), message);
    }

    public void send(Player player, Component component) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(component, "component");
        audiences.player(player).sendMessage(component);
    }

    public void send(CommandSender sender, Component component) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(component, "component");
        if (sender instanceof Player player) {
            send(player, component);
        } else {
            audiences.sender(sender).sendMessage(component);
        }
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
            this.clickEvent = ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, command);
            return this;
        }

        public ClickableTextBuilder suggestCommand(String command) {
            this.clickEvent = ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, command);
            return this;
        }

        public ClickableTextBuilder openUrl(String url) {
            this.clickEvent = ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, url);
            return this;
        }

        public ClickableTextBuilder copyToClipboard(String text) {
            this.clickEvent = ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text);
            return this;
        }

        public ClickableTextBuilder showText(String hoverText) {
            Component hover = LegacyComponentSerializer.legacySection().deserialize(
                    this.text.colorize(hoverText));
            this.hoverEvent = HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, hover);
            return this;
        }

        public Component build() {
            Component component = LegacyComponentSerializer.legacySection().deserialize(
                    text.colorize(message));
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
