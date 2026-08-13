package com.skyblockexp.ezauction.compat;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Folia-safe scheduler facade.
 *
 * <p>Routes sync/delayed/repeating tasks to the global region scheduler on Folia and to the
 * regular Bukkit scheduler everywhere else, so the plugin boots cleanly on regionised servers
 * without throwing {@code IllegalStateException} ("async thread access" / no global
 * BukkitScheduler). Async tasks are routed to the Folia async scheduler.</p>
 *
 * <p>The FoliaLib third-party library is intentionally <em>not</em> used as a dependency; the
 * detection and routing is done by reflection so the plugin keeps zero extra runtime
 * dependencies. On non-Folia servers every method is a thin pass-through to the Bukkit
 * scheduler and therefore a no-op from a behaviour standpoint.</p>
 *
 * <p>All methods return a real {@link BukkitTask} (on Folia this is a
 * {@code ScheduledTask} which is a {@code BukkitTask} subtype) so callers can cancel timers as
 * before.</p>
 */
public final class Scheduler {

    private static final boolean FOLIA = detectFolia();

    private static final Method GLOBAL_RUN;
    private static final Method GLOBAL_RUN_DELAYED;
    private static final Method GLOBAL_RUN_FIXED_RATE;
    private static final Method ASYNC_RUN_NOW;

    static {
        Method gr = null, grd = null, grfr = null, arn = null;
        if (FOLIA) {
            try {
                Class<?> serverClass = Bukkit.getServer().getClass();
                Class<?> consumer = java.util.function.Consumer.class;
                gr = serverClass.getMethod("getGlobalRegionScheduler")
                        .invoke(Bukkit.getServer()).getClass()
                        .getMethod("run", Plugin.class, consumer);
                grd = serverClass.getMethod("getGlobalRegionScheduler")
                        .invoke(Bukkit.getServer()).getClass()
                        .getMethod("runDelayed", Plugin.class, consumer, long.class);
                grfr = serverClass.getMethod("getGlobalRegionScheduler")
                        .invoke(Bukkit.getServer()).getClass()
                        .getMethod("runAtFixedRate", Plugin.class, consumer, long.class, long.class);
                arn = serverClass.getMethod("getAsyncScheduler")
                        .invoke(Bukkit.getServer()).getClass()
                        .getMethod("runNow", Plugin.class, consumer);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        GLOBAL_RUN = gr;
        GLOBAL_RUN_DELAYED = grd;
        GLOBAL_RUN_FIXED_RATE = grfr;
        ASYNC_RUN_NOW = arn;
    }

    private Scheduler() {
    }

    /**
     * @return {@code true} if the running server is Folia (regionised) and exposes the
     *         threaded-regions scheduler API.
     */
    public static boolean isFolia() {
        return FOLIA;
    }

    /**
     * Run a task on the main/global region thread as soon as possible.
     */
    public static BukkitTask run(Plugin plugin, Runnable task) {
        if (FOLIA) {
            return (BukkitTask) invoke(GLOBAL_RUN, globalScheduler(), plugin, (Consumer<Object>) t -> task.run());
        }
        return Bukkit.getScheduler().runTask(plugin, task);
    }

    /**
     * Run a task on the main/global region thread after the given delay in ticks.
     */
    public static BukkitTask runLater(Plugin plugin, Runnable task, long delayTicks) {
        if (FOLIA) {
            return (BukkitTask) invoke(GLOBAL_RUN_DELAYED, globalScheduler(), plugin, (Consumer<Object>) t -> task.run(), delayTicks);
        }
        return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    /**
     * Run a repeating task on the main/global region thread.
     */
    public static BukkitTask runTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (FOLIA) {
            return (BukkitTask) invoke(GLOBAL_RUN_FIXED_RATE, globalScheduler(), plugin, (Consumer<Object>) t -> task.run(), delayTicks, periodTicks);
        }
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    /**
     * Run a task asynchronously (off the tick thread) as soon as possible.
     */
    public static BukkitTask runAsync(Plugin plugin, Runnable task) {
        if (FOLIA) {
            return (BukkitTask) invoke(ASYNC_RUN_NOW, asyncScheduler(), plugin, (Consumer<Object>) t -> task.run());
        }
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    private static Object globalScheduler() {
        try {
            return Bukkit.getServer().getClass()
                    .getMethod("getGlobalRegionScheduler")
                    .invoke(Bukkit.getServer());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Folia detected but getGlobalRegionScheduler unavailable", e);
        }
    }

    private static Object asyncScheduler() {
        try {
            return Bukkit.getServer().getClass()
                    .getMethod("getAsyncScheduler")
                    .invoke(Bukkit.getServer());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Folia detected but getAsyncScheduler unavailable", e);
        }
    }

    private static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to schedule Folia task", e);
        }
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
