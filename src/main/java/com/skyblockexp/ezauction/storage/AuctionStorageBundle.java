package com.skyblockexp.ezauction.storage;

/**
 * Convenience holder for the storage implementations used by the auction plugin.
 */
public record AuctionStorageBundle(AuctionStorage listingStorage, AuctionHistoryStorage historyStorage) {

    public AuctionStorageBundle {
        if (listingStorage == null) {
            throw new IllegalArgumentException("listingStorage must not be null");
        }
        if (historyStorage == null) {
            throw new IllegalArgumentException("historyStorage must not be null");
        }
    }
}
