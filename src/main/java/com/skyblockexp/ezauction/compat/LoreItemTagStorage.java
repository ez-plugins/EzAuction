package com.skyblockexp.ezauction.compat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Legacy item tag storage using hidden lore lines for 1.7-1.13 servers.
 */
public final class LoreItemTagStorage implements ItemTagStorage {

    private static final String PREFIX = ChatColor.DARK_GRAY + "[EA]";

    @Override
    public void setString(ItemStack item, String key, String value) {
        if (item == null || key == null || key.isEmpty()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        String tagPrefix = PREFIX + key + "=";
        Iterator<String> iterator = lore.iterator();
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line != null && line.startsWith(tagPrefix)) {
                iterator.remove();
            }
        }
        if (value != null) {
            lore.add(tagPrefix + value);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    @Override
    public String getString(ItemStack item, String key) {
        if (item == null || key == null || key.isEmpty()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return null;
        }
        List<String> lore = meta.getLore();
        if (lore == null) {
            return null;
        }
        String tagPrefix = PREFIX + key + "=";
        for (String line : lore) {
            if (line != null && line.startsWith(tagPrefix)) {
                return line.substring(tagPrefix.length());
            }
        }
        return null;
    }

    @Override
    public void setDouble(ItemStack item, String key, double value) {
        setString(item, key, Double.toString(value));
    }

    @Override
    public Double getDouble(ItemStack item, String key) {
        String value = getString(item, key);
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public void setInt(ItemStack item, String key, int value) {
        setString(item, key, Integer.toString(value));
    }

    @Override
    public Integer getInt(ItemStack item, String key) {
        String value = getString(item, key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
