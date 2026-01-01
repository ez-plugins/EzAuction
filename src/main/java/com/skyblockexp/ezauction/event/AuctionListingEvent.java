package com.skyblockexp.ezauction.event;

import com.skyblockexp.ezauction.AuctionListing;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Base event for auction listing events, exposing the AuctionListing object.
 */
public abstract class AuctionListingEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final AuctionListing listing;

    public AuctionListingEvent(AuctionListing listing) {
        this.listing = listing;
    }

    public AuctionListing getListing() {
        return listing;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
