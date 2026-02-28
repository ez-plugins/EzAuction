package com.skyblockexp.ezauction.bootstrap.component;

// keep compile-time EzFramework dependency optional for runtime detection
import org.bukkit.plugin.Plugin;
import java.util.logging.Level;
import com.skyblockexp.ezframework.bootstrap.Component;

/**
 * Component responsible for creating a bridge to EzFramework (EzPlugin).
 *
 * Attempts multiple safe strategies to obtain the EzPlugin instance and stores
 * the Result for consumers to read via `getResult()`.
 */
public class EzFrameworkIntegrationComponent implements Component {

    public static final class Result {
        // Use Object to avoid hard cast/classloader pitfalls in test environments
        public final Object ezPlugin;

        public Result(Object ezPlugin) {
            this.ezPlugin = ezPlugin;
        }
    }

    private final Plugin plugin;
    private Result result;

    public EzFrameworkIntegrationComponent(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() throws Exception {
        try {
            // Try plugin manager first (most robust when EzFramework is a loaded plugin)
            Plugin p = plugin.getServer().getPluginManager().getPlugin("EzFramework");
            if (p != null) {
                java.util.logging.Logger logger = plugin.getLogger();
                if (logger != null) logger.info("EzFramework detected via plugin manager — integration enabled.");
                this.result = new Result(p);
                return;
            }
        } catch (Throwable ignored) {}

        try {
            // Fallback: try static accessor via reflection if present
            Class<?> cls = Class.forName("com.skyblockexp.ezframework.EzPlugin");
            try {
                java.lang.reflect.Method m = cls.getMethod("getInstance");
                Object inst = m.invoke(null);
                if (inst != null) {
                    java.util.logging.Logger logger = plugin.getLogger();
                    if (logger != null) logger.info("EzFramework instance obtained via static accessor.");
                    this.result = new Result(inst);
                    return;
                }
            } catch (NoSuchMethodException ns) {
                // No static accessor; nothing more to try.
            }
        } catch (ClassNotFoundException cnf) {
            plugin.getLogger().info("EzFramework not present on classpath; skipping integration.");
            this.result = new Result(null);
            return;
        } catch (Throwable ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize EzFramework bridge.", ex);
        }

        this.result = new Result(null);
    }

    @Override
    public void stop() throws Exception {
        // no-op
    }

    public Result getResult() {
        return this.result;
    }
}
