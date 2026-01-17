package com.skyblockexp.ezauction.gui;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.AuctionOperationResult;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.compat.ItemTagStorage;
import com.skyblockexp.ezauction.config.AuctionListingRules;
import com.skyblockexp.ezauction.config.AuctionMenuInteractionConfiguration;
import com.skyblockexp.ezauction.config.AuctionMessageConfiguration;
import com.skyblockexp.ezauction.config.AuctionMessageConfiguration.SellMessages;
import com.skyblockexp.ezauction.util.EconomyUtils;
import com.skyblockexp.ezauction.util.ItemValueProvider;
import com.skyblockexp.ezauction.gui.SellMenuHolder.Target;
import com.skyblockexp.ezauction.live.LiveAuctionManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Presents an interactive interface for creating auction listings.
 * Event handling is delegated via AuctionSellMenuListener.
 */
public class AuctionSellMenu {

    private static final String ACTION_PRICE_ADJUST = "price_adjust";
    private static final String ACTION_PRICE_CUSTOM = "price_custom";
    private static final String ACTION_DURATION_NEXT = "duration_next";
    private static final String ACTION_CONFIRM = "confirm";
    private static final String ACTION_CANCEL = "cancel";

    private final JavaPlugin plugin;
    private final AuctionManager auctionManager;
    private final AuctionTransactionService transactionService;
    private final AuctionListingRules listingRules;
    private final ItemValueProvider itemValueProvider;

    private final String actionKey;
    private final String priceAdjustKey;
    private final ItemTagStorage itemTagStorage;

    private final AuctionMenuInteractionConfiguration.SellMenuLayoutConfiguration layout;
    private final ItemStack fillerPane;

    private final ConcurrentMap<UUID, SellMenuState> pendingPriceInputs;
    private final ConcurrentMap<UUID, Target> pendingTargets = new ConcurrentHashMap<>();

    private final Duration[] durationOptions;
    private final int defaultDurationIndex;
    private final double minimumPrice;
    private final Duration longestDurationOption;
    private final double defaultPrice;
    private final double[] priceAdjustments;
    private final SellMessages messages;
    private LiveAuctionManager liveAuctionManager; 

    public AuctionSellMenu(JavaPlugin plugin, AuctionManager auctionManager,
                           AuctionTransactionService transactionService, AuctionListingRules listingRules,
                           List<Duration> configuredDurationOptions,
                           AuctionMenuInteractionConfiguration.SellMenuInteractionConfiguration sellConfiguration,
                           ItemValueProvider itemValueProvider, SellMessages messages, ItemTagStorage itemTagStorage) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.transactionService = Objects.requireNonNull(transactionService, "transactionService");
        this.listingRules = Objects.requireNonNull(listingRules, "listingRules");
        this.itemValueProvider = itemValueProvider != null ? itemValueProvider : ItemValueProvider.none();
        this.messages = messages != null ? messages : SellMessages.defaults();
        this.actionKey = "auction_sell_action";
        this.priceAdjustKey = "auction_sell_adjust";
        this.itemTagStorage = Objects.requireNonNull(itemTagStorage, "itemTagStorage");
        this.pendingPriceInputs = new ConcurrentHashMap<>();
        this.durationOptions = buildDurationOptions(listingRules, configuredDurationOptions);
        this.defaultDurationIndex = resolveDefaultDurationIndex(durationOptions, listingRules.defaultDuration());
        this.minimumPrice = Math.max(0.0D, listingRules.minimumPrice());
        this.longestDurationOption = resolveLongestDuration(durationOptions, listingRules);
        AuctionMenuInteractionConfiguration.SellMenuInteractionConfiguration interactions = sellConfiguration != null
                ? sellConfiguration
                : AuctionMenuInteractionConfiguration.defaults().sellMenu();
        this.layout = interactions.layout();
        this.fillerPane = createBaseItem(layout.filler());
        this.defaultPrice = Math.max(0.0D, interactions.defaultPrice());
        List<Double> adjustments = interactions.priceAdjustments();
        this.priceAdjustments = new double[adjustments.size()];
        for (int i = 0; i < adjustments.size(); i++) {
            Double value = adjustments.get(i);
            this.priceAdjustments[i] = value != null ? value.doubleValue() : 0.0D;
        }
        
        this.liveAuctionManager = null;
    }

    public AuctionSellMenu(JavaPlugin plugin,
                           AuctionManager auctionManager,
                           LiveAuctionManager liveAuctionManager,
                           AuctionTransactionService transactionService,
                           AuctionListingRules listingRules,
                           List<Duration> configuredDurationOptions,
                           AuctionMenuInteractionConfiguration.SellMenuInteractionConfiguration sellConfiguration,
                           ItemValueProvider itemValueProvider,
                           AuctionMessageConfiguration.SellMessages messages,
                           ItemTagStorage itemTagStorage) {
        this(plugin, auctionManager, transactionService, listingRules,
             configuredDurationOptions, sellConfiguration, itemValueProvider, messages, itemTagStorage);
        this.liveAuctionManager = liveAuctionManager;
    }

    /** Opens the sell menu for the item in the player's main hand. */
    public void openSellMenu(Player player, Target target) {
        if (player == null) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            sendMessage(player, messages.itemRequired());
            return;
        }

        double startingPrice = Math.max(minimumPrice, defaultPrice);
        Double recommendedPrice = null;

        OptionalDouble estimate = itemValueProvider.estimate(item);
        if (estimate.isPresent() && estimate.getAsDouble() > 0.0D) {
            double normalized = EconomyUtils.normalizeCurrency(Math.max(minimumPrice, estimate.getAsDouble()));
            if (normalized > 0.0D) {
                recommendedPrice = normalized;
                startingPrice = normalized;
            }
        }

        SellMenuState state = new SellMenuState(
                item.clone(),
                startingPrice,
                defaultDurationIndex,
                recommendedPrice,
                durationOptions,
                listingRules
        );

        openSellMenu(player, state, target);
    }

    /**
     * Opens the sell menu for the live auction target.
     * Used by the /liveauction sell command.
     */
    public void openLive(Player player) {
        openSellMenu(player, SellMenuHolder.Target.LIVE);
    }

    private void openSellMenu(Player player, SellMenuState state, Target target) {
        if (player == null || state == null) return;

        SellMenuHolder holder = new SellMenuHolder(player.getUniqueId(), state);
        holder.setTarget(target);


        Inventory inventory = Bukkit.createInventory(holder, layout.size(), colorize(layout.title()));
        holder.setInventory(inventory);
        refreshMenu(holder);
        player.openInventory(inventory);
    }

    private void refreshMenu(SellMenuHolder holder) {
        Inventory inventory = holder.getInventory();
        if (inventory == null) return;

        inventory.clear();
        applyFiller(inventory);

        placePriceAdjustmentButtons(inventory);

        ItemStack listingItem = holder.state().item();
        ItemMeta meta = listingItem.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Listing Price: " + ChatColor.GOLD + formatPrice(holder.state().price()));
        Double recommended = holder.state().recommendedPrice();
        if (recommended != null) {
            lore.add(ChatColor.GRAY + "Recommended Price: " + ChatColor.GOLD + formatPrice(recommended));
        }
        lore.add(ChatColor.GRAY + "Duration: " + ChatColor.YELLOW + formatDuration(holder.state().duration()));
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Confirm below to create the listing.");
        if (meta != null) {
            List<String> existingLore = meta.hasLore() ? meta.getLore() : List.of();
            List<String> combined = new ArrayList<>(existingLore);
            if (!combined.isEmpty()) combined.add(" ");
            combined.addAll(lore);
            meta.setLore(combined);
            listingItem.setItemMeta(meta);
        }
        inventory.setItem(layout.listingSlot(), listingItem);

        ItemStack priceDisplay = createPriceDisplay(holder);
        inventory.setItem(layout.priceDisplay().slot(), priceDisplay);

        ItemStack durationDisplay = createDurationDisplay(holder);
        setPersistent(durationDisplay, actionKey, ACTION_DURATION_NEXT);
        inventory.setItem(layout.durationDisplay().slot(), durationDisplay);

        ItemStack customPrice = createCustomPriceButton();
        setPersistent(customPrice, actionKey, ACTION_PRICE_CUSTOM);
        inventory.setItem(layout.customPrice().slot(), customPrice);

        ItemStack confirm = createConfirmButton(holder);
        setPersistent(confirm, actionKey, ACTION_CONFIRM);
        inventory.setItem(layout.confirmButton().slot(), confirm);

        ItemStack cancel = createCancelButton();
        setPersistent(cancel, actionKey, ACTION_CANCEL);
        inventory.setItem(layout.cancelButton().slot(), cancel);
    }

    private void placePriceAdjustmentButtons(Inventory inventory) {
        double[] adjustments = priceAdjustments;
        int[] slots = layout.priceAdjustmentSlots();
        for (int i = 0; i < adjustments.length && i < slots.length; i++) {
            double amount = adjustments[i];
            if (amount == 0.0D) continue;
            ItemStack button = createPriceAdjustButton(amount);
            setPersistent(button, actionKey, ACTION_PRICE_ADJUST);
            setPersistent(button, priceAdjustKey, amount);
            inventory.setItem(slots[i], button);
        }
    }

    private ItemStack createBaseItem(AuctionMenuInteractionConfiguration.MenuButtonDefinition definition) {
        Material material = Material.GRAY_STAINED_GLASS_PANE;
        String displayName = null;
        if (definition != null) {
            Material configured = definition.material();
            if (configured != null) {
                material = configured;
            }
            displayName = definition.displayName();
        }
        ItemStack item = material != null ? new ItemStack(material) : null;
        if (item == null) {
            item = new ItemStack(Material.STONE);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(colorize(displayName));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPriceDisplay(SellMenuHolder holder) {
        AuctionMenuInteractionConfiguration.ButtonLayoutConfiguration definition = layout.priceDisplay();
        ItemStack item = createBaseItem(definition.button());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = definition.button().displayName();
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(colorize(displayName));
            }
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Current Price: " + ChatColor.GOLD + formatPrice(holder.state().price()));
            Double recommended = holder.state().recommendedPrice();
            if (recommended != null) {
                lore.add(ChatColor.GRAY + "Recommended Price: " + ChatColor.GOLD + formatPrice(recommended));
            }
            lore.add(ChatColor.GRAY + "Use the buttons above to adjust.");
            if (minimumPrice > 0.0D) {
                lore.add(ChatColor.GRAY + "Minimum Price: " + ChatColor.GOLD + formatPrice(minimumPrice));
            }
            double depositPercent = listingRules.depositPercent();
            if (depositPercent > 0.0D) {
                lore.add(ChatColor.GRAY + "Deposit: " + ChatColor.GOLD
                        + String.format(Locale.ENGLISH, "%.1f%%", depositPercent));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDurationDisplay(SellMenuHolder holder) {
        AuctionMenuInteractionConfiguration.ButtonLayoutConfiguration definition = layout.durationDisplay();
        ItemStack item = createBaseItem(definition.button());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = definition.button().displayName();
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(colorize(displayName));
            }
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Current Duration: " + ChatColor.YELLOW + formatDuration(holder.state().duration()));
            lore.add(ChatColor.GRAY + "Click to cycle through available durations.");
            lore.add(ChatColor.GRAY + "Longest Option: " + ChatColor.YELLOW + formatDuration(longestDurationOption));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCustomPriceButton() {
        AuctionMenuInteractionConfiguration.ButtonLayoutConfiguration definition = layout.customPrice();
        ItemStack item = createBaseItem(definition.button());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = definition.button().displayName();
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(colorize(displayName));
            }
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Enter a custom price in chat.");
            lore.add(ChatColor.DARK_GRAY + "Type 'cancel' to abort.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createConfirmButton(SellMenuHolder holder) {
        AuctionMenuInteractionConfiguration.ButtonLayoutConfiguration definition = layout.confirmButton();
        ItemStack item = createBaseItem(definition.button());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = definition.button().displayName();
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(colorize(displayName));
            }
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Item: " + ChatColor.AQUA + describeItem(holder.state().item()));
            lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + formatPrice(holder.state().price()));
            Double recommended = holder.state().recommendedPrice();
            if (recommended != null) {
                lore.add(ChatColor.GRAY + "Recommended Price: " + ChatColor.GOLD + formatPrice(recommended));
                double percentOfRecommended = (holder.state().price() / recommended) * 100;
                if (percentOfRecommended < 50) {
                    lore.add(ChatColor.RED + "⚠ Warning: Price is " + String.format(Locale.ENGLISH, "%.0f%%", percentOfRecommended) + " of recommended!");
                    lore.add(ChatColor.RED + "This may be significantly underpriced.");
                }
            }
            lore.add(ChatColor.GRAY + "Duration: " + ChatColor.YELLOW + formatDuration(holder.state().duration()));
            double depositAmount = EconomyUtils.normalizeCurrency(listingRules.depositAmount(holder.state().price()));
            if (depositAmount > 0.0D) {
                lore.add(ChatColor.GRAY + "Listing Fee: " + ChatColor.GOLD + formatPrice(depositAmount)
                        + ChatColor.GRAY + " (" + String.format(Locale.ENGLISH, "%.1f%%", listingRules.depositPercent()) + ")");
            }
            lore.add(" ");
            lore.add(ChatColor.GREEN + "Click to list this item on the auction house.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCancelButton() {
        AuctionMenuInteractionConfiguration.ButtonLayoutConfiguration definition = layout.cancelButton();
        ItemStack item = createBaseItem(definition.button());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = definition.button().displayName();
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(colorize(displayName));
            }
            meta.setLore(List.of(ChatColor.GRAY + "Return to the auction browser."));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPriceAdjustButton(double amount) {
        boolean positive = amount > 0;
        double displayAmount = Math.abs(amount);
        Material material;
        if (displayAmount >= 1000.0D) {
            material = positive ? Material.DIAMOND_BLOCK : Material.REDSTONE_BLOCK;
        } else if (displayAmount >= 100.0D) {
            material = positive ? Material.DIAMOND : Material.REDSTONE;
        } else if (displayAmount >= 10.0D) {
            material = positive ? Material.EMERALD_BLOCK : Material.REDSTONE_TORCH;
        } else {
            material = positive ? Material.EMERALD : Material.COAL;
        }
        String prefix = positive ? ChatColor.GREEN + "+" : ChatColor.RED + "-";
        String display = prefix + formatNumber(displayAmount);
        ItemStack item = material != null ? new ItemStack(material) : null;
        if (item == null) {
            item = new ItemStack(Material.STONE);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(display + ChatColor.GOLD + " coins");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Adjust the price by this amount.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatNumber(double amount) {
        if (amount >= 1000.0D && amount % 1000.0D == 0) return String.format(Locale.ENGLISH, "%.0fk", amount / 1000.0D);
        if (amount % 1.0D == 0) return String.format(Locale.ENGLISH, "%.0f", amount);
        return String.format(Locale.ENGLISH, "%.2f", amount);
    }

    private void applyFiller(Inventory inventory) {
        if (fillerPane == null) return;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.getType() == Material.AIR) {
                inventory.setItem(slot, fillerPane.clone());
            }
        }
    }

    private String colorize(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private void sendMessage(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) return;
        player.sendMessage(colorize(message));
    }

    private void setPersistent(ItemStack item, String key, String value) {
        if (item == null || key == null || value == null) {
            return;
        }
        itemTagStorage.setString(item, key, value);
    }

    private void setPersistent(ItemStack item, String key, double value) {
        if (item == null || key == null) {
            return;
        }
        itemTagStorage.setDouble(item, key, value);
    }

    private String formatPrice(double price) {
        return transactionService.formatCurrency(price);
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

    private String describeItem(ItemStack item) {
        if (item == null) return "Unknown";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return ChatColor.stripColor(meta.getDisplayName());
        return formatMaterialName(item.getType());
    }

    private String formatMaterialName(Material material) {
        if (material == null) return "Unknown";
        String[] parts = material.name().toLowerCase(Locale.ENGLISH).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        if (builder.length() > 0) builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    /* =========================
       Event handler delegates
       ========================= */

    public void handleInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof SellMenuHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!holder.owner().equals(player.getUniqueId())) return;

        int topSize = topInventory.getSize();
        boolean affectsTop = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (affectsTop) event.setCancelled(true);
    }

    public void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SellMenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!holder.owner().equals(player.getUniqueId())) return;
        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != holder) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        String action = itemTagStorage.getString(clicked, actionKey);
        if (action == null) return;

        switch (action) {
            case ACTION_PRICE_ADJUST -> handlePriceAdjust(player, holder, clicked);
            case ACTION_PRICE_CUSTOM -> startCustomPriceInput(player, holder);
            case ACTION_DURATION_NEXT -> {
                holder.state().cycleDuration();
                refreshMenu(holder);
            }
            case ACTION_CONFIRM -> handleConfirm(player, holder);
            case ACTION_CANCEL -> player.closeInventory();
            default -> { }
        }
    }

    public void handleAsyncPlayerChat(AsyncPlayerChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        SellMenuState state = pendingPriceInputs.get(playerId);
        if (state == null) return;

        event.setCancelled(true);
        pendingPriceInputs.remove(playerId);
        plugin.getServer().getScheduler().runTask(plugin,
                () -> handleCustomPriceInput(event.getPlayer(), state, event.getMessage()));
    }

    public void handlePlayerQuit(PlayerQuitEvent event) {
        pendingPriceInputs.remove(event.getPlayer().getUniqueId());
    }

    /* =========================
       Internal helpers
       ========================= */

    private void handlePriceAdjust(Player player, SellMenuHolder holder, ItemStack clicked) {
        Double amount = itemTagStorage.getDouble(clicked, priceAdjustKey);
        if (amount == null) return;
        double newPrice = EconomyUtils.normalizeCurrency(holder.state().price() + amount);
        if (newPrice < minimumPrice) newPrice = minimumPrice;
        holder.state().setPrice(newPrice);
        refreshMenu(holder);
    }

    private void handleConfirm(Player player, SellMenuHolder holder) {
        SellMenuState state = holder.state();
        if (state.price() < minimumPrice) {
            sendMessage(player, messages.priceMinimum().replace("{minimum}", formatPrice(minimumPrice)));
            refreshMenu(holder);
            return;
        }
        

        AuctionOperationResult result;
        if (holder.target() == Target.LIVE && liveAuctionManager != null) {
            // LIVE flow
            result = liveAuctionManager.createLiveListing(player, state.item(), state.price(), state.duration());
        } else {
            // Normal AH flow (fallback)
            result = auctionManager.createListing(player, state.item(), state.price(), state.duration());
        }

        if (result.message() != null && !result.message().isEmpty()) {
            player.sendMessage(result.message());
        }
        if (result.success()) {
            player.closeInventory();
        } else {
            refreshMenu(holder);
        }
    }

    
    private void startCustomPriceInput(Player player, SellMenuHolder holder) {
        pendingPriceInputs.put(player.getUniqueId(), holder.state());
        pendingTargets.put(player.getUniqueId(), holder.target());
        player.closeInventory();
        sendMessage(player, messages.pricePrompt());
    }

    private void handleCustomPriceInput(Player player, SellMenuState state, String message) {
        if (player == null || state == null) return;
        Target target = pendingTargets.getOrDefault(player.getUniqueId(), Target.NORMAL);
        pendingTargets.remove(player.getUniqueId());

        String input = message == null ? "" : message.trim();
        if (input.equalsIgnoreCase("cancel")) {
            sendMessage(player, messages.priceEntryCancelled());
            openSellMenu(player, state, target);
            return;
        }
        double value;
        try { value = com.skyblockexp.ezauction.util.NumberShortcutParser.parse(input); }
        catch (IllegalArgumentException ex) {
            sendMessage(player, messages.priceInvalidNumber());
            openSellMenu(player, state, target);
            return;
        }
        if (value <= 0) {
            sendMessage(player, messages.priceMustBePositive());
            openSellMenu(player, state, target);
            return;
        }
        double normalized = EconomyUtils.normalizeCurrency(value);
        if (normalized < minimumPrice) normalized = minimumPrice;

        state.setPrice(normalized);
        sendMessage(player, messages.priceUpdated().replace("{price}", formatPrice(normalized)));
        openSellMenu(player, state, target);
    }

    // buildDurationOptions, resolveLongestDuration, resolveDefaultDurationIndex
    // ... (unchanged from your last version)
    private Duration[] buildDurationOptions(AuctionListingRules rules, List<Duration> configuredOptions) {
        Objects.requireNonNull(rules, "rules");
        TreeSet<Long> minutes = new TreeSet<>();
        boolean usedConfigured = false;
        if (configuredOptions != null) {
            for (Duration option : configuredOptions) {
                if (option == null || option.isNegative() || option.isZero()) continue;
                Duration sanitized = rules.clampDuration(option);
                if (sanitized != null && !sanitized.isNegative() && !sanitized.isZero()) {
                    minutes.add(Math.max(1L, sanitized.toMinutes()));
                    usedConfigured = true;
                }
            }
        }
        if (!usedConfigured) {
            Duration[] defaults = new Duration[] {
                Duration.ofHours(6), Duration.ofHours(12), Duration.ofHours(24), Duration.ofHours(48)
            };
            for (Duration candidate : defaults) {
                Duration sanitized = rules.clampDuration(candidate);
                if (sanitized != null && !sanitized.isNegative() && !sanitized.isZero()) {
                    minutes.add(Math.max(1L, sanitized.toMinutes()));
                }
            }
            Duration defaultDuration = rules.clampDuration(rules.defaultDuration());
            if (defaultDuration != null && !defaultDuration.isNegative() && !defaultDuration.isZero()) {
                minutes.add(Math.max(1L, defaultDuration.toMinutes()));
            }
            Duration maxDuration = rules.maxDuration();
            if (maxDuration != null && !maxDuration.isNegative() && !maxDuration.isZero()) {
                minutes.add(Math.max(1L, maxDuration.toMinutes()));
            }
        }
        if (minutes.isEmpty()) minutes.add(Math.max(1L, Duration.ofHours(24).toMinutes()));
        Duration[] options = new Duration[minutes.size()];
        int index = 0;
        for (Long minute : minutes) {
            options[index++] = Duration.ofMinutes(Math.max(1L, minute));
        }
        return options;
    }

    private Duration resolveLongestDuration(Duration[] options, AuctionListingRules rules) {
        if (options != null && options.length > 0) return options[options.length - 1];
        Duration fallback = rules != null ? rules.maxDuration() : null;
        if (fallback == null || fallback.isNegative() || fallback.isZero()) {
            fallback = rules != null ? rules.defaultDuration() : null;
        }
        if (fallback == null || fallback.isNegative() || fallback.isZero()) fallback = Duration.ofHours(24);
        return fallback;
    }

    private int resolveDefaultDurationIndex(Duration[] options, Duration defaultDuration) {
        if (options == null || options.length == 0 || defaultDuration == null) return 0;
        long targetMinutes = Math.max(1L, defaultDuration.toMinutes());
        for (int i = 0; i < options.length; i++) {
            if (options[i] != null && Math.max(1L, options[i].toMinutes()) == targetMinutes) return i;
        }
        return 0;
    }
}
