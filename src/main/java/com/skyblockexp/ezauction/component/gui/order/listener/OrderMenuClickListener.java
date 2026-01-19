package com.skyblockexp.ezauction.component.gui.order.listener;

import com.skyblockexp.ezauction.component.gui.order.OrderMenuHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.Objects;

public class OrderMenuClickListener implements Listener {
    private final OrderMenuClickHandler handler;

    public OrderMenuClickListener(OrderMenuClickHandler handler) {
        this.handler = handler;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof OrderMenuHolder holder)) {
            return;
        }
        if (!Objects.equals(holder.owner(), event.getWhoClicked().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        handler.handleClick((Player) event.getWhoClicked(), holder, clicked);
    }
}
