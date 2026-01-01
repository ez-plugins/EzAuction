package com.skyblockexp.ezauction.event;

import com.skyblockexp.ezauction.AuctionListing;
import org.bukkit.event.Cancellable;

/**
 * Fired before an auction listing is sold. Cancellable.
 */
public class AuctionListingSellEvent extends AuctionListingEvent implements Cancellable {
    private boolean cancelled;

    public AuctionListingSellEvent(AuctionListing listing) {
        super(listing);
        this.cancelled = false;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
