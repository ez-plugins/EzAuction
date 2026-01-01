package com.skyblockexp.ezauction.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Persistent data container backed item tag storage for 1.14+ servers.
 */
public final class PdcItemTagStorage implements ItemTagStorage {

    private final JavaPlugin plugin;

    public PdcItemTagStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void setString(ItemStack item, String key, String value) {
        if (item == null || key == null || key.isEmpty() || value == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(plugin, key), PersistentDataType.STRING, value);
        item.setItemMeta(meta);
    }

    @Override
    public String getString(ItemStack item, String key) {
        if (item == null || key == null || key.isEmpty()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(new NamespacedKey(plugin, key), PersistentDataType.STRING);
    }

    @Override
    public void setDouble(ItemStack item, String key, double value) {
        if (item == null || key == null || key.isEmpty()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(plugin, key), PersistentDataType.DOUBLE, value);
        item.setItemMeta(meta);
    }

    @Override
    public Double getDouble(ItemStack item, String key) {
        if (item == null || key == null || key.isEmpty()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(new NamespacedKey(plugin, key), PersistentDataType.DOUBLE);
    }

    @Override
    public void setInt(ItemStack item, String key, int value) {
        if (item == null || key == null || key.isEmpty()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(plugin, key), PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
    }

    @Override
    public Integer getInt(ItemStack item, String key) {
        if (item == null || key == null || key.isEmpty()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(new NamespacedKey(plugin, key), PersistentDataType.INTEGER);
    }
}
