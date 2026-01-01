package com.skyblockexp.ezauction.live;

import com.skyblockexp.ezauction.AuctionListing;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a queued live auction announcement.
 */
public record LiveAuctionEntry(AuctionListing listing, UUID sellerId, String sellerName) {

    public LiveAuctionEntry {
        Objects.requireNonNull(listing, "listing");
        sellerName = sellerName != null ? sellerName : "Unknown";
    }

    public ItemStack item() {
        return listing.item();
    }

    public double price() {
        return listing.price();
    }
}
