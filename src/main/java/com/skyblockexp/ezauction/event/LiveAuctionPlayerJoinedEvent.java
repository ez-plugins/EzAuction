package com.skyblockexp.ezauction.event;

import com.skyblockexp.ezauction.live.model.LiveAuctionState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player explicitly joins an active live auction as a participant.
 */
public class LiveAuctionPlayerJoinedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LiveAuctionState state;
    private final Player player;

    public LiveAuctionPlayerJoinedEvent(LiveAuctionState state, Player player) {
        this.state = state;
        this.player = player;
    }

    public LiveAuctionState getState() { return state; }

    public Player getPlayer() { return player; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
