package com.skyblockexp.ezauction.gui.menu;

import com.skyblockexp.ezauction.model.AuctionOperationResult;
import com.skyblockexp.ezauction.live.LiveAuctionService;
import com.skyblockexp.ezauction.live.model.LiveAuctionState;
import com.skyblockexp.ezauction.live.registry.LiveBidSessionRegistry;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.util.NumberShortcutParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A 54-slot GUI for placing bids in a live auction.
 *
 * <p>Layout (row × col, 0-indexed):</p>
 * <ul>
 *   <li>Slot 13 - auctioned item (centre top area)</li>
 *   <li>Slot 15 - status pane (current bid, top bidder, time remaining)</li>
 *   <li>Slot 28 - "Minimum bid" button (lime terracotta)</li>
 *   <li>Slot 29 - "+25%" button (lime concrete)</li>
 *   <li>Slot 30 - "+50%" button (lime stained glass)</li>
 *   <li>Slot 31 - "×2 current bid" button (gold ingot)</li>
 *   <li>Slot 33 - "Custom amount" button (yellow stained glass) - prompts via chat</li>
 *   <li>Slot 49 - "Leave auction" button (red stained glass)</li>
 *   <li>All others - gray stained glass pane filler</li>
 * </ul>
 *
 * <p>The menu auto-refreshes every 40 ticks while open. Custom-amount chat input is handled
 * via {@link AsyncPlayerChatEvent}.</p>
 */
public final class BidMenu implements Listener {

    private static final int SIZE = 54;

    // Slot assignments
    private static final int SLOT_ITEM = 13;
    private static final int SLOT_STATUS = 15;
    private static final int SLOT_MIN_BID = 28;
    private static final int SLOT_PLUS_25 = 29;
    private static final int SLOT_PLUS_50 = 30;
    private static final int SLOT_DOUBLE = 31;
    private static final int SLOT_CUSTOM = 33;
    private static final int SLOT_LEAVE = 49;

    private static final long REFRESH_TICKS = 40L;

    private final JavaPlugin plugin;
    private final LiveAuctionService liveAuctionService;
    private final AuctionTransactionService transactionService;
    private final LiveBidSessionRegistry sessionRegistry;
    private final double bidIncrementPct;
    private final double bidIncrementFixed;

    /** Active menu holders keyed by player UUID. */
    private final Map<UUID, BidMenuHolder> openMenus = new ConcurrentHashMap<>();
    /** Players waiting for chat input for a custom bid amount; value = auction ID. */
    private final Map<UUID, String> pendingCustomBids = new ConcurrentHashMap<>();

    public BidMenu(JavaPlugin plugin,
            LiveAuctionService liveAuctionService,
            AuctionTransactionService transactionService,
            LiveBidSessionRegistry sessionRegistry,
            double bidIncrementPct,
            double bidIncrementFixed) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.liveAuctionService = Objects.requireNonNull(liveAuctionService, "liveAuctionService");
        this.transactionService = Objects.requireNonNull(transactionService, "transactionService");
        this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry");
        this.bidIncrementPct = bidIncrementPct;
        this.bidIncrementFixed = bidIncrementFixed;
    }

    // ---- public API ----

    /**
     * Opens the bid GUI for the given player, tied to the specified auction.
     *
     * @param player    the viewer
     * @param auctionId the live auction ID the player is participating in
     */
    public void open(Player player, String auctionId) {
        Optional<LiveAuctionState> stateOpt = liveAuctionService.getActiveAuction(auctionId);
        if (stateOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "That auction is no longer active.");
            return;
        }
        LiveAuctionState state = stateOpt.get();

        // Close any existing open menu for this player first
        BidMenuHolder existing = openMenus.remove(player.getUniqueId());
        if (existing != null) existing.cancelRefresh();

        String title = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Live Auction - Bid";
        Inventory inv = Bukkit.createInventory(null, SIZE, title);
        BidMenuHolder holder = new BidMenuHolder(player.getUniqueId(), auctionId, inv);
        openMenus.put(player.getUniqueId(), holder);

        populate(inv, state);
        player.openInventory(inv);

        // Schedule periodic refresh
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            BidMenuHolder h = openMenus.get(player.getUniqueId());
            if (h == null || !h.auctionId.equals(auctionId)) return;
            Optional<LiveAuctionState> refreshed = liveAuctionService.getActiveAuction(auctionId);
            if (refreshed.isEmpty()) {
                // Auction ended - close inventory on main thread (we're already on main thread via runTaskTimer)
                openMenus.remove(player.getUniqueId());
                h.cancelRefresh();
                player.closeInventory();
                player.sendMessage(ChatColor.GOLD + "The auction has ended.");
                return;
            }
            populate(h.inventory, refreshed.get());
        }, REFRESH_TICKS, REFRESH_TICKS);
        holder.setRefreshTask(task);
    }

    // ---- inventory population ----

    private void populate(Inventory inv, LiveAuctionState state) {
        // Fill with gray pane
        ItemStack filler = buildFiller();
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler.clone());
        }

        // Auctioned item
        inv.setItem(SLOT_ITEM, buildItemDisplay(state));

        // Status pane
        inv.setItem(SLOT_STATUS, buildStatusPane(state));

        // Bid buttons
        double current = state.currentBid();
        double minimum = state.minimumNextBid(bidIncrementPct, bidIncrementFixed);

        inv.setItem(SLOT_MIN_BID, buildBidButton(
                Material.LIME_TERRACOTTA,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Minimum Bid",
                minimum,
                List.of(ChatColor.GRAY + "Place the minimum required bid.",
                        ChatColor.GRAY + "Amount: " + ChatColor.GREEN
                                + transactionService.formatCurrency(minimum))));

        double plus25 = current * 1.25;
        inv.setItem(SLOT_PLUS_25, buildBidButton(
                Material.LIME_CONCRETE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Bid +25%",
                plus25,
                List.of(ChatColor.GRAY + "Bid 25% above current bid.",
                        ChatColor.GRAY + "Amount: " + ChatColor.AQUA
                                + transactionService.formatCurrency(plus25))));

        double plus50 = current * 1.5;
        inv.setItem(SLOT_PLUS_50, buildBidButton(
                Material.LIME_STAINED_GLASS,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Bid +50%",
                plus50,
                List.of(ChatColor.GRAY + "Bid 50% above current bid.",
                        ChatColor.GRAY + "Amount: " + ChatColor.AQUA
                                + transactionService.formatCurrency(plus50))));

        double doubled = current * 2.0;
        inv.setItem(SLOT_DOUBLE, buildBidButton(
                Material.GOLD_INGOT,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Double Bid",
                doubled,
                List.of(ChatColor.GRAY + "Bid double the current amount.",
                        ChatColor.GRAY + "Amount: " + ChatColor.GOLD
                                + transactionService.formatCurrency(doubled))));

        inv.setItem(SLOT_CUSTOM, buildActionButton(
                Material.YELLOW_STAINED_GLASS,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Custom Amount",
                List.of(ChatColor.GRAY + "Enter a custom bid amount in chat.",
                        ChatColor.GRAY + "Supports k/m/b/t suffixes (e.g. 5k, 1.2m).")));

        inv.setItem(SLOT_LEAVE, buildActionButton(
                Material.RED_STAINED_GLASS,
                ChatColor.RED + "" + ChatColor.BOLD + "Leave Auction",
                List.of(ChatColor.GRAY + "Leave the current live auction.")));
    }

    // ---- item builders ----

    private static ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GRAY + " ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildItemDisplay(LiveAuctionState state) {
        ItemStack raw = state.listing().item();
        ItemStack display = (raw != null && raw.getType() != Material.AIR)
                ? raw.clone() : new ItemStack(Material.PAPER);

        ItemMeta meta = display.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(display.getType());
        if (meta == null) return display;

        List<String> lore = new ArrayList<>(meta.hasLore() && meta.getLore() != null ? meta.getLore() : List.of());
        lore.add("");
        lore.add(ChatColor.GRAY + "Seller: " + ChatColor.GREEN + state.entry().sellerName());
        lore.add(ChatColor.GRAY + "Starting price: " + ChatColor.GOLD
                + transactionService.formatCurrency(state.listing().price()));
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack buildStatusPane(LiveAuctionState state) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Auction Status");

        long secondsLeft = state.secondsRemaining();
        String timeStr = secondsLeft > 0 ? formatTime(secondsLeft) : ChatColor.RED + "Ending soon!";

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current bid: " + ChatColor.GOLD
                + transactionService.formatCurrency(state.currentBid()));
        if (state.hasBids()) {
            lore.add(ChatColor.GRAY + "Top bidder: " + ChatColor.YELLOW + state.topBidderName());
        } else {
            lore.add(ChatColor.GRAY + "No bids yet!");
        }
        lore.add(ChatColor.GRAY + "Time left: " + ChatColor.YELLOW + timeStr);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBidButton(Material material, String name, double amount, List<String> extraLore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>(extraLore);
        lore.add("");
        lore.add(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Click to bid!");
        // Store the bid amount in display name tag (we parse from the lore "Amount:" line on click)
        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "bid_amount"),
                org.bukkit.persistence.PersistentDataType.DOUBLE,
                amount);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildActionButton(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String formatTime(long seconds) {
        if (seconds >= 3600) {
            return String.format("%dh %02dm %02ds", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
        } else if (seconds >= 60) {
            return String.format("%dm %02ds", seconds / 60, seconds % 60);
        } else {
            return seconds + "s";
        }
    }

    // ---- event handling ----

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        BidMenuHolder holder = openMenus.get(player.getUniqueId());
        if (holder == null) return;

        // Guard: only handle clicks in this holder's inventory
        if (!event.getInventory().equals(holder.inventory)) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;

        Optional<LiveAuctionState> stateOpt = liveAuctionService.getActiveAuction(holder.auctionId);
        if (stateOpt.isEmpty()) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "That auction has ended.");
            return;
        }
        LiveAuctionState state = stateOpt.get();

        switch (slot) {
            case SLOT_MIN_BID, SLOT_PLUS_25, SLOT_PLUS_50, SLOT_DOUBLE -> {
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null) return;
                ItemMeta meta = clicked.getItemMeta();
                if (meta == null) return;
                double amount = meta.getPersistentDataContainer().getOrDefault(
                        new org.bukkit.NamespacedKey(plugin, "bid_amount"),
                        org.bukkit.persistence.PersistentDataType.DOUBLE, 0.0);
                if (amount <= 0) return;
                placeBid(player, holder.auctionId, amount, state);
            }
            case SLOT_CUSTOM -> promptCustomAmount(player, holder.auctionId);
            case SLOT_LEAVE -> {
                player.closeInventory();
                liveAuctionService.leaveAuction(player);
            }
            default -> { /* filler - ignore */ }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        BidMenuHolder holder = openMenus.get(player.getUniqueId());
        if (holder != null && event.getInventory().equals(holder.inventory)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        BidMenuHolder holder = openMenus.get(player.getUniqueId());
        if (holder != null && event.getInventory().equals(holder.inventory)) {
            holder.cancelRefresh();
            openMenus.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String auctionId = pendingCustomBids.remove(player.getUniqueId());
        if (auctionId == null) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if ("cancel".equalsIgnoreCase(input)) {
                open(player, auctionId);
                return;
            }
            Optional<LiveAuctionState> stateOpt = liveAuctionService.getActiveAuction(auctionId);
            if (stateOpt.isEmpty()) {
                player.sendMessage(ChatColor.RED + "That auction has ended.");
                return;
            }
            LiveAuctionState state = stateOpt.get();
            if (input.isEmpty()) {
                // Empty input - bid the minimum required amount.
                double minimum = state.minimumNextBid(bidIncrementPct, bidIncrementFixed);
                placeBid(player, auctionId, minimum, state);
                return;
            }
            double amount;
            try {
                amount = NumberShortcutParser.parse(input);
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Invalid amount: " + input
                        + ". Use a number or suffixes like 5k, 1.2m.");
                open(player, auctionId);
                return;
            }
            placeBid(player, auctionId, amount, state);
        });
    }

    // ---- internal helpers ----

    private void promptCustomAmount(Player player, String auctionId) {
        pendingCustomBids.put(player.getUniqueId(), auctionId);
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Enter your bid amount in chat (e.g. "
                + ChatColor.GREEN + "5000" + ChatColor.YELLOW + ", "
                + ChatColor.GREEN + "5k" + ChatColor.YELLOW + ", "
                + ChatColor.GREEN + "1.2m" + ChatColor.YELLOW + "):");
        player.sendMessage(ChatColor.GRAY + "Press " + ChatColor.GREEN + "Enter"
                + ChatColor.GRAY + " with no input to bid the minimum amount.");
        player.sendMessage(ChatColor.GRAY + "Type " + ChatColor.RED + "cancel"
                + ChatColor.GRAY + " to abort.");
    }

    private void placeBid(Player player, String auctionId, double amount, LiveAuctionState state) {
        AuctionOperationResult result = liveAuctionService.bid(player, auctionId, amount);
        if (!result.message().isEmpty()) {
            player.sendMessage(result.message());
        }
        // Re-open the GUI with the latest state
        open(player, auctionId);
    }

    // ---- inner holder ----

    static final class BidMenuHolder implements InventoryHolder {
        final UUID viewerId;
        final String auctionId;
        final Inventory inventory;
        private BukkitTask refreshTask;

        BidMenuHolder(UUID viewerId, String auctionId, Inventory inventory) {
            this.viewerId = viewerId;
            this.auctionId = auctionId;
            this.inventory = inventory;
        }

        void setRefreshTask(BukkitTask task) { this.refreshTask = task; }

        void cancelRefresh() {
            if (refreshTask != null) {
                refreshTask.cancel();
                refreshTask = null;
            }
        }

        @Override
        public Inventory getInventory() { return inventory; }
    }
}
