package com.skyblockexp.ezauction.transaction;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a single entry in a player's auction transaction history.
 */
public record AuctionTransactionHistoryEntry(
        AuctionTransactionType type,
        UUID counterpartId,
        String counterpartName,
        double price,
        long timestamp,
        ItemStack item) {

    public AuctionTransactionHistoryEntry {
        Objects.requireNonNull(type, "type");
        if (price < 0) {
            price = 0.0D;
        }
        if (item != null) {
            item = item.clone();
        }
    }

    @Override
    public ItemStack item() {
        return item != null ? item.clone() : null;
    }
}
