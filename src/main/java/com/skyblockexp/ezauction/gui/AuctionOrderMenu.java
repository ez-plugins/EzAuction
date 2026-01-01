package com.skyblockexp.ezauction.gui;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.AuctionOperationResult;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.compat.ItemTagStorage;
import com.skyblockexp.ezauction.config.AuctionListingRules;
import com.skyblockexp.ezauction.config.AuctionMenuInteractionConfiguration;
import com.skyblockexp.ezauction.config.AuctionMessageConfiguration;
import com.skyblockexp.ezauction.util.EconomyUtils;
import com.skyblockexp.ezauction.util.ItemValueProvider;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Interactive menu that guides players through creating auction buy orders.
 */
public class AuctionOrderMenu implements Listener {

    private static final String ACTION_PRICE_ADJUST = "price_adjust";
    private static final String ACTION_PRICE_CUSTOM = "price_custom";
    private static final String ACTION_QUANTITY_ADJUST = "quantity_adjust";
    private static final String ACTION_QUANTITY_CUSTOM = "quantity_custom";
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
    private final String quantityAdjustKey;
    private final ItemTagStorage itemTagStorage;

    private final AuctionMenuInteractionConfiguration.OrderMenuLayoutConfiguration layout;
    private final ItemStack fillerPane;

    private final ConcurrentMap<UUID, OrderMenuState> pendingPriceInputs;
    private final ConcurrentMap<UUID, OrderMenuState> pendingQuantityInputs;

    private final Duration[] durationOptions;
    private final int defaultDurationIndex;
    private final double minimumPricePerItem;
    private final Duration longestDurationOption;
    private final double defaultPricePerItem;
    private final double[] priceAdjustments;
    private final int[] quantityAdjustments;
    private final AuctionMessageConfiguration.OrderMessages messages;

    public AuctionOrderMenu(JavaPlugin plugin, AuctionManager auctionManager,
            AuctionTransactionService transactionService, AuctionListingRules listingRules,
            List<Duration> configuredDurationOptions,
            AuctionMenuInteractionConfiguration.OrderMenuInteractionConfiguration orderConfiguration,
            ItemValueProvider itemValueProvider, AuctionMessageConfiguration.OrderMessages messages,
            ItemTagStorage itemTagStorage) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.transactionService = Objects.requireNonNull(transactionService, "transactionService");
        this.listingRules = Objects.requireNonNull(listingRules, "listingRules");
        this.itemValueProvider = itemValueProvider != null ? itemValueProvider : ItemValueProvider.none();
        this.messages = messages != null ? messages : AuctionMessageConfiguration.OrderMessages.defaults();
        this.actionKey = "auction_order_action";
        this.priceAdjustKey = "auction_order_price_adjust";
        this.quantityAdjustKey = "auction_order_quantity_adjust";
        this.itemTagStorage = Objects.requireNonNull(itemTagStorage, "itemTagStorage");
        this.pendingPriceInputs = new ConcurrentHashMap<>();
        this.pendingQuantityInputs = new ConcurrentHashMap<>();
        this.durationOptions = buildDurationOptions(listingRules, configuredDurationOptions);
        this.defaultDurationIndex = resolveDefaultDurationIndex(durationOptions, listingRules.defaultDuration());
        this.minimumPricePerItem = Math.max(0.0D, listingRules.minimumPrice());
        this.longestDurationOption = resolveLongestDuration(durationOptions, listingRules);
        AuctionMenuInteractionConfiguration.OrderMenuInteractionConfiguration interactions = orderConfiguration != null
                ? orderConfiguration
                : AuctionMenuInteractionConfiguration.defaults().orderMenu();
        this.layout = interactions.layout();
        this.fillerPane = createBaseItem(layout.filler());
        this.defaultPricePerItem = Math.max(0.0D, interactions.defaultPricePerItem());
        List<Double> priceAdjustmentValues = interactions.priceAdjustments();
        this.priceAdjustments = new double[priceAdjustmentValues.size()];
        for (int i = 0; i < priceAdjustmentValues.size(); i++) {
            Double value = priceAdjustmentValues.get(i);
            this.priceAdjustments[i] = value != null ? value.doubleValue() : 0.0D;
        }
        List<Integer> quantityAdjustmentValues = interactions.quantityAdjustments();
        this.quantityAdjustments = new int[quantityAdjustmentValues.size()];
        for (int i = 0; i < quantityAdjustmentValues.size(); i++) {
            Integer value = quantityAdjustmentValues.get(i);
            this.quantityAdjustments[i] = value != null ? value.intValue() : 0;
        }
    }

    /**
     * Opens the buy order menu for the item in the player's main hand.
     */
    public void openOrderMenu(Player player) {
        if (player == null) {
            return;
        }
        if (!player.hasPermission("ezauction.auction.order")) {
            sendMessage(player, messages.noPermission());
            return;
        }
        ItemStack template = player.getInventory().getItemInMainHand();
        if (template == null || template.getType() == Material.AIR) {
            sendMessage(player, messages.itemRequired());
            return;
        }
        ItemStack base = template.clone();
        base.setAmount(Math.max(1, Math.min(template.getAmount(), base.getMaxStackSize())));
        double startingPrice = Math.max(minimumPricePerItem, defaultPricePerItem);
        Double recommendedPricePerItem = null;
        OptionalDouble estimate = itemValueProvider.estimate(base);
        int baseAmount = Math.max(1, base.getAmount());
        if (estimate.isPresent() && estimate.getAsDouble() > 0.0D) {
            double perItem = estimate.getAsDouble() / baseAmount;
            if (perItem > 0.0D) {
                double normalized = EconomyUtils
                        .normalizeCurrency(Math.max(minimumPricePerItem, perItem));
                if (normalized > 0.0D) {
                    recommendedPricePerItem = normalized;
                    startingPrice = normalized;
                }
            }
        }
        OrderMenuState state = new OrderMenuState(base, startingPrice, base.getAmount(), defaultDurationIndex,
                recommendedPricePerItem);
        openOrderMenu(player, state);
    }

    private void openOrderMenu(Player player, OrderMenuState state) {
        if (player == null || state == null) {
            return;
        }
        OrderMenuHolder holder = new OrderMenuHolder(player.getUniqueId(), state);
        Inventory inventory = Bukkit.createInventory(holder, layout.size(), colorize(layout.title()));
        holder.setInventory(inventory);
        refreshMenu(holder);
        player.openInventory(inventory);
    }

    private void refreshMenu(OrderMenuHolder holder) {
        Inventory inventory = holder.getInventory();
        if (inventory == null) {
            return;
        }
        inventory.clear();
        applyFiller(inventory);

        placePriceAdjustmentButtons(inventory);
        placeQuantityAdjustmentButtons(inventory);

        ItemStack requested = holder.state().item();
        ItemMeta meta = requested.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Price per Item: " + ChatColor.GOLD + formatPrice(holder.state().pricePerItem()));
        Double recommended = holder.state().recommendedPricePerItem();
        if (recommended != null) {
            lore.add(ChatColor.GRAY + "Recommended per Item: " + ChatColor.GOLD + formatPrice(recommended));
            double recommendedTotal = EconomyUtils.normalizeCurrency(recommended * holder.state().quantity());
            lore.add(ChatColor.GRAY + "Recommended Total: " + ChatColor.GOLD + formatPrice(recommendedTotal));
        }
        lore.add(ChatColor.GRAY + "Requested Quantity: " + ChatColor.AQUA + holder.state().quantity());
        lore.add(ChatColor.GRAY + "Total Offer: " + ChatColor.GOLD + formatPrice(holder.state().totalPrice()));
        lore.add(ChatColor.GRAY + "Duration: " + ChatColor.YELLOW + formatDuration(holder.state().duration()));
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Confirm below to create the buy order.");
        if (meta != null) {
            List<String> existingLore = meta.hasLore() ? meta.getLore() : List.of();
            List<String> combined = new ArrayList<>(existingLore);
            if (!combined.isEmpty()) {
                combined.add(" ");
            }
            combined.addAll(lore);
            meta.setLore(combined);
            requested.setItemMeta(meta);
        }
        inventory.setItem(layout.itemSlot(), requested);

        ItemStack priceDisplay = createPriceDisplay(holder);
        inventory.setItem(layout.priceDisplay().slot(), priceDisplay);

        ItemStack quantityDisplay = createQuantityDisplay(holder);
        setPersistent(quantityDisplay, actionKey, ACTION_QUANTITY_CUSTOM);
        inventory.setItem(layout.quantityDisplay().slot(), quantityDisplay);

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
            if (amount == 0.0D) {
                continue;
            }
            ItemStack button = createPriceAdjustButton(amount);
            setPersistent(button, actionKey, ACTION_PRICE_ADJUST);
            setPersistent(button, priceAdjustKey, amount);
            inventory.setItem(slots[i], button);
        }
    }

    private void placeQuantityAdjustmentButtons(Inventory inventory) {
        int[] adjustments = quantityAdjustments;
        int[] slots = layout.quantityAdjustmentSlots();
        for (int i = 0; i < adjustments.length && i < slots.length; i++) {
            int amount = adjustments[i];
            if (amount == 0) {
                continue;
            }
            ItemStack button = createQuantityAdjustButton(amount);
            setPersistent(button, actionKey, ACTION_QUANTITY_ADJUST);
            setPersistent(button, quantityAdjustKey, amount);
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

    private ItemStack createPriceDisplay(OrderMenuHolder holder) {
        AuctionMenuInteractionConfiguration.ButtonLayoutConfiguration definition = layout.priceDisplay();
        ItemStack item = createBaseItem(definition.button());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = definition.button().displayName();
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(colorize(displayName));
            }
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Price per Item: " + ChatColor.GOLD + formatPrice(holder.state().pricePerItem()));
            lore.add(ChatColor.GRAY + "Total Offer: " + ChatColor.GOLD + formatPrice(holder.state().totalPrice()));
            Double recommended = holder.state().recommendedPricePerItem();
            if (recommended != null) {
                lore.add(ChatColor.GRAY + "Recommended per Item: " + ChatColor.GOLD + formatPrice(recommended));
                double recommendedTotal = EconomyUtils.normalizeCurrency(recommended * holder.state().quantity());
                lore.add(ChatColor.GRAY + "Recommended Total: " + ChatColor.GOLD + formatPrice(recommendedTotal));
            }
            lore.add(ChatColor.GRAY + "Use the buttons above to adjust.");
            if (minimumPricePerItem > 0.0D) {
                lore.add(ChatColor.GRAY + "Minimum Price: " + ChatColor.GOLD + formatPrice(minimumPricePerItem));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createQuantityDisplay(OrderMenuHolder holder) {
        AuctionMenuInteractionConfiguration.ButtonLayoutConfiguration definition = layout.quantityDisplay();
        ItemStack item = createBaseItem(definition.button());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = definition.button().displayName();
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(colorize(displayName));
            }
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Current Quantity: " + ChatColor.AQUA + holder.state().quantity());
            lore.add(ChatColor.GRAY + "Max Stack Size: " + ChatColor.AQUA + holder.state().maxQuantity());
            lore.add(ChatColor.GRAY + "Use the buttons around to adjust.");
            lore.add(ChatColor.GRAY + "Click to enter a custom amount.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDurationDisplay(OrderMenuHolder holder) {
        AuctionMenuInteractionConfiguration.ButtonLayoutConfiguration definition = layout.durationDisplay();
        ItemStack item = createBaseItem(definition.button());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = definition.button().displayName();
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(colorize(displayName));
            }
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Current Duration: " + ChatColor.YELLOW
                    + formatDuration(holder.state().duration()));
            lore.add(ChatColor.GRAY + "Click to cycle through available durations.");
            lore.add(ChatColor.GRAY + "Longest Option: " + ChatColor.YELLOW
                    + formatDuration(longestDurationOption));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPriceAdjustButton(double amount) {
        boolean positive = amount > 0;
        Material material = positive ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        String prefix = positive ? ChatColor.GREEN + "+" : ChatColor.RED + "";
        ItemStack item = material != null ? new ItemStack(material) : null;
        if (item == null) {
            item = new ItemStack(Material.STONE);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String label = prefix + formatPrice(Math.abs(amount));
            meta.setDisplayName(label);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Adjust the price per item by this amount.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createQuantityAdjustButton(int amount) {
        boolean positive = amount > 0;
        Material material = positive ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        ItemStack item = material != null ? new ItemStack(material) : null;
        if (item == null) {
            item = new ItemStack(Material.STONE);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String label = (positive ? ChatColor.GREEN + "+" : ChatColor.RED + "") + Math.abs(amount);
            meta.setDisplayName(label + ChatColor.GRAY + " quantity");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Adjust the requested quantity by this amount.");
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
            meta.setLore(List.of(ChatColor.GRAY + "Enter a custom price per item."));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createConfirmButton(OrderMenuHolder holder) {
        AuctionMenuInteractionConfiguration.ButtonLayoutConfiguration definition = layout.confirmButton();
        ItemStack item = createBaseItem(definition.button());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = definition.button().displayName();
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(colorize(displayName));
            }
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Request: " + ChatColor.AQUA + describeItem(holder.state().item()));
            lore.add(ChatColor.GRAY + "Quantity: " + ChatColor.AQUA + holder.state().quantity());
            lore.add(ChatColor.GRAY + "Total Offer: " + ChatColor.GOLD + formatPrice(holder.state().totalPrice()));
            Double recommended = holder.state().recommendedPricePerItem();
            if (recommended != null) {
                lore.add(ChatColor.GRAY + "Recommended per Item: " + ChatColor.GOLD + formatPrice(recommended));
                double recommendedTotal = EconomyUtils.normalizeCurrency(recommended * holder.state().quantity());
                lore.add(ChatColor.GRAY + "Recommended Total: " + ChatColor.GOLD + formatPrice(recommendedTotal));
            }
            lore.add(ChatColor.GRAY + "Duration: " + ChatColor.YELLOW + formatDuration(holder.state().duration()));
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
            meta.setLore(List.of(ChatColor.GRAY + "Return to the previous menu."));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void applyFiller(Inventory inventory) {
        if (fillerPane == null) {
            return;
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, fillerPane.clone());
            }
        }
    }

    private String colorize(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private void sendMessage(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        player.sendMessage(colorize(message));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof OrderMenuHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!holder.owner().equals(player.getUniqueId())) {
            return;
        }
        int topSize = topInventory.getSize();
        boolean affectsTop = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (affectsTop) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof OrderMenuHolder holder)) {
            return;
        }
        if (!Objects.equals(holder.owner(), event.getWhoClicked().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        String action = itemTagStorage.getString(clicked, actionKey);
        if (action == null) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        switch (action) {
            case ACTION_PRICE_ADJUST -> handlePriceAdjust(player, holder, clicked);
            case ACTION_PRICE_CUSTOM -> startCustomPriceInput(player, holder);
            case ACTION_QUANTITY_ADJUST -> handleQuantityAdjust(player, holder, clicked);
            case ACTION_QUANTITY_CUSTOM -> startCustomQuantityInput(player, holder);
            case ACTION_DURATION_NEXT -> {
                holder.state().cycleDuration();
                refreshMenu(holder);
            }
            case ACTION_CONFIRM -> handleConfirm(player, holder);
            case ACTION_CANCEL -> player.closeInventory();
            default -> {
            }
        }
    }

    private void handlePriceAdjust(Player player, OrderMenuHolder holder, ItemStack clicked) {
        Double amount = itemTagStorage.getDouble(clicked, priceAdjustKey);
        if (amount == null) {
            return;
        }
        double newPrice = EconomyUtils.normalizeCurrency(holder.state().pricePerItem() + amount);
        if (newPrice < minimumPricePerItem) {
            newPrice = minimumPricePerItem;
        }
        holder.state().setPricePerItem(newPrice);
        refreshMenu(holder);
    }

    private void handleQuantityAdjust(Player player, OrderMenuHolder holder, ItemStack clicked) {
        Integer amount = itemTagStorage.getInt(clicked, quantityAdjustKey);
        if (amount == null) {
            return;
        }
        holder.state().adjustQuantity(amount);
        refreshMenu(holder);
    }

    private void startCustomPriceInput(Player player, OrderMenuHolder holder) {
        UUID playerId = player.getUniqueId();
        pendingPriceInputs.put(playerId, holder.state());
        pendingQuantityInputs.remove(playerId);
        player.closeInventory();
        sendMessage(player, messages.pricePrompt());
    }

    private void startCustomQuantityInput(Player player, OrderMenuHolder holder) {
        UUID playerId = player.getUniqueId();
        pendingQuantityInputs.put(playerId, holder.state());
        pendingPriceInputs.remove(playerId);
        player.closeInventory();
        sendMessage(player, messages.quantityPrompt());
    }

    private void handleConfirm(Player player, OrderMenuHolder holder) {
        if (!player.hasPermission("ezauction.auction.order")) {
            sendMessage(player, messages.noPermission());
            player.closeInventory();
            return;
        }
        OrderMenuState state = holder.state();
        if (state.quantity() <= 0) {
            sendMessage(player, messages.quantityMinimum());
            refreshMenu(holder);
            return;
        }
        if (state.pricePerItem() < minimumPricePerItem) {
            sendMessage(player, messages.priceMinimum().replace("{minimum}", formatPrice(minimumPricePerItem)));
            refreshMenu(holder);
            return;
        }
        double total = EconomyUtils.normalizeCurrency(state.totalPrice());
        if (total <= 0.0D) {
            sendMessage(player, messages.totalMustBePositive());
            refreshMenu(holder);
            return;
        }
        AuctionOperationResult result = auctionManager.createOrder(player, state.item(), total, state.duration(), total);
        if (result.message() != null && !result.message().isEmpty()) {
            player.sendMessage(result.message());
        }
        if (result.success()) {
            player.closeInventory();
        } else {
            refreshMenu(holder);
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        OrderMenuState priceState = pendingPriceInputs.get(playerId);
        OrderMenuState quantityState = pendingQuantityInputs.get(playerId);
        if (priceState == null && quantityState == null) {
            return;
        }
        event.setCancelled(true);
        pendingPriceInputs.remove(playerId);
        pendingQuantityInputs.remove(playerId);
        OrderMenuState targetState = priceState != null ? priceState : quantityState;
        boolean updatingPrice = priceState != null;
        plugin.getServer().getScheduler().runTask(plugin,
                () -> handleChatInput(event.getPlayer(), targetState, event.getMessage(), updatingPrice));
    }

    private void handleChatInput(Player player, OrderMenuState state, String message, boolean updatingPrice) {
        if (player == null || state == null) {
            return;
        }
        String input = message == null ? "" : message.trim();
        if (input.equalsIgnoreCase("cancel")) {
            sendMessage(player, messages.entryCancelled());
            openOrderMenu(player, state);
            return;
        }
        if (updatingPrice) {
            double value;
            try {
                value = Double.parseDouble(input);
            } catch (NumberFormatException ex) {
                sendMessage(player, messages.invalidNumber());
                openOrderMenu(player, state);
                return;
            }
            if (value <= 0.0D) {
                sendMessage(player, messages.priceMustBePositive());
                openOrderMenu(player, state);
                return;
            }
            double normalized = EconomyUtils.normalizeCurrency(value);
            if (normalized < minimumPricePerItem) {
                normalized = minimumPricePerItem;
            }
            state.setPricePerItem(normalized);
            sendMessage(player, messages.priceUpdated().replace("{price}", formatPrice(normalized)));
            openOrderMenu(player, state);
        } else {
            int quantity;
            try {
                quantity = Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                sendMessage(player, messages.wholeNumberRequired());
                openOrderMenu(player, state);
                return;
            }
            if (quantity <= 0) {
                sendMessage(player, messages.quantityMinimum());
                openOrderMenu(player, state);
                return;
            }
            state.setQuantity(quantity);
            sendMessage(player, messages.quantityUpdated().replace("{quantity}", String.valueOf(state.quantity())));
            openOrderMenu(player, state);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        pendingPriceInputs.remove(playerId);
        pendingQuantityInputs.remove(playerId);
    }

    private Duration[] buildDurationOptions(AuctionListingRules rules, List<Duration> configuredOptions) {
        Objects.requireNonNull(rules, "rules");
        TreeSet<Long> minutes = new TreeSet<>();
        boolean usedConfigured = false;
        if (configuredOptions != null) {
            for (Duration option : configuredOptions) {
                if (option == null || option.isNegative() || option.isZero()) {
                    continue;
                }
                Duration sanitized = rules.clampDuration(option);
                if (sanitized != null && !sanitized.isNegative() && !sanitized.isZero()) {
                    minutes.add(Math.max(1L, sanitized.toMinutes()));
                    usedConfigured = true;
                }
            }
        }
        if (!usedConfigured) {
            Duration[] defaults = new Duration[] {
                Duration.ofHours(6),
                Duration.ofHours(12),
                Duration.ofHours(24),
                Duration.ofHours(48)
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
        if (minutes.isEmpty()) {
            minutes.add(Math.max(1L, Duration.ofHours(24).toMinutes()));
        }
        Duration[] options = new Duration[minutes.size()];
        int index = 0;
        for (Long minute : minutes) {
            options[index++] = Duration.ofMinutes(Math.max(1L, minute));
        }
        return options;
    }

    private Duration resolveLongestDuration(Duration[] options, AuctionListingRules rules) {
        if (options != null && options.length > 0) {
            return options[options.length - 1];
        }
        Duration fallback = rules != null ? rules.maxDuration() : null;
        if (fallback == null || fallback.isNegative() || fallback.isZero()) {
            fallback = rules != null ? rules.defaultDuration() : null;
        }
        if (fallback == null || fallback.isNegative() || fallback.isZero()) {
            fallback = Duration.ofHours(24);
        }
        return fallback;
    }

    private int resolveDefaultDurationIndex(Duration[] options, Duration defaultDuration) {
        if (options == null || options.length == 0 || defaultDuration == null) {
            return 0;
        }
        long targetMinutes = Math.max(1L, defaultDuration.toMinutes());
        for (int i = 0; i < options.length; i++) {
            if (options[i] != null && Math.max(1L, options[i].toMinutes()) == targetMinutes) {
                return i;
            }
        }
        return 0;
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

    private void setPersistent(ItemStack item, String key, int value) {
        if (item == null || key == null) {
            return;
        }
        itemTagStorage.setInt(item, key, value);
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
        if (days > 0) {
            parts.add(days + "d");
        }
        if (hours > 0) {
            parts.add(hours + "h");
        }
        if (minutes > 0 && days == 0) {
            parts.add(minutes + "m");
        }
        if (parts.isEmpty()) {
            parts.add("0m");
        }
        return String.join(" ", parts);
    }

    private String describeItem(ItemStack item) {
        if (item == null) {
            return "Unknown";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return ChatColor.stripColor(meta.getDisplayName());
        }
        return formatMaterialName(item.getType());
    }

    private String formatMaterialName(Material material) {
        if (material == null) {
            return "Unknown";
        }
        String[] parts = material.name().toLowerCase(Locale.ENGLISH).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private abstract static class AbstractOrderHolder implements InventoryHolder {

        private final UUID owner;
        private Inventory inventory;

        protected AbstractOrderHolder(UUID owner) {
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

    private static final class OrderMenuHolder extends AbstractOrderHolder {

        private final OrderMenuState state;

        private OrderMenuHolder(UUID owner, OrderMenuState state) {
            super(owner);
            this.state = state;
        }

        public OrderMenuState state() {
            return state;
        }
    }

    private final class OrderMenuState {

        private final ItemStack template;
        private final int maxQuantity;
        private double pricePerItem;
        private int quantity;
        private int durationIndex;
        private final Double recommendedPricePerItem;

        private OrderMenuState(ItemStack template, double pricePerItem, int quantity, int durationIndex,
                Double recommendedPricePerItem) {
            this.template = template.clone();
            this.template.setAmount(1);
            this.maxQuantity = Math.max(1, template.getMaxStackSize());
            this.pricePerItem = pricePerItem;
            setQuantity(quantity);
            this.durationIndex = normalizeIndex(durationIndex);
            this.recommendedPricePerItem = recommendedPricePerItem;
        }

        public ItemStack item() {
            ItemStack clone = template.clone();
            clone.setAmount(quantity);
            return clone;
        }

        public double pricePerItem() {
            return pricePerItem;
        }

        public void setPricePerItem(double pricePerItem) {
            this.pricePerItem = pricePerItem;
        }

        public Double recommendedPricePerItem() {
            return recommendedPricePerItem;
        }

        public int quantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            int clamped = Math.max(1, Math.min(quantity, maxQuantity));
            this.quantity = clamped;
        }

        public void adjustQuantity(int delta) {
            setQuantity(quantity + delta);
        }

        public int maxQuantity() {
            return maxQuantity;
        }

        public double totalPrice() {
            return pricePerItem * quantity;
        }

        public Duration duration() {
            if (durationOptions.length == 0) {
                return listingRules.defaultDuration();
            }
            return durationOptions[durationIndex];
        }

        public void cycleDuration() {
            if (durationOptions.length == 0) {
                return;
            }
            durationIndex = (durationIndex + 1) % durationOptions.length;
        }

        private int normalizeIndex(int requested) {
            if (durationOptions.length == 0) {
                return 0;
            }
            if (requested < 0 || requested >= durationOptions.length) {
                return Math.min(defaultDurationIndex, durationOptions.length - 1);
            }
            return requested;
        }
    }
}
