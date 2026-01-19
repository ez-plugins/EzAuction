package com.skyblockexp.ezauction.component.gui.order.listener;

import com.skyblockexp.ezauction.component.gui.order.OrderMenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface OrderMenuClickHandler {
    void handleClick(Player player, OrderMenuHolder holder, ItemStack clicked);
}
