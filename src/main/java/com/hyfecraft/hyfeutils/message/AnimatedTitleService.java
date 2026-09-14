package com.hyfecraft.hyfeutils.message;

import com.hyfecraft.hyfeutils.scheduler.SchedulerService;
import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class AnimatedTitleService {
    private final BukkitAudiences audiences;
    private final TextService text;
    private final SchedulerService scheduler;

    public AnimatedTitleService(BukkitAudiences audiences, TextService text, SchedulerService scheduler) {
        this.audiences = Objects.requireNonNull(audiences, "audiences");
        this.text = Objects.requireNonNull(text, "text");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    public void wave(Player player, String message, String waveColor, int totalTicks, int frameDelay) {
        waveInternal(player, message, null, waveColor, totalTicks, frameDelay);
    }

    public void waveWithSubtitle(Player player, String message, String subtitle, String waveColor, int totalTicks, int frameDelay) {
        waveInternal(player, message, subtitle, waveColor, totalTicks, frameDelay);
    }

    public void animate(Player player, List<String> frames, int totalTicks) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(frames, "frames");
        if (frames.isEmpty()) return;

        int frameDelay = Math.max(1, totalTicks / frames.size());
        int[] index = {0};

        scheduler.runRepeating(0L, frameDelay, () -> {
            if (!player.isOnline()) return;
            String frame = frames.get(index[0] % frames.size());
            showTitle(player, frame, null, 0, frameDelay, 0);
            index[0]++;
        });
    }

    public void rainbowWave(Player player, String message, int totalTicks, int frameDelay) {
        waveInternal(player, message, null, null, totalTicks, frameDelay);
    }

    public void rainbowWaveWithSubtitle(Player player, String message, String subtitle, int totalTicks, int frameDelay) {
        waveInternal(player, message, subtitle, null, totalTicks, frameDelay);
    }

    private void waveInternal(Player player, String message, String subtitle, String waveColor, int totalTicks, int frameDelay) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(message, "message");

        int protocol = ClientProtocol.get(player);
        String plainText = stripFormattingCodes(message);
        String baseColor = extractFirstColorCode(message);
        String baseFormatting = extractFirstFormattingCodes(message);

        boolean isRainbow = (waveColor == null);
        String[] rainbowColors = {"&c", "&6", "&e", "&a", "&b", "&9", "&d"};

        int[] offset = {0};
        int[] tickCount = {0};

        scheduler.runRepeating(0L, frameDelay, () -> {
            if (!player.isOnline()) return;

            StringBuilder waveText = new StringBuilder();
            for (int i = 0; i < plainText.length(); i++) {
                char c = plainText.charAt(i);
                if (c == ' ') {
                    waveText.append(' ');
                    continue;
                }

                String color;
                if (isRainbow) {
                    color = rainbowColors[(i + offset[0]) % rainbowColors.length];
                } else {
                    boolean useWave = (i + offset[0]) % 2 == 0;
                    color = useWave ? waveColor : baseColor;
                }

                waveText.append(color);
                waveText.append(baseFormatting);
                waveText.append(c);
            }

            String subtitleText = null;
            if (subtitle != null) {
                subtitleText = text.colorize(subtitle, protocol);
            }

            showTitle(player, waveText.toString(), subtitleText, 0, frameDelay, 0);

            tickCount[0] += frameDelay;
            offset[0]++;
        });
    }

    private void showTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        int protocol = ClientProtocol.get(player);
        Component main = component(text.colorize(title, protocol));
        Component sub = component(subtitle != null ? text.colorize(subtitle, protocol) : "");
        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L));
        audiences.player(player).showTitle(Title.title(main, sub, times));
    }

    private Component component(String value) {
        return LegacyComponentSerializer.legacySection().deserialize(value == null ? "" : value);
    }

    private String stripFormattingCodes(String input) {
        if (input == null) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '&' && i + 1 < input.length()) {
                char next = Character.toLowerCase(input.charAt(i + 1));
                if ("0123456789abcdefklmnor".indexOf(next) >= 0) {
                    i++;
                    continue;
                }
            }
            result.append(c);
        }
        return result.toString();
    }

    private String extractFirstColorCode(String input) {
        if (input == null) return "&f";
        for (int i = 0; i < input.length() - 1; i++) {
            if (input.charAt(i) == '&') {
                char next = Character.toLowerCase(input.charAt(i + 1));
                if ("0123456789abcdef".indexOf(next) >= 0) {
                    return "&" + next;
                }
            }
        }
        return "&f";
    }

    private String extractFirstFormattingCodes(String input) {
        if (input == null) return "";
        StringBuilder formatting = new StringBuilder();
        for (int i = 0; i < input.length() - 1; i++) {
            if (input.charAt(i) == '&') {
                char next = Character.toLowerCase(input.charAt(i + 1));
                if ("klmnor".indexOf(next) >= 0) {
                    formatting.append("&").append(next);
                }
            }
        }
        return formatting.toString();
    }
}
