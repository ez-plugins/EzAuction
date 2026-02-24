package com.skyblockexp.ezauction.gui;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.Locale;

public final class AuctionMenuUtils {

    private AuctionMenuUtils() {}

    public static String listingItemSortKey(AuctionListing listing) {
        return listing == null ? "" : itemSortKey(listing.item());
    }

    public static String orderItemSortKey(AuctionOrder order) {
        return order == null ? "" : itemSortKey(order.requestedItem());
    }

    public static int listingQuantity(com.skyblockexp.ezauction.AuctionListing listing) {
        if (listing == null) {
            return 0;
        }
        ItemStack item = listing.item();
        return item != null ? item.getAmount() : 0;
    }

    public static int orderQuantity(com.skyblockexp.ezauction.AuctionOrder order) {
        if (order == null) {
            return 0;
        }
        ItemStack item = order.requestedItem();
        return item != null ? item.getAmount() : 0;
    }

    public static String itemSortKey(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                String name = meta.getDisplayName();
                if (name != null && !name.isEmpty()) {
                    return normalizeText(name);
                }
            }
            if (meta.hasEnchants()) {
                for (Enchantment enc : meta.getEnchants().keySet()) {
                    String name = enc.getKey().getKey();
                    if (name != null && !name.isEmpty()) {
                        return normalizeText(name.replace('_', ' '));
                    }
                }
            }
            if (meta instanceof EnchantmentStorageMeta storage && storage.hasStoredEnchants()) {
                for (Enchantment enc : storage.getStoredEnchants().keySet()) {
                    String name = enc.getKey().getKey();
                    if (name != null && !name.isEmpty()) {
                        return normalizeText(name.replace('_', ' '));
                    }
                }
            }
            String localized = resolveLocalizedName(meta);
            if (localized != null && !localized.isEmpty()) {
                return normalizeText(localized);
            }
        }
        return normalizeMaterialName(item.getType());
    }

    public static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String stripped = ChatColor.stripColor(value);
        if (stripped == null) {
            return "";
        }
        String trimmed = stripped.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.toLowerCase(Locale.ENGLISH);
    }

    public static String normalizeMaterialName(Material material) {
        if (material == null) {
            return "";
        }
        return material.name().replace('_', ' ').toLowerCase(Locale.ENGLISH);
    }

    public static String toRomanNumeral(int number) {
        if (number <= 0) {
            return "";
        }
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        int remaining = number;
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.length && remaining > 0; index++) {
            while (remaining >= values[index]) {
                remaining -= values[index];
                builder.append(numerals[index]);
            }
        }
        return builder.toString();
    }

    public static String resolveLocalizedName(ItemMeta meta) {
        if (meta == null) {
            return null;
        }
        try {
            Method m = meta.getClass().getMethod("getLocalizedName");
            Object res = m.invoke(meta);
            if (res instanceof String) {
                return (String) res;
            }
        } catch (NoSuchMethodException ex) {
            // ignore
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }
}
