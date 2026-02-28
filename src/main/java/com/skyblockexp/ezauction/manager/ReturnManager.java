package com.skyblockexp.ezauction.manager;

import com.skyblockexp.ezauction.AuctionOperationResult;
import com.skyblockexp.ezauction.service.AuctionReturnService;
import com.skyblockexp.ezauction.claim.AuctionClaimService;
import com.skyblockexp.ezauction.persistence.AuctionPersistenceManager;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Manager for pending returns / claim logic.
 */
public final class ReturnManager {
    private final AuctionReturnService returnService;
    private final AuctionClaimService claimService;
    private final Map<UUID, List<ItemStack>> pendingReturns;
    private final AuctionPersistenceManager persistenceManager;

    public ReturnManager(AuctionReturnService returnService, AuctionClaimService claimService, Map<UUID, List<ItemStack>> pendingReturns, AuctionPersistenceManager persistenceManager) {
        this.returnService = returnService;
        this.claimService = claimService;
        this.pendingReturns = pendingReturns;
        this.persistenceManager = persistenceManager;
    }

    public int countPendingReturnItems(UUID playerId) {
        return returnService.countPendingReturnItems(playerId);
    }

    public void handlePlayerLogin(Player player) {
        // both services may produce notifications; ensure claimService runs first
        claimService.handlePlayerLogin(player);
        returnService.handlePlayerLogin(player);
    }

    public AuctionOperationResult claimReturnItems(Player player) {
        AuctionOperationResult result = returnService.claimReturnItems(player);
        // persist any changes performed by returnService
        try { persistenceManager.saveReturns(pendingReturns); } catch (Throwable ignored) {}
        return result;
    }

    public void storeReturnItem(UUID playerId, ItemStack item) {
        claimService.storeReturnItem(playerId, item, pendingReturns);
        try { persistenceManager.saveReturns(pendingReturns); } catch (Throwable ignored) {}
    }
}
