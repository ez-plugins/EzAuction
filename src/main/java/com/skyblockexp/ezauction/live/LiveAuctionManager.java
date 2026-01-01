
package com.skyblockexp.ezauction.live;

import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.*;
import com.skyblockexp.ezauction.config.AuctionBackendMessages;
import com.skyblockexp.ezauction.config.AuctionListingRules;
import com.skyblockexp.ezauction.config.LiveAuctionConfiguration;
import com.skyblockexp.ezauction.util.EconomyUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class LiveAuctionManager {

    private final JavaPlugin plugin;
    private final AuctionTransactionService transactionService;
    private final AuctionListingRules listingRules;
    private final LiveAuctionService liveAuctionService;
    private final LiveAuctionConfiguration liveConfig;
    private final AuctionBackendMessages backendMessages;

    // Optional normal manager for hybrid mode
    private final AuctionManager auctionManager; 

    public LiveAuctionManager(
            JavaPlugin plugin,
            AuctionTransactionService transactionService,
            AuctionListingRules listingRules,
            LiveAuctionService liveAuctionService,
            LiveAuctionConfiguration liveConfig,
            AuctionBackendMessages backendMessages,
            AuctionManager auctionManager // can be null if you don't want hybrid
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.transactionService = Objects.requireNonNull(transactionService, "transactionService");
        this.listingRules = Objects.requireNonNull(listingRules, "listingRules");
        this.liveAuctionService = Objects.requireNonNull(liveAuctionService, "liveAuctionService");
        this.liveConfig = liveConfig != null ? liveConfig : LiveAuctionConfiguration.defaults();
        this.backendMessages = backendMessages != null ? backendMessages : AuctionBackendMessages.defaults();
        this.auctionManager = auctionManager; // optional
    }

    /**
     * Create a listing for the LIVE auction flow.
     * - Validates price/duration and removes the item from the player
     * - Charges a deposit if configured
     * - Enqueues into LiveAuctionService (announces or queues)
     * - DOES NOT add to normal AuctionManager (unless liveConfig.storeInAuctionHouse() is true)
     */
    public AuctionOperationResult createLiveListing(Player seller, ItemStack item, double price, Duration duration) {
        var creation = backendMessages.listing().creation();

        if (seller == null) {
            return AuctionOperationResult.failure(colorize(creation.playersOnly()));
        }
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return AuctionOperationResult.failure(colorize(creation.itemRequired()));
        }

        // Duration: clamp with the same rules (or allow a separate live duration in config later)
        Duration sanitizedDuration = listingRules.clampDuration(duration);
        if (sanitizedDuration == null || sanitizedDuration.isZero() || sanitizedDuration.isNegative()) {
            return AuctionOperationResult.failure(colorize(creation.durationPositive()));
        }

        // Price & minimum price check
        double normalizedPrice = EconomyUtils.normalizeCurrency(price);
        if (normalizedPrice <= 0.0D) {
            return AuctionOperationResult.failure(colorize(creation.pricePositive()));
        }
        double minimumPrice = listingRules.minimumPrice();
        if (normalizedPrice < minimumPrice) {
            return AuctionOperationResult.failure(format(creation.priceMinimum(),
                    "minimum", transactionService.formatCurrency(minimumPrice)));
        }

        // Deposit (same semantics as normal listings)
        double normalizedDeposit = EconomyUtils.normalizeCurrency(listingRules.depositAmount(normalizedPrice));
        if (normalizedDeposit < 0) normalizedDeposit = 0.0D;

        // Remove the item from the seller first (like normal create)
        PlayerInventory inv = seller.getInventory();
        ItemStack listingItem = item.clone();
        if (!inv.containsAtLeast(listingItem, listingItem.getAmount())) {
            return AuctionOperationResult.failure(colorize(creation.inventoryMissing()));
        }

        // Charge the deposit up-front if any
        if (normalizedDeposit > 0.0D) {
            AuctionOperationResult depositResult = transactionService.chargeListingDeposit(seller, normalizedDeposit);
            if (!depositResult.success()) {
                return depositResult;
            }
        }

        var leftover = inv.removeItem(listingItem);
        if (!leftover.isEmpty()) {
            if (normalizedDeposit > 0.0D) {
                transactionService.refundListingDeposit(seller.getUniqueId(), normalizedDeposit);
            }
            return AuctionOperationResult.failure(colorize(creation.removalFailed()));
        }

        // Build a listing object (NOT stored in AuctionManager unless hybrid is enabled)
        String id = UUID.randomUUID().toString();
        long expiry = System.currentTimeMillis() + sanitizedDuration.toMillis();

        AuctionListing listing = new AuctionListing(
                id,
                seller.getUniqueId(),
                normalizedPrice,
                expiry,
                listingItem,
                normalizedDeposit
        );

        // Enqueue to LIVE (announces / queues depending on config)
        liveAuctionService.enqueue(listing, seller.getUniqueId(), seller.getName());

        // Hybrid mode (optional): also put it into the normal marketplace so it’s purchasable there
        if (auctionManager != null) {
            // Delegate to normal flow to persist and expose it in the AH
            AuctionOperationResult result = auctionManager.createListing(seller, listingItem, normalizedPrice, sanitizedDuration);
            // If delegation failed, try to roll back deposit & item
            if (!result.success()) {
                // Return item and refund deposit
                inv.addItem(listingItem.clone());
                if (normalizedDeposit > 0.0D) {
                    transactionService.refundListingDeposit(seller.getUniqueId(), normalizedDeposit);
                }
                return result;
            }
        }

        // Success message (mirrors normal)
        boolean durationClamped = duration == null || !sanitizedDuration.equals(duration);
        List<String> parts = new ArrayList<>();
        parts.add(format(creation.success(),
                "item", describeItem(listingItem),
                "price", transactionService.formatCurrency(normalizedPrice)));

        if (normalizedDeposit > 0.0D) {
            double depositPercent = listingRules.depositPercent();
            String percentText = depositPercent > 0.0D
                    ? " (" + String.format(Locale.ENGLISH, "%.1f%%", depositPercent) + ")"
                    : "";
            parts.add(format(creation.depositCharged(),
                    "amount", transactionService.formatCurrency(normalizedDeposit),
                    "percent", percentText));
        }
        if (durationClamped) {
            parts.add(format(creation.durationClamped(),
                    "duration", formatDuration(sanitizedDuration)));
        }

        return AuctionOperationResult.success(String.join(" ", parts).trim());
    }

    // ---------- small helpers ----------

    private String describeItem(ItemStack item) {
        if (item == null) return backendMessages.fallback().unknownItem();
        var meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String stripped = ChatColor.stripColor(meta.getDisplayName());
            if (stripped != null && !stripped.isEmpty()) return item.getAmount() + "x " + stripped;
            return item.getAmount() + "x " + meta.getDisplayName();
        }
        String mat = item.getType().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        if (mat.isEmpty()) mat = backendMessages.fallback().unknownMaterial();
        mat = Character.toUpperCase(mat.charAt(0)) + mat.substring(1);
        return item.getAmount() + "x " + mat;
    }

    private String formatDuration(Duration duration) {
        long totalMinutes = duration.toMinutes();
        long days = totalMinutes / (60 * 24);
        totalMinutes %= 60 * 24;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        List<String> parts = new ArrayList<>();
        if (days > 0) parts.add(days + "d");
        if (hours > 0) parts.add(hours + "h");
        if (minutes > 0 && days == 0) parts.add(minutes + "m");
        if (parts.isEmpty()) parts.add("0m");
        return String.join(" ", parts);
    }

    private String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    private String format(String template, String... kv) {
        if (template == null || template.isEmpty()) return "";
        String out = template;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            out = out.replace("{" + kv[i] + "}", kv[i + 1] == null ? "" : kv[i + 1]);
        }
        return colorize(out);
    }
}
