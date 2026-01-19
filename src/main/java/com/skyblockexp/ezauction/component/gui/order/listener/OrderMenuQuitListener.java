package com.skyblockexp.ezauction.component.gui.order.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.UUID;
import java.util.Map;

public class OrderMenuQuitListener implements Listener {
    private final Map<UUID, ?> pendingPriceInputs;
    private final Map<UUID, ?> pendingQuantityInputs;

    public OrderMenuQuitListener(Map<UUID, ?> pendingPriceInputs, Map<UUID, ?> pendingQuantityInputs) {
        this.pendingPriceInputs = pendingPriceInputs;
        this.pendingQuantityInputs = pendingQuantityInputs;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        pendingPriceInputs.remove(playerId);
        pendingQuantityInputs.remove(playerId);
    }
}
