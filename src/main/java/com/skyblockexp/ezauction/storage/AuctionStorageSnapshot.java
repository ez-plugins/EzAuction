package com.skyblockexp.ezauction.storage;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/**
 * Immutable snapshot of auction data loaded from storage.
 */
public final class AuctionStorageSnapshot {

    private final Map<String, AuctionListing> listings;
    private final Map<String, AuctionOrder> orders;
    private final Map<UUID, List<ItemStack>> pendingReturns;

    public AuctionStorageSnapshot(Map<String, AuctionListing> listings, Map<String, AuctionOrder> orders,
            Map<UUID, List<ItemStack>> pendingReturns) {
        this.listings = listings != null ? new HashMap<>(listings) : new HashMap<>();
        this.orders = orders != null ? new HashMap<>(orders) : new HashMap<>();
        this.pendingReturns = pendingReturns != null ? new HashMap<>(pendingReturns) : new HashMap<>();
    }

    public Map<String, AuctionListing> listings() {
        return Collections.unmodifiableMap(listings);
    }

    public Map<String, AuctionOrder> orders() {
        return Collections.unmodifiableMap(orders);
    }

    public Map<UUID, List<ItemStack>> pendingReturns() {
        return Collections.unmodifiableMap(pendingReturns);
    }

    public static AuctionStorageSnapshot empty() {
        return new AuctionStorageSnapshot(Map.of(), Map.of(), Map.of());
    }
}
