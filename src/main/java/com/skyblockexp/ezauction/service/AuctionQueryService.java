package com.skyblockexp.ezauction.service;

import com.skyblockexp.ezauction.*;
import com.skyblockexp.ezauction.live.LiveAuctionEntry;
import com.skyblockexp.ezauction.live.LiveAuctionService;
import com.skyblockexp.ezauction.config.AuctionConfiguration;

import java.util.*;

/**
 * Handles queries and statistics for listings and orders (count, find, etc.).
 */
public class AuctionQueryService {
    private final Map<String, AuctionListing> listings;
    private final Map<String, AuctionOrder> orders;
    private final LiveAuctionService liveAuctionService;
    private final AuctionConfiguration configuration;

    public AuctionQueryService(Map<String, AuctionListing> listings,
                              Map<String, AuctionOrder> orders,
                              LiveAuctionService liveAuctionService,
                              AuctionConfiguration configuration) {
        this.listings = listings;
        this.orders = orders;
        this.liveAuctionService = liveAuctionService;
        this.configuration = configuration;
    }

    public List<AuctionListing> listActiveListings() {
        List<AuctionListing> active = new ArrayList<>(listings.values());
        long now = System.currentTimeMillis();
        active.removeIf(l -> l.expiryEpochMillis() <= now);
        active.sort(Comparator.comparingLong(AuctionListing::expiryEpochMillis));
        if (configuration != null && configuration.debug()) {
            System.out.println("[EzAuction][DEBUG] listActiveListings: " + active.size() + " listings: " + active);
        }
        return Collections.unmodifiableList(active);
    }

    public List<AuctionOrder> listActiveOrders() {
        List<AuctionOrder> active = new ArrayList<>(orders.values());
        long now = System.currentTimeMillis();
        active.removeIf(o -> o.expiryEpochMillis() <= now);
        active.sort(Comparator.comparingLong(AuctionOrder::expiryEpochMillis));
        return Collections.unmodifiableList(active);
    }

    public long countActiveListings(UUID sellerId) {
        if (sellerId == null) return 0L;
        long now = System.currentTimeMillis();
        return listings.values().stream()
                .filter(listing -> sellerId.equals(listing.sellerId()))
                .filter(listing -> listing.expiryEpochMillis() > now)
                .count();
    }

    public long countActiveOrders(UUID buyerId) {
        if (buyerId == null) return 0L;
        long now = System.currentTimeMillis();
        return orders.values().stream()
                .filter(order -> buyerId.equals(order.buyerId()))
                .filter(order -> order.expiryEpochMillis() > now)
                .count();
    }

    public long countAllActiveListings() {
        long now = System.currentTimeMillis();
        return listings.values().stream()
                .filter(listing -> listing.expiryEpochMillis() > now)
                .count();
    }

    public long countAllActiveOrders() {
        long now = System.currentTimeMillis();
        return orders.values().stream()
                .filter(order -> order.expiryEpochMillis() > now)
                .count();
    }

    public AuctionListing findHighestPricedListing() {
        long now = System.currentTimeMillis();
        return listings.values().stream()
                .filter(l -> l.expiryEpochMillis() > now)
                .max(Comparator.comparingDouble(AuctionListing::price))
                .orElse(null);
    }

    public AuctionOrder findHighestPricedOrder() {
        long now = System.currentTimeMillis();
        return orders.values().stream()
                .filter(o -> o.expiryEpochMillis() > now)
                .max(Comparator.comparingDouble(AuctionOrder::offeredPrice))
                .orElse(null);
    }

    public AuctionListing findNextExpiringListing() {
        long now = System.currentTimeMillis();
        return listings.values().stream()
                .filter(l -> l.expiryEpochMillis() > now)
                .min(Comparator.comparingLong(AuctionListing::expiryEpochMillis))
                .orElse(null);
    }

    public boolean liveAuctionsEnabled() {
        return configuration != null && configuration.liveAuctionConfiguration() != null && configuration.liveAuctionConfiguration().enabled();
    }

    public List<LiveAuctionEntry> listQueuedLiveAuctions() {
        if (liveAuctionService == null) return Collections.emptyList();
        return liveAuctionService.snapshotQueue();
    }

    public long countActiveListings() {
        long now = System.currentTimeMillis();
        return listings.values().stream()
                .filter(listing -> listing.expiryEpochMillis() > now)
                .count();
    }

    public long countActiveOrders() {
        long now = System.currentTimeMillis();
        return orders.values().stream()
                .filter(order -> order.expiryEpochMillis() > now)
                .count();
    }
}
