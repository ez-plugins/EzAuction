package com.skyblockexp.ezauction.gui.menu.browser;

import com.skyblockexp.ezauction.gui.holder.BrowserMenuHolder;
import com.skyblockexp.ezauction.gui.holder.ConfirmMenuHolder;
import com.skyblockexp.ezauction.gui.holder.PreviewMenuHolder;
import com.skyblockexp.ezauction.gui.menu.AuctionActivityMenu;
import com.skyblockexp.ezauction.gui.menu.ClaimMenu;
import com.skyblockexp.ezauction.gui.menu.LiveAuctionPickerMenu;
import com.skyblockexp.ezauction.gui.view.ActivityTab;
import com.skyblockexp.ezauction.gui.view.BrowserView;
import com.skyblockexp.ezauction.gui.view.ConfirmAction;
import com.skyblockexp.ezauction.model.AuctionListing;
import com.skyblockexp.ezauction.model.AuctionOperationResult;
import com.skyblockexp.ezauction.model.AuctionOrder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Handles all click events for browser and confirm menus.
 */
public final class BrowserClickHandler {

    private final BrowserMenuContext ctx;
    private final BrowserMenuBuilder menuBuilder;
    private final BrowserSearchHandler searchHandler;
    private final OpenBrowserCallback openBrowserCallback;
    private final Supplier<AuctionActivityMenu> activityMenuSupplier;
    private final Supplier<ClaimMenu> claimMenuSupplier;
    private final Supplier<LiveAuctionPickerMenu> livePickerMenuSupplier;

    public BrowserClickHandler(
            BrowserMenuContext ctx,
            BrowserMenuBuilder menuBuilder,
            BrowserSearchHandler searchHandler,
            OpenBrowserCallback openBrowserCallback,
            Supplier<AuctionActivityMenu> activityMenuSupplier,
            Supplier<ClaimMenu> claimMenuSupplier,
            Supplier<LiveAuctionPickerMenu> livePickerMenuSupplier) {
        this.ctx = ctx;
        this.menuBuilder = menuBuilder;
        this.searchHandler = searchHandler;
        this.openBrowserCallback = openBrowserCallback;
        this.activityMenuSupplier = activityMenuSupplier;
        this.claimMenuSupplier = claimMenuSupplier;
        this.livePickerMenuSupplier = livePickerMenuSupplier;
    }

    // -------------------------------------------------------------------------
    // Browser menu clicks
    // -------------------------------------------------------------------------

    public void handleBrowserClick(Player player, BrowserMenuHolder holder, String action,
            ItemStack clicked, InventoryClickEvent event) {
        if (BrowserActions.PREVIOUS.equalsIgnoreCase(action)) {
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.page() - 1));
            return;
        }
        if (BrowserActions.NEXT.equalsIgnoreCase(action)) {
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.page() + 1));
            return;
        }
        if (action == null) {
            return;
        }
        if (BrowserActions.CLOSE.equalsIgnoreCase(action)) {
            player.closeInventory();
            return;
        }
        if (BrowserActions.TOGGLE_LISTINGS.equalsIgnoreCase(action)) {
            runSync(() -> openBrowserCallback.open(player, BrowserView.LISTINGS, 0));
            return;
        }
        if (BrowserActions.TOGGLE_ORDERS.equalsIgnoreCase(action)) {
            runSync(() -> openBrowserCallback.open(player, BrowserView.ORDERS, 0));
            return;
        }
        if (BrowserActions.TOGGLE_TEAM_LISTINGS.equalsIgnoreCase(action)) {
            if (!ctx.auctionManager.isTeamAuctionsAvailable() || !player.hasPermission("ezauction.auction.team")) {
                return;
            }
            runSync(() -> openBrowserCallback.open(player, BrowserView.TEAM_LISTINGS, 0));
            return;
        }
        if (BrowserActions.SEARCH.equalsIgnoreCase(action)) {
            searchHandler.handleSearchClick(player, holder, event);
            return;
        }
        if (BrowserActions.SORT.equalsIgnoreCase(action)) {
            searchHandler.handleSortClick(player, holder, event);
            return;
        }
        if (BrowserActions.SEARCH_TIPS.equalsIgnoreCase(action)) {
            return;
        }
        if (BrowserActions.CLAIMS.equalsIgnoreCase(action)) {
            player.closeInventory();
            ClaimMenu claimMenu = claimMenuSupplier.get();
            if (claimMenu != null) {
                Bukkit.getScheduler().runTask(ctx.plugin, () -> claimMenu.open(player));
            } else {
                player.performCommand("auction claim");
            }
            return;
        }
        if (BrowserActions.LIVE_AUCTION.equalsIgnoreCase(action)) {
            player.closeInventory();
            LiveAuctionPickerMenu liveAuctionPickerMenu = livePickerMenuSupplier.get();
            if (liveAuctionPickerMenu != null) {
                Bukkit.getScheduler().runTask(ctx.plugin, () -> liveAuctionPickerMenu.open(player));
            } else {
                player.performCommand("liveauction");
            }
            return;
        }
        if (BrowserActions.ACTIVITY.equalsIgnoreCase(action)) {
            player.closeInventory();
            AuctionActivityMenu activityMenu = activityMenuSupplier.get();
            if (activityMenu != null) {
                activityMenu.openActivityMenu(player, ActivityTab.MY_LISTINGS);
            } else {
                player.sendMessage(ChatColor.RED + "Activity menu is not available.");
            }
            return;
        }
        if (!BrowserActions.LISTING.equalsIgnoreCase(action)) {
            if (!BrowserActions.ORDER.equalsIgnoreCase(action)) {
                return;
            }
            handleOrderSelection(player, holder, clicked, event);
            return;
        }

        String listingId = ctx.itemTagStorage.getString(clicked, ctx.listingKey);
        if (listingId == null) {
            sendMessage(player, ctx.messages.listingNotFound());
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.page()));
            return;
        }

        AuctionListing listing = menuBuilder.findListing(listingId);
        if (listing == null) {
            sendMessage(player, ctx.messages.listingUnavailable());
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.page()));
            return;
        }

        if (event.isShiftClick() && event.isLeftClick()) {
            if (tryOpenShulkerPreview(player, listing.item(), holder.view(), holder.page(), null, null)) {
                return;
            }
        }

        if (event.isRightClick()) {
            if (!player.hasPermission("ezauction.auction.sell")) {
                sendMessage(player, ctx.messages.cancelListingNoPermission());
                return;
            }
            AuctionOperationResult result = ctx.auctionManager.cancelListing(player.getUniqueId(), listingId);
            if (result.message() != null && !result.message().isEmpty()) {
                player.sendMessage(result.message());
            }
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.page()));
            return;
        }

        if (!player.hasPermission("ezauction.auction.buy")) {
            sendMessage(player, ctx.messages.buyNoPermission());
            return;
        }

        if (listing.sellerId().equals(player.getUniqueId())) {
            sendMessage(player, ctx.messages.ownListingPurchase());
            return;
        }

        runSync(() -> openListingConfirmMenu(player, holder, listingId));
    }

    // -------------------------------------------------------------------------
    // Confirm menu clicks
    // -------------------------------------------------------------------------

    public void handleConfirmClick(Player player, ConfirmMenuHolder holder, String action, ItemStack clicked) {
        if (BrowserActions.CANCEL.equalsIgnoreCase(action)) {
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.previousPage()));
            return;
        }
        if (!BrowserActions.CONFIRM.equalsIgnoreCase(action)) {
            return;
        }
        if (holder.action() == ConfirmAction.PURCHASE_LISTING) {
            handleListingConfirm(player, holder, clicked);
        } else if (holder.action() == ConfirmAction.FULFILL_ORDER) {
            handleOrderConfirm(player, holder, clicked);
        }
    }

    public void handlePreviewClick(Player player, ConfirmMenuHolder holder, String action,
            ItemStack clicked, InventoryClickEvent event) {
        if (!event.isShiftClick() || !event.isLeftClick()) {
            return;
        }
        String targetId = ctx.itemTagStorage.getString(clicked, ctx.listingKey);
        if (targetId == null) {
            return;
        }
        if (BrowserActions.LISTING.equalsIgnoreCase(action)) {
            AuctionListing listing = menuBuilder.findListing(targetId);
            if (listing == null) {
                sendMessage(player, ctx.messages.listingUnavailable());
                runSync(() -> openBrowserCallback.open(player, holder.view(), holder.previousPage()));
                return;
            }
            if (tryOpenShulkerPreview(player, listing.item(), holder.view(), holder.previousPage(),
                    holder.action(), targetId)) {
                return;
            }
        } else if (BrowserActions.ORDER.equalsIgnoreCase(action)) {
            AuctionOrder order = menuBuilder.findOrder(targetId);
            if (order == null) {
                sendMessage(player, ctx.messages.orderUnavailable());
                runSync(() -> openBrowserCallback.open(player, holder.view(), holder.previousPage()));
                return;
            }
            if (tryOpenShulkerPreview(player, order.requestedItem(), holder.view(), holder.previousPage(),
                    holder.action(), targetId)) {
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Shulker box preview
    // -------------------------------------------------------------------------

    public void reopenPreviewOrigin(Player player, PreviewMenuHolder holder) {
        if (player == null || holder == null) {
            return;
        }
        if (holder.confirmAction() == null || holder.targetId() == null) {
            openBrowserCallback.open(player, holder.originView(), holder.originPage());
            return;
        }
        BrowserMenuHolder previousHolder = new BrowserMenuHolder(player.getUniqueId(),
                holder.originPage(), holder.originView());
        if (holder.confirmAction() == ConfirmAction.PURCHASE_LISTING) {
            openListingConfirmMenu(player, previousHolder, holder.targetId());
        } else if (holder.confirmAction() == ConfirmAction.FULFILL_ORDER) {
            openOrderConfirmMenu(player, previousHolder, holder.targetId());
        } else {
            openBrowserCallback.open(player, holder.originView(), holder.originPage());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void handleOrderSelection(Player player, BrowserMenuHolder holder,
            ItemStack clicked, InventoryClickEvent event) {
        String orderId = ctx.itemTagStorage.getString(clicked, ctx.listingKey);
        if (orderId == null) {
            sendMessage(player, ctx.messages.orderNotFound());
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.page()));
            return;
        }

        AuctionOrder order = menuBuilder.findOrder(orderId);
        if (order == null) {
            sendMessage(player, ctx.messages.orderUnavailable());
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.page()));
            return;
        }

        if (event.isShiftClick() && event.isLeftClick()) {
            if (tryOpenShulkerPreview(player, order.requestedItem(), holder.view(), holder.page(), null, null)) {
                return;
            }
        }

        if (event.isRightClick()) {
            if (!order.buyerId().equals(player.getUniqueId())) {
                return;
            }
            AuctionOperationResult result = ctx.auctionManager.cancelOrder(player.getUniqueId(), orderId);
            if (result.message() != null && !result.message().isEmpty()) {
                player.sendMessage(result.message());
            }
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.page()));
            return;
        }

        if (!player.hasPermission("ezauction.auction.fulfill")) {
            sendMessage(player, ctx.messages.fulfillNoPermission());
            return;
        }

        if (order.buyerId().equals(player.getUniqueId())) {
            sendMessage(player, ctx.messages.fulfillOwnOrder());
            return;
        }

        if (!player.getInventory().containsAtLeast(order.requestedItem(), order.requestedItem().getAmount())) {
            sendMessage(player, ctx.messages.fulfillInsufficientItems());
            return;
        }

        runSync(() -> openOrderConfirmMenu(player, holder, orderId));
    }

    private void handleListingConfirm(Player player, ConfirmMenuHolder holder, ItemStack clicked) {
        if (!player.hasPermission("ezauction.auction.buy")) {
            sendMessage(player, ctx.messages.buyNoPermission());
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.previousPage()));
            return;
        }
        String listingId = ctx.itemTagStorage.getString(clicked, ctx.listingKey);
        if (listingId == null) {
            sendMessage(player, ctx.messages.listingNotFound());
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.previousPage()));
            return;
        }

        AuctionOperationResult result = ctx.auctionManager.purchaseListing(player, listingId);
        if (result.message() != null && !result.message().isEmpty()) {
            player.sendMessage(result.message());
        }
        runSync(() -> openBrowserCallback.open(player, holder.view(), holder.previousPage()));
    }

    private void handleOrderConfirm(Player player, ConfirmMenuHolder holder, ItemStack clicked) {
        if (!player.hasPermission("ezauction.auction.fulfill")) {
            sendMessage(player, ctx.messages.fulfillNoPermission());
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.previousPage()));
            return;
        }
        String orderId = ctx.itemTagStorage.getString(clicked, ctx.listingKey);
        if (orderId == null) {
            sendMessage(player, ctx.messages.orderNotFound());
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.previousPage()));
            return;
        }

        AuctionOrder order = menuBuilder.findOrder(orderId);
        if (order == null) {
            sendMessage(player, ctx.messages.orderUnavailable());
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.previousPage()));
            return;
        }

        if (!player.getInventory().containsAtLeast(order.requestedItem(), order.requestedItem().getAmount())) {
            sendMessage(player, ctx.messages.fulfillInsufficientItems());
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.previousPage()));
            return;
        }

        AuctionOperationResult result = ctx.auctionManager.fulfillOrder(player, orderId);
        if (result.message() != null && !result.message().isEmpty()) {
            player.sendMessage(result.message());
        }
        runSync(() -> openBrowserCallback.open(player, holder.view(), holder.previousPage()));
    }

    private void openListingConfirmMenu(Player player, BrowserMenuHolder holder, String listingId) {
        Inventory inv = menuBuilder.buildListingConfirmInventory(player, holder, listingId);
        if (inv == null) {
            sendMessage(player, ctx.messages.listingUnavailable());
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.page()));
        } else {
            player.openInventory(inv);
        }
    }

    private void openOrderConfirmMenu(Player player, BrowserMenuHolder holder, String orderId) {
        Inventory inv = menuBuilder.buildOrderConfirmInventory(player, holder, orderId);
        if (inv == null) {
            sendMessage(player, ctx.messages.orderUnavailable());
            runSync(() -> openBrowserCallback.open(player, holder.view(), holder.page()));
        } else {
            player.openInventory(inv);
        }
    }

    private boolean tryOpenShulkerPreview(Player player, ItemStack item, BrowserView view, int page,
            ConfirmAction confirmAction, String targetId) {
        if (player == null || item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (!isShulkerBox(item)) {
            return false;
        }
        ItemStack source = item.clone();
        runSync(() -> openShulkerPreview(player, source, view, page, confirmAction, targetId));
        return true;
    }

    private void openShulkerPreview(Player player, ItemStack shulker, BrowserView view, int page,
            ConfirmAction confirmAction, String targetId) {
        if (player == null || shulker == null) {
            return;
        }

        ItemStack[] contents = readShulkerContents(shulker);
        int size = contents.length > 0 ? contents.length : 27;
        int normalizedSize = Math.max(9, Math.min(54, ((size + 8) / 9) * 9));
        PreviewMenuHolder holder = new PreviewMenuHolder(player.getUniqueId(), view, page, confirmAction, targetId);
        String title = ChatColor.translateAlternateColorCodes('&', ctx.messages.shulkerPreviewTitle());
        if (title == null || title.isEmpty()) {
            title = ChatColor.DARK_PURPLE + "Shulker Contents";
        }
        Inventory preview = Bukkit.createInventory(holder, normalizedSize, title);
        holder.setInventory(preview);
        if (contents != null) {
            for (int i = 0; i < Math.min(contents.length, preview.getSize()); i++) {
                ItemStack content = contents[i];
                if (content != null && content.getType() != Material.AIR && content.getAmount() > 0) {
                    preview.setItem(i, content);
                }
            }
        }
        player.openInventory(preview);
    }

    private ItemStack[] readShulkerContents(ItemStack shulker) {
        if (shulker == null) {
            return new ItemStack[0];
        }
        org.bukkit.inventory.meta.ItemMeta meta = shulker.getItemMeta();
        if (meta == null) {
            return new ItemStack[0];
        }
        try {
            Class<?> blockStateMetaClass = Class.forName("org.bukkit.inventory.meta.BlockStateMeta");
            if (!blockStateMetaClass.isInstance(meta)) {
                return new ItemStack[0];
            }
            Method hasBlockState = blockStateMetaClass.getMethod("hasBlockState");
            if (!Boolean.TRUE.equals(hasBlockState.invoke(meta))) {
                return new ItemStack[0];
            }
            Method getBlockState = blockStateMetaClass.getMethod("getBlockState");
            Object blockState = getBlockState.invoke(meta);
            if (blockState == null) {
                return new ItemStack[0];
            }
            Class<?> shulkerClass = Class.forName("org.bukkit.block.ShulkerBox");
            if (!shulkerClass.isInstance(blockState)) {
                return new ItemStack[0];
            }
            Method getInventory = shulkerClass.getMethod("getInventory");
            Object inventory = getInventory.invoke(blockState);
            if (!(inventory instanceof Inventory snapshot)) {
                return new ItemStack[0];
            }
            int invSize = snapshot.getSize();
            ItemStack[] itemContents = new ItemStack[invSize];
            ItemStack[] original = snapshot.getContents();
            for (int i = 0; i < invSize; i++) {
                ItemStack content = original != null && i < original.length ? original[i] : null;
                if (content == null || content.getType() == Material.AIR || content.getAmount() <= 0) {
                    itemContents[i] = null;
                } else {
                    itemContents[i] = content.clone();
                }
            }
            return itemContents;
        } catch (ReflectiveOperationException ex) {
            return new ItemStack[0];
        }
    }

    private boolean isShulkerBox(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material type = item.getType();
        if (type == null) {
            return false;
        }
        String name = type.name();
        return name != null && name.endsWith("SHULKER_BOX");
    }

    private void sendMessage(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private void runSync(Runnable runnable) {
        ctx.plugin.getServer().getScheduler().runTask(ctx.plugin, runnable);
    }
}
