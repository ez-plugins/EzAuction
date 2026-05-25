package com.skyblockexp.ezauction.gui.menu.browser;

import com.skyblockexp.ezauction.gui.state.SearchPrompt;
import com.skyblockexp.ezauction.gui.util.AuctionMenuUtils;
import com.skyblockexp.ezauction.gui.view.BrowserView;
import com.skyblockexp.ezauction.gui.view.ListingSort;
import com.skyblockexp.ezauction.gui.view.OrderSort;
import com.skyblockexp.ezauction.model.AuctionListing;
import com.skyblockexp.ezauction.model.AuctionOrder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages per-player search/sort state and handles search/sort click events.
 */
public final class BrowserSearchHandler {

    private static final int MAX_SEARCH_LENGTH = 48;

    private final BrowserMenuContext ctx;
    private final OpenBrowserCallback openBrowserCallback;

    private final ConcurrentMap<UUID, String> activeSearchQueries = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, SearchPrompt> pendingSearchInputs = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ListingSort> activeListingSorts = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, OrderSort> activeOrderSorts = new ConcurrentHashMap<>();

    public BrowserSearchHandler(BrowserMenuContext ctx, OpenBrowserCallback openBrowserCallback) {
        this.ctx = ctx;
        this.openBrowserCallback = openBrowserCallback;
    }

    // -------------------------------------------------------------------------
    // Public state accessors (used by BrowserMenuBuilder and AuctionMenu)
    // -------------------------------------------------------------------------

    public SearchPrompt getPendingSearchInput(UUID playerId) {
        return pendingSearchInputs.get(playerId);
    }

    public void removePendingSearchInput(UUID playerId) {
        pendingSearchInputs.remove(playerId);
    }

    public void clearPlayer(UUID playerId) {
        pendingSearchInputs.remove(playerId);
        activeSearchQueries.remove(playerId);
        activeListingSorts.remove(playerId);
        activeOrderSorts.remove(playerId);
    }

    public String getSearchQuery(UUID playerId) {
        String query = activeSearchQueries.get(playerId);
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            activeSearchQueries.remove(playerId);
            return null;
        }
        return trimmed;
    }

    public void setSearchQuery(UUID playerId, String query) {
        if (query == null || query.isBlank()) {
            activeSearchQueries.remove(playerId);
        } else {
            activeSearchQueries.put(playerId, query);
        }
    }

    public ListingSort getListingSort(UUID playerId) {
        return activeListingSorts.computeIfAbsent(playerId, id -> ListingSort.ENDING_SOON);
    }

    public OrderSort getOrderSort(UUID playerId) {
        return activeOrderSorts.computeIfAbsent(playerId, id -> OrderSort.ENDING_SOON);
    }

    // -------------------------------------------------------------------------
    // Search/sort click handlers
    // -------------------------------------------------------------------------

    public void handleSearchClick(Player player, com.skyblockexp.ezauction.gui.holder.BrowserMenuHolder holder,
            InventoryClickEvent event) {
        UUID playerId = player.getUniqueId();
        if (event.isRightClick()) {
            if (activeSearchQueries.remove(playerId) != null) {
                sendMessage(player, ctx.messages.searchCleared());
            } else {
                sendMessage(player, ctx.messages.searchAlreadyClear());
            }
            openBrowserCallback.open(player, holder.view(), 0);
            return;
        }
        pendingSearchInputs.put(playerId, new SearchPrompt(holder.view(), holder.page()));
        player.closeInventory();
        sendMessage(player, ctx.messages.searchPrompt());
        sendMessage(player, ctx.messages.searchClearPrompt());
    }

    public void handleSortClick(Player player, com.skyblockexp.ezauction.gui.holder.BrowserMenuHolder holder,
            InventoryClickEvent event) {
        UUID playerId = player.getUniqueId();
        boolean backwards = event.isRightClick();
        if (holder.view() == BrowserView.LISTINGS || holder.view() == BrowserView.TEAM_LISTINGS) {
            ListingSort current = getListingSort(playerId);
            ListingSort updated = backwards ? current.previous() : current.next();
            activeListingSorts.put(playerId, updated);
            sendMessage(player, ctx.messages.sortListings().replace("{sort}", updated.label()));
        } else {
            OrderSort current = getOrderSort(playerId);
            OrderSort updated = backwards ? current.previous() : current.next();
            activeOrderSorts.put(playerId, updated);
            sendMessage(player, ctx.messages.sortOrders().replace("{sort}", updated.label()));
        }
        openBrowserCallback.open(player, holder.view(), holder.page());
    }

    public void handleSearchInput(Player player, SearchPrompt prompt, String message) {
        if (player == null || prompt == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        String input = message != null ? message.trim() : "";
        if (input.equalsIgnoreCase("cancel")) {
            sendMessage(player, ctx.messages.searchCancelled());
            openBrowserCallback.open(player, prompt.view(), prompt.page());
            return;
        }
        if (input.equalsIgnoreCase("clear") || input.isEmpty()) {
            boolean removed = activeSearchQueries.remove(playerId) != null;
            if (removed) {
                sendMessage(player, ctx.messages.searchCleared());
            } else {
                sendMessage(player, ctx.messages.searchAlreadyClear());
            }
            openBrowserCallback.open(player, prompt.view(), 0);
            return;
        }
        String sanitized = sanitizeSearchInput(input);
        if (sanitized == null) {
            sendMessage(player, ctx.messages.searchInvalid());
            openBrowserCallback.open(player, prompt.view(), prompt.page());
            return;
        }
        activeSearchQueries.put(playerId, sanitized);
        sendMessage(player, ctx.messages.searchApplied().replace("{query}", sanitized));
        openBrowserCallback.open(player, prompt.view(), 0);
    }

    // -------------------------------------------------------------------------
    // Filtering
    // -------------------------------------------------------------------------

    public void filterListings(List<AuctionListing> listings, String normalizedQuery) {
        if (listings == null || listings.isEmpty() || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return;
        }
        listings.removeIf(listing -> listing == null || !itemMatchesQuery(listing.item(), normalizedQuery));
    }

    public void filterOrders(List<AuctionOrder> orders, String normalizedQuery) {
        if (orders == null || orders.isEmpty() || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return;
        }
        orders.removeIf(order -> order == null || !itemMatchesQuery(order.requestedItem(), normalizedQuery));
    }

    public String formatSearchQueryForLore(String query) {
        if (query == null) {
            return "";
        }
        if (query.length() <= 32) {
            return query;
        }
        return query.substring(0, 29) + "...";
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String sanitizeSearchInput(String input) {
        if (input == null) {
            return null;
        }
        String stripped = ChatColor.stripColor(input);
        if (stripped == null) {
            stripped = input;
        }
        String trimmed = stripped.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_SEARCH_LENGTH) {
            trimmed = trimmed.substring(0, MAX_SEARCH_LENGTH);
        }
        return trimmed;
    }

    private boolean itemMatchesQuery(ItemStack item, String normalizedQuery) {
        if (item == null || item.getType() == Material.AIR || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = meta.hasDisplayName()
                    ? AuctionMenuUtils.normalizeText(meta.getDisplayName())
                    : null;
            if (displayName != null && !displayName.isEmpty() && displayName.contains(normalizedQuery)) {
                return true;
            }
            String localized = AuctionMenuUtils.normalizeText(AuctionMenuUtils.resolveLocalizedName(meta));
            if (localized != null && !localized.isEmpty() && localized.contains(normalizedQuery)) {
                return true;
            }
            if (enchantmentsMatch(meta, normalizedQuery)) {
                return true;
            }
        }
        if (!item.getEnchantments().isEmpty() && enchantmentMatches(item.getEnchantments(), normalizedQuery)) {
            return true;
        }
        String materialName = AuctionMenuUtils.normalizeMaterialName(item.getType());
        return materialName.contains(normalizedQuery);
    }

    private boolean enchantmentsMatch(ItemMeta meta, String normalizedQuery) {
        if (meta == null || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return false;
        }
        if (meta.hasEnchants() && enchantmentMatches(meta.getEnchants(), normalizedQuery)) {
            return true;
        }
        if (meta instanceof EnchantmentStorageMeta storage
                && storage.hasStoredEnchants()
                && enchantmentMatches(storage.getStoredEnchants(), normalizedQuery)) {
            return true;
        }
        return false;
    }

    private boolean enchantmentMatches(Map<Enchantment, Integer> enchantments, String normalizedQuery) {
        if (enchantments == null || enchantments.isEmpty() || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return false;
        }
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            if (enchantmentMatches(entry.getKey(), entry.getValue(), normalizedQuery)) {
                return true;
            }
        }
        return false;
    }

    private boolean enchantmentMatches(Enchantment enchantment, int level, String normalizedQuery) {
        if (enchantment == null || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return false;
        }
        String keyName = resolveEnchantmentKey(enchantment);
        if (matchesEnchantmentNameVariant(keyName, level, normalizedQuery)) {
            return true;
        }
        if (matchesEnchantmentNameVariant(keyName != null ? keyName.replace('_', ' ') : null, level, normalizedQuery)) {
            return true;
        }
        return false;
    }

    private String resolveEnchantmentKey(Enchantment enchantment) {
        if (enchantment == null) {
            return null;
        }
        try {
            Method getKey = Enchantment.class.getMethod("getKey");
            Object key = getKey.invoke(enchantment);
            return key != null ? key.toString() : null;
        } catch (NoSuchMethodException ex) {
            // Fallback to legacy getName
        } catch (Exception ex) {
            return null;
        }
        try {
            Method getName = Enchantment.class.getMethod("getName");
            Object name = getName.invoke(enchantment);
            return name != null ? name.toString() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean matchesEnchantmentNameVariant(String name, int level, String normalizedQuery) {
        if (name == null || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return false;
        }
        String normalizedName = name.toLowerCase(Locale.ENGLISH).trim();
        if (normalizedName.isEmpty()) {
            return false;
        }
        if (normalizedName.contains(normalizedQuery)) {
            return true;
        }
        if (level > 0) {
            String numeric = normalizedName + " " + level;
            if (numeric.contains(normalizedQuery)) {
                return true;
            }
            String roman = AuctionMenuUtils.toRomanNumeral(level);
            if (!roman.isEmpty()) {
                String romanTerm = normalizedName + " " + roman.toLowerCase(Locale.ENGLISH);
                if (romanTerm.contains(normalizedQuery)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendMessage(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
