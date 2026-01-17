package com.skyblockexp.ezauction;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a single auction listing.
 */
public record AuctionListing(
        String id,
        UUID sellerId,
        double price,
        long expiryEpochMillis,
        ItemStack item,
        double deposit) {

    public AuctionListing {
        Objects.requireNonNull(id, "id");
        sellerId = Objects.requireNonNull(sellerId, "sellerId");
        deposit = Math.max(0.0D, deposit);
        item = item != null ? item.clone() : null;
    }

    @Override
    public ItemStack item() {
        return item != null ? item.clone() : null;
    }

    public boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    public boolean isExpired(long currentTimeMillis) {
        return currentTimeMillis >= expiryEpochMillis;
    }

    // Compatibility aliases for alternative naming conventions
    public UUID seller() {
        return sellerId;
    }

    public long expiresAt() {
        return expiryEpochMillis;
    }
}
