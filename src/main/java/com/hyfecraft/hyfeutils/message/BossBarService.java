package com.hyfecraft.hyfeutils.message;

import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class BossBarService {
    private final BukkitAudiences audiences;
    private final TextService text;

    public BossBarService(BukkitAudiences audiences, TextService text) {
        this.audiences = Objects.requireNonNull(audiences, "audiences");
        this.text = Objects.requireNonNull(text, "text");
    }

    public BossBarBuilder builder() {
        return new BossBarBuilder(audiences, text);
    }

    public void show(Player player, BossBar bossBar) {
        audiences.player(player).showBossBar(bossBar);
    }

    public void hide(Player player, BossBar bossBar) {
        audiences.player(player).hideBossBar(bossBar);
    }

    public static final class BossBarBuilder {
        private final BukkitAudiences audiences;
        private final TextService text;
        private Component name;
        private float progress = 1.0f;
        private BossBar.Color color = BossBar.Color.PINK;
        private BossBar.Overlay overlay = BossBar.Overlay.PROGRESS;
        private final Set<BossBar.Flag> flags = EnumSet.noneOf(BossBar.Flag.class);

        private BossBarBuilder(BukkitAudiences audiences, TextService text) {
            this.audiences = audiences;
            this.text = text;
        }

        public BossBarBuilder name(String name) {
            this.name = LegacyComponentSerializer.legacySection().deserialize(text.colorize(name));
            return this;
        }

        public BossBarBuilder name(Component name) {
            this.name = Objects.requireNonNull(name, "name");
            return this;
        }

        public BossBarBuilder progress(float progress) {
            this.progress = Math.max(0f, Math.min(1f, progress));
            return this;
        }

        public BossBarBuilder color(BossBar.Color color) {
            this.color = Objects.requireNonNull(color, "color");
            return this;
        }

        public BossBarBuilder color(String colorName) {
            this.color = parseColor(colorName);
            return this;
        }

        public BossBarBuilder overlay(BossBar.Overlay overlay) {
            this.overlay = Objects.requireNonNull(overlay, "overlay");
            return this;
        }

        public BossBarBuilder overlay(String overlayName) {
            this.overlay = parseOverlay(overlayName);
            return this;
        }

        public BossBarBuilder flags(Set<BossBar.Flag> flags) {
            this.flags.clear();
            this.flags.addAll(flags);
            return this;
        }

        public BossBarBuilder darkenScreen(boolean darken) {
            if (darken) flags.add(BossBar.Flag.DARKEN_SCREEN);
            else flags.remove(BossBar.Flag.DARKEN_SCREEN);
            return this;
        }

        public BossBarBuilder playBossMusic(boolean play) {
            if (play) flags.add(BossBar.Flag.PLAY_BOSS_MUSIC);
            else flags.remove(BossBar.Flag.PLAY_BOSS_MUSIC);
            return this;
        }

        public BossBarBuilder createWorldFog(boolean fog) {
            if (fog) flags.add(BossBar.Flag.CREATE_WORLD_FOG);
            else flags.remove(BossBar.Flag.CREATE_WORLD_FOG);
            return this;
        }

        public BossBar build() {
            Component nameComponent = name != null ? name : Component.empty();
            return BossBar.bossBar(nameComponent, progress, color, overlay, EnumSet.copyOf(flags));
        }

        public void show(Player player) {
            audiences.player(player).showBossBar(build());
        }

        private BossBar.Color parseColor(String name) {
            if (name == null) return BossBar.Color.PINK;
            try {
                return BossBar.Color.valueOf(name.toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException e) {
                return switch (name.toLowerCase().replace(" ", "")) {
                    case "red" -> BossBar.Color.RED;
                    case "blue" -> BossBar.Color.BLUE;
                    case "green" -> BossBar.Color.GREEN;
                    case "yellow" -> BossBar.Color.YELLOW;
                    case "purple" -> BossBar.Color.PURPLE;
                    case "white" -> BossBar.Color.WHITE;
                    default -> BossBar.Color.PINK;
                };
            }
        }

        private BossBar.Overlay parseOverlay(String name) {
            if (name == null) return BossBar.Overlay.PROGRESS;
            try {
                return BossBar.Overlay.valueOf(name.toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException e) {
                return switch (name.toLowerCase().replace(" ", "")) {
                    case "6" -> BossBar.Overlay.NOTCHED_6;
                    case "10" -> BossBar.Overlay.NOTCHED_10;
                    case "12" -> BossBar.Overlay.NOTCHED_12;
                    case "20" -> BossBar.Overlay.NOTCHED_20;
                    default -> BossBar.Overlay.PROGRESS;
                };
            }
        }
    }
}
