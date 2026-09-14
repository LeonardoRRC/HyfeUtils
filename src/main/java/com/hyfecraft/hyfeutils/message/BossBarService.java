package com.hyfecraft.hyfeutils.message;

import com.hyfecraft.hyfeutils.scheduler.SchedulerService;
import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;

public final class BossBarService {
    private final BukkitAudiences audiences;
    private final TextService text;
    private final SchedulerService scheduler;
    private final Map<Integer, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    public BossBarService(BukkitAudiences audiences, TextService text, SchedulerService scheduler) {
        this.audiences = Objects.requireNonNull(audiences, "audiences");
        this.text = Objects.requireNonNull(text, "text");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    public BossBarBuilder builder() {
        return new BossBarBuilder(audiences, text);
    }

    public TimerBossBarBuilder timer() {
        return new TimerBossBarBuilder(audiences, text, scheduler, activeTasks);
    }

    public WaveBossBarBuilder wave() {
        return new WaveBossBarBuilder(audiences, text, scheduler, activeTasks);
    }

    public void show(Player player, BossBar bossBar) {
        audiences.player(player).showBossBar(bossBar);
    }

    public void hide(Player player, BossBar bossBar) {
        audiences.player(player).hideBossBar(bossBar);
    }

    public void cancelAll() {
        activeTasks.values().forEach(BukkitTask::cancel);
        activeTasks.clear();
    }

    // ------------------------------------------------------------------
    //  Timer BossBar - countdown from full to empty
    // ------------------------------------------------------------------

    public static final class TimerBossBarBuilder {
        private final BukkitAudiences audiences;
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

        TimerBossBarBuilder(BukkitAudiences audiences, TextService text,
                            SchedulerService scheduler, Map<Integer, BukkitTask> activeTasks) {
            this.audiences = audiences;
            this.text = text;
            this.scheduler = scheduler;
            this.activeTasks = activeTasks;
        }

        public TimerBossBarBuilder player(Player player) {
            this.player = Objects.requireNonNull(player, "player");
            return this;
        }

        public TimerBossBarBuilder name(String name) {
            this.name = LegacyComponentSerializer.legacySection().deserialize(text.colorize(name));
            return this;
        }

        public TimerBossBarBuilder name(Component name) {
            this.name = Objects.requireNonNull(name, "name");
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
            this.color = parseColor(colorName);
            return this;
        }

        public TimerBossBarBuilder overlay(BossBar.Overlay overlay) {
            this.overlay = Objects.requireNonNull(overlay, "overlay");
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

        /**
         * Starts the timer and returns the BossBar for external control.
         * The bar starts full (1.0) and decreases to 0.0 over the specified duration.
         */
        public BossBar start() {
            Objects.requireNonNull(player, "player is required");
            if (durationTicks <= 0) {
                throw new IllegalArgumentException("duration must be > 0");
            }

            Component nameComponent = name != null ? name : Component.empty();
            EnumSet<BossBar.Flag> flagSet = EnumSet.noneOf(BossBar.Flag.class);
            flagSet.addAll(Arrays.asList(flags));
            BossBar bar = BossBar.bossBar(nameComponent, 1.0f, color, overlay, flagSet);
            audiences.player(player).showBossBar(bar);

            final float startProgress = 1.0f;
            final float decrement = startProgress / (float) durationTicks;
            final int taskId = player.getUniqueId().hashCode();

            // Cancel any existing timer for this player
            BukkitTask existing = activeTasks.remove(taskId);
            if (existing != null) existing.cancel();

            BukkitTask task = scheduler.runRepeating(1L, 1L, () -> {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                float current = bar.progress();
                float next = Math.max(0f, current - decrement);
                bar.progress(next);

                if (onTick != null) {
                    onTick.accept(next);
                }

                if (next <= 0f) {
                    if (onComplete != null) {
                        onComplete.accept(bar);
                    }
                    if (autoHide) {
                        audiences.player(player).hideBossBar(bar);
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

        private static BossBar.Color parseColor(String name) {
            if (name == null) return BossBar.Color.RED;
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
                    default -> BossBar.Color.RED;
                };
            }
        }
    }

    // ------------------------------------------------------------------
    //  Wave BossBar - oscillates progress between min and max
    // ------------------------------------------------------------------

    public static final class WaveBossBarBuilder {
        private final BukkitAudiences audiences;
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

        WaveBossBarBuilder(BukkitAudiences audiences, TextService text,
                           SchedulerService scheduler, Map<Integer, BukkitTask> activeTasks) {
            this.audiences = audiences;
            this.text = text;
            this.scheduler = scheduler;
            this.activeTasks = activeTasks;
        }

        public WaveBossBarBuilder player(Player player) {
            this.player = Objects.requireNonNull(player, "player");
            return this;
        }

        public WaveBossBarBuilder name(String name) {
            this.name = LegacyComponentSerializer.legacySection().deserialize(text.colorize(name));
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
            this.color = parseColor(colorName);
            return this;
        }

        public WaveBossBarBuilder overlay(BossBar.Overlay overlay) {
            this.overlay = Objects.requireNonNull(overlay, "overlay");
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
            audiences.player(player).showBossBar(bar);

            final int taskId = player.getUniqueId().hashCode() + "_wave".hashCode();
            final float mid = (minProgress + maxProgress) / 2f;
            final float amp = (maxProgress - minProgress) / 2f;
            final long[] tickCount = {0};
            final int[] cycleCount = {0};

            // Cancel existing wave for this player
            BukkitTask existing = activeTasks.remove(taskId);
            if (existing != null) existing.cancel();

            WaveHandle handle = new WaveHandle(activeTasks, taskId, audiences, player, bar, onStop);

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

        private static BossBar.Color parseColor(String name) {
            if (name == null) return BossBar.Color.PURPLE;
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
                    default -> BossBar.Color.PURPLE;
                };
            }
        }
    }

    // ------------------------------------------------------------------
    //  WaveHandle - handle to control a running wave
    // ------------------------------------------------------------------

    public static final class WaveHandle {
        private final Map<Integer, BukkitTask> activeTasks;
        private final int taskId;
        private final BukkitAudiences audiences;
        private final Player player;
        private final BossBar bar;
        private final Runnable onStop;
        private volatile boolean stopped = false;

        WaveHandle(Map<Integer, BukkitTask> activeTasks, int taskId,
                   BukkitAudiences audiences, Player player, BossBar bar, Runnable onStop) {
            this.activeTasks = activeTasks;
            this.taskId = taskId;
            this.audiences = audiences;
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
                audiences.player(player).hideBossBar(bar);
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
    //  Static BossBarBuilder (unchanged)
    // ------------------------------------------------------------------

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
