package com.skyblockexp.ezauction.storage;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOrder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/**
 * Provides persistence for auction listings, buy orders, and pending returns.
 */
public interface AuctionStorage extends AutoCloseable {

    boolean initialize();

    AuctionStorageSnapshot load();

    void saveListings(Collection<AuctionListing> listings, Collection<AuctionOrder> orders);

    void saveReturns(Map<UUID, List<ItemStack>> returnsByPlayer);

    @Override
    void close();
}
