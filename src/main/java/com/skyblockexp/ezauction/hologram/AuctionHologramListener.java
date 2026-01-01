
package com.skyblockexp.ezauction.hologram;

import com.skyblockexp.ezauction.config.AuctionHologramConfiguration;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Listener for interactive EzAuction hologram events.
 */
public class AuctionHologramListener implements Listener {
    private final AuctionHologramManager hologramManager;

    public AuctionHologramListener(AuctionHologramManager hologramManager) {
        this.hologramManager = hologramManager;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (!hologramManager.isHologramEntity(clicked)) {
            return;
        }
        Player player = event.getPlayer();
        AuctionHologramType type = hologramManager.readType(clicked);
        if (type == null) {
            return;
        }
        AuctionHologramConfiguration config = hologramManager.getConfiguration();
        // Permission check
        if (config.requirePermission() && !player.hasPermission(config.viewPermission())) {
            player.sendMessage("You do not have permission to view this auction hologram.");
            event.setCancelled(true);
            return;
        }
        // Proximity check
        if (config.proximityLimit()) {
            double maxDist = config.proximityDistance();
            if (player.getLocation().distanceSquared(clicked.getLocation()) > maxDist * maxDist) {
                player.sendMessage("You are too far away to interact with this auction hologram.");
                event.setCancelled(true);
                return;
            }
        }
        // TODO: Add interactive actions (e.g., open auction menu, place bid)
        player.sendMessage("You interacted with an auction hologram: " + type.displayName());
        event.setCancelled(true);
    }
}
