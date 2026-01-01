
package com.skyblockexp.ezauction.history;

import com.skyblockexp.ezauction.transaction.AuctionTransactionType;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistory;
import com.skyblockexp.ezauction.config.AuctionBackendMessages;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

/**
 * Handles transaction history recording for auctions and orders.
 */
public class AuctionTransactionHistoryService {
    private final AuctionTransactionHistory transactionHistory;
    private final JavaPlugin plugin;
    private final AuctionBackendMessages.FallbackMessages fallbackMessages;

    public AuctionTransactionHistoryService(AuctionTransactionHistory transactionHistory, JavaPlugin plugin, AuctionBackendMessages.FallbackMessages fallbackMessages) {
        this.transactionHistory = transactionHistory;
        this.plugin = plugin;
        this.fallbackMessages = fallbackMessages;
    }

    public void recordListingTransactionHistory(AuctionListing listing, Object buyer) {
        if (listing == null || buyer == null) return;
        long timestamp = System.currentTimeMillis();
        org.bukkit.inventory.ItemStack item = listing.item();
        UUID buyerId;
        String buyerName;
        if (buyer instanceof Player) {
            buyerId = ((Player) buyer).getUniqueId();
            buyerName = ((Player) buyer).getName();
        } else if (buyer instanceof UUID) {
            buyerId = (UUID) buyer;
            org.bukkit.OfflinePlayer offlineBuyer = plugin.getServer().getOfflinePlayer(buyerId);
            buyerName = (offlineBuyer != null && offlineBuyer.getName() != null) ? offlineBuyer.getName() : fallbackMessages.unknownName();
        } else {
            return;
        }
        org.bukkit.OfflinePlayer seller = plugin.getServer().getOfflinePlayer(listing.sellerId());
        String sellerName = seller != null && seller.getName() != null ? seller.getName() : fallbackMessages.unknownName();
        transactionHistory.recordTransaction(
                AuctionTransactionType.SELL,
                listing.sellerId(),
                buyerId,
                buyerName,
                listing.price(),
                item,
                timestamp);
        transactionHistory.recordTransaction(
                AuctionTransactionType.BUY,
                buyerId,
                listing.sellerId(),
                sellerName,
                listing.price(),
                item,
                timestamp);
    }

    public void recordOrderTransactionHistory(AuctionOrder order, java.util.UUID sellerId, String sellerName, org.bukkit.inventory.ItemStack deliveredItem) {
        if (order == null || sellerId == null) return;
        long timestamp = System.currentTimeMillis();
        org.bukkit.OfflinePlayer buyer = plugin.getServer().getOfflinePlayer(order.buyerId());
        String buyerName = buyer != null && buyer.getName() != null ? buyer.getName() : fallbackMessages.unknownName();
        String normalizedSellerName = (sellerName != null && !sellerName.isEmpty()) ? sellerName : fallbackMessages.unknownName();
        transactionHistory.recordOrderFulfillment(
                order.buyerId(),
                sellerId,
                normalizedSellerName,
                buyerName,
                order.offeredPrice(),
                deliveredItem,
                timestamp);
    }
}
