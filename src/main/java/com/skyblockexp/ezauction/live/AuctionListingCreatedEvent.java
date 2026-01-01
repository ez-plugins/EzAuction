package com.skyblockexp.ezauction.live;

import com.skyblockexp.ezauction.AuctionListing;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AuctionListingCreatedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player seller;
    private final AuctionListing listing;

    public AuctionListingCreatedEvent(Player seller, AuctionListing listing) {
        super(true); // async=false if you fire on main thread; set true only if off-thread
        this.seller = seller;
        this.listing = listing;
    }

    public Player getSeller() { return seller; }
    public AuctionListing getListing() { return listing; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
