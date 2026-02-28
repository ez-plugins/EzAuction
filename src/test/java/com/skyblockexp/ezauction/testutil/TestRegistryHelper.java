package com.skyblockexp.ezauction.testutil;

import com.skyblockexp.ezframework.Registry;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Small test helper to register mock instances into the EzFramework Registry for tests.
 */
public final class TestRegistryHelper {

    private TestRegistryHelper() {}

    public static <T> void register(JavaPlugin plugin, Class<T> type, T instance) {
        if (plugin == null || type == null || instance == null) return;
        try {
            Registry.forPlugin(plugin).register(type, instance);
        } catch (Throwable ignored) {
            // Tests may run without EzFramework available; ignore registration failures.
        }
    }
}
