package com.skyblockexp.ezauction.storage;

}
/**
 * Convenience holder for the storage implementations used by the auction plugin.
 */
public record AuctionStorageBundle(AuctionListingRepository listingRepository, AuctionHistoryRepository historyRepository) {

    public AuctionStorageBundle {
        Objects.requireNonNull(listingRepository, "listingRepository");
        Objects.requireNonNull(historyRepository, "historyRepository");
    }


}
}
