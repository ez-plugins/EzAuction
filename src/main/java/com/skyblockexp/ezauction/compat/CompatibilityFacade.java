package com.skyblockexp.ezauction.compat;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Resolves version-specific compatibility adapters for EzAuction features.
 */
public final class CompatibilityFacade {

    private final ItemTagStorage itemTagStorage;
    private final HologramPlatform hologramPlatform;

    private CompatibilityFacade(JavaPlugin plugin) {
        ClassLoader loader;
        try {
            var method = JavaPlugin.class.getDeclaredMethod("getClassLoader");
            method.setAccessible(true);
            loader = (ClassLoader) method.invoke(plugin);
        } catch (Exception e) {
            throw new RuntimeException("Unable to access plugin classloader", e);
        }
        boolean pdcAvailable = isClassPresent("org.bukkit.persistence.PersistentDataContainer", loader)
                && isClassPresent("org.bukkit.NamespacedKey", loader);
        this.itemTagStorage = pdcAvailable ? new PdcItemTagStorage(plugin) : new LoreItemTagStorage();
        boolean textDisplayAvailable = isClassPresent("org.bukkit.entity.TextDisplay", loader)
                && isClassPresent("org.bukkit.entity.Display", loader);
        this.hologramPlatform = textDisplayAvailable
                ? new TextDisplayHologramPlatform(plugin)
                : new NoopHologramPlatform();
    }

    public static CompatibilityFacade create(JavaPlugin plugin) {
        return new CompatibilityFacade(plugin);
    }

    public ItemTagStorage itemTagStorage() {
        return itemTagStorage;
    }

    public HologramPlatform hologramPlatform() {
        return hologramPlatform;
    }

    private static boolean isClassPresent(String className, ClassLoader loader) {
        try {
            Class.forName(className, false, loader);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
