package com.skyblockexp.ezauction.event;

import com.skyblockexp.ezauction.live.model.LiveAuctionState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player explicitly leaves a live auction they were participating in.
 */
public class LiveAuctionPlayerLeftEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LiveAuctionState state;
    private final Player player;

    public LiveAuctionPlayerLeftEvent(LiveAuctionState state, Player player) {
        this.state = state;
        this.player = player;
    }

    public LiveAuctionState getState() { return state; }

    public Player getPlayer() { return player; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
