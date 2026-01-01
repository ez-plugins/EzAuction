package com.skyblockexp.ezauction.live;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.config.AuctionBackendMessages;
import com.skyblockexp.ezauction.config.LiveAuctionConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Handles live auction queueing and announcements.
 */
public final class LiveAuctionService {

    private final JavaPlugin plugin;
    private final com.skyblockexp.ezauction.transaction.AuctionTransactionService transactionService;
    private final LiveAuctionConfiguration configuration;
    private final AuctionBackendMessages.LiveMessages messages;
    private final AuctionBackendMessages.FallbackMessages fallbackMessages;
    private final Queue<LiveAuctionEntry> queue = new ConcurrentLinkedQueue<>();
    private BukkitTask announcementTask;
    private boolean enabled = false;

    public LiveAuctionService(JavaPlugin plugin, com.skyblockexp.ezauction.transaction.AuctionTransactionService transactionService,
            LiveAuctionConfiguration configuration, AuctionBackendMessages.LiveMessages messages,
            AuctionBackendMessages.FallbackMessages fallbackMessages) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.transactionService = Objects.requireNonNull(transactionService, "transactionService");
        this.configuration = configuration != null ? configuration : LiveAuctionConfiguration.defaults();
        this.messages = messages != null ? messages : AuctionBackendMessages.LiveMessages.defaults();
        this.fallbackMessages = fallbackMessages != null
                ? fallbackMessages
                : AuctionBackendMessages.FallbackMessages.defaults();
    }

    public void enable() {
        if (!configuration.enabled()) {
            return;
        }
        if (!configuration.queueEnabled() || !configuration.displayInChat()) {
            return;
        }
        long interval = Math.max(1L, configuration.announcementIntervalTicks());
        announcementTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::announceNext, interval, interval);
        enabled = true;
    }

    public void disable() {
        if (announcementTask != null) {
            announcementTask.cancel();
            announcementTask = null;
        }
        queue.clear();
    }

    public boolean enqueue(AuctionListing listing, UUID sellerId, String sellerName) {
        if (!configuration.enabled() || listing == null) {
            return false;
        }
        String resolvedName = resolveSellerName(sellerId, sellerName);
        LiveAuctionEntry entry = new LiveAuctionEntry(listing, sellerId, resolvedName);
        if (!configuration.queueEnabled()) {
            announce(entry);
            return true;
        }
        queue.offer(entry);
        if (announcementTask == null && configuration.displayInChat()) {
            announceNext();
        }
        return true;
    }

    public List<LiveAuctionEntry> snapshotQueue() {
        if (queue.isEmpty()) {
            return List.of();
        }
        return List.copyOf(new ArrayList<>(queue));
    }

    public boolean isFeatureEnabled() {
        return configuration.enabled();
    }

    public boolean isQueueEnabled() {
        return configuration.queueEnabled();
    }

    public boolean displayInChat() {
        return configuration.displayInChat();
    }

    public LiveAuctionEntry poll() {
        return queue.poll();
    }
    
    public boolean isEnabled()
    {
    	return enabled;
    }

    private void announceNext() {
        LiveAuctionEntry entry = poll();
        if (entry == null) {
            return;
        }
        announce(entry);
    }

    private void announce(LiveAuctionEntry entry) {
        if (!configuration.displayInChat()) {
            return;
        }
        AuctionListing listing = entry.listing();
        ItemStack item = listing.item();
        String itemDescription = describeItem(item);
        String price = transactionService.formatCurrency(listing.price());
        String message = formatMessage(messages.broadcast(),
                "seller", entry.sellerName(),
                "item", itemDescription,
                "price", price);
        Bukkit.broadcastMessage(message);
    }

    private String resolveSellerName(UUID sellerId, String fallback) {
        if (fallback != null && !fallback.isEmpty()) {
            return fallback;
        }
        if (sellerId != null) {
            Player online = Bukkit.getPlayer(sellerId);
            if (online != null) {
                return online.getName();
            }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(sellerId);
            if (offline != null && offline.getName() != null) {
                return offline.getName();
            }
        }
        return fallbackMessages.unknownName();
    }

    private String describeItem(ItemStack item) {
        if (item == null) {
            return fallbackMessages.unknownItem();
        }
        int amount = Math.max(1, item.getAmount());
        ItemMeta meta = item.getItemMeta();
        String name;
        if (meta != null && meta.hasDisplayName()) {
            String displayName = meta.getDisplayName();
            String stripped = displayName != null ? ChatColor.stripColor(displayName) : "";
            name = (stripped != null && !stripped.isEmpty()) ? stripped : displayName;
        } else {
            name = friendlyMaterialName(item.getType());
        }
        if (name == null || name.isEmpty()) {
            name = fallbackMessages.unknownItem();
        }
        return amount + "x " + name;
    }

    private String friendlyMaterialName(Material material) {
        if (material == null) {
            return fallbackMessages.unknownMaterial();
        }
        String lowercase = material.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        if (lowercase.isEmpty()) {
            return fallbackMessages.unknownMaterial();
        }
        return Character.toUpperCase(lowercase.charAt(0)) + lowercase.substring(1);
    }

    private String formatMessage(String template, String... replacements) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        String formatted = template;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String key = replacements[i];
            String value = replacements[i + 1];
            formatted = formatted.replace("{" + key + "}", value != null ? value : "");
        }
        return ChatColor.translateAlternateColorCodes('&', formatted);
    }
}
