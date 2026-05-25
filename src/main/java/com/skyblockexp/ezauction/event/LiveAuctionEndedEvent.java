package com.skyblockexp.ezauction.event;

import com.skyblockexp.ezauction.live.model.LiveAuctionState;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired when a live auction ends, whether with a winner or with no bids.
 */
public class LiveAuctionEndedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LiveAuctionState state;
    /** The UUID of the winning bidder, or {@code null} if the auction ended with no bids. */
    private final UUID winner;

    public LiveAuctionEndedEvent(LiveAuctionState state, UUID winner) {
        this.state = state;
        this.winner = winner;
    }

    public LiveAuctionState getState() { return state; }

    public UUID getWinner() { return winner; }

    public boolean hasBids() { return winner != null; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
