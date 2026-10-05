package com.hyfecraft.hyfeutils.message;

import com.hyfecraft.hyfeutils.platform.Dispatcher;
import com.hyfecraft.hyfeutils.scheduler.SchedulerService;
import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public final class BossBarService {
    private final Dispatcher dispatcher;
    private final TextService text;
    private final SchedulerService scheduler;
    private final Map<Integer, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    public BossBarService(Dispatcher dispatcher, TextService text, SchedulerService scheduler) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.text = Objects.requireNonNull(text, "text");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    public BossBarBuilder builder() {
        return new BossBarBuilder(dispatcher, text);
    }

    public TimerBossBarBuilder timer() {
        return new TimerBossBarBuilder(dispatcher, text, scheduler, activeTasks);
    }

    public WaveBossBarBuilder wave() {
        return new WaveBossBarBuilder(dispatcher, text, scheduler, activeTasks);
    }

    public void show(Player player, BossBar bossBar) {
        dispatcher.showBossBar(player, bossBar);
    }

    public void hide(Player player, BossBar bossBar) {
        dispatcher.hideBossBar(player, bossBar);
    }

    /** Shows the same bar to several players. Updates to the bar reach all of them. */
    public void show(Iterable<? extends Player> players, BossBar bossBar) {
        for (Player player : players) {
            dispatcher.showBossBar(player, bossBar);
        }
    }

    public void hide(Iterable<? extends Player> players, BossBar bossBar) {
        for (Player player : players) {
            dispatcher.hideBossBar(player, bossBar);
        }
    }

    /** Changes the name of a bar using MiniMessage or legacy codes. */
    public void rename(BossBar bossBar, String name) {
        bossBar.name(text.parse(name));
    }

    /** Accepts names like {@code "red"}, {@code "RED"}; unknown values return {@code fallback}. */
    public static BossBar.Color parseColor(String name, BossBar.Color fallback) {
        if (name == null) return fallback;
        String key = name.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (BossBar.Color color : BossBar.Color.values()) {
            if (color.name().equals(key)) return color;
        }
        return fallback;
    }

    /** Accepts {@code "NOTCHED_12"}, {@code "notched 12"} or just {@code "12"}; unknown values return {@code fallback}. */
    public static BossBar.Overlay parseOverlay(String name, BossBar.Overlay fallback) {
        if (name == null) return fallback;
        String key = name.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (BossBar.Overlay overlay : BossBar.Overlay.values()) {
            if (overlay.name().equals(key) || overlay.name().equals("NOTCHED_" + key)) return overlay;
        }
        return fallback;
    }

    public void cancelAll() {
        activeTasks.values().forEach(BukkitTask::cancel);
        activeTasks.clear();
    }

    // ------------------------------------------------------------------
    //  Timer BossBar - countdown from full to empty
    // ------------------------------------------------------------------

    public static final class TimerBossBarBuilder {
        private final Dispatcher dispatcher;
        private final TextService text;
        private final SchedulerService scheduler;
        private final Map<Integer, BukkitTask> activeTasks;

        private Player player;
        private Component name;
        private long durationTicks;
        private BossBar.Color color = BossBar.Color.RED;
        private BossBar.Overlay overlay = BossBar.Overlay.PROGRESS;
        private BossBar.Flag[] flags = new BossBar.Flag[0];
        private Consumer<BossBar> onComplete;
        private Consumer<Float> onTick;
        private boolean autoHide = true;
        private long tickInterval = 1L;
        private IntFunction<String> nameFormat;

        TimerBossBarBuilder(Dispatcher dispatcher, TextService text,
                            SchedulerService scheduler, Map<Integer, BukkitTask> activeTasks) {
            this.dispatcher = dispatcher;
            this.text = text;
            this.scheduler = scheduler;
            this.activeTasks = activeTasks;
        }

        public TimerBossBarBuilder player(Player player) {
            this.player = Objects.requireNonNull(player, "player");
            return this;
        }

        public TimerBossBarBuilder name(String name) {
            this.name = text.parse(name);
            return this;
        }

        public TimerBossBarBuilder name(Component name) {
            this.name = Objects.requireNonNull(name, "name");
            return this;
        }

        /**
         * Name recalculated with the remaining seconds, e.g.
         * {@code nameFormat(s -> "&cTiempo: &f" + s + "s")}. Only re-sent when the second changes.
         */
        public TimerBossBarBuilder nameFormat(IntFunction<String> format) {
            this.nameFormat = Objects.requireNonNull(format, "format");
            return this;
        }

        public TimerBossBarBuilder duration(long value, java.util.concurrent.TimeUnit unit) {
            this.durationTicks = unit.toMillis(value) / 50L;
            return this;
        }

        public TimerBossBarBuilder durationTicks(long ticks) {
            this.durationTicks = ticks;
            return this;
        }

        public TimerBossBarBuilder seconds(double seconds) {
            this.durationTicks = (long) (seconds * 20.0);
            return this;
        }

        public TimerBossBarBuilder color(BossBar.Color color) {
            this.color = Objects.requireNonNull(color, "color");
            return this;
        }

        public TimerBossBarBuilder color(String colorName) {
            this.color = parseColor(colorName, this.color);
            return this;
        }

        public TimerBossBarBuilder overlay(BossBar.Overlay overlay) {
            this.overlay = Objects.requireNonNull(overlay, "overlay");
            return this;
        }

        public TimerBossBarBuilder overlay(String overlayName) {
            this.overlay = parseOverlay(overlayName, this.overlay);
            return this;
        }

        public TimerBossBarBuilder flags(BossBar.Flag... flags) {
            this.flags = flags;
            return this;
        }

        public TimerBossBarBuilder onComplete(Consumer<BossBar> onComplete) {
            this.onComplete = onComplete;
            return this;
        }

        public TimerBossBarBuilder onTick(Consumer<Float> onTick) {
            this.onTick = onTick;
            return this;
        }

        public TimerBossBarBuilder autoHide(boolean autoHide) {
            this.autoHide = autoHide;
            return this;
        }

        /** How often the bar updates. Default 1 (every tick). Use 20 for once per second. */
        public TimerBossBarBuilder tickInterval(long ticks) {
            this.tickInterval = Math.max(1L, ticks);
            return this;
        }

        /**
         * Starts the timer and returns the BossBar for external control.
         * The bar starts full (1.0) and decreases to 0.0 over the specified duration.
         */
        public BossBar start() {
            Objects.requireNonNull(player, "player is required");
            if (durationTicks <= 0) {
                throw new IllegalArgumentException("duration must be > 0");
            }

            final int[] lastSeconds = {(int) Math.ceil(durationTicks / 20.0)};
            Component nameComponent = nameFormat != null
                    ? text.parse(nameFormat.apply(lastSeconds[0]))
                    : name != null ? name : Component.empty();
            EnumSet<BossBar.Flag> flagSet = EnumSet.noneOf(BossBar.Flag.class);
            flagSet.addAll(Arrays.asList(flags));
            BossBar bar = BossBar.bossBar(nameComponent, 1.0f, color, overlay, flagSet);
            dispatcher.showBossBar(player, bar);

            final long[] elapsed = {0L};
            final int taskId = player.getUniqueId().hashCode();

            // Cancel any existing timer for this player
            BukkitTask existing = activeTasks.remove(taskId);
            if (existing != null) existing.cancel();

            BukkitTask task = scheduler.runRepeating(tickInterval, tickInterval, () -> {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                elapsed[0] = Math.min(durationTicks, elapsed[0] + tickInterval);
                float next = Math.max(0f, 1f - (float) elapsed[0] / durationTicks);
                bar.progress(next);

                if (nameFormat != null) {
                    int seconds = (int) Math.ceil((durationTicks - elapsed[0]) / 20.0);
                    if (seconds != lastSeconds[0]) {
                        lastSeconds[0] = seconds;
                        bar.name(text.parse(nameFormat.apply(seconds)));
                    }
                }

                if (onTick != null) {
                    onTick.accept(next);
                }

                if (next <= 0f) {
                    if (onComplete != null) {
                        onComplete.accept(bar);
                    }
                    if (autoHide) {
                        dispatcher.hideBossBar(player, bar);
                    }
                    cancel();
                }
            });

            activeTasks.put(taskId, task);
            return bar;
        }

        private void cancel() {
            int taskId = player.getUniqueId().hashCode();
            BukkitTask task = activeTasks.remove(taskId);
            if (task != null) task.cancel();
        }

    }

    // ------------------------------------------------------------------
    //  Wave BossBar - oscillates progress between min and max
    // ------------------------------------------------------------------

    public static final class WaveBossBarBuilder {
        private final Dispatcher dispatcher;
        private final TextService text;
        private final SchedulerService scheduler;
        private final Map<Integer, BukkitTask> activeTasks;

        private Player player;
        private Component name;
        private float minProgress = 0.2f;
        private float maxProgress = 1.0f;
        private float speed = 0.02f;
        private int totalCycles = -1;
        private BossBar.Color color = BossBar.Color.PURPLE;
        private BossBar.Overlay overlay = BossBar.Overlay.PROGRESS;
        private BossBar.Flag[] flags = new BossBar.Flag[0];
        private Consumer<Integer> onCycle;
        private Runnable onStop;

        WaveBossBarBuilder(Dispatcher dispatcher, TextService text,
                           SchedulerService scheduler, Map<Integer, BukkitTask> activeTasks) {
            this.dispatcher = dispatcher;
            this.text = text;
            this.scheduler = scheduler;
            this.activeTasks = activeTasks;
        }

        public WaveBossBarBuilder player(Player player) {
            this.player = Objects.requireNonNull(player, "player");
            return this;
        }

        public WaveBossBarBuilder name(String name) {
            this.name = text.parse(name);
            return this;
        }

        public WaveBossBarBuilder name(Component name) {
            this.name = Objects.requireNonNull(name, "name");
            return this;
        }

        public WaveBossBarBuilder minProgress(float min) {
            this.minProgress = Math.max(0f, Math.min(1f, min));
            return this;
        }

        public WaveBossBarBuilder maxProgress(float max) {
            this.maxProgress = Math.max(0f, Math.min(1f, max));
            return this;
        }

        /** Speed of oscillation. Higher = faster wave. Default 0.02. */
        public WaveBossBarBuilder speed(float speed) {
            this.speed = speed;
            return this;
        }

        /** Number of full wave cycles. -1 = infinite until stopped. */
        public WaveBossBarBuilder cycles(int cycles) {
            this.totalCycles = cycles;
            return this;
        }

        public WaveBossBarBuilder color(BossBar.Color color) {
            this.color = Objects.requireNonNull(color, "color");
            return this;
        }

        public WaveBossBarBuilder color(String colorName) {
            this.color = parseColor(colorName, this.color);
            return this;
        }

        public WaveBossBarBuilder overlay(BossBar.Overlay overlay) {
            this.overlay = Objects.requireNonNull(overlay, "overlay");
            return this;
        }

        public WaveBossBarBuilder overlay(String overlayName) {
            this.overlay = parseOverlay(overlayName, this.overlay);
            return this;
        }

        public WaveBossBarBuilder flags(BossBar.Flag... flags) {
            this.flags = flags;
            return this;
        }

        /** Called each time a full cycle completes. Receives the cycle number (1-based). */
        public WaveBossBarBuilder onCycle(Consumer<Integer> onCycle) {
            this.onCycle = onCycle;
            return this;
        }

        /** Called when the wave stops (all cycles done or cancelled). */
        public WaveBossBarBuilder onStop(Runnable onStop) {
            this.onStop = onStop;
            return this;
        }

        /**
         * Starts the wave animation and returns a handle to stop it manually.
         * The bar oscillates between minProgress and maxProgress using a sine wave.
         */
        public WaveHandle start() {
            Objects.requireNonNull(player, "player is required");

            Component nameComponent = name != null ? name : Component.empty();
            EnumSet<BossBar.Flag> flagSet = EnumSet.noneOf(BossBar.Flag.class);
            flagSet.addAll(Arrays.asList(flags));
            BossBar bar = BossBar.bossBar(nameComponent, maxProgress, color, overlay, flagSet);
            dispatcher.showBossBar(player, bar);

            final int taskId = player.getUniqueId().hashCode() + "_wave".hashCode();
            final float mid = (minProgress + maxProgress) / 2f;
            final float amp = (maxProgress - minProgress) / 2f;
            final long[] tickCount = {0};
            final int[] cycleCount = {0};

            // Cancel existing wave for this player
            BukkitTask existing = activeTasks.remove(taskId);
            if (existing != null) existing.cancel();

            WaveHandle handle = new WaveHandle(activeTasks, taskId, dispatcher, player, bar, onStop);

            BukkitTask task = scheduler.runRepeating(1L, 1L, () -> {
                if (!player.isOnline()) {
                    handle.stop();
                    return;
                }

                tickCount[0]++;
                double angle = tickCount[0] * speed;
                float progress = (float) (mid + amp * Math.sin(angle));
                bar.progress(Math.max(minProgress, Math.min(maxProgress, progress)));

                // Check for completed cycle (sin crosses zero going positive)
                if (tickCount[0] > 1) {
                    double prev = ((tickCount[0] - 1) * speed);
                    double curr = angle;
                    if (Math.sin(prev) <= 0 && Math.sin(curr) > 0) {
                        cycleCount[0]++;
                        if (onCycle != null) {
                            onCycle.accept(cycleCount[0]);
                        }
                        if (totalCycles > 0 && cycleCount[0] >= totalCycles) {
                            handle.stop();
                            return;
                        }
                    }
                }
            });

            activeTasks.put(taskId, task);
            return handle;
        }

    }

    // ------------------------------------------------------------------
    //  WaveHandle - handle to control a running wave
    // ------------------------------------------------------------------

    public static final class WaveHandle {
        private final Map<Integer, BukkitTask> activeTasks;
        private final int taskId;
        private final Dispatcher dispatcher;
        private final Player player;
        private final BossBar bar;
        private final Runnable onStop;
        private volatile boolean stopped = false;

        WaveHandle(Map<Integer, BukkitTask> activeTasks, int taskId,
                   Dispatcher dispatcher, Player player, BossBar bar, Runnable onStop) {
            this.activeTasks = activeTasks;
            this.taskId = taskId;
            this.dispatcher = dispatcher;
            this.player = player;
            this.bar = bar;
            this.onStop = onStop;
        }

        public void stop() {
            if (stopped) return;
            stopped = true;
            BukkitTask task = activeTasks.remove(taskId);
            if (task != null) task.cancel();
            if (player.isOnline()) {
                dispatcher.hideBossBar(player, bar);
            }
            if (onStop != null) {
                onStop.run();
            }
        }

        public BossBar getBar() {
            return bar;
        }

        public boolean isStopped() {
            return stopped;
        }
    }

    // ------------------------------------------------------------------
    //  Static BossBarBuilder
    // ------------------------------------------------------------------

    public static final class BossBarBuilder {
        private final Dispatcher dispatcher;
        private final TextService text;
        private Component name;
        private float progress = 1.0f;
        private BossBar.Color color = BossBar.Color.PINK;
        private BossBar.Overlay overlay = BossBar.Overlay.PROGRESS;
        private final Set<BossBar.Flag> flags = EnumSet.noneOf(BossBar.Flag.class);

        private BossBarBuilder(Dispatcher dispatcher, TextService text) {
            this.dispatcher = dispatcher;
            this.text = text;
        }

        public BossBarBuilder name(String name) {
            this.name = text.parse(name);
            return this;
        }

        public BossBarBuilder name(String name, TagResolver... placeholders) {
            this.name = text.parse(name, placeholders);
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
            this.color = parseColor(colorName, this.color);
            return this;
        }

        public BossBarBuilder overlay(BossBar.Overlay overlay) {
            this.overlay = Objects.requireNonNull(overlay, "overlay");
            return this;
        }

        public BossBarBuilder overlay(String overlayName) {
            this.overlay = parseOverlay(overlayName, this.overlay);
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

        /** Builds the bar, shows it and returns it so it can be updated or hidden later. */
        public BossBar show(Player player) {
            BossBar bar = build();
            dispatcher.showBossBar(player, bar);
            return bar;
        }

    }
}
