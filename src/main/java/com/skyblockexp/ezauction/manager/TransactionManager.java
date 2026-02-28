package com.skyblockexp.ezauction.manager;

import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.AuctionOperationResult;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Thin transaction façade for economy operations.
 */
public final class TransactionManager {
    private final AuctionTransactionService txService;

    public TransactionManager(AuctionTransactionService txService) {
        this.txService = txService;
    }

    public AuctionOperationResult chargeListingDeposit(Player seller, double deposit) {
        return txService.chargeListingDeposit(seller, deposit);
    }

    public AuctionOperationResult withdrawBuyer(Player buyer, double amount) {
        return txService.withdrawBuyer(buyer, amount);
    }

    public AuctionOperationResult creditSeller(UUID sellerId, double amount) {
        return txService.creditSeller(sellerId, amount);
    }

    public AuctionOperationResult reserveOrderFunds(Player buyer, double amount) {
        return txService.reserveOrderFunds(buyer, amount);
    }

    public void refundOrderBuyer(UUID buyerId, double amount) {
        txService.refundOrderBuyer(buyerId, amount);
    }

    public AuctionOperationResult payOrderSeller(UUID sellerId, double amount) {
        return txService.payOrderSeller(sellerId, amount);
    }
}
