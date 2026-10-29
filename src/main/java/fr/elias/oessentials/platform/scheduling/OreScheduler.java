package fr.elias.oessentials.platform.scheduling;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Folia/Luminol-compatible scheduler utility.
 *
 * Uses Paper's new regional scheduler APIs (available since Paper 1.19.4+) which
 * work transparently on both standard Paper and Folia/Luminol:
 *   - GlobalRegionScheduler  → sync tasks not tied to a specific entity/location
 *   - EntityScheduler        → sync tasks tied to a specific entity (e.g. players)
 *   - AsyncScheduler         → async tasks
 *
 * Drop-in replacement for all Bukkit.getScheduler() calls.
 */
public final class OreScheduler {

    private OreScheduler() {}

    // -------------------------------------------------------------------------
    // Sync – global region
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // While the plugin is disabling, the server refuses new tasks ("Plugin attempted to register
    // task while disabled"), which broke shutdown cleanup that schedules work, e.g. removing
    // nametag entities of online players (they were then left in the world). Work meant to run
    // "now" runs immediately instead, when this thread may touch it; delayed or repeating work
    // could never run any more and is skipped.
    // -------------------------------------------------------------------------

    private static boolean stopping(Plugin plugin) {
        return !plugin.isEnabled();
    }

    private static OreTask runNowWhileStopping(Plugin plugin, Runnable task, boolean allowedOnThisThread) {
        if (allowedOnThisThread) {
            try {
                task.run();
            } catch (Throwable t) {
                plugin.getLogger().warning("[Scheduler] Shutdown task failed: " + t);
            }
        }
        return OreTask.EMPTY;
    }

    /** Run a sync task on the next available global tick. */
    public static OreTask run(Plugin plugin, Runnable task) {
        if (stopping(plugin)) return runNowWhileStopping(plugin, task, org.bukkit.Bukkit.isGlobalTickThread());
        return ModuleTaskScope.schedule(plugin, task, false, owned ->
                plugin.getServer().getGlobalRegionScheduler()
                        .run(plugin, ctx -> owned.run())
        );
    }

    /** Run a sync task after {@code delayTicks} ticks. */
    public static OreTask runLater(Plugin plugin, Runnable task, long delayTicks) {
        if (stopping(plugin)) return OreTask.EMPTY;
        return ModuleTaskScope.schedule(plugin, task, false, owned ->
                plugin.getServer().getGlobalRegionScheduler()
                        .runDelayed(plugin, ctx -> owned.run(), Math.max(1, delayTicks))
        );
    }

    // -------------------------------------------------------------------------
    // Sync – location-bound (use for entity spawns / entity access on Folia)
    // -------------------------------------------------------------------------

    /**
     * Run a sync task on the region owning the given location.
     * On standard Paper this resolves to the main thread, on Folia it schedules
     * on the correct regional thread — required for spawnEntity / getNearbyEntities
     * and any entity property changes.
     */
    public static OreTask runAtLocation(Plugin plugin, org.bukkit.Location location, Runnable task) {
        if (stopping(plugin)) return runNowWhileStopping(plugin, task, org.bukkit.Bukkit.isOwnedByCurrentRegion(location));
        return ModuleTaskScope.schedule(plugin, task, false, owned ->
                plugin.getServer().getRegionScheduler()
                        .run(plugin, location, ctx -> owned.run())
        );
    }

    /** Run a repeating sync task (global). */
    public static OreTask runTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (stopping(plugin)) return OreTask.EMPTY;
        return ModuleTaskScope.schedule(plugin, task, true, owned ->
                plugin.getServer().getGlobalRegionScheduler()
                        .runAtFixedRate(plugin, ctx -> owned.run(),
                                Math.max(1, delayTicks), Math.max(1, periodTicks))
        );
    }

    // -------------------------------------------------------------------------
    // Sync – entity-bound (use for player-specific tasks to stay in the right region)
    // -------------------------------------------------------------------------

    /** Run a sync task bound to an entity's region. Falls back to EMPTY if entity is invalid. */
    public static OreTask runForEntity(Plugin plugin, Entity entity, Runnable task) {
        if (stopping(plugin)) return runNowWhileStopping(plugin, task, org.bukkit.Bukkit.isOwnedByCurrentRegion(entity));
        return ModuleTaskScope.schedule(plugin, task, false, owned ->
                entity.getScheduler().run(plugin, ctx -> owned.run(), null));
    }

    /** Run a delayed sync task bound to an entity's region. */
    public static OreTask runLaterForEntity(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        if (stopping(plugin)) return OreTask.EMPTY;
        return ModuleTaskScope.schedule(plugin, task, false, owned ->
                entity.getScheduler().runDelayed(plugin, ctx -> owned.run(), null, Math.max(1, delayTicks)));
    }

    /** Run a repeating sync task bound to an entity's region. */
    public static OreTask runTimerForEntity(Plugin plugin, Entity entity, Runnable task, long delayTicks, long periodTicks) {
        if (stopping(plugin)) return OreTask.EMPTY;
        return ModuleTaskScope.schedule(plugin, task, true, owned ->
                entity.getScheduler().runAtFixedRate(plugin, ctx -> owned.run(), null,
                        Math.max(1, delayTicks), Math.max(1, periodTicks)));
    }

    // -------------------------------------------------------------------------
    // Async
    // -------------------------------------------------------------------------

    /** Run a task asynchronously. */
    public static OreTask runAsync(Plugin plugin, Runnable task) {
        // Shutdown work such as final saves still completes, on the stopping thread.
        if (stopping(plugin)) return runNowWhileStopping(plugin, task, true);
        return ModuleTaskScope.schedule(plugin, task, false, owned ->
                plugin.getServer().getAsyncScheduler()
                        .runNow(plugin, ctx -> owned.run())
        );
    }

    /** Run an async task after {@code delayTicks} ticks (converted to ms). */
    public static OreTask runAsyncLater(Plugin plugin, Runnable task, long delayTicks) {
        if (stopping(plugin)) return OreTask.EMPTY;
        return ModuleTaskScope.schedule(plugin, task, false, owned ->
                plugin.getServer().getAsyncScheduler()
                        .runDelayed(plugin, ctx -> owned.run(),
                                delayTicks * 50L, TimeUnit.MILLISECONDS)
        );
    }

    /** Run a repeating async task. */
    public static OreTask runAsyncTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (stopping(plugin)) return OreTask.EMPTY;
        return ModuleTaskScope.schedule(plugin, task, true, owned ->
                plugin.getServer().getAsyncScheduler()
                        .runAtFixedRate(plugin, ctx -> owned.run(),
                                Math.max(1, delayTicks) * 50L,
                                Math.max(1, periodTicks) * 50L,
                                TimeUnit.MILLISECONDS)
        );
    }

    // -------------------------------------------------------------------------
    // Cancellation
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Folia detection
    // -------------------------------------------------------------------------

    private static final boolean FOLIA;
    static {
        boolean f;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            f = true;
        } catch (ClassNotFoundException ignored) {
            f = false;
        }
        FOLIA = f;
    }

    /** Returns true when running on Folia/Luminol (threaded region scheduler). */
    public static boolean isFolia() { return FOLIA; }

    // -------------------------------------------------------------------------
    // Cancellation
    // -------------------------------------------------------------------------

    /**
     * Cancel all tasks registered by {@code plugin}.
     * Replaces Bukkit.getScheduler().cancelTasks(plugin).
     */
    public static void cancelAll(Plugin plugin) {
        try {
            plugin.getServer().getGlobalRegionScheduler().cancelTasks(plugin);
        } catch (Throwable ignored) {}
        try {
            plugin.getServer().getAsyncScheduler().cancelTasks(plugin);
        } catch (Throwable ignored) {}
    }
}
