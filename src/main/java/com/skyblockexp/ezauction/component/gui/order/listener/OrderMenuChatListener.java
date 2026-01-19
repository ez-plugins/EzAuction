package com.skyblockexp.ezauction.component.gui.order.listener;

import com.skyblockexp.ezauction.component.gui.order.OrderMenuState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.Map;

public class OrderMenuChatListener implements Listener {
    private final Map<UUID, OrderMenuState> pendingPriceInputs;
    private final Map<UUID, OrderMenuState> pendingQuantityInputs;
    private final OrderMenuChatHandler handler;

    public OrderMenuChatListener(Map<UUID, OrderMenuState> pendingPriceInputs,
                                 Map<UUID, OrderMenuState> pendingQuantityInputs,
                                 OrderMenuChatHandler handler) {
        this.pendingPriceInputs = pendingPriceInputs;
        this.pendingQuantityInputs = pendingQuantityInputs;
        this.handler = handler;
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        OrderMenuState priceState = pendingPriceInputs.get(playerId);
        OrderMenuState quantityState = pendingQuantityInputs.get(playerId);
        if (priceState == null && quantityState == null) {
            return;
        }
        event.setCancelled(true);
        pendingPriceInputs.remove(playerId);
        pendingQuantityInputs.remove(playerId);
        OrderMenuState targetState = priceState != null ? priceState : quantityState;
        boolean updatingPrice = priceState != null;
        handler.handleChat((Player) event.getPlayer(), targetState, event.getMessage(), updatingPrice);
    }
}
