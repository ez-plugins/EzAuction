package com.skyblockexp.ezauction.compat;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Minimal {@link JavaPlugin} used by compatibility tests that need a real plugin instance for
 * {@link org.bukkit.NamespacedKey} construction.
 */
public final class TestPlugin extends JavaPlugin {
}
