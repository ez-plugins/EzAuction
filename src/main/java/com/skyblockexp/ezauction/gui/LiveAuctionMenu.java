package com.skyblockexp.ezauction.gui;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.compat.ItemTagStorage;
import com.skyblockexp.ezauction.config.AuctionMessageConfiguration;
import com.skyblockexp.ezauction.config.AuctionValueConfiguration;
import com.skyblockexp.ezauction.live.LiveAuctionEntry;
import com.skyblockexp.ezauction.live.LiveAuctionService;
import com.skyblockexp.ezauction.util.ItemValueProvider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Displays the queued live auctions in a dedicated GUI.
 */
public final class LiveAuctionMenu implements Listener {

    private static final String ACTION_CLOSE = "close";
    private static final String ACTION_REFRESH = "refresh";
    private static final String ACTION_BROWSE = "browse";
    private static final String ACTION_BACK = "back";

    private static final int INVENTORY_SIZE = 45;
    private static final String INVENTORY_TITLE = ChatColor.GOLD + "Live Auctions";
    private static final int INFO_SLOT = 4;
    private static final int CLOSE_SLOT = 40;
    private static final int REFRESH_SLOT = 42;
    private static final int BROWSE_SLOT = 38;
    private static final int EMPTY_SLOT = 22;
    private static final int BACK_SLOT = 36;

    private static final int[] QUEUE_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    private final JavaPlugin plugin;
    private final AuctionManager auctionManager;
    private final AuctionTransactionService transactionService;
    private final AuctionMenu auctionMenu;
    private final LiveAuctionService liveAuctionService;
    private final AuctionMessageConfiguration.LiveMessages messages;
    private final ItemValueProvider shopPriceProvider;
    private final boolean shopPriceDisplayEnabled;
    private final String shopPriceFormat;

    private final String actionKey;
    private final ItemTagStorage itemTagStorage;

    private final ItemStack fillerButton;
    private final ItemStack closeButton;
    private final ItemStack refreshButton;
    private final ItemStack browseButton;
    private final ItemStack backButton;
    private final ItemStack infoButton;

    public LiveAuctionMenu(JavaPlugin plugin, AuctionManager auctionManager,
            AuctionTransactionService transactionService, AuctionMenu auctionMenu,
            LiveAuctionService liveAuctionService, AuctionMessageConfiguration.LiveMessages messages,
            AuctionValueConfiguration valueConfiguration, ItemValueProvider shopPriceProvider,
            boolean shopPriceDisplayEnabled, ItemTagStorage itemTagStorage) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.transactionService = Objects.requireNonNull(transactionService, "transactionService");
        this.auctionMenu = Objects.requireNonNull(auctionMenu, "auctionMenu");
        this.liveAuctionService = Objects.requireNonNull(liveAuctionService, "liveAuctionService");
        this.messages = messages != null ? messages : AuctionMessageConfiguration.LiveMessages.defaults();
        AuctionValueConfiguration.ShopPriceConfiguration shopPriceConfiguration = valueConfiguration != null
                ? valueConfiguration.shopPriceConfiguration()
                : null;
        this.shopPriceProvider = shopPriceProvider != null ? shopPriceProvider : ItemValueProvider.none();
        this.shopPriceDisplayEnabled = shopPriceDisplayEnabled;
        this.shopPriceFormat = shopPriceConfiguration != null ? shopPriceConfiguration.format() : null;
        this.actionKey = "live_auction_action";
        this.itemTagStorage = Objects.requireNonNull(itemTagStorage, "itemTagStorage");
        this.fillerButton = createButton(Material.GRAY_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + " ", List.of());
        this.closeButton = createButton(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Exit the menu."));
        this.refreshButton = createButton(Material.SUNFLOWER, ChatColor.GOLD + "Refresh",
                List.of(ChatColor.GRAY + "Update the live auction queue."));
        this.browseButton = createButton(Material.CHEST, ChatColor.GREEN + "Browse Auctions",
                List.of(ChatColor.GRAY + "Open the main auction house."));
        this.backButton = createButton(Material.ARROW, ChatColor.YELLOW + "Back to Browser",
                List.of(ChatColor.GRAY + "Return to the auction browser."));
        this.infoButton = createButton(Material.WRITABLE_BOOK, ChatColor.AQUA + "Live Auction Queue",
                List.of(ChatColor.GRAY + "View upcoming live announcements.",
                        ChatColor.GRAY + "Listings are broadcast from first to last.",
                        ChatColor.DARK_GRAY + "Showing up to " + QUEUE_SLOTS.length + " auctions."));
    }

    /**
     * Opens the live auction queue for the given player.
     */
    public void open(Player player) {
        if (player == null) {
            return;
        }
        if (!liveAuctionService.isFeatureEnabled()) {
            sendMessage(player, messages.disabled());
            return;
        }
        if (!liveAuctionService.isQueueEnabled()) {
            sendMessage(player, messages.queueInstant());
            return;
        }
        LiveAuctionMenuHolder holder = new LiveAuctionMenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, INVENTORY_TITLE);
        holder.setInventory(inventory);
        populateInventory(inventory);
        player.openInventory(inventory);
    }

    private void populateInventory(Inventory inventory) {
        applyFiller(inventory);

        ItemStack info = infoButton.clone();
        inventory.setItem(INFO_SLOT, info);

        ItemStack close = closeButton.clone();
        setAction(close, ACTION_CLOSE);
        inventory.setItem(CLOSE_SLOT, close);

        ItemStack refresh = refreshButton.clone();
        setAction(refresh, ACTION_REFRESH);
        inventory.setItem(REFRESH_SLOT, refresh);

        ItemStack browse = browseButton.clone();
        setAction(browse, ACTION_BROWSE);
        inventory.setItem(BROWSE_SLOT, browse);

        ItemStack back = backButton.clone();
        setAction(back, ACTION_BACK);
        inventory.setItem(BACK_SLOT, back);

        List<LiveAuctionEntry> queue = new ArrayList<>(auctionManager.listQueuedLiveAuctions());
        if (queue.isEmpty()) {
            ItemStack empty = createButton(Material.BARRIER, ChatColor.RED + "No Queued Auctions",
                    List.of(ChatColor.GRAY + "The live auction queue is currently empty."));
            inventory.setItem(EMPTY_SLOT, empty);
            return;
        }

        long now = System.currentTimeMillis();
        for (int index = 0; index < QUEUE_SLOTS.length && index < queue.size(); index++) {
            LiveAuctionEntry entry = queue.get(index);
            ItemStack icon = createQueueIcon(entry, index, now);
            int slot = QUEUE_SLOTS[index];
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, icon);
            }
        }
    }

    private ItemStack createQueueIcon(LiveAuctionEntry entry, int position, long now) {
        AuctionListing listing = entry.listing();
        ItemStack baseItem = listing != null ? listing.item() : null;
        ItemStack icon = baseItem != null ? baseItem.clone() : new ItemStack(Material.CHEST);
        if (icon.getType() == Material.AIR) {
            icon.setType(Material.CHEST);
        }

        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(icon.getType());
        }

        String queueName = ChatColor.YELLOW + describeItem(icon);
        if (meta != null) {
            if (!meta.hasDisplayName() || meta.getDisplayName() == null || meta.getDisplayName().isEmpty()) {
                meta.setDisplayName(queueName);
            }

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "Queue Position #" + (position + 1));
            if (position == 0) {
                lore.add(ChatColor.GREEN + "Up next for announcement!");
            }
            lore.add(ChatColor.GRAY + "Seller: " + ChatColor.AQUA + entry.sellerName());
            lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + transactionService.formatCurrency(listing.price()));
            appendShopPrice(baseItem != null ? baseItem : icon, lore);
            long expiryMillis = listing.expiryEpochMillis();
            if (expiryMillis > now) {
                Duration remaining = Duration.ofMillis(expiryMillis - now);
                lore.add(ChatColor.GRAY + "Ends in: " + ChatColor.YELLOW + formatDuration(remaining));
            } else {
                lore.add(ChatColor.RED + "This listing is expiring soon.");
            }
            if (listing.deposit() > 0.0D) {
                lore.add(ChatColor.GRAY + "Deposit: " + ChatColor.GOLD
                        + transactionService.formatCurrency(listing.deposit()));
            }
            lore.add(ChatColor.DARK_GRAY + "ID: " + listing.id());
            meta.setLore(lore);

            if (position == 0) {
                meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            icon.setItemMeta(meta);
        }

        return icon;
    }

    private void appendShopPrice(ItemStack item, List<String> lore) {
        if (!shopPriceDisplayEnabled || item == null || lore == null) {
            return;
        }
        OptionalDouble shopPrice = shopPriceProvider != null
                ? shopPriceProvider.estimate(item)
                : OptionalDouble.empty();
        if (shopPrice.isEmpty()) {
            return;
        }
        String format = shopPriceFormat != null && !shopPriceFormat.isEmpty()
                ? shopPriceFormat
                : "&7Shop Price: &6{value}";
        String formatted = format.replace("{value}", transactionService.formatCurrency(shopPrice.getAsDouble()));
        lore.add(colorize(formatted));
    }

    private void applyFiller(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, fillerButton.clone());
        }
    }

    private ItemStack createButton(Material material, String displayName, List<String> lore) {
        ItemStack item = material != null ? new ItemStack(material) : new ItemStack(Material.STONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(displayName);
            }
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void setAction(ItemStack item, String action) {
        if (item == null || action == null || action.isEmpty()) {
            return;
        }
        itemTagStorage.setString(item, actionKey, action);
    }

    private void sendMessage(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private String getAction(ItemStack item) {
        if (item == null) {
            return null;
        }
        return itemTagStorage.getString(item, actionKey);
    }

    private String describeItem(ItemStack item) {
        if (item == null) {
            return "Unknown Item";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.getDisplayName() != null
                && !ChatColor.stripColor(meta.getDisplayName()).isEmpty()) {
            return ChatColor.stripColor(meta.getDisplayName());
        }
        String materialName = item.getType().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        if (materialName.isEmpty()) {
            return "Item";
        }
        return Character.toUpperCase(materialName.charAt(0)) + materialName.substring(1);
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return "Expired";
        }
        long totalSeconds = duration.getSeconds();
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        if (days > 0) {
            if (hours > 0) {
                return days + "d " + hours + "h";
            }
            return days + "d";
        }
        if (hours > 0) {
            if (minutes > 0) {
                return hours + "h " + minutes + "m";
            }
            return hours + "h";
        }
        if (minutes > 0) {
            return minutes + "m";
        }
        long seconds = Math.max(1L, totalSeconds % 60L);
        return seconds + "s";
    }

    private String colorize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof LiveAuctionMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!holder.owner().equals(player.getUniqueId())) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        String action = getAction(clicked);
        if (action == null) {
            return;
        }
        switch (action) {
            case ACTION_CLOSE -> player.closeInventory();
            case ACTION_REFRESH -> reopenAsync(player);
            case ACTION_BROWSE -> {
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin, () -> auctionMenu.openBrowser(player));
            }
            case ACTION_BACK -> {
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin, () -> auctionMenu.openBrowser(player));
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof LiveAuctionMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void reopenAsync(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                open(player);
            }
        }.runTask(plugin);
    }

    private static final class LiveAuctionMenuHolder implements InventoryHolder {

        private final UUID owner;
        private Inventory inventory;

        private LiveAuctionMenuHolder(UUID owner) {
            this.owner = owner;
        }

        public UUID owner() {
            return owner;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
