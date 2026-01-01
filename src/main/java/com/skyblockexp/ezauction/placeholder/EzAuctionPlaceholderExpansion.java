package com.skyblockexp.ezauction.placeholder;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.EzAuctionPlugin;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.PluginDescriptionFile;

/**
 * PlaceholderAPI expansion that exposes EzAuction listing statistics.
 */
public final class EzAuctionPlaceholderExpansion extends PlaceholderExpansion {

    private final AuctionManager auctionManager;
    private final PluginDescriptionFile description;

    public EzAuctionPlaceholderExpansion(AuctionManager auctionManager, PluginDescriptionFile description) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.description = Objects.requireNonNull(description, "description");
    }

    @Override
    public String getIdentifier() {
        return EzAuctionPlugin.DISPLAY_NAME.toLowerCase(Locale.ENGLISH);
    }

    @Override
    public String getAuthor() {
        List<String> authors = description.getAuthors();
        if (authors == null || authors.isEmpty()) {
            return description.getName();
        }
        return String.join(", ", authors);
    }

    @Override
    public String getVersion() {
        return description.getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        String normalized = params.toLowerCase(Locale.ENGLISH);
        UUID playerId = offlinePlayer != null ? offlinePlayer.getUniqueId() : null;

        switch (normalized) {
            case "active_listings":
            case "listings_active":
            case "listings_count":
                return Long.toString(auctionManager.countActiveListings(playerId));
            case "active_orders":
            case "orders_active":
            case "orders_count":
                return Long.toString(auctionManager.countActiveOrders(playerId));
            case "listing_limit":
            case "listings_limit":
            case "limit":
                return formatLimit(auctionManager.resolveListingLimit(playerId));
            case "listing_space":
            case "listings_remaining":
            case "listings_slots":
                return formatRemainingSlots(playerId);
            case "returns_items":
            case "returns_count":
            case "returns_pending":
                return Integer.toString(auctionManager.countPendingReturnItems(playerId));
            case "global_active_listings":
            case "global_listings":
            case "listings_global":
                return Long.toString(auctionManager.countAllActiveListings());
            case "global_active_orders":
            case "global_orders":
            case "orders_global":
                return Long.toString(auctionManager.countAllActiveOrders());
            default:
                return "";
        }
    }

    private String formatRemainingSlots(UUID playerId) {
        int limit = auctionManager.resolveListingLimit(playerId);
        if (limit <= 0) {
            return "-1";
        }
        long activeListings = auctionManager.countActiveListings(playerId);
        long remaining = Math.max(0L, limit - activeListings);
        return Long.toString(remaining);
    }

    private String formatLimit(int limit) {
        if (limit <= 0) {
            return "-1";
        }
        return Integer.toString(limit);
    }
}
