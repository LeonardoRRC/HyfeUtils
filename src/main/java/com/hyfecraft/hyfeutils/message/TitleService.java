package com.hyfecraft.hyfeutils.message;

import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Objects;

public final class TitleService {
    private final BukkitAudiences audiences;
    private final TextService text;

    public TitleService(BukkitAudiences audiences, TextService text) {
        this.audiences = audiences;
        this.text = text;
    }

    public void send(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Objects.requireNonNull(player, "player");
        int protocol = ClientProtocol.get(player);
        Component main = component(text.colorize(title, protocol));
        Component sub = component(text.colorize(subtitle, protocol));
        Title.Times times = Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L));
        audiences.player(player).showTitle(Title.title(main, sub, times));
    }

    private Component component(String value) {
        return LegacyComponentSerializer.legacySection().deserialize(value == null ? "" : value);
    }
}
