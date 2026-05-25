package com.skyblockexp.ezauction.live.component;

import com.skyblockexp.ezauction.live.model.LiveAuctionState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds Adventure {@link Component} instances for live auction chat announcements and notifications.
 *
 * <p>Provides rich hover previews for items (showing enchantments, potion effects, and shulker
 * contents) and clickable action buttons.</p>
 */
public final class LiveAuctionComponentBuilder {

    private LiveAuctionComponentBuilder() {}

    // ---- Public API ----

    /**
     * Builds the main announcement component sent to all online players when a new live auction starts.
     * Contains item hover preview, a clickable [Join] button, and a clickable [Bid $X] button.
     *
     * @param state          the active auction state
     * @param formattedPrice starting price already formatted as a currency string
     * @param minimumBid        minimum accepted first bid, already formatted
     * @param rawMinimumBid     minimum accepted first bid as a raw number (used for click-event command)
     */
    public static Component buildAnnouncementComponent(LiveAuctionState state,
            String formattedPrice, String minimumBid, double rawMinimumBid) {
        String itemDescription = describeItem(state.listing().item());
        String auctionId = state.id();
        String bidAmountCmd = formatCommandAmount(rawMinimumBid);

        Component itemComponent = legacy("&b" + itemDescription)
                .hoverEvent(HoverEvent.showText(buildItemHoverComponent(state.listing().item())))
                .clickEvent(ClickEvent.runCommand("/bid select " + auctionId));

        Component joinButton = Component.text("[Join]")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Click to join this live auction")
                        .color(NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/bid select " + auctionId));

        Component bidButton = Component.text("[Bid " + minimumBid + "+]")
                .color(NamedTextColor.GOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Click to place the minimum bid of " + minimumBid)
                        .color(NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.suggestCommand("/bid " + bidAmountCmd));

        return legacy("&6[Live Auction] &a" + state.entry().sellerName() + " &7is auctioning ")
                .append(itemComponent)
                .append(legacy(" &7for &6" + formattedPrice + " &7| "))
                .append(joinButton)
                .append(Component.space())
                .append(bidButton);
    }

    /**
     * Builds the bid-update component broadcast to all auction participants when a new bid is placed.
     */
    public static Component buildBidUpdateComponent(LiveAuctionState state,
            String formattedBid, String minimumNext, String bidderName, double rawMinimumNext) {
        String itemDescription = describeItem(state.listing().item());
        String auctionId = state.id();
        String bidAmountCmd = formatCommandAmount(rawMinimumNext);

        Component itemComponent = legacy("&b" + itemDescription)
                .hoverEvent(HoverEvent.showText(buildItemHoverComponent(state.listing().item())))
                .clickEvent(ClickEvent.runCommand("/bid select " + auctionId));

        Component bidButton = Component.text("[Bid " + minimumNext + "+]")
                .color(NamedTextColor.GOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Click to place a bid of " + minimumNext)
                        .color(NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.suggestCommand("/bid " + bidAmountCmd));

        return legacy("&6[Live Bid] &a" + bidderName + " &7bid &6" + formattedBid + " &7on ")
                .append(itemComponent)
                .append(legacy(" &7| "))
                .append(bidButton);
    }

    /**
     * Builds the chat reminder component sent at configurable intervals.
     */
    public static Component buildReminderComponent(LiveAuctionState state,
            String formattedBid, long secondsLeft) {
        String itemDescription = describeItem(state.listing().item());
        String bidderName = state.hasBids() ? state.topBidderName() : "none";
        String auctionId = state.id();

        Component itemComponent = legacy("&b" + itemDescription)
                .hoverEvent(HoverEvent.showText(buildItemHoverComponent(state.listing().item())));

        Component joinButton = Component.text("[Join]")
                .color(NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/bid select " + auctionId));

        return legacy("&6[Live] ")
                .append(itemComponent)
                .append(legacy(" &7ending in &e" + secondsLeft + "s&7! Top bid: &6" + formattedBid
                        + "&7 by &a" + bidderName + " &7| "))
                .append(joinButton);
    }

    /**
     * Builds the end-of-auction component broadcast to all online players.
     */
    public static Component buildEndComponent(LiveAuctionState state,
            String formattedPrice, boolean hasBids, String winnerName) {
        String itemDescription = describeItem(state.listing().item());
        if (hasBids) {
            return legacy("&6[Live Auction] &b" + itemDescription + " &7sold to &a"
                    + winnerName + " &7for &6" + formattedPrice + "&7!");
        } else {
            return legacy("&7[Live Auction] The auction for &b" + itemDescription
                    + " &7ended with no bids.");
        }
    }

    /**
     * Builds a hover component that shows rich item details: name, enchantments, potion effects,
     * and shulker box contents.
     */
    public static Component buildItemHoverComponent(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return Component.text("Unknown item").color(NamedTextColor.RED);
        }

        List<Component> lines = new ArrayList<>();

        // Display name
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            lines.add(legacy(meta.getDisplayName()));
        } else {
            lines.add(legacy("&f" + friendlyMaterialName(item.getType())));
        }

        // Amount
        if (item.getAmount() > 1) {
            lines.add(Component.text("Amount: " + item.getAmount()).color(NamedTextColor.GRAY));
        }

        // Enchantments on the item itself
        Map<Enchantment, Integer> enchants = item.getEnchantments();
        if (!enchants.isEmpty()) {
            lines.add(Component.text("Enchantments:").color(NamedTextColor.GOLD));
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                String enchName = friendlyEnchantName(entry.getKey());
                lines.add(Component.text("  " + enchName + " " + toRoman(entry.getValue()))
                        .color(NamedTextColor.AQUA));
            }
        }

        // Stored enchantments (enchanted books)
        if (meta instanceof EnchantmentStorageMeta esm) {
            Map<Enchantment, Integer> stored = esm.getStoredEnchants();
            if (!stored.isEmpty()) {
                lines.add(Component.text("Stored Enchantments:").color(NamedTextColor.GOLD));
                for (Map.Entry<Enchantment, Integer> entry : stored.entrySet()) {
                    String enchName = friendlyEnchantName(entry.getKey());
                    lines.add(Component.text("  " + enchName + " " + toRoman(entry.getValue()))
                            .color(NamedTextColor.AQUA));
                }
            }
        }

        // Potion effects
        if (meta instanceof PotionMeta pm) {
            List<PotionEffect> effects = pm.getCustomEffects();
            if (!effects.isEmpty()) {
                lines.add(Component.text("Effects:").color(NamedTextColor.LIGHT_PURPLE));
                for (PotionEffect effect : effects) {
                    String effectName = friendlyEffectName(effect);
                    lines.add(Component.text("  " + effectName).color(NamedTextColor.LIGHT_PURPLE));
                }
            } else {
                // Base potion type
                var potionType = pm.getBasePotionType();
                if (potionType != null) {
                    lines.add(Component.text("Potion: " + friendlyName(potionType.name()))
                            .color(NamedTextColor.LIGHT_PURPLE));
                }
            }
        }

        // Shulker box contents
        if (meta instanceof BlockStateMeta bsm) {
            org.bukkit.block.BlockState blockState = bsm.getBlockState();
            if (blockState instanceof org.bukkit.block.ShulkerBox shulker) {
                org.bukkit.inventory.Inventory inv = shulker.getInventory();
                lines.add(Component.text("Contents:").color(NamedTextColor.YELLOW));
                int shown = 0;
                for (ItemStack content : inv.getContents()) {
                    if (content != null && content.getType() != Material.AIR) {
                        if (shown >= 10) {
                            lines.add(Component.text("  ... and more").color(NamedTextColor.DARK_GRAY));
                            break;
                        }
                        lines.add(Component.text("  " + content.getAmount() + "x "
                                + friendlyMaterialName(content.getType()))
                                .color(NamedTextColor.WHITE));
                        shown++;
                    }
                }
                if (shown == 0) {
                    lines.add(Component.text("  (empty)").color(NamedTextColor.DARK_GRAY));
                }
            }
        }

        // Lore
        if (meta != null && meta.hasLore() && meta.getLore() != null) {
            List<String> lore = meta.getLore();
            // show up to 5 lore lines
            int maxLore = Math.min(lore.size(), 5);
            for (int i = 0; i < maxLore; i++) {
                lines.add(legacy(lore.get(i)));
            }
            if (lore.size() > maxLore) {
                lines.add(Component.text("... +" + (lore.size() - maxLore) + " more").color(NamedTextColor.DARK_GRAY));
            }
        }

        return joinLines(lines);
    }

    /**
     * Builds a rich notification sent exclusively to the seller when their live auction starts.
     * Shows a short listing ID and is fully clickable to open the auction view.
     */
    public static Component buildSellerLiveComponent(LiveAuctionState state) {
        String itemDescription = describeItem(state.listing().item());
        String auctionId = state.id();
        String shortId = auctionId.substring(0, 8);

        Component clickable = Component.text("[#" + shortId + "]")
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(
                        Component.text("Click to view your live auction")
                                .color(NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/bid select " + auctionId));

        return legacy("&6[Live Auction] &aYour &b" + itemDescription + " &ais now live! ")
                .append(clickable)
                .append(legacy(" &7- use &e/bid <amount>&7 to participate."));
    }

    // ---- Helpers ----

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    private static Component joinLines(List<Component> lines) {
        if (lines.isEmpty()) return Component.empty();
        Component result = lines.get(0);
        for (int i = 1; i < lines.size(); i++) {
            result = result.append(Component.newline()).append(lines.get(i));
        }
        return result;
    }

    public static String describeItem(ItemStack item) {
        if (item == null) return "Unknown item";
        int amount = Math.max(1, item.getAmount());
        ItemMeta meta = item.getItemMeta();
        String name;
        if (meta != null && meta.hasDisplayName()) {
            String dn = meta.getDisplayName();
            String stripped = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(legacy(dn));
            name = stripped.isBlank() ? dn : stripped;
        } else {
            name = friendlyMaterialName(item.getType());
        }
        return amount + "x " + name;
    }

    private static String friendlyMaterialName(Material material) {
        if (material == null) return "Unknown";
        String lower = material.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String friendlyEnchantName(Enchantment enchant) {
        String key = enchant.getKey().getKey();
        return key.replace('_', ' ');
    }

    private static String friendlyEffectName(PotionEffect effect) {
        String name = friendlyName(effect.getType().getName());
        int amp = effect.getAmplifier();
        int duration = effect.getDuration() / 20; // ticks → seconds
        return name + (amp > 0 ? " " + toRoman(amp + 1) : "")
                + " (" + formatDuration(duration) + ")";
    }

    private static String friendlyName(String rawName) {
        if (rawName == null || rawName.isEmpty()) return "Unknown";
        String lower = rawName.toLowerCase(Locale.ENGLISH).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String toRoman(int number) {
        if (number < 1 || number > 20) return String.valueOf(number);
        String[] thousands = {"", "M", "MM", "MMM"};
        String[] hundreds  = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] tens      = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] ones      = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return thousands[number / 1000] + hundreds[(number % 1000) / 100]
                + tens[(number % 100) / 10] + ones[number % 10];
    }

    private static String formatDuration(int seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        if (seconds >= 60)   return (seconds / 60) + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }

    /**
     * Formats a bid amount as a plain decimal string suitable for use in a chat command.
     * Strips currency symbols and locale-specific separators so {@code /bid <amount>} parses correctly.
     */
    private static String formatCommandAmount(double amount) {
        if (amount == Math.floor(amount) && !Double.isInfinite(amount)) {
            return String.valueOf((long) amount);
        }
        return String.format(java.util.Locale.ENGLISH, "%.2f", amount);
    }
}
