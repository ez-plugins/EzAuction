package com.skyblockexp.ezauction.event;

import com.skyblockexp.ezauction.AuctionListing;

/**
 * Fired after an auction listing is successfully sold. Not cancellable.
 */
public class AuctionListingSoldEvent extends AuctionListingEvent {
    public AuctionListingSoldEvent(AuctionListing listing) {
        super(listing);
    }
}
