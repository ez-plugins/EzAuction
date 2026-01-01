package com.skyblockexp.ezauction.api;

import java.util.UUID;

/**
 * Resolves the maximum number of concurrent auction listings a seller may have.
 */
@FunctionalInterface
public interface AuctionListingLimitResolver {

    /**
     * Determines the maximum listings allowed for the given seller.
     *
     * @param sellerId the unique id of the seller requesting to list an item
     * @param baseLimit the configured base limit for all players
     * @return the resolved maximum listings allowed
     */
    int resolveLimit(UUID sellerId, int baseLimit);

    /**
     * Returns a resolver that always returns the provided base limit.
     */
    static AuctionListingLimitResolver useBaseLimit() {
        return (sellerId, baseLimit) -> Math.max(0, baseLimit);
    }
}
