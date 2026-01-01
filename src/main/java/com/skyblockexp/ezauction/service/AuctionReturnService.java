package com.skyblockexp.ezauction.service;

import com.skyblockexp.ezauction.AuctionOperationResult;
import com.skyblockexp.ezauction.persistence.AuctionPersistenceManager;
import com.skyblockexp.ezauction.util.AuctionValidationUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles logic for pending returns (claiming, counting, storing return items).
 */
public class AuctionReturnService {
    private final Map<UUID, List<ItemStack>> pendingReturns;
    private final AuctionPersistenceManager persistenceManager;

    public AuctionReturnService(AuctionPersistenceManager persistenceManager) {
        this.pendingReturns = new ConcurrentHashMap<>();
        this.persistenceManager = persistenceManager;
    }

    public int countPendingReturnItems(UUID playerId) {
        if (playerId == null) return 0;
        List<ItemStack> stored = pendingReturns.get(playerId);
        return AuctionValidationUtils.countItemAmount(stored);
    }

    public void handlePlayerLogin(Player player) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();
        List<ItemStack> stored = pendingReturns.get(playerId);
        if (stored == null || stored.isEmpty()) return;
        int totalItems = AuctionValidationUtils.countItemAmount(stored);
        if (totalItems <= 0) {
            pendingReturns.remove(playerId);
            persistenceManager.saveReturns(pendingReturns);
            return;
        }
        player.sendMessage("You have " + totalItems + " item(s) to claim from expired/cancelled auctions. Use /auction claim.");
    }

    public AuctionOperationResult claimReturnItems(Player player) {
        if (player == null) {
            return AuctionOperationResult.failure("Only players can claim return items.");
        }
        UUID playerId = player.getUniqueId();
        List<ItemStack> stored = pendingReturns.get(playerId);
        if (stored == null || stored.isEmpty()) {
            return AuctionOperationResult.failure("You have no items to claim.");
        }
        List<ItemStack> remaining = new ArrayList<>();
        int claimedAmount = 0;
        int availableAmount = 0;
        for (ItemStack storedItem : stored) {
            if (storedItem == null || storedItem.getType() == Material.AIR || storedItem.getAmount() <= 0) {
                continue;
            }
            ItemStack toGive = storedItem.clone();
            availableAmount += toGive.getAmount();
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);
            if (leftover.isEmpty()) {
                claimedAmount += toGive.getAmount();
                continue;
            }
            int leftoverAmount = 0;
            for (ItemStack remainder : leftover.values()) {
                if (remainder == null || remainder.getType() == Material.AIR || remainder.getAmount() <= 0) {
                    continue;
                }
                leftoverAmount += remainder.getAmount();
                remaining.add(remainder.clone());
            }
            claimedAmount += Math.max(0, toGive.getAmount() - leftoverAmount);
        }
        if (availableAmount <= 0) {
            pendingReturns.remove(playerId);
            persistenceManager.saveReturns(pendingReturns);
            return AuctionOperationResult.failure("You have no items to claim.");
        }
        if (remaining.isEmpty()) {
            pendingReturns.remove(playerId);
        } else {
            pendingReturns.put(playerId, remaining);
        }
        persistenceManager.saveReturns(pendingReturns);
        if (claimedAmount <= 0) {
            return AuctionOperationResult.failure("Your inventory is full.");
        }
        int remainingAmount = AuctionValidationUtils.countItemAmount(remaining);
        if (remainingAmount > 0) {
            return AuctionOperationResult.success("Claimed some items, but not all. " + claimedAmount + " claimed, " + remainingAmount + " remaining.");
        }
        return AuctionOperationResult.success("Successfully claimed all items (" + claimedAmount + ").");
    }

    // Expose pendingReturns for other services if needed
    public Map<UUID, List<ItemStack>> getPendingReturns() {
        return pendingReturns;
    }
}
