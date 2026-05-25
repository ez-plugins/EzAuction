package com.skyblockexp.ezauction.event;

import com.skyblockexp.ezauction.live.model.LiveAuctionState;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a live auction becomes active (promoted from the queue).
 * Can be cancelled by other plugins to prevent the auction from starting.
 */
public class LiveAuctionStartedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LiveAuctionState state;
    private boolean cancelled;

    public LiveAuctionStartedEvent(LiveAuctionState state) {
        this.state = state;
    }

    public LiveAuctionState getState() { return state; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
