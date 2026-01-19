package com.skyblockexp.ezauction.component.gui.order.listener;

import com.skyblockexp.ezauction.component.gui.order.OrderMenuState;
import org.bukkit.entity.Player;

public interface OrderMenuChatHandler {
    void handleChat(Player player, OrderMenuState state, String message, boolean updatingPrice);
}
