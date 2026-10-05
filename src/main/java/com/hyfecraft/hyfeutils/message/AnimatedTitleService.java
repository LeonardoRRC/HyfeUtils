package com.hyfecraft.hyfeutils.message;

import com.hyfecraft.hyfeutils.platform.Dispatcher;
import com.hyfecraft.hyfeutils.scheduler.SchedulerService;
import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Animated titles. Every method returns the task so the animation can be cancelled early;
 * otherwise it stops by itself after {@code totalTicks}.
 */
public final class AnimatedTitleService {
    private static final TextColor[] RAINBOW = {
            NamedTextColor.RED, NamedTextColor.GOLD, NamedTextColor.YELLOW, NamedTextColor.GREEN,
            NamedTextColor.AQUA, NamedTextColor.BLUE, NamedTextColor.LIGHT_PURPLE
    };

    private final Dispatcher dispatcher;
    private final TextService text;
    private final SchedulerService scheduler;

    public AnimatedTitleService(Dispatcher dispatcher, TextService text, SchedulerService scheduler) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.text = Objects.requireNonNull(text, "text");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    /** Two colors alternating per character. {@code waveColor} accepts {@code "&f&l"} or {@code "<#ffffff><bold>"}. */
    public BukkitTask wave(Player player, String message, String waveColor, int totalTicks, int frameDelay) {
        return waveInternal(player, message, null, Objects.requireNonNull(waveColor, "waveColor"), totalTicks, frameDelay);
    }

    public BukkitTask waveWithSubtitle(Player player, String message, String subtitle, String waveColor, int totalTicks, int frameDelay) {
        return waveInternal(player, message, subtitle, Objects.requireNonNull(waveColor, "waveColor"), totalTicks, frameDelay);
    }

    public BukkitTask rainbowWave(Player player, String message, int totalTicks, int frameDelay) {
        return waveInternal(player, message, null, null, totalTicks, frameDelay);
    }

    public BukkitTask rainbowWaveWithSubtitle(Player player, String message, String subtitle, int totalTicks, int frameDelay) {
        return waveInternal(player, message, subtitle, null, totalTicks, frameDelay);
    }

    /** Shows each frame in order, spreading them over {@code totalTicks}. */
    public BukkitTask animate(Player player, List<String> frames, int totalTicks) {
        Objects.requireNonNull(frames, "frames");
        List<Component> parsed = new ArrayList<>(frames.size());
        for (String frame : frames) {
            parsed.add(text.parse(frame));
        }
        return animateComponents(player, parsed, Component.empty(), totalTicks);
    }

    /** Same as {@link #animate(Player, List, int)} with a fixed subtitle. */
    public BukkitTask animate(Player player, List<String> frames, String subtitle, int totalTicks) {
        Objects.requireNonNull(frames, "frames");
        List<Component> parsed = new ArrayList<>(frames.size());
        for (String frame : frames) {
            parsed.add(text.parse(frame));
        }
        return animateComponents(player, parsed, text.parse(subtitle), totalTicks);
    }

    public BukkitTask animateComponents(Player player, List<Component> frames, Component subtitle, int totalTicks) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(frames, "frames");
        if (frames.isEmpty()) {
            return null;
        }
        int frameDelay = Math.max(1, totalTicks / frames.size());
        Component sub = subtitle == null ? Component.empty() : subtitle;
        return run(player, frameDelay, totalTicks, frame ->
                dispatcher.title(player, frames.get(frame % frames.size()), sub, 0, frameDelay + 1, 0));
    }

    private BukkitTask waveInternal(Player player, String message, String subtitle, String waveColor,
                                    int totalTicks, int frameDelay) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(message, "message");
        int delay = Math.max(1, frameDelay);

        Component parsed = text.parse(message);
        String plain = PlainTextComponentSerializer.plainText().serialize(parsed);
        Style base = firstStyle(parsed);
        if (base.color() == null) {
            base = base.color(NamedTextColor.WHITE);
        }
        // The wave keeps the decorations of the message and only changes what waveColor sets.
        Style wave = waveColor == null ? null : base.merge(firstStyle(text.parse(waveColor + "x")));
        Style baseStyle = base;
        Component sub = text.parse(subtitle);

        return run(player, delay, totalTicks, frame -> {
            TextComponent.Builder builder = Component.text();
            for (int i = 0; i < plain.length(); i++) {
                char c = plain.charAt(i);
                Style style;
                if (wave == null) {
                    style = baseStyle.color(RAINBOW[(i + frame) % RAINBOW.length]);
                } else {
                    style = (i + frame) % 2 == 0 ? wave : baseStyle;
                }
                builder.append(Component.text(String.valueOf(c), style));
            }
            dispatcher.title(player, builder.build(), sub, 0, delay + 1, 0);
        });
    }

    private BukkitTask run(Player player, int frameDelay, int totalTicks, FrameRenderer renderer) {
        int totalFrames = Math.max(1, (int) Math.ceil(totalTicks / (double) frameDelay));
        int[] frame = {0};
        BukkitTask[] task = new BukkitTask[1];
        task[0] = scheduler.runRepeating(0L, frameDelay, () -> {
            if (!player.isOnline() || frame[0] >= totalFrames) {
                task[0].cancel();
                return;
            }
            renderer.render(frame[0]++);
        });
        return task[0];
    }

    /** Style of the first visible character, including inherited color and decorations. */
    private static Style firstStyle(Component component) {
        Style[] found = {null};
        Deque<Style> styles = new ArrayDeque<>();
        styles.push(Style.empty());
        ComponentFlattener.basic().flatten(component, new FlattenerListener() {
            @Override
            public void pushStyle(Style style) {
                styles.push(styles.peek().merge(style, Style.Merge.Strategy.ALWAYS));
            }

            @Override
            public void component(String value) {
                if (found[0] == null && !value.isEmpty()) {
                    found[0] = styles.peek();
                }
            }

            @Override
            public void popStyle(Style style) {
                if (styles.size() > 1) {
                    styles.pop();
                }
            }
        });
        Style style = found[0] == null ? Style.empty() : found[0];
        return Style.style().color(style.color()).decorations(style.decorations()).build();
    }

    @FunctionalInterface
    private interface FrameRenderer {
        void render(int frame);
    }
}
