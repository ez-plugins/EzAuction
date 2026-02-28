package com.skyblockexp.ezauction.manager;

import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.AuctionOperationResult;
import com.skyblockexp.ezauction.service.AuctionOrderService;
import com.skyblockexp.ezauction.persistence.AuctionPersistenceManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.UUID;

/**
 * Thin manager façade for order-related operations.
 */
public final class OrderManager {
    private final AuctionOrderService orderService;
    private final AuctionPersistenceManager persistenceManager;
    private final AuctionTransactionService transactionService;

    public OrderManager(AuctionOrderService orderService, AuctionPersistenceManager persistenceManager, AuctionTransactionService transactionService) {
        this.orderService = orderService;
        this.persistenceManager = persistenceManager;
        this.transactionService = transactionService;
    }

    public AuctionOperationResult createOrder(Player buyer, ItemStack template, double offeredPrice, Duration duration, double reservedAmount) {
        return orderService.createOrder(buyer, template, offeredPrice, duration, reservedAmount);
    }

    public AuctionOperationResult fulfillOrder(Player seller, String orderId) {
        return orderService.fulfillOrder(seller, orderId);
    }

    public AuctionOperationResult cancelOrder(UUID buyerId, String orderId) {
        return orderService.cancelOrder(buyerId, orderId);
    }

    public Map<String, AuctionOrder> getOrders() {
        return orderService.getOrders();
    }
}
