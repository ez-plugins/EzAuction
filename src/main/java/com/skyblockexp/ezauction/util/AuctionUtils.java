package com.skyblockexp.ezauction.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.time.Duration;

/**
 * Utility methods for formatting durations, pluralization, and other common auction-related helpers.
 * 
 * <p>This class is stateless and safe for concurrent use. All methods are static and intended for reuse
 * across the plugin and other projects.</p>
 *
 * @author Shadow48402
 * @since 1.1.0
 */
public class AuctionUtils {

    /**
     * Formats a {@link Duration} as a human-readable string (e.g., "1d 2h 30m").
     *
     * @param duration     The duration to format
     * @param unknownValue The value to return if duration is null
     * @return A formatted string representing the duration
     */
    public static String formatDuration(java.time.Duration duration, String unknownValue) {
        if (duration == null) return unknownValue;
        long totalMinutes = Math.max(0L, duration.toMinutes());
        long days = totalMinutes / (60L * 24L);
        long hours = (totalMinutes % (60L * 24L)) / 60L;
        long minutes = totalMinutes % 60L;
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (days > 0) parts.add(days + "d");
        if (hours > 0) parts.add(hours + "h");
        if (minutes > 0 && parts.size() < 2) parts.add(minutes + "m");
        if (parts.isEmpty()) parts.add("0m");
        return String.join(" ", parts);
    }

    /**
     * Returns the plural suffix ("s") for a given amount, or an empty string for singular.
     *
     * @param amount The amount to check
     * @return "s" if amount is not 1, otherwise ""
     */
    public static String pluralSuffix(int amount) {
        return amount == 1 ? "" : "s";
    }
}
