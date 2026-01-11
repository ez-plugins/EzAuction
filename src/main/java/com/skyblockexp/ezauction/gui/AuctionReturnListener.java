package com.skyblockexp.ezauction.gui;

import com.skyblockexp.ezauction.claim.AuctionClaimService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

/**
 * Listens for player join events and notifies them if they have pending auction return items.
 * Integrates with AuctionClaimService for notification logic.
 *
 * @author Shadow48402
 * @since 1.1.0
 */
public class AuctionReturnListener implements Listener {
    private final AuctionClaimService claimService;

    /**
     * Constructs a new AuctionReturnListener.
     * @param claimService the claim service to use for notifications
     */
    public AuctionReturnListener(AuctionClaimService claimService) {
        this.claimService = claimService;
    }

    /**
     * Handles player join events and notifies them of pending returns.
     * @param event the player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        claimService.handlePlayerLogin(player);
    }
}
