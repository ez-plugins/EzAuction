package com.skyblockexp.ezauction.service;

import com.skyblockexp.ezauction.*;
import com.skyblockexp.ezauction.integration.TeamsIntegration;
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
    private final TeamsIntegration teamsIntegration;

    public AuctionQueryService(Map<String, AuctionListing> listings,
                              Map<String, AuctionOrder> orders,
                              LiveAuctionService liveAuctionService,
                              AuctionConfiguration configuration) {
        this(listings, orders, liveAuctionService, configuration, null);
    }

    public AuctionQueryService(Map<String, AuctionListing> listings,
                              Map<String, AuctionOrder> orders,
                              LiveAuctionService liveAuctionService,
                              AuctionConfiguration configuration,
                              TeamsIntegration teamsIntegration) {
        this.listings = listings;
        this.orders = orders;
        this.liveAuctionService = liveAuctionService;
        this.configuration = configuration;
        this.teamsIntegration = teamsIntegration;
    }

    public List<AuctionListing> listActiveListings() {
        List<AuctionListing> active = new ArrayList<>(listings.values());
        long now = System.currentTimeMillis();
        active.removeIf(l -> l.expiryEpochMillis() <= now);
        // Team-scoped listings are hidden from the global browse view
        active.removeIf(AuctionListing::isTeamListing);
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

    /**
     * Returns active listings scoped to the team of the given viewer, sorted by expiry.
     * Returns an empty list if the viewer is not in a team or TeamsAPI is unavailable.
     *
     * @param viewerId the UUID of the player viewing team listings
     * @return immutable list of team-scoped active listings visible to this player
     */
    public List<AuctionListing> listActiveTeamListings(UUID viewerId) {
        if (viewerId == null
                || configuration == null || !configuration.teamAuctionsEnabled()
                || teamsIntegration == null || !teamsIntegration.isAvailable()) {
            return Collections.emptyList();
        }
        Optional<UUID> teamId = teamsIntegration.getTeamId(viewerId);
        if (teamId.isEmpty()) {
            return Collections.emptyList();
        }
        UUID tid = teamId.get();
        long now = System.currentTimeMillis();
        List<AuctionListing> result = new ArrayList<>();
        for (AuctionListing l : listings.values()) {
            if (l.expiryEpochMillis() > now && l.isTeamListing() && tid.equals(l.teamId())) {
                result.add(l);
            }
        }
        result.sort(Comparator.comparingLong(AuctionListing::expiryEpochMillis));
        return Collections.unmodifiableList(result);
    }

    /**
     * Finds a listing by id regardless of team scope. Used by the GUI confirm flow so
     * team listings can still be acted upon after the global-filter.
     *
     * @param listingId the listing id
     * @return the {@link AuctionListing}, or {@code null} if not found
     */
    public AuctionListing findListingById(String listingId) {
        if (listingId == null || listingId.isEmpty()) return null;
        return listings.get(listingId);
    }
}
