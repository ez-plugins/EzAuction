package com.skyblockexp.ezauction.command;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.AuctionOperationResult;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistory;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistoryEntry;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.transaction.AuctionTransactionType;
import com.skyblockexp.ezauction.util.DateUtil;
import com.skyblockexp.ezauction.config.AuctionListingRules;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration;
import com.skyblockexp.ezauction.gui.AuctionMenu;
import com.skyblockexp.ezauction.gui.AuctionOrderMenu;
import com.skyblockexp.ezauction.gui.AuctionSellMenu;
import com.skyblockexp.ezauction.gui.LiveAuctionMenu;
import com.skyblockexp.ezauction.gui.SellMenuHolder.Target;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles the {@code /auction} command for browsing and managing auction listings.
 */
public class AuctionCommand implements CommandExecutor, TabCompleter {

    private static final int HISTORY_DISPLAY_LIMIT = 10;
    private static final DateTimeFormatter HISTORY_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, h:mm a")
            .withLocale(Locale.ENGLISH);

    private final AuctionManager auctionManager;
    private final AuctionMenu auctionMenu;
    private final AuctionOrderMenu auctionOrderMenu;
    private final AuctionSellMenu auctionSellMenu;
    private final AuctionTransactionHistory transactionHistory;
    private final AuctionTransactionService transactionService;
    private final AuctionListingRules listingRules;
    private final LiveAuctionMenu liveAuctionMenu;
    private final AuctionCommandMessageConfiguration messages;

    public AuctionCommand(AuctionManager auctionManager, AuctionMenu auctionMenu, AuctionSellMenu auctionSellMenu,
            AuctionOrderMenu auctionOrderMenu, AuctionTransactionHistory transactionHistory,
            AuctionTransactionService transactionService, AuctionListingRules listingRules,
            LiveAuctionMenu liveAuctionMenu, AuctionCommandMessageConfiguration messages) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.auctionMenu = Objects.requireNonNull(auctionMenu, "auctionMenu");
        this.auctionSellMenu = Objects.requireNonNull(auctionSellMenu, "auctionSellMenu");
        this.auctionOrderMenu = Objects.requireNonNull(auctionOrderMenu, "auctionOrderMenu");
        this.transactionHistory = Objects.requireNonNull(transactionHistory, "transactionHistory");
        this.transactionService = Objects.requireNonNull(transactionService, "transactionService");
        this.listingRules = Objects.requireNonNull(listingRules, "listingRules");
        this.liveAuctionMenu = Objects.requireNonNull(liveAuctionMenu, "liveAuctionMenu");
        this.messages = messages != null ? messages : AuctionCommandMessageConfiguration.defaults();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("ezauction.admin.reload")) {
                sendMessage(sender, ChatColor.RED + "You do not have permission to reload EzAuction.");
                return true;
            }
            
            try {
                // Reload configuration files
                if (EzAuctionPlugin.getStaticRegistry() != null) {
                    EzAuctionPlugin.getStaticRegistry().reloadConfiguration();
                    sendMessage(sender, ChatColor.GREEN + "EzAuction configuration reloaded successfully.");
                    sendMessage(sender, ChatColor.YELLOW + "Note: Some settings may require a server restart to fully apply.");
                } else {
                    sendMessage(sender, ChatColor.RED + "Failed to reload: Plugin registry not initialized.");
                }
            } catch (Exception ex) {
                sendMessage(sender, ChatColor.RED + "Failed to reload configuration: " + ex.getMessage());
                if (sender.hasPermission("ezauction.admin.debug")) {
                    ex.printStackTrace();
                }
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            sendMessage(sender, messages.general().consoleOnly());
            return true;
        }

        if (!player.hasPermission("ezauction.auction")) {
            sendMessage(player, messages.general().auctionNoPermission());
            return true;
        }

        if (args.length == 0 || (args.length == 1 && args[0].trim().isEmpty())) {
            auctionMenu.openBrowser(player);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ENGLISH);
        if (subcommand.equals("sell")) {
            handleSell(player, label, args);
            return true;
        }

        if (subcommand.equals("live")) {
            handleLive(player);
            return true;
        }

        if (subcommand.equals("order")) {
            handleOrder(player, label, args);
            return true;
        }

        if (subcommand.equals("cancel")) {
            handleCancel(player, args);
            return true;
        }

        if (subcommand.equals("history")) {
            handleHistory(player, args);
            return true;
        }

        if (subcommand.equals("claim")) {
            handleClaim(player);
            return true;
        }

        if (subcommand.equals("search")) {
            handleSearch(player, args);
            return true;
        }

        sendUsage(player, label);
        return true;
    }
    private static final int SEARCH_PAGE_SIZE = 10;

    private void handleSearch(Player player, String[] args) {
        if (!player.hasPermission("ezauction.auction.search")) {
            sendMessage(player, ChatColor.RED + "You do not have permission to search auctions.");
            return;
        }

        if (args.length < 2) {
            sendMessage(player, ChatColor.YELLOW + "Usage: /auction search <query>");
            return;
        }

        String query = args[1];
        // Set the search query for the player, then open the GUI
        java.util.UUID playerId = player.getUniqueId();
        // Use the same sanitization as the GUI
        String sanitized = org.bukkit.ChatColor.stripColor(query).trim();
        if (!sanitized.isEmpty()) {
            try {
                java.lang.reflect.Field field = auctionMenu.getClass().getDeclaredField("activeSearchQueries");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.concurrent.ConcurrentMap<java.util.UUID, String> map = (java.util.concurrent.ConcurrentMap<java.util.UUID, String>) field.get(auctionMenu);
                map.put(playerId, sanitized);
            } catch (Exception e) {
                // fallback: just open the GUI without filter
                auctionMenu.openBrowser(player);
                return;
            }
        }
        auctionMenu.openBrowser(player);
    }

    private void handleSell(Player player, String label, String[] args) {
        if (!player.hasPermission("ezauction.auction.sell")) {
            sendMessage(player, messages.sell().noPermission());
            return;
        }

        if (args.length < 2) {
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand == null || itemInHand.getType() == Material.AIR) {
                sendMessage(player, messages.sell().itemRequired());
                return;
            }
            auctionSellMenu.openSellMenu(player, Target.NORMAL);
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            sendMessage(player, messages.sell().itemRequired());
            return;
        }

        double price;
        try {
            price = com.skyblockexp.ezauction.util.NumberShortcutParser.parse(args[1]);
        } catch (NumberFormatException ex) {
            sendMessage(player, messages.sell().invalidPrice().replace("{price}", args[1]));
            return;
        }

        if (price <= 0) {
            sendMessage(player, messages.sell().pricePositive());
            return;
        }

        double minimumPrice = listingRules.minimumPrice();
        if (price < minimumPrice) {
            String formattedMinimum = transactionService.formatCurrency(minimumPrice);
            sendMessage(player, messages.sell().priceMinimum().replace("{minimum}", formattedMinimum));
            return;
        }

        Duration duration = listingRules.defaultDuration();
        if (args.length >= 3 && !args[2].isEmpty()) {
            Duration parsed = parseDuration(args[2]);
            if (parsed == null) {
                sendMessage(player, messages.general().invalidDuration());
                return;
            }
            duration = listingRules.clampDuration(parsed);
        }

        AuctionOperationResult result = auctionManager.createListing(player, itemInHand, price, duration);
        if (result.message() != null && !result.message().isEmpty()) {
            player.sendMessage(result.message());
        }
    }

    private void handleLive(Player player) {
        if (!player.hasPermission("ezauction.auction.live")) {
            sendMessage(player, messages.live().noPermission());
            return;
        }
        if (!auctionManager.liveAuctionsEnabled()) {
            sendMessage(player, messages.live().disabled());
            return;
        }
        liveAuctionMenu.open(player);
    }

    private void sendUsage(Player player, String label) {
        sendMessage(player, messages.usage().base().replace("{label}", label));
        sendMessage(player, messages.usage().live().replace("{label}", label));
        sendMessage(player, messages.usage().sell().replace("{label}", label));
        sendMessage(player, messages.usage().order().replace("{label}", label));
        sendMessage(player, messages.usage().cancel().replace("{label}", label));
        sendMessage(player, messages.usage().history().replace("{label}", label));
        sendMessage(player, messages.usage().claim().replace("{label}", label));
        sendMessage(player, ChatColor.YELLOW + "/" + label + " reload - Reload all EzAuction configuration files.");
        sendMessage(player, messages.usage().search().replace("{label}", label));
    }

    private void handleClaim(Player player) {
        AuctionOperationResult result = auctionManager.claimReturnItems(player);
        if (result.message() != null && !result.message().isEmpty()) {
            player.sendMessage(result.message());
        }
    }

    private void handleCancel(Player player, String[] args) {
        UUID sellerId = player.getUniqueId();
        List<AuctionListing> ownListings = new ArrayList<>();
        for (AuctionListing listing : auctionManager.listActiveListings()) {
            if (sellerId.equals(listing.sellerId())) {
                ownListings.add(listing);
            }
        }

        List<AuctionOrder> ownOrders = new ArrayList<>();
        for (AuctionOrder order : auctionManager.listActiveOrders()) {
            if (sellerId.equals(order.buyerId())) {
                ownOrders.add(order);
            }
        }

        if (args.length < 2 || args[1].trim().isEmpty()) {
            if (ownListings.isEmpty() && ownOrders.isEmpty()) {
                sendMessage(player, messages.cancel().nothingToCancel());
                return;
            }

            if (!ownListings.isEmpty()) {
                sendMessage(player, messages.cancel().listingsHeader());
                for (AuctionListing listing : ownListings) {
                    String priceText = transactionService.formatCurrency(listing.price());
                    String expiryText = DateUtil.formatDate(listing.expiryEpochMillis());
                    String message = messages.cancel().listingEntry()
                            .replace("{id}", listing.id())
                            .replace("{item}", describeItem(listing.item()))
                            .replace("{price}", priceText)
                            .replace("{expiry}", expiryText);
                    sendMessage(player, message);
                }
            }

            if (!ownOrders.isEmpty()) {
                sendMessage(player, messages.cancel().ordersHeader());
                for (AuctionOrder order : ownOrders) {
                    String priceText = transactionService.formatCurrency(order.offeredPrice());
                    String expiryText = DateUtil.formatDate(order.expiryEpochMillis());
                    String message = messages.cancel().orderEntry()
                            .replace("{id}", order.id())
                            .replace("{item}", describeItem(order.requestedItem()))
                            .replace("{price}", priceText)
                            .replace("{expiry}", expiryText);
                    sendMessage(player, message);
                }
            }
            sendMessage(player, messages.cancel().cancelHint());
            return;
        }

        String listingId = args[1];

        for (AuctionListing listing : ownListings) {
            if (listing.id().equals(listingId)) {
                AuctionOperationResult result = auctionManager.cancelListing(sellerId, listingId);
                if (result.message() != null && !result.message().isEmpty()) {
                    player.sendMessage(result.message());
                }
                return;
            }
        }

        for (AuctionOrder order : ownOrders) {
            if (order.id().equals(listingId)) {
                AuctionOperationResult result = auctionManager.cancelOrder(sellerId, listingId);
                if (result.message() != null && !result.message().isEmpty()) {
                    player.sendMessage(result.message());
                }
                return;
            }
        }

        AuctionOperationResult listingResult = auctionManager.cancelListing(sellerId, listingId);
        if (listingResult.success()) {
            if (listingResult.message() != null && !listingResult.message().isEmpty()) {
                player.sendMessage(listingResult.message());
            }
            return;
        }

        AuctionOperationResult orderResult = auctionManager.cancelOrder(sellerId, listingId);
        AuctionOperationResult response = (orderResult.message() != null && !orderResult.message().isEmpty())
                ? orderResult
                : listingResult;
        if (response.message() != null && !response.message().isEmpty()) {
            player.sendMessage(response.message());
        }
    }

    private void handleOrder(Player player, String label, String[] args) {
        if (!player.hasPermission("ezauction.auction.order")) {
            sendMessage(player, messages.order().noPermission());
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            sendMessage(player, messages.order().itemRequired());
            return;
        }

        if (args.length < 3) {
            auctionOrderMenu.openOrderMenu(player);
            return;
        }

        double pricePerItem;
        try {
            pricePerItem = com.skyblockexp.ezauction.util.NumberShortcutParser.parse(args[1]);
        } catch (NumberFormatException ex) {
            sendMessage(player, messages.order().invalidPrice().replace("{price}", args[1]));
            return;
        }
        if (pricePerItem <= 0.0D) {
            sendMessage(player, messages.order().pricePositive());
            return;
        }

        double minimumPrice = listingRules.minimumPrice();
        if (pricePerItem < minimumPrice) {
            String formattedMinimum = transactionService.formatCurrency(minimumPrice);
            sendMessage(player, messages.order().priceMinimum().replace("{minimum}", formattedMinimum));
            return;
        }

        int amount;
        try {
            amount = (int) com.skyblockexp.ezauction.util.NumberShortcutParser.parse(args[2]);
        } catch (NumberFormatException ex) {
            sendMessage(player, messages.order().invalidAmount().replace("{amount}", args[2]));
            return;
        }
        if (amount <= 0) {
            sendMessage(player, messages.order().amountMinimum());
            return;
        }

        int maxStack = Math.max(1, itemInHand.getMaxStackSize());
        if (amount > maxStack) {
            sendMessage(player, messages.order().amountMaxStack().replace("{max}", String.valueOf(maxStack)));
            return;
        }

        Duration duration = listingRules.defaultDuration();
        if (args.length >= 4 && !args[3].isEmpty()) {
            Duration parsed = parseDuration(args[3]);
            if (parsed == null) {
                sendMessage(player, messages.general().invalidDuration());
                return;
            }
            duration = listingRules.clampDuration(parsed);
        }

        ItemStack template = itemInHand.clone();
        template.setAmount(amount);
        double total = pricePerItem * amount;
        AuctionOperationResult result = auctionManager.createOrder(player, template, total, duration, total);
        if (result.message() != null && !result.message().isEmpty()) {
            player.sendMessage(result.message());
        }
    }

    private void handleHistory(Player player, String[] args) {
        if (!player.hasPermission("ezauction.auction.history") && !player.hasPermission("ezauction.auction")) {
            sendMessage(player, messages.history().noPermission());
            return;
        }

        AuctionTransactionType filter = null;
        if (args.length >= 2) {
            String filterArg = args[1].toLowerCase(Locale.ENGLISH);
            if (filterArg.equals("buy") || filterArg.equals("purchases")) {
                filter = AuctionTransactionType.BUY;
            } else if (filterArg.equals("sell") || filterArg.equals("sales")) {
                filter = AuctionTransactionType.SELL;
            } else {
                sendMessage(player, messages.history().unknownFilter());
                return;
            }
        }

        List<AuctionTransactionHistoryEntry> entries = new ArrayList<>(transactionHistory.getHistory(player.getUniqueId()));
        if (filter != null) {
            final AuctionTransactionType filterType = filter;
            entries.removeIf(entry -> entry.type() != filterType);
        }

        if (entries.isEmpty()) {
            if (filter == AuctionTransactionType.BUY) {
                sendMessage(player, messages.history().noPurchases());
            } else if (filter == AuctionTransactionType.SELL) {
                sendMessage(player, messages.history().noSales());
            } else {
                sendMessage(player, messages.history().noHistory());
            }
            return;
        }

        String heading;
        if (filter == AuctionTransactionType.BUY) {
            heading = messages.history().headingPurchases();
        } else if (filter == AuctionTransactionType.SELL) {
            heading = messages.history().headingSales();
        } else {
            heading = messages.history().headingActivity();
        }
        sendMessage(player, "&6" + heading + "&7:");

        int count = 0;
        for (AuctionTransactionHistoryEntry entry : entries) {
            if (count++ >= HISTORY_DISPLAY_LIMIT) {
                break;
            }
            String actionVerb = entry.type() == AuctionTransactionType.BUY
                    ? messages.history().actionBought()
                    : messages.history().actionSold();
            String counterpartLabel = entry.type() == AuctionTransactionType.BUY
                    ? messages.history().counterpartFrom()
                    : messages.history().counterpartTo();
            String counterpartName = resolveCounterpartName(entry);
            String priceText = transactionService.formatCurrency(entry.price());
            String dateText = HISTORY_DATE_FORMAT
                    .format(Instant.ofEpochMilli(entry.timestamp()).atZone(ZoneId.systemDefault()));
            String itemDescription = describeItem(entry.item());

            String message = messages.history().entry()
                    .replace("{action}", actionVerb)
                    .replace("{item}", itemDescription)
                    .replace("{counterpart-preposition}", counterpartLabel)
                    .replace("{counterpart}", counterpartName)
                    .replace("{price}", priceText)
                    .replace("{date}", dateText);
            sendMessage(player, message);
        }

        if (entries.size() > HISTORY_DISPLAY_LIMIT) {
            String truncated = messages.history().truncated()
                    .replace("{displayed}", String.valueOf(HISTORY_DISPLAY_LIMIT))
                    .replace("{total}", String.valueOf(entries.size()));
            sendMessage(player, truncated);
        }
    }

    private String resolveCounterpartName(AuctionTransactionHistoryEntry entry) {
        if (entry.counterpartName() != null && !entry.counterpartName().isEmpty()) {
            return entry.counterpartName();
        }
        if (entry.counterpartId() != null) {
            return entry.counterpartId().toString();
        }
        return messages.general().unknownCounterpart();
    }

    private String describeItem(ItemStack item) {
        if (item == null) {
            return messages.general().unknownItem();
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
        return amount + "x " + name;
    }

    private String friendlyMaterialName(Material material) {
        if (material == null) {
            return messages.general().unknownMaterial();
        }
        String lowercase = material.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        if (lowercase.isEmpty()) {
            return messages.general().unknownMaterial();
        }
        return Character.toUpperCase(lowercase.charAt(0)) + lowercase.substring(1);
    }

    private void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }
        sender.sendMessage(colorize(message));
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private Duration parseDuration(String input) {
        String trimmed = input.toLowerCase(Locale.ENGLISH).trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        long unitSeconds = 3600; // default hours
        char last = trimmed.charAt(trimmed.length() - 1);
        String numericPortion = trimmed;

        switch (last) {
            case 'd':
                unitSeconds = 86400;
                numericPortion = trimmed.substring(0, trimmed.length() - 1);
                break;
            case 'h':
                unitSeconds = 3600;
                numericPortion = trimmed.substring(0, trimmed.length() - 1);
                break;
            case 'm':
                unitSeconds = 60;
                numericPortion = trimmed.substring(0, trimmed.length() - 1);
                break;
            case 's':
                unitSeconds = 1;
                numericPortion = trimmed.substring(0, trimmed.length() - 1);
                break;
            default:
                // no suffix provided, treat the whole string as hours
                unitSeconds = 3600;
                break;
        }

        if (numericPortion.isEmpty()) {
            return null;
        }

        double value;
        try {
            value = Double.parseDouble(numericPortion);
        } catch (NumberFormatException ex) {
            return null;
        }

        if (value <= 0) {
            return null;
        }

        long seconds = Math.round(value * unitSeconds);
        if (seconds <= 0) {
            return null;
        }

        try {
            return Duration.ofSeconds(seconds);
        } catch (ArithmeticException ex) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(args[0], List.of("sell", "order", "cancel", "history", "claim", "live"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            return filter(args[1], List.of("1000", "2500", "5000", "10000"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("order")) {
            return filter(args[1], List.of("100", "250", "500", "1000"));
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("order")) {
            return filter(args[2], List.of("1", "16", "32", "64"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("cancel") && sender instanceof Player player) {
            UUID sellerId = player.getUniqueId();
            List<String> ids = new ArrayList<>();
            for (AuctionListing listing : auctionManager.listActiveListings()) {
                if (sellerId.equals(listing.sellerId())) {
                    ids.add(listing.id());
                }
            }
            for (AuctionOrder order : auctionManager.listActiveOrders()) {
                if (sellerId.equals(order.buyerId())) {
                    ids.add(order.id());
                }
            }
            return filter(args[1], ids);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("sell")) {
            return filter(args[2], List.of("30m", "1h", "6h", "12h", "24h", "2d"));
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("order")) {
            return filter(args[3], List.of("30m", "1h", "6h", "12h", "24h", "2d"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("history")) {
            return filter(args[1], List.of("buy", "sell"));
        }

        return Collections.emptyList();
    }

    private List<String> filter(String current, List<String> completions) {
        if (current == null || current.isEmpty()) {
            return completions;
        }

        String lowerCurrent = current.toLowerCase(Locale.ENGLISH);
        List<String> matches = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase(Locale.ENGLISH).startsWith(lowerCurrent)) {
                matches.add(completion);
            }
        }
        return matches;
    }
}
