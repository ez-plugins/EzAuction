package com.skyblockexp.ezauction.event;

import com.skyblockexp.ezauction.AuctionListing;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * Fired when a player places a bid on a listing. Can be cancelled by other plugins.
 */
public class AuctionListingBidEvent extends AuctionListingEvent implements Cancellable {
    private final Player bidder;
    private final double amount;
    private boolean cancelled;

    public AuctionListingBidEvent(AuctionListing listing, Player bidder, double amount) {
        super(listing);
        this.bidder = bidder;
        this.amount = amount;
        this.cancelled = false;
    }

    public Player getBidder() { return bidder; }
    public double getAmount() { return amount; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }
}
