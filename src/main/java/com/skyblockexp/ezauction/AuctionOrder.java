package com.skyblockexp.ezauction;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a buy order placed by a player on the auction house.
 */
public record AuctionOrder(
        String id,
        UUID buyerId,
        double offeredPrice,
        long expiryEpochMillis,
        ItemStack requestedItem,
        double reservedAmount) {

    public AuctionOrder {
        Objects.requireNonNull(id, "id");
        buyerId = Objects.requireNonNull(buyerId, "buyerId");
        requestedItem = requestedItem != null ? requestedItem.clone() : null;
        reservedAmount = Math.max(0.0D, reservedAmount);
    }

    @Override
    public ItemStack requestedItem() {
        return requestedItem != null ? requestedItem.clone() : null;
    }

    public boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    public boolean isExpired(long currentTimeMillis) {
        return currentTimeMillis >= expiryEpochMillis;
    }
}
