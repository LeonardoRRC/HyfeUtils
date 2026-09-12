package com.hyfecraft.hyfeutils.message;

import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class ActionBarService {
    private final BukkitAudiences audiences;
    private final TextService text;

    public ActionBarService(BukkitAudiences audiences, TextService text) {
        this.audiences = audiences;
        this.text = text;
    }

    public void send(Player player, String message) {
        Objects.requireNonNull(player, "player");
        int protocol = ClientProtocol.get(player);
        String value = text.colorize(message, protocol);
        audiences.player(player).sendActionBar(LegacyComponentSerializer.legacySection().deserialize(value));
    }
}
