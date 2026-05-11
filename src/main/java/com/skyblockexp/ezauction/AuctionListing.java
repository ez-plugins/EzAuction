package com.skyblockexp.ezauction;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a single auction listing.
 *
 * <p>A listing is considered <em>team-scoped</em> when {@link #teamId()} is non-null.
 * Team-scoped listings are only visible and purchasable by members of that team.</p>
 */
public record AuctionListing(
        String id,
        UUID sellerId,
        double price,
        long expiryEpochMillis,
        ItemStack item,
        double deposit,
        /* @Nullable */ UUID teamId) {

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

    /**
     * Returns {@code true} if this listing is restricted to members of a specific team.
     */
    public boolean isTeamListing() {
        return teamId != null;
    }

    /**
     * Convenience factory that creates a global (non-team) listing with a {@code null} teamId.
     */
    public static AuctionListing global(String id, UUID sellerId, double price, long expiryEpochMillis,
            ItemStack item, double deposit) {
        return new AuctionListing(id, sellerId, price, expiryEpochMillis, item, deposit, null);
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
