package com.skyblockexp.ezauction;

import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listens for player logins to notify them about pending auction return items.
 */
public class AuctionReturnListener implements Listener {

    private final AuctionManager auctionManager;

    public AuctionReturnListener(AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        auctionManager.handlePlayerLogin(event.getPlayer());
    }
}
