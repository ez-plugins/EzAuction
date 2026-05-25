package com.skyblockexp.ezauction.event;

import com.skyblockexp.ezauction.live.model.LiveAuctionState;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player places a bid during a live auction.
 * Can be cancelled by other plugins to prevent the bid from being accepted.
 */
public class LiveAuctionBidPlacedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LiveAuctionState state;
    private final Player bidder;
    private final double amount;
    private boolean cancelled;

    public LiveAuctionBidPlacedEvent(LiveAuctionState state, Player bidder, double amount) {
        this.state = state;
        this.bidder = bidder;
        this.amount = amount;
    }

    public LiveAuctionState getState() { return state; }

    public Player getBidder() { return bidder; }

    public double getAmount() { return amount; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
