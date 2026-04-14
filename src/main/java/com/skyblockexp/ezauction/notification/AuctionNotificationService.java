package com.skyblockexp.ezauction.notification;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.config.AuctionBackendMessages;
import org.bukkit.entity.Player;

/**
 * Handles notifications for auction events and forwards enabled events to DiscordSRV via DiscordIntegration.
 */
public class AuctionNotificationService {

    private final AuctionBackendMessages backendMessages;
    private final com.skyblockexp.ezauction.transaction.AuctionTransactionService transactionService;
    private final AuctionBackendMessages.FallbackMessages fallbackMessages;
    private final org.bukkit.plugin.java.JavaPlugin plugin;

    public AuctionNotificationService(org.bukkit.plugin.java.JavaPlugin plugin, AuctionBackendMessages backendMessages, com.skyblockexp.ezauction.transaction.AuctionTransactionService transactionService) {
        this.plugin = plugin;
        this.backendMessages = backendMessages;
        this.transactionService = transactionService;
        this.fallbackMessages = backendMessages.fallback();
    }

    public void notifySellerSale(AuctionListing listing) {
        Player seller = org.bukkit.Bukkit.getPlayer(listing.sellerId());
        if (seller != null && seller.isOnline()) {
            java.util.List<String> parts = new java.util.ArrayList<>();
            parts.add(formatMessage(backendMessages.notifications().sellerSold(), "item", describeItem(listing.item()), "price", transactionService.formatCurrency(listing.price())));
            if (listing.deposit() > 0.0D) {
                parts.add(formatMessage(backendMessages.notifications().sellerDepositRefunded(), "amount", transactionService.formatCurrency(listing.deposit())));
            }
            seller.sendMessage(String.join(" ", parts).trim());
        }
        com.skyblockexp.ezauction.integration.DiscordIntegration di = null;
        if (com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry() != null) di = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry().discordIntegration;
        if (di != null && di.isEnabled() && di.isEventEnabled("auction_end")) {
            String template = di.getTemplate("auction_end");
            if (template == null || template.isBlank()) template = backendMessages.notifications().sellerSold();
            String message = formatMessage(template, "item", describeItem(listing.item()), "price", transactionService.formatCurrency(listing.price()), "seller", org.bukkit.Bukkit.getOfflinePlayer(listing.sellerId()).getName(), "listingId", listing.id());
            di.sendMessageIfAllowed(listing.sellerId(), stripColor(message));
        }
        com.skyblockexp.ezauction.integration.DiscordWebhookNotifier wh = null;
        if (com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry() != null) wh = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry().discordWebhookNotifier;
        if (wh != null && wh.isEnabled() && wh.isEventEnabled("auction_end")) {
            wh.sendAuctionEnd(describeItem(listing.item()), org.bukkit.Bukkit.getOfflinePlayer(listing.sellerId()).getName(), transactionService.formatCurrency(listing.price()), listing.id());
        }
    }

    public void notifySellerExpiry(AuctionListing listing) {
        Player seller = org.bukkit.Bukkit.getPlayer(listing.sellerId());
        if (seller != null && seller.isOnline()) {
            java.util.List<String> parts = new java.util.ArrayList<>();
            parts.add(formatMessage(backendMessages.notifications().sellerExpired(), "item", describeItem(listing.item())));
            if (listing.deposit() > 0) {
                parts.add(formatMessage(backendMessages.notifications().sellerDepositRefunded(), "amount", transactionService.formatCurrency(listing.deposit())));
            }
            seller.sendMessage(String.join(" ", parts).trim());
        }
        com.skyblockexp.ezauction.integration.DiscordIntegration di = null;
        if (com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry() != null) di = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry().discordIntegration;
        if (di != null && di.isEnabled() && di.isEventEnabled("auction_end")) {
            String template = di.getTemplate("auction_end");
            if (template == null || template.isBlank()) template = backendMessages.notifications().sellerExpired();
            String message = formatMessage(template, "item", describeItem(listing.item()), "seller", org.bukkit.Bukkit.getOfflinePlayer(listing.sellerId()).getName(), "listingId", listing.id());
            di.sendMessageIfAllowed(listing.sellerId(), stripColor(message));
        }
        com.skyblockexp.ezauction.integration.DiscordWebhookNotifier wh = null;
        if (com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry() != null) wh = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry().discordWebhookNotifier;
        if (wh != null && wh.isEnabled() && wh.isEventEnabled("auction_end")) {
            wh.sendAuctionExpiry(describeItem(listing.item()), org.bukkit.Bukkit.getOfflinePlayer(listing.sellerId()).getName(), listing.id());
        }
    }

    public void notifySellerCancelled(AuctionListing listing) {
        Player seller = org.bukkit.Bukkit.getPlayer(listing.sellerId());
        if (seller != null && seller.isOnline()) {
            java.util.List<String> parts = new java.util.ArrayList<>();
            parts.add(formatMessage(backendMessages.notifications().sellerReturned(), "item", describeItem(listing.item())));
            if (listing.deposit() > 0) {
                parts.add(formatMessage(backendMessages.notifications().sellerDepositRefunded(), "amount", transactionService.formatCurrency(listing.deposit())));
            }
            seller.sendMessage(String.join(" ", parts).trim());
        }
        com.skyblockexp.ezauction.integration.DiscordIntegration di = null;
        if (com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry() != null) di = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry().discordIntegration;
        if (di != null && di.isEnabled() && di.isEventEnabled("auction_cancel")) {
            String template = di.getTemplate("auction_cancel");
            if (template == null || template.isBlank()) template = backendMessages.notifications().sellerReturned();
            String message = formatMessage(template, "item", describeItem(listing.item()), "seller", org.bukkit.Bukkit.getOfflinePlayer(listing.sellerId()).getName(), "listingId", listing.id());
            di.sendMessageIfAllowed(listing.sellerId(), stripColor(message));
        }
        com.skyblockexp.ezauction.integration.DiscordWebhookNotifier wh = null;
        if (com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry() != null) wh = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry().discordWebhookNotifier;
        if (wh != null && wh.isEnabled() && wh.isEventEnabled("auction_cancel")) {
            wh.sendAuctionCancel(describeItem(listing.item()), org.bukkit.Bukkit.getOfflinePlayer(listing.sellerId()).getName(), listing.id());
        }
    }

    public void notifyOrderCreated(AuctionOrder order, Player buyer) {
        if (buyer == null || !buyer.isOnline()) return;
        buyer.sendMessage(formatMessage(backendMessages.notifications().buyerFulfilled(), "item", describeItem(order.requestedItem()), "price", transactionService.formatCurrency(order.offeredPrice())));
    }

    public void notifyOrderFulfilled(AuctionOrder order, Player seller) {
        if (seller == null || !seller.isOnline()) return;
        seller.sendMessage(formatMessage(backendMessages.notifications().sellerSold(), "item", describeItem(order.requestedItem()), "price", transactionService.formatCurrency(order.offeredPrice())));
        com.skyblockexp.ezauction.integration.DiscordIntegration di = null;
        if (com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry() != null) di = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry().discordIntegration;
        if (di != null && di.isEnabled() && di.isEventEnabled("auction_end")) {
            String template = di.getTemplate("auction_end");
            if (template == null || template.isBlank()) template = backendMessages.notifications().sellerSold();
            String message = formatMessage(template, "item", describeItem(order.requestedItem()), "price", transactionService.formatCurrency(order.offeredPrice()), "seller", (seller != null ? seller.getName() : ""), "buyer", org.bukkit.Bukkit.getOfflinePlayer(order.buyerId()).getName());
            di.sendMessageIfAllowed(seller.getUniqueId(), stripColor(message));
        }
        com.skyblockexp.ezauction.integration.DiscordWebhookNotifier wh = null;
        if (com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry() != null) wh = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry().discordWebhookNotifier;
        if (wh != null && wh.isEnabled() && wh.isEventEnabled("auction_end")) {
            wh.sendAuctionEnd(describeItem(order.requestedItem()), seller.getName(), transactionService.formatCurrency(order.offeredPrice()), "order");
        }
    }

    public void notifyOrderCancelled(AuctionOrder order) {
        Player buyer = org.bukkit.Bukkit.getPlayer(order.buyerId());
        if (buyer == null || !buyer.isOnline()) return;
        buyer.sendMessage(formatMessage(backendMessages.notifications().buyerItemsStored(), "item", describeItem(order.requestedItem()), "amount", transactionService.formatCurrency(order.reservedAmount())));
    }

    public void notifyOrderExpiry(AuctionOrder order) {
        if (order == null) return;
        Player buyer = org.bukkit.Bukkit.getPlayer(order.buyerId());
        if (buyer == null || !buyer.isOnline()) return;
        buyer.sendMessage(formatMessage(backendMessages.notifications().buyerExpired(), "item", describeItem(order.requestedItem())));
    }

    public void notifyAuctionStarted(AuctionListing listing, Player seller, java.time.Duration duration) {
        if (listing == null) return;
        if (seller != null && seller.isOnline()) {
            String inGame = formatMessage("&aYour listing is now live: {item} for {price} (id:{listingId})", "item", describeItem(listing.item()), "price", transactionService.formatCurrency(listing.price()), "listingId", listing.id(), "quantity", String.valueOf(listing.item() != null ? listing.item().getAmount() : 0), "duration", duration != null ? com.skyblockexp.ezauction.util.DateUtil.formatDuration(duration) : "N/A");
            seller.sendMessage(inGame);
        }
        com.skyblockexp.ezauction.integration.DiscordIntegration di = null;
        if (com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry() != null) di = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry().discordIntegration;
        if (di != null && di.isEnabled() && di.isEventEnabled("auction_start")) {
            String template = di.getTemplate("auction_start");
            if (template == null || template.isBlank()) template = "Auction started: {item} listed by {seller} for {price} (id:{listingId})";
            String message = formatMessage(template, "item", describeItem(listing.item()), "price", transactionService.formatCurrency(listing.price()), "seller", seller != null ? seller.getName() : org.bukkit.Bukkit.getOfflinePlayer(listing.sellerId()).getName(), "listingId", listing.id(), "quantity", String.valueOf(listing.item() != null ? listing.item().getAmount() : 0), "duration", duration != null ? com.skyblockexp.ezauction.util.DateUtil.formatDuration(duration) : "N/A");
            java.util.UUID uid = (seller != null) ? seller.getUniqueId() : listing.sellerId();
            di.sendMessageIfAllowed(uid, stripColor(message));
        }
        com.skyblockexp.ezauction.integration.DiscordWebhookNotifier wh = null;
        if (com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry() != null) wh = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry().discordWebhookNotifier;
        if (wh != null && wh.isEnabled() && wh.isEventEnabled("auction_start")) {
            String sellerName = seller != null ? seller.getName() : org.bukkit.Bukkit.getOfflinePlayer(listing.sellerId()).getName();
            String quantityStr = String.valueOf(listing.item() != null ? listing.item().getAmount() : 1);
            String durationStr = duration != null ? com.skyblockexp.ezauction.util.DateUtil.formatDuration(duration) : "N/A";
            wh.sendAuctionStart(describeItem(listing.item()), quantityStr, sellerName, transactionService.formatCurrency(listing.price()), durationStr, listing.id());
        }
    }

    /**
     * Notifies about a new bid on an auction listing. This does not modify listing state.
     */
    public void notifyAuctionBid(AuctionListing listing, Player bidder, double amount) {
        if (listing == null || bidder == null) return;
        // Notify bidder in-game
        if (bidder.isOnline()) {
            bidder.sendMessage(formatMessage("&eYour bid of {amount} on {item} was received (id:{listingId})",
                    "amount", transactionService.formatCurrency(amount),
                    "item", describeItem(listing.item()),
                    "listingId", listing.id()));
        }

        com.skyblockexp.ezauction.integration.DiscordIntegration di = null;
        if (com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry() != null) di = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry().discordIntegration;
        if (di != null && di.isEnabled() && di.isEventEnabled("auction_bid")) {
            String template = di.getTemplate("auction_bid");
            if (template == null || template.isBlank()) template = "New bid on {item}: {bidder} bid {amount} (id:{listingId})";
            String message = formatMessage(template,
                "item", describeItem(listing.item()),
                "bidder", bidder.getName(),
                "amount", transactionService.formatCurrency(amount),
                "listingId", listing.id());
            di.sendMessageIfAllowed(bidder.getUniqueId(), stripColor(message));
        }
        com.skyblockexp.ezauction.integration.DiscordWebhookNotifier wh = null;
        if (com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry() != null) wh = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry().discordWebhookNotifier;
        if (wh != null && wh.isEnabled() && wh.isEventEnabled("auction_bid")) {
            wh.sendAuctionBid(describeItem(listing.item()), bidder.getName(), transactionService.formatCurrency(amount), listing.id());
        }
    }

    private String stripColor(String message) {
        if (message == null) return "";
        return org.bukkit.ChatColor.stripColor(message).replace('§', '&');
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

    private String friendlyMaterialName(org.bukkit.Material material) {
        if (material == null) return fallbackMessages.unknownMaterial();
        String lowercase = material.name().toLowerCase(java.util.Locale.ENGLISH).replace('_', ' ');
        if (lowercase.isEmpty()) return fallbackMessages.unknownMaterial();
        return Character.toUpperCase(lowercase.charAt(0)) + lowercase.substring(1);
    }
}
