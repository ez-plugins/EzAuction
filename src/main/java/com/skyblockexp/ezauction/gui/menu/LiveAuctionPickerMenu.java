package com.skyblockexp.ezauction.gui.menu;

import com.skyblockexp.ezauction.model.AuctionOperationResult;
import com.skyblockexp.ezauction.config.AuctionBackendMessages;
import com.skyblockexp.ezauction.live.LiveAuctionService;
import com.skyblockexp.ezauction.live.component.LiveAuctionComponentBuilder;
import com.skyblockexp.ezauction.live.model.LiveAuctionState;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A GUI that lists all currently active live auctions and lets a player click to join one.
 *
 * <p>The inventory is rebuilt fresh each time it is opened because the auction list can change.
 * The menu auto-closes if there are no active auctions when opened.</p>
 */
public final class LiveAuctionPickerMenu implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final String TITLE = ChatColor.GOLD + "Active Live Auctions";

    /** PDC key used to store the auction ID on a slot item. */
    private final NamespacedKey auctionIdKey;

    private final JavaPlugin plugin;
    private final LiveAuctionService liveAuctionService;
    private final AuctionTransactionService transactionService;
    private final AuctionBackendMessages.LiveMessages messages;
    private volatile BidMenu bidMenu;

    public LiveAuctionPickerMenu(JavaPlugin plugin,
            LiveAuctionService liveAuctionService,
            AuctionTransactionService transactionService,
            AuctionBackendMessages.LiveMessages messages) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.liveAuctionService = Objects.requireNonNull(liveAuctionService, "liveAuctionService");
        this.transactionService = Objects.requireNonNull(transactionService, "transactionService");
        this.messages = messages != null ? messages : AuctionBackendMessages.LiveMessages.defaults();
        this.auctionIdKey = new NamespacedKey(plugin, "live_picker_auction_id");
    }

    /** Sets the bid menu to open after a player successfully joins an auction. */
    public void setBidMenu(BidMenu bidMenu) {
        this.bidMenu = bidMenu;
    }

    public void open(Player player) {
        Collection<LiveAuctionState> active = liveAuctionService.getActiveAuctions();
        if (active.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.noActive()));
            return;
        }

        PickerHolder holder = new PickerHolder(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, INVENTORY_SIZE, TITLE);
        holder.setInventory(inv);
        populate(inv, active);
        player.openInventory(inv);
    }

    private void populate(Inventory inv, Collection<LiveAuctionState> active) {
        // Fill with glass pane
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(ChatColor.DARK_GRAY + " ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inv.setItem(i, filler.clone());
        }

        int slot = 10;
        for (LiveAuctionState state : active) {
            if (slot >= INVENTORY_SIZE) break;
            inv.setItem(slot, buildStateIcon(state));
            slot = nextContentSlot(slot);
        }
    }

    /** Returns the next display slot, skipping border slots (rows 0 and 5, cols 0 and 8). */
    private static int nextContentSlot(int current) {
        int next = current + 1;
        while (next < 54) {
            int col = next % 9;
            int row = next / 9;
            if (col > 0 && col < 8 && row > 0 && row < 5) return next;
            next++;
        }
        return next;
    }

    private ItemStack buildStateIcon(LiveAuctionState state) {
        ItemStack item = state.listing().item();
        ItemStack icon = item != null ? item.clone() : new ItemStack(Material.PAPER);
        if (icon.getAmount() < 1) icon.setAmount(1);

        ItemMeta meta = icon.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(icon.getType());
        if (meta == null) return icon;

        String itemDescription = LiveAuctionComponentBuilder.describeItem(state.listing().item());
        meta.setDisplayName(ChatColor.AQUA + itemDescription);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Seller: " + ChatColor.GREEN + state.entry().sellerName());
        lore.add(ChatColor.GRAY + "Current bid: " + ChatColor.GOLD
                + transactionService.formatCurrency(state.currentBid()));
        if (state.hasBids()) {
            lore.add(ChatColor.GRAY + "Top bidder: " + ChatColor.YELLOW + state.topBidderName());
        }
        lore.add(ChatColor.GRAY + "Time left: " + ChatColor.YELLOW + state.secondsRemaining() + "s");
        lore.add("");
        lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Click to join this auction!");

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(auctionIdKey, PersistentDataType.STRING, state.id());
        icon.setItemMeta(meta);
        return icon;
    }

    // ---- event handling ----

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PickerHolder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String auctionId = meta.getPersistentDataContainer().get(auctionIdKey, PersistentDataType.STRING);
        if (auctionId == null) return;

        player.closeInventory();

        AuctionOperationResult result = liveAuctionService.joinAuction(player, auctionId);
        if (!result.message().isEmpty()) {
            player.sendMessage(result.message());
        }
        // Open the bid GUI on successful join
        if (result.success() && bidMenu != null) {
            bidMenu.open(player, auctionId);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof PickerHolder) {
            event.setCancelled(true);
        }
    }

    // ---- inner holder ----

    static final class PickerHolder implements InventoryHolder {
        private final UUID viewerId;
        private Inventory inventory;

        PickerHolder(UUID viewerId) {
            this.viewerId = viewerId;
        }

        public UUID viewerId() { return viewerId; }

        void setInventory(Inventory inventory) { this.inventory = inventory; }

        @Override
        public Inventory getInventory() { return inventory; }
    }
}
