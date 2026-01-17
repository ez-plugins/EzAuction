package com.skyblockexp.ezauction.compat;

import org.bukkit.inventory.ItemStack;

/**
 * Abstraction for storing lightweight tag data on items.
 */
public interface ItemTagStorage {

    void setString(ItemStack item, String key, String value);

    String getString(ItemStack item, String key);

    void setDouble(ItemStack item, String key, double value);

    Double getDouble(ItemStack item, String key);

    void setInt(ItemStack item, String key, int value);

    Integer getInt(ItemStack item, String key);

    // Convenience methods for generic set/get
    default void set(ItemStack item, String key, String value) {
        setString(item, key, value);
    }

    default String get(ItemStack item, String key) {
        return getString(item, key);
    }
}
