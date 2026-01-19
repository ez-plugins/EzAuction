package com.skyblockexp.ezauction.component.gui.order.listener;

import com.skyblockexp.ezauction.component.gui.order.OrderMenuHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.entity.Player;
import java.util.Objects;

public class OrderMenuDragListener implements Listener {
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof OrderMenuHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!holder.owner().equals(player.getUniqueId())) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        boolean affectsTop = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (affectsTop) {
            event.setCancelled(true);
        }
    }
}
