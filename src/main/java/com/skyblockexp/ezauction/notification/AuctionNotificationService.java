
package com.skyblockexp.ezauction.notification;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.config.AuctionBackendMessages;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import org.bukkit.entity.Player;

/**
 * Handles all notification logic for auction events, including seller and buyer updates
 * for sales, expiries, cancellations, and order events. Uses backend message templates
 * and transaction service for formatting and currency display.
 *
 * <p>This class is thread-safe for use in Bukkit event handlers.</p>
 *
 * @author Shadow48402
 * @since 1.1.0
 */
public class AuctionNotificationService {

    // === Fields ===
    private final AuctionBackendMessages backendMessages;
    private final com.skyblockexp.ezauction.transaction.AuctionTransactionService transactionService;
    private final AuctionBackendMessages.FallbackMessages fallbackMessages;

    // === Constructor ===
    /**
     * Constructs a new AuctionNotificationService.
     *
     * @param backendMessages The backend message templates for notifications
     * @param transactionService The transaction service for currency formatting
     */
    public AuctionNotificationService(AuctionBackendMessages backendMessages, com.skyblockexp.ezauction.transaction.AuctionTransactionService transactionService) {
        this.backendMessages = backendMessages;
        this.transactionService = transactionService;
        this.fallbackMessages = backendMessages.fallback();
    }

    // === Public Notification Methods ===

    /**
     * Notifies the seller that their listing was sold.
     *
     * @param listing The sold auction listing
     */
    public void notifySellerSale(AuctionListing listing) {
        Player seller = org.bukkit.Bukkit.getPlayer(listing.sellerId());
        if (seller == null || !seller.isOnline()) return;
        java.util.List<String> parts = new java.util.ArrayList<>();
        parts.add(formatMessage(backendMessages.notifications().sellerSold(),
                "item", describeItem(listing.item()),
                "price", transactionService.formatCurrency(listing.price())));
        if (listing.deposit() > 0.0D) {
            parts.add(formatMessage(backendMessages.notifications().sellerDepositRefunded(),
                    "amount", transactionService.formatCurrency(listing.deposit())));
        }
        seller.sendMessage(String.join(" ", parts).trim());
    }

    /**
     * Notifies the seller that their listing expired.
     *
     * @param listing The expired auction listing
     */
    public void notifySellerExpiry(AuctionListing listing) {
        Player seller = org.bukkit.Bukkit.getPlayer(listing.sellerId());
        if (seller == null || !seller.isOnline()) return;
        java.util.List<String> parts = new java.util.ArrayList<>();
        parts.add(formatMessage(backendMessages.notifications().sellerExpired(),
                "item", describeItem(listing.item())));
        if (listing.deposit() > 0) {
            parts.add(formatMessage(backendMessages.notifications().sellerDepositRefunded(),
                    "amount", transactionService.formatCurrency(listing.deposit())));
        }
        seller.sendMessage(String.join(" ", parts).trim());
    }

    /**
     * Notifies the seller that their listing was cancelled.
     *
     * @param listing The cancelled auction listing
     */
    public void notifySellerCancelled(AuctionListing listing) {
        Player seller = org.bukkit.Bukkit.getPlayer(listing.sellerId());
        if (seller == null || !seller.isOnline()) return;
        java.util.List<String> parts = new java.util.ArrayList<>();
        parts.add(formatMessage(backendMessages.notifications().sellerReturned(),
            "item", describeItem(listing.item())));
        if (listing.deposit() > 0) {
            parts.add(formatMessage(backendMessages.notifications().sellerDepositRefunded(),
                    "amount", transactionService.formatCurrency(listing.deposit())));
        }
        seller.sendMessage(String.join(" ", parts).trim());
    }

    /**
     * Notifies the buyer that their order was created.
     *
     * @param order The created auction order
     * @param buyer The player who created the order
     */
    public void notifyOrderCreated(AuctionOrder order, Player buyer) {
        if (buyer == null || !buyer.isOnline()) return;
        buyer.sendMessage(formatMessage(backendMessages.notifications().buyerFulfilled(),
            "item", describeItem(order.requestedItem()),
            "price", transactionService.formatCurrency(order.offeredPrice())));
    }

    /**
     * Notifies the seller that their order was fulfilled.
     *
     * @param order The fulfilled auction order
     * @param seller The player who fulfilled the order
     */
    public void notifyOrderFulfilled(AuctionOrder order, Player seller) {
        if (seller == null || !seller.isOnline()) return;
        seller.sendMessage(formatMessage(backendMessages.notifications().sellerSold(),
            "item", describeItem(order.requestedItem()),
            "price", transactionService.formatCurrency(order.offeredPrice())));
    }

    /**
     * Notifies the buyer that their order was cancelled.
     *
     * @param order The cancelled auction order
     */
    public void notifyOrderCancelled(AuctionOrder order) {
        Player buyer = org.bukkit.Bukkit.getPlayer(order.buyerId());
        if (buyer == null || !buyer.isOnline()) return;
        buyer.sendMessage(formatMessage(backendMessages.notifications().buyerItemsStored(),
            "item", describeItem(order.requestedItem()),
            "amount", transactionService.formatCurrency(order.reservedAmount())));
    }

    /**
     * Notifies the buyer that their order expired.
     *
     * @param order The expired auction order
     */
    public void notifyOrderExpiry(AuctionOrder order) {
        if (order == null) return;
        Player buyer = org.bukkit.Bukkit.getPlayer(order.buyerId());
        if (buyer == null || !buyer.isOnline()) return;
        buyer.sendMessage(formatMessage(backendMessages.notifications().buyerExpired(),
                "item", describeItem(order.requestedItem())));
    }

    // === Private Helpers ===

    /**
     * Formats a backend message template with replacements and applies color codes.
     *
     * @param template The message template
     * @param replacements Key-value pairs for replacement
     * @return The formatted and colorized message
     */
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

    /**
     * Applies Bukkit color codes to a message string.
     *
     * @param message The message to colorize
     * @return The colorized message
     */
    private String colorize(String message) {
        if (message == null || message.isEmpty()) return "";
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Returns a human-readable description of an item, including amount and name.
     *
     * @param item The item to describe
     * @return A string description of the item
     */
    private String describeItem(org.bukkit.inventory.ItemStack item) {
        if (item == null) return fallbackMessages.unknownItem();
        int amount = Math.max(1, item.getAmount());
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        String name;
        if (meta != null && meta.hasDisplayName()) {
            String displayName = meta.getDisplayName();
            String stripped = displayName != null ? org.bukkit.ChatColor.stripColor(displayName) : "";
            name = (stripped != null && !stripped.isEmpty()) ? stripped : displayName;
        } else {
            name = friendlyMaterialName(item.getType());
        }
        if (name == null || name.isEmpty()) name = fallbackMessages.unknownItem();
        return amount + "x " + name;
    }

    /**
     * Converts a Bukkit Material to a friendly display name.
     *
     * @param material The material to convert
     * @return A user-friendly material name
     */
    private String friendlyMaterialName(org.bukkit.Material material) {
        if (material == null) return fallbackMessages.unknownMaterial();
        String lowercase = material.name().toLowerCase(java.util.Locale.ENGLISH).replace('_', ' ');
        if (lowercase.isEmpty()) return fallbackMessages.unknownMaterial();
        return Character.toUpperCase(lowercase.charAt(0)) + lowercase.substring(1);
    }
}
