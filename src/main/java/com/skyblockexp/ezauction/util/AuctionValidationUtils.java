package com.skyblockexp.ezauction.util;

import com.skyblockexp.ezauction.api.AuctionListingLimitResolver;
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Utility methods for validation and item counting.
 */
public class AuctionValidationUtils {
    /**
     * Counts the total item amount in a list of ItemStacks.
     *
     * @param items the list of ItemStacks
     * @return the total item amount
     */
    public static int countItemAmount(List<ItemStack> items) {
        if (items == null || items.isEmpty()) return 0;
        int total = 0;
        for (ItemStack stack : items) {
            if (stack == null || stack.getType() == Material.AIR) continue;
            total += Math.max(0, stack.getAmount());
        }
        return total;
    }

    /**
     * Resolves the listing limit for a seller based on configuration and limit resolver.
     *
     * @param sellerId the UUID of the seller
     * @param config the auction configuration
     * @param resolver the listing limit resolver
     * @return the resolved listing limit
     */
    public static int resolveListingLimit(UUID sellerId, AuctionConfiguration config, AuctionListingLimitResolver resolver) {
        int baseListingLimit = config != null ? Math.max(0, config.baseListingLimit()) : 0;
        if (sellerId == null) return baseListingLimit;
        if (resolver == null) return baseListingLimit;
        int resolvedLimit = resolver.resolveLimit(sellerId, baseListingLimit);
        return Math.max(0, resolvedLimit);
    }

}
