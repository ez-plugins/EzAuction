
package com.skyblockexp.ezauction.claim;

import org.bukkit.Material;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import com.skyblockexp.ezauction.config.AuctionBackendMessages;
import com.skyblockexp.ezauction.AuctionOperationResult;

/**
 * Handles claim and return logic for auction items.
 */
public class AuctionClaimService {
        /**
         * Returns an item from a cancelled or expired listing to the seller's inventory or pending returns.
         */
        public void returnListingItem(com.skyblockexp.ezauction.AuctionListing listing, Map<UUID, List<ItemStack>> pendingReturns) {
            if (listing == null) return;
            ItemStack item = listing.item();
            if (item == null || item.getType() == org.bukkit.Material.AIR || item.getAmount() <= 0) return;
            ItemStack itemToReturn = item.clone();
            UUID sellerId = listing.sellerId();
            org.bukkit.entity.Player seller = sellerId != null ? org.bukkit.Bukkit.getPlayer(sellerId) : null;
            if (seller != null && seller.isOnline()) {
                Map<Integer, ItemStack> leftover = seller.getInventory().addItem(itemToReturn);
                if (leftover.isEmpty()) {
                    seller.sendMessage("Your auction item has been returned to your inventory.");
                    return;
                }
                for (ItemStack remainder : leftover.values()) {
                    if (remainder == null || remainder.getType() == org.bukkit.Material.AIR || remainder.getAmount() <= 0) continue;
                    storeReturnItem(sellerId, remainder, pendingReturns);
                }
                seller.sendMessage("Some items could not fit in your inventory and were stored for later claim.");
                return;
            }
            storeReturnItem(sellerId, itemToReturn, pendingReturns);
        }

        /**
         * Checks if a player has enough inventory space for an item.
         */
        public boolean hasInventorySpace(org.bukkit.entity.Player player, ItemStack item) {
            if (item == null) return false;
            int remaining = item.getAmount();
            int stackLimit = Math.min(item.getMaxStackSize(), player.getInventory().getMaxStackSize());
            for (ItemStack content : player.getInventory().getStorageContents()) {
                if (remaining <= 0) break;
                if (content == null || content.getType() == org.bukkit.Material.AIR) {
                    remaining -= stackLimit;
                    continue;
                }
                if (!content.isSimilar(item)) continue;
                int contentLimit = Math.min(stackLimit, content.getMaxStackSize());
                remaining -= Math.max(0, contentLimit - content.getAmount());
            }
            return remaining <= 0;
        }

        /**
         * Attempts to claim a listing for a buyer, removing it from the listings map and updating pending returns.
         */
        public boolean claimListing(String listingId, Map<String, com.skyblockexp.ezauction.AuctionListing> listings, Map<UUID, List<ItemStack>> pendingReturns) {
            if (listingId == null || listingId.isEmpty()) return false;
            com.skyblockexp.ezauction.AuctionListing listing = listings.remove(listingId);
            if (listing == null) return false;
            // No-op: actual item delivery is handled elsewhere
            return true;
        }

        /**
         * Stores an item for a player in their pending returns.
         */
        public void storeReturnItem(UUID playerId, ItemStack item, Map<UUID, List<ItemStack>> pendingReturns) {
            if (playerId == null || item == null || item.getType() == org.bukkit.Material.AIR || item.getAmount() <= 0) return;
            pendingReturns.compute(playerId, (uuid, existing) -> {
                List<ItemStack> updated = existing != null ? new java.util.ArrayList<>(existing) : new java.util.ArrayList<>();
                updated.add(item.clone());
                return updated;
            });
        }

        /**
         * Delivers an order item to the buyer, or stores it in pending returns if inventory is full.
         */
        public void deliverOrderItem(com.skyblockexp.ezauction.AuctionOrder order, ItemStack item, Map<UUID, List<ItemStack>> pendingReturns) {
            if (order == null || item == null || item.getType() == org.bukkit.Material.AIR || item.getAmount() <= 0) return;
            ItemStack toDeliver = item.clone();
            UUID buyerId = order.buyerId();
            org.bukkit.entity.Player buyer = buyerId != null ? org.bukkit.Bukkit.getPlayer(buyerId) : null;
            if (buyer != null && buyer.isOnline()) {
                Map<Integer, ItemStack> leftover = buyer.getInventory().addItem(toDeliver);
                int storedAmount = 0;
                if (!leftover.isEmpty()) {
                    for (ItemStack remainder : leftover.values()) {
                        if (remainder == null || remainder.getType() == org.bukkit.Material.AIR || remainder.getAmount() <= 0) continue;
                        storedAmount += remainder.getAmount();
                        storeReturnItem(buyerId, remainder.clone(), pendingReturns);
                    }
                }
                // Optionally notify buyer here
                return;
            }
            storeReturnItem(buyerId, toDeliver, pendingReturns);
        }
    private final Map<UUID, List<ItemStack>> pendingReturns;
    private final AuctionBackendMessages backendMessages;
    private final String CLAIM_COMMAND = "/auction claim";


    /**
     * Constructs a new AuctionClaimService.
     *
     * @param pendingReturns  Map of player UUIDs to their pending return items
     * @param backendMessages Backend messages for claim notifications
     */
    public AuctionClaimService(Map<UUID, List<ItemStack>> pendingReturns, AuctionBackendMessages backendMessages) {
        this.pendingReturns = pendingReturns;
        this.backendMessages = backendMessages;
    }


    /**
     * Handles player login and notifies them if they have pending return items.
     *
     * @param player The player logging in
     */
    public void handlePlayerLogin(Player player) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();
        List<ItemStack> stored = pendingReturns.get(playerId);
        if (stored == null || stored.isEmpty()) return;
        int totalItems = countItemAmount(stored);
        if (totalItems <= 0) {
            pendingReturns.remove(playerId);
            return;
        }
        player.sendMessage(formatMessage(backendMessages.claim().reminder(),
                "total", String.valueOf(totalItems),
                "returned-suffix", pluralSuffix(totalItems),
                "command", CLAIM_COMMAND));
    }


    /**
     * Counts the number of pending return items for a player.
     *
     * @param playerId The UUID of the player
     * @return The total number of pending return items
     */
    public int countPendingReturnItems(UUID playerId) {
        if (playerId == null) return 0;
        List<ItemStack> stored = pendingReturns.get(playerId);
        return countItemAmount(stored);
    }


    /**
     * Attempts to deliver all pending return items to the player's inventory.
     * Updates the pending returns and notifies the player of the result.
     *
     * @param player The player claiming their return items
     * @return AuctionOperationResult indicating success, partial, or failure
     */
    public AuctionOperationResult claimReturnItems(Player player) {
        if (player == null) return AuctionOperationResult.failure(formatMessage(backendMessages.claim().playersOnly()));
        UUID playerId = player.getUniqueId();
        List<ItemStack> stored = pendingReturns.get(playerId);
        if (stored == null || stored.isEmpty()) return AuctionOperationResult.failure(formatMessage(backendMessages.claim().noneAvailable()));
        List<ItemStack> remaining = new java.util.ArrayList<>();
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
            return AuctionOperationResult.failure(formatMessage(backendMessages.claim().noneAvailable()));
        }
        if (remaining.isEmpty()) {
            pendingReturns.remove(playerId);
        } else {
            pendingReturns.put(playerId, remaining);
        }
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
