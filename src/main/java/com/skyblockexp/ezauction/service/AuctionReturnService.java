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
    private final com.skyblockexp.ezauction.config.AuctionBackendMessages backendMessages;
    private final String CLAIM_COMMAND = "/auction claim";

    /**
     * Refactored constructor: inject shared pendingReturns and backendMessages.
     */
    public AuctionReturnService(Map<UUID, List<ItemStack>> pendingReturns,
                               AuctionPersistenceManager persistenceManager,
                               com.skyblockexp.ezauction.config.AuctionBackendMessages backendMessages) {
        this.pendingReturns = pendingReturns;
        this.persistenceManager = persistenceManager;
        this.backendMessages = backendMessages;
    }

    /**
     * Counts the number of pending return items for a player.
     */
    public int countPendingReturnItems(UUID playerId) {
        if (playerId == null) return 0;
        List<ItemStack> stored = pendingReturns.get(playerId);
        return countItemAmount(stored);
    }

    /**
     * Handles player login and notifies them if they have pending return items.
     */
    public void handlePlayerLogin(Player player) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();
        List<ItemStack> stored = pendingReturns.get(playerId);
        if (stored == null || stored.isEmpty()) return;
        int totalItems = countItemAmount(stored);
        if (totalItems <= 0) {
            pendingReturns.remove(playerId);
            persistenceManager.saveReturns(pendingReturns);
            return;
        }
        player.sendMessage(formatMessage(backendMessages.claim().reminder(),
                "total", String.valueOf(totalItems),
                "returned-suffix", pluralSuffix(totalItems),
                "command", CLAIM_COMMAND));
    }

    /**
     * Attempts to deliver all pending return items to the player's inventory.
     * Updates the pending returns and notifies the player of the result.
     */
    public AuctionOperationResult claimReturnItems(Player player) {
        if (player == null) return AuctionOperationResult.failure(formatMessage(backendMessages.claim().playersOnly()));
        UUID playerId = player.getUniqueId();
        List<ItemStack> stored = pendingReturns.get(playerId);
        if (stored == null || stored.isEmpty()) return AuctionOperationResult.failure(formatMessage(backendMessages.claim().noneAvailable()));
        List<ItemStack> remaining = new ArrayList<>();
        int claimedAmount = 0;
        int availableAmount = 0;
        for (ItemStack storedItem : stored) {
            if (storedItem == null || storedItem.getType() == Material.AIR || storedItem.getAmount() <= 0) continue;
            ItemStack toGive = storedItem.clone();
            availableAmount += toGive.getAmount();
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);
            if (leftover.isEmpty()) {
                claimedAmount += toGive.getAmount();
                continue;
            }
            int leftoverAmount = 0;
            for (ItemStack remainder : leftover.values()) {
                if (remainder == null || remainder.getType() == Material.AIR || remainder.getAmount() <= 0) continue;
                leftoverAmount += remainder.getAmount();
                remaining.add(remainder.clone());
            }
            claimedAmount += Math.max(0, toGive.getAmount() - leftoverAmount);
        }
        if (availableAmount <= 0) {
            pendingReturns.remove(playerId);
            persistenceManager.saveReturns(pendingReturns);
            return AuctionOperationResult.failure(formatMessage(backendMessages.claim().noneAvailable()));
        }
        if (remaining.isEmpty()) {
            pendingReturns.remove(playerId);
        } else {
            pendingReturns.put(playerId, remaining);
        }
        persistenceManager.saveReturns(pendingReturns);
        if (claimedAmount <= 0) return AuctionOperationResult.failure(formatMessage(backendMessages.claim().inventoryFull()));
        int remainingAmount = countItemAmount(remaining);
        if (remainingAmount > 0) {
            String message = formatMessage(backendMessages.claim().partial(),
                    "claimed", String.valueOf(claimedAmount),
                    "claimed-suffix", pluralSuffix(claimedAmount),
                    "remaining", String.valueOf(remainingAmount),
                    "remaining-suffix", pluralSuffix(remainingAmount));
            return AuctionOperationResult.success(message);
        }
        String message = formatMessage(backendMessages.claim().complete(),
                "claimed", String.valueOf(claimedAmount),
                "claimed-suffix", pluralSuffix(claimedAmount));
        return AuctionOperationResult.success(message);
    }

    private int countItemAmount(List<ItemStack> items) {
        if (items == null || items.isEmpty()) return 0;
        int total = 0;
        for (ItemStack stack : items) {
            if (stack == null || stack.getType() == Material.AIR) continue;
            total += Math.max(0, stack.getAmount());
        }
        return total;
    }

    private String formatMessage(String template, String... replacements) {
        if (template == null || template.isEmpty()) return "";
        String formatted = template;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String key = replacements[i];
            String value = replacements[i + 1];
            formatted = formatted.replace("{" + key + "}", value != null ? value : "");
        }
        return colorize(formatted);
    }

    private String colorize(String message) {
        if (message == null || message.isEmpty()) return "";
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }

    private String pluralSuffix(int amount) {
        return amount == 1 ? "" : "s";
    }
}
