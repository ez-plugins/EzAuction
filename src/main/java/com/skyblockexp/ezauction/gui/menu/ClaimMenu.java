package com.skyblockexp.ezauction.gui.menu;

import com.skyblockexp.ezauction.service.AuctionManager;
import com.skyblockexp.ezauction.model.AuctionOperationResult;
import com.skyblockexp.ezauction.claim.AuctionClaimService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A 54-slot GUI that shows a player's pending claim items with expiry dates.
 *
 * <p>Layout:</p>
 * <ul>
 *   <li>Slots 0-44 — pending items (first 45 shown; each item lore shows expiry date)</li>
 *   <li>Slot 45 — Back button (returns to main auction browser)</li>
 *   <li>Slot 49 — Claim All button</li>
 *   <li>Other bottom-row slots — gray pane filler</li>
 * </ul>
 */
public final class ClaimMenu implements Listener {

    private static final int SIZE = 54;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_CLAIM_ALL = 49;

    private final JavaPlugin plugin;
    private final AuctionManager auctionManager;
    private final AuctionClaimService claimService;
    private final AuctionMenu auctionMenu;

    /** Open menu holders keyed by player UUID. */
    private final Map<UUID, ClaimMenuHolder> openMenus = new ConcurrentHashMap<>();

    public ClaimMenu(JavaPlugin plugin,
                     AuctionManager auctionManager,
                     AuctionClaimService claimService,
                     AuctionMenu auctionMenu) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.claimService = Objects.requireNonNull(claimService, "claimService");
        this.auctionMenu = Objects.requireNonNull(auctionMenu, "auctionMenu");
    }

    // ---- public API ----

    public void open(Player player) {
        String title = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Pending Claims";
        Inventory inv = Bukkit.createInventory(null, SIZE, title);
        ClaimMenuHolder holder = new ClaimMenuHolder(player.getUniqueId(), inv);
        openMenus.put(player.getUniqueId(), holder);
        populate(inv, player);
        player.openInventory(inv);
    }

    // ---- inventory population ----

    private void populate(Inventory inv, Player player) {
        // Fill all slots with gray pane first
        ItemStack filler = buildFiller();
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler.clone());
        }

        List<ItemStack> items = auctionManager.getPendingReturnItems(player.getUniqueId());
        long expiryMs = claimService.getClaimExpiryMs(player.getUniqueId());
        String expiryStr = expiryMs > 0 ? formatExpiry(expiryMs) : ChatColor.GRAY + "Unknown";

        int maxSlot = Math.min(items.size(), 45);
        for (int i = 0; i < maxSlot; i++) {
            ItemStack base = items.get(i);
            if (base == null || base.getType() == Material.AIR) continue;
            ItemStack display = base.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(display.getType());
            if (meta != null) {
                List<String> lore = new ArrayList<>(meta.hasLore() && meta.getLore() != null ? meta.getLore() : List.of());
                lore.add("");
                lore.add(ChatColor.RED + "Expires: " + expiryStr);
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(i, display);
        }

        // Back button
        inv.setItem(SLOT_BACK, buildBackButton());

        // Claim All button
        int count = items.stream().filter(it -> it != null && it.getType() != Material.AIR).mapToInt(ItemStack::getAmount).sum();
        inv.setItem(SLOT_CLAIM_ALL, buildClaimAllButton(count));
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

    private static ItemStack buildBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "← Back");
            meta.setLore(List.of(ChatColor.GRAY + "Return to the Auction House."));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildClaimAllButton(int totalAmount) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Claim All");
            List<String> lore = new ArrayList<>();
            if (totalAmount > 0) {
                lore.add(ChatColor.GRAY + "Claim all " + ChatColor.GREEN + totalAmount + ChatColor.GRAY + " pending item(s).");
            } else {
                lore.add(ChatColor.GRAY + "No items to claim.");
            }
            lore.add("");
            lore.add(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Click to claim!");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatExpiry(long epochMs) {
        long now = System.currentTimeMillis();
        long diffMs = epochMs - now;
        if (diffMs <= 0) {
            return ChatColor.RED + "Expired";
        }
        long days = diffMs / 86_400_000L;
        long hours = (diffMs % 86_400_000L) / 3_600_000L;
        String dateStr = new SimpleDateFormat("dd MMM yyyy HH:mm").format(new Date(epochMs));
        if (days > 0) {
            return ChatColor.YELLOW + "in " + days + "d " + hours + "h (" + dateStr + ")";
        } else {
            long minutes = (diffMs % 3_600_000L) / 60_000L;
            return ChatColor.RED + "in " + hours + "h " + minutes + "m (" + dateStr + ")";
        }
    }

    // ---- event handling ----

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ClaimMenuHolder holder = openMenus.get(player.getUniqueId());
        if (holder == null) return;
        if (!event.getInventory().equals(holder.inventory)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;

        switch (slot) {
            case SLOT_BACK -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> auctionMenu.openBrowser(player));
            }
            case SLOT_CLAIM_ALL -> {
                AuctionOperationResult result = auctionManager.claimReturnItems(player);
                if (!result.message().isEmpty()) {
                    player.sendMessage(result.message());
                }
                // Refresh or close if all claimed
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (auctionManager.countPendingReturnItems(player.getUniqueId()) > 0) {
                        player.closeInventory();
                        open(player);
                    } else {
                        player.closeInventory();
                    }
                });
            }
            default -> { /* item slots — no action for now */ }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ClaimMenuHolder holder = openMenus.get(player.getUniqueId());
        if (holder != null && event.getInventory().equals(holder.inventory)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        ClaimMenuHolder holder = openMenus.get(player.getUniqueId());
        if (holder != null && event.getInventory().equals(holder.inventory)) {
            openMenus.remove(player.getUniqueId());
        }
    }

    // ---- inner holder ----

    static final class ClaimMenuHolder implements InventoryHolder {
        final UUID viewerId;
        final Inventory inventory;

        ClaimMenuHolder(UUID viewerId, Inventory inventory) {
            this.viewerId = viewerId;
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
