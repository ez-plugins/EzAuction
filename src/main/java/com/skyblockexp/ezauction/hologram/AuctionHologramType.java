package com.skyblockexp.ezauction.hologram;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import java.time.Duration;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;

/**
 * Enumerates the supported EzAuction hologram types.
 */
public enum AuctionHologramType {

    ACTIVE_LISTINGS("Active Listings", "listings") {
        @Override
        Component render(AuctionManager manager, AuctionTransactionService transactionService) {
            long count = manager.countActiveListings();
            String noun = count == 1 ? "listing" : "listings";
            return Component.text()
                    .append(title("Active Listings"))
                    .append(Component.newline())
                    .append(Component.text(String.format(Locale.US, "%d %s", count, noun), NamedTextColor.GREEN))
                    .build();
        }
    },

    ACTIVE_ORDERS("Active Orders", "orders") {
        @Override
        Component render(AuctionManager manager, AuctionTransactionService transactionService) {
            long count = manager.countActiveOrders();
            String noun = count == 1 ? "order" : "orders";
            return Component.text()
                    .append(title("Active Orders"))
                    .append(Component.newline())
                    .append(Component.text(String.format(Locale.US, "%d %s", count, noun), NamedTextColor.GREEN))
                    .build();
        }
    },

    HIGHEST_LISTING("Top Listing", "top", "listing") {
        @Override
        Component render(AuctionManager manager, AuctionTransactionService transactionService) {
            AuctionListing listing = manager.findHighestPricedListing();
            TextComponent.Builder builder = Component.text().append(title("Top Listing"));
            if (listing == null) {
                builder.append(Component.newline())
                        .append(Component.text("No active listings", NamedTextColor.GRAY));
                return builder.build();
            }
            builder.append(Component.newline())
                    .append(Component.text(describeItem(listing.item()), NamedTextColor.AQUA))
                    .append(Component.newline())
                    .append(Component.text(transactionService.formatCurrency(listing.price()), NamedTextColor.GOLD));
            return builder.build();
        }
    },

    BEST_BUY_ORDER("Best Buy Order", "buy", "order") {
        @Override
        Component render(AuctionManager manager, AuctionTransactionService transactionService) {
            AuctionOrder order = manager.findHighestPricedOrder();
            TextComponent.Builder builder = Component.text().append(title("Best Buy Order"));
            if (order == null) {
                builder.append(Component.newline())
                        .append(Component.text("No active orders", NamedTextColor.GRAY));
                return builder.build();
            }
            builder.append(Component.newline())
                    .append(Component.text(describeItem(order.requestedItem()), NamedTextColor.AQUA))
                    .append(Component.newline())
                    .append(Component.text(transactionService.formatCurrency(order.offeredPrice()), NamedTextColor.GOLD));
            return builder.build();
        }
    },

    NEXT_EXPIRING("Next Expiring", "expiring", "expires") {
        @Override
        Component render(AuctionManager manager, AuctionTransactionService transactionService) {
            AuctionListing listing = manager.findNextExpiringListing();
            TextComponent.Builder builder = Component.text().append(title("Next Expiring"));
            if (listing == null) {
                builder.append(Component.newline())
                        .append(Component.text("No active listings", NamedTextColor.GRAY));
                return builder.build();
            }
            long remainingMillis = listing.expiryEpochMillis() - System.currentTimeMillis();
            Duration remaining = remainingMillis > 0 ? Duration.ofMillis(remainingMillis) : Duration.ZERO;
            builder.append(Component.newline())
                    .append(Component.text(describeItem(listing.item()), NamedTextColor.AQUA))
                    .append(Component.newline())
                    .append(Component.text("Ends in " + formatDuration(remaining), NamedTextColor.YELLOW));
            return builder.build();
        }
    };

    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    private final String displayName;
    private final String[] aliases;

    AuctionHologramType(String displayName, String... aliases) {
        this.displayName = displayName;
        this.aliases = aliases != null ? aliases.clone() : new String[0];
    }

    public String displayName() {
        return displayName;
    }

    public String[] aliases() {
        return aliases.clone();
    }

    abstract Component render(AuctionManager manager, AuctionTransactionService transactionService);

    public static AuctionHologramType fromName(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ENGLISH).replace('-', '_').replace(' ', '_');
        for (AuctionHologramType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
            for (String alias : type.aliases) {
                if (alias.equalsIgnoreCase(input)) {
                    return type;
                }
                if (alias.equalsIgnoreCase(normalized)) {
                    return type;
                }
            }
        }
        return null;
    }

    private static Component title(String value) {
        return Component.text(value, NamedTextColor.GOLD, TextDecoration.BOLD);
    }

    private static String describeItem(ItemStack item) {
        if (item == null) {
            return "Unknown item";
        }
        int amount = Math.max(1, item.getAmount());
        String baseName = friendlyMaterialName(item);
        if (item.hasItemMeta()) {
            var meta = item.getItemMeta();
            if (meta != null) {
                if (meta.hasDisplayName()) {
                    String legacyName = meta.getDisplayName();
                    if (legacyName != null && !legacyName.isBlank()) {
                        baseName = legacyName;
                    }
                }
                Component component = meta.displayName();
                if (component != null) {
                    String plain = PLAIN_SERIALIZER.serialize(component).trim();
                    if (!plain.isEmpty()) {
                        baseName = plain;
                    }
                }
            }
        }
        return amount + "x " + baseName;
    }

    private static String friendlyMaterialName(ItemStack item) {
        if (item == null || item.getType() == null) {
            return "Unknown";
        }
        String name = item.getType().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        if (name.isEmpty()) {
            return "Unknown";
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return "now";
        }
        long seconds = duration.getSeconds();
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        if (days > 0) {
            return String.format(Locale.US, "%dd %dh", days, hours);
        }
        if (hours > 0) {
            return String.format(Locale.US, "%dh %dm", hours, minutes);
        }
        if (minutes > 0) {
            return String.format(Locale.US, "%dm", minutes);
        }
        return String.format(Locale.US, "%ds", Math.max(1, seconds % 60));
    }
}
