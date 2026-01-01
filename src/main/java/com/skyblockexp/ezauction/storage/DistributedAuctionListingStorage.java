package com.skyblockexp.ezauction.storage;

import com.skyblockexp.ezauction.AuctionListing;

/**
 * Provides fine-grained persistence hooks for auction listings when multiple
 * servers access the same backing store concurrently.
 */
public interface DistributedAuctionListingStorage {

    /**
     * Persist a single listing so that it becomes visible to other servers.
     *
     * @param listing the listing to store
     */
    void insertListing(AuctionListing listing);

    /**
     * Attempt to remove the listing from persistent storage as part of an
     * exclusive claim, returning {@code true} only if the listing was still
     * present.
     *
     * @param listingId the listing identifier
     * @return {@code true} if the listing was removed, {@code false} otherwise
     */
    boolean tryClaimListing(String listingId);

    /**
     * Remove a listing from persistent storage without requiring knowledge of
     * whether it was already claimed elsewhere.
     *
     * @param listingId the listing identifier
     */
    void deleteListing(String listingId);
}
