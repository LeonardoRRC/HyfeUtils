package com.hyfecraft.hyfeutils.platform;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends components to players, choosing per player between the ViaVersion bridge
 * (RGB and real boss bars on old servers) and the regular Adventure platform.
 */
public final class Dispatcher {
    private static final byte POSITION_SYSTEM = 1;
    private static final byte POSITION_ACTION_BAR = 2;
    private static final int TITLE_CLEAR = 4;
    private static final int TITLE_RESET = 5;

    private final BukkitAudiences audiences;
    private final ViaBridge via;
    private final Map<BossBar, ViaBossBar> bossBars = new ConcurrentHashMap<>();

    public Dispatcher(BukkitAudiences audiences, ViaBridge via) {
        this.audiences = Objects.requireNonNull(audiences, "audiences");
        this.via = Objects.requireNonNull(via, "via");
    }

    public BukkitAudiences audiences() {
        return audiences;
    }

    public ViaBridge via() {
        return via;
    }

    public void message(CommandSender sender, Component message) {
        Objects.requireNonNull(sender, "sender");
        if (sender instanceof Player player && via.sendChat(player, message, POSITION_SYSTEM)) {
            return;
        }
        audiences.sender(sender).sendMessage(message);
    }

    public void actionBar(Player player, Component message) {
        Objects.requireNonNull(player, "player");
        if (!via.sendChat(player, message, POSITION_ACTION_BAR)) {
            audiences.player(player).sendActionBar(message);
        }
    }

    public void title(Player player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        Objects.requireNonNull(player, "player");
        if (!via.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut)) {
            Title.Times times = Title.Times.times(ticks(fadeIn), ticks(stay), ticks(fadeOut));
            audiences.player(player).showTitle(Title.title(title, subtitle, times));
        }
    }

    public void clearTitle(Player player) {
        Objects.requireNonNull(player, "player");
        if (!via.sendTitleAction(player, TITLE_CLEAR)) {
            audiences.player(player).clearTitle();
        }
    }

    public void resetTitle(Player player) {
        Objects.requireNonNull(player, "player");
        if (!via.sendTitleAction(player, TITLE_RESET)) {
            audiences.player(player).resetTitle();
        }
    }

    public void tabList(Player player, Component header, Component footer) {
        Objects.requireNonNull(player, "player");
        if (!via.sendTabList(player, header, footer)) {
            audiences.player(player).sendPlayerListHeaderAndFooter(header, footer);
        }
    }

    public void showBossBar(Player player, BossBar bar) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(bar, "bar");
        if (via.handlesBossBars(player)) {
            ViaBossBar tracked = bossBars.computeIfAbsent(bar, ViaBossBar::new);
            if (tracked.show(player)) {
                return;
            }
            // The bridge failed: undo the registration and use the normal path.
            tracked.forget(player.getUniqueId());
            releaseIfEmpty(tracked);
        }
        audiences.player(player).showBossBar(bar);
    }

    public void hideBossBar(Player player, BossBar bar) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(bar, "bar");
        ViaBossBar tracked = bossBars.get(bar);
        if (tracked != null && tracked.hide(player)) {
            releaseIfEmpty(tracked);
            return;
        }
        audiences.player(player).hideBossBar(bar);
    }

    /** Releases everything related to a player that left the server. */
    public void forget(UUID playerId) {
        for (ViaBossBar tracked : bossBars.values()) {
            if (tracked.forget(playerId)) {
                releaseIfEmpty(tracked);
            }
        }
        via.forget(playerId);
    }

    /** Hides every boss bar shown through the bridge. */
    public void close() {
        for (ViaBossBar tracked : bossBars.values()) {
            tracked.hideAll();
        }
        bossBars.clear();
    }

    private void releaseIfEmpty(ViaBossBar tracked) {
        if (tracked.isEmpty() && bossBars.remove(tracked.bar, tracked)) {
            tracked.bar.removeListener(tracked);
        }
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }

    /** Mirrors an Adventure boss bar to the players served through ViaVersion. */
    private final class ViaBossBar implements BossBar.Listener {
        private static final int ADD = 0;
        private static final int REMOVE = 1;
        private static final int PROGRESS = 2;
        private static final int NAME = 3;
        private static final int STYLE = 4;
        private static final int FLAGS = 5;

        private final BossBar bar;
        private final UUID id = UUID.randomUUID();
        private final Map<UUID, Player> viewers = new ConcurrentHashMap<>();

        ViaBossBar(BossBar bar) {
            this.bar = bar;
            bar.addListener(this);
        }

        boolean show(Player player) {
            if (viewers.putIfAbsent(player.getUniqueId(), player) != null) {
                return true;
            }
            return via.sendBossBar(player, id, ADD, writer -> {
                writer.title(bar.name());
                writer.progress(bar.progress());
                writer.style(bar.color().ordinal(), bar.overlay().ordinal());
                writer.flags(flags(bar.flags()));
            });
        }

        boolean hide(Player player) {
            if (viewers.remove(player.getUniqueId()) == null) {
                return false;
            }
            via.sendBossBar(player, id, REMOVE, writer -> { });
            return true;
        }

        boolean forget(UUID playerId) {
            return viewers.remove(playerId) != null;
        }

        void hideAll() {
            bar.removeListener(this);
            for (Player player : viewers.values()) {
                via.sendBossBar(player, id, REMOVE, writer -> { });
            }
            viewers.clear();
        }

        boolean isEmpty() {
            return viewers.isEmpty();
        }

        @Override
        public void bossBarNameChanged(BossBar bar, Component oldName, Component newName) {
            broadcast(NAME, writer -> writer.title(newName));
        }

        @Override
        public void bossBarProgressChanged(BossBar bar, float oldProgress, float newProgress) {
            broadcast(PROGRESS, writer -> writer.progress(newProgress));
        }

        @Override
        public void bossBarColorChanged(BossBar bar, BossBar.Color oldColor, BossBar.Color newColor) {
            broadcast(STYLE, writer -> writer.style(newColor.ordinal(), bar.overlay().ordinal()));
        }

        @Override
        public void bossBarOverlayChanged(BossBar bar, BossBar.Overlay oldOverlay, BossBar.Overlay newOverlay) {
            broadcast(STYLE, writer -> writer.style(bar.color().ordinal(), newOverlay.ordinal()));
        }

        @Override
        public void bossBarFlagsChanged(BossBar bar, Set<BossBar.Flag> flagsAdded, Set<BossBar.Flag> flagsRemoved) {
            broadcast(FLAGS, writer -> writer.flags(flags(bar.flags())));
        }

        private void broadcast(int action, ViaBridge.BossBarFields fields) {
            for (Player player : viewers.values()) {
                via.sendBossBar(player, id, action, fields);
            }
        }

        private byte flags(Set<BossBar.Flag> flags) {
            byte value = 0;
            if (flags.contains(BossBar.Flag.DARKEN_SCREEN)) value |= 0x1;
            if (flags.contains(BossBar.Flag.PLAY_BOSS_MUSIC)) value |= 0x2;
            if (flags.contains(BossBar.Flag.CREATE_WORLD_FOG)) value |= 0x4;
            return value;
        }
    }
}
