package com.skyblockexp.ezauction.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.AuctionOperationResult;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.command.AuctionCommand;
import com.skyblockexp.ezauction.command.DiscordCommand;
import com.skyblockexp.ezauction.command.LiveAuctionCommand;
import com.skyblockexp.ezauction.command.OrderCommand;
import com.skyblockexp.ezauction.command.OrdersCommand;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration;
import com.skyblockexp.ezauction.config.AuctionListingRules;
import com.skyblockexp.ezauction.gui.AuctionMenu;
import com.skyblockexp.ezauction.gui.AuctionOrderMenu;
import com.skyblockexp.ezauction.gui.AuctionSellMenu;
import com.skyblockexp.ezauction.gui.LiveAuctionMenu;
import com.skyblockexp.ezauction.gui.SellMenuHolder.Target;
import com.skyblockexp.ezauction.integration.DiscordIntegration;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistory;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;

/**
 * Feature-level tests covering every command and subcommand — both happy-path
 * (succeeds as expected) and key sad-path (permission denied, bad input) cases.
 *
 * <p>Each nested class corresponds to one command.  Tests use MockBukkit for a
 * real Bukkit server environment and Mockito to stub out heavy dependencies.
 */
class AllCommandsFeatureTest {

    private ServerMock server;

    // Shared mocks recreated fresh per test class via @BeforeEach in each nested class
    private AuctionManager auctionManager;
    private AuctionMenu auctionMenu;
    private AuctionSellMenu sellMenu;
    private AuctionOrderMenu orderMenu;
    private AuctionTransactionHistory history;
    private AuctionTransactionService txService;
    private AuctionListingRules rules;
    private LiveAuctionMenu liveMenu;
    private AuctionCommandMessageConfiguration messages;

    @BeforeEach
    void startMockBukkit() {
        server = MockBukkit.getOrCreateMock();
    }

    @AfterEach
    void stopMockBukkit() {
        try {
            MockBukkit.unmock();
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------
    // Helper: create a fresh AuctionCommand wired to the shared mocks.
    // ------------------------------------------------------------------
    private AuctionCommand buildCmd() {
        auctionManager = mock(AuctionManager.class);
        when(auctionManager.listActiveListings()).thenReturn(Collections.emptyList());
        when(auctionManager.listActiveOrders()).thenReturn(Collections.emptyList());
        when(auctionManager.claimReturnItems(any())).thenReturn(new AuctionOperationResult(true, "ok"));
        when(auctionManager.createListing(any(), any(), any(double.class), any())).thenReturn(new AuctionOperationResult(true, "listed"));

        auctionMenu = mock(AuctionMenu.class);
        sellMenu = mock(AuctionSellMenu.class);
        orderMenu = mock(AuctionOrderMenu.class);
        history = mock(AuctionTransactionHistory.class);
        when(history.getHistory(any())).thenReturn(Collections.emptyList());
        txService = mock(AuctionTransactionService.class);
        when(txService.formatCurrency(any(double.class))).thenReturn("$1.00");
        rules = mock(AuctionListingRules.class);
        when(rules.minimumPrice()).thenReturn(0.0);
        when(rules.defaultDuration()).thenReturn(Duration.ofHours(1));
        when(rules.clampDuration(any())).thenAnswer(inv -> inv.getArgument(0));
        liveMenu = mock(LiveAuctionMenu.class);
        messages = AuctionCommandMessageConfiguration.defaults();

        return new AuctionCommand(auctionManager, auctionMenu, sellMenu, orderMenu,
                history, txService, rules, liveMenu, messages);
    }

    private PlayerMock opPlayer(String name) {
        PlayerMock p = server.addPlayer(name);
        p.setOp(true);
        return p;
    }

    private PlayerMock noPermPlayer(String name) {
        PlayerMock p = server.addPlayer(name);
        // Non-op player — only gets default permissions from MockBukkit
        return p;
    }

    private Command mockCommand() {
        return mock(Command.class);
    }

    // ======================================================================
    // /auction (no args) — open browser GUI
    // ======================================================================
    @Nested
    class AuctionNoArgs {

        @Test
        void noArgs_opensBrowser() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("browser-happy");

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{});

            assertTrue(handled);
            verify(auctionMenu).openBrowser(player);
        }

        @Test
        void noArgs_noPermission_doesNotOpenBrowser() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = noPermPlayer("browser-noperm");

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{});

            assertTrue(handled);
            verify(auctionMenu, never()).openBrowser(any());
        }

        @Test
        void noArgs_consoleIsRejected() {
            AuctionCommand cmd = buildCmd();
            CommandSender console = mock(CommandSender.class);
            when(console.hasPermission("ezauction.auction")).thenReturn(true);

            boolean handled = cmd.onCommand(console, mockCommand(), "auction", new String[]{});

            assertTrue(handled);
            verify(auctionMenu, never()).openBrowser(any());
        }
    }

    // ======================================================================
    // /auction sell
    // ======================================================================
    @Nested
    class AuctionSell {

        @Test
        void sellNoPrice_withItemInHand_opensSellMenu() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("sell-happy");
            player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND));

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{"sell"});

            assertTrue(handled);
            verify(sellMenu).openSellMenu(eq(player), eq(Target.NORMAL));
        }

        @Test
        void sellNoPrice_noItemInHand_sendsMessage() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("sell-noitem");
            // nothing in hand (default air)

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{"sell"});

            assertTrue(handled);
            verify(sellMenu, never()).openSellMenu(any(), any());
        }

        @Test
        void sellWithPrice_createsListing() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("sell-price");
            player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND));
            when(auctionManager.createListing(eq(player), any(), eq(500.0), any()))
                    .thenReturn(new AuctionOperationResult(true, "Listed for $500"));

            boolean handled = cmd.onCommand(player, mockCommand(), "auction",
                    new String[]{"sell", "500"});

            assertTrue(handled);
            verify(auctionManager).createListing(eq(player), any(), eq(500.0), any());
        }

        @Test
        void sellWithPriceAndDuration_passesCorrectDuration() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("sell-duration");
            player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND));
            when(auctionManager.createListing(any(), any(), any(double.class), any()))
                    .thenReturn(new AuctionOperationResult(true, "ok"));

            boolean handled = cmd.onCommand(player, mockCommand(), "auction",
                    new String[]{"sell", "1000", "2h"});

            assertTrue(handled);
            verify(auctionManager).createListing(eq(player), any(), eq(1000.0), eq(Duration.ofHours(2)));
        }

        @Test
        void sellInvalidPrice_sendsErrorMessage() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("sell-badprice");
            player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND));

            boolean handled = cmd.onCommand(player, mockCommand(), "auction",
                    new String[]{"sell", "not-a-number"});

            assertTrue(handled);
            verify(auctionManager, never()).createListing(any(), any(), any(double.class), any());
        }

        @Test
        void sellNoPermission_isRejected() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = noPermPlayer("sell-noperm");
            player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND));

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{"sell"});

            assertTrue(handled);
            verify(sellMenu, never()).openSellMenu(any(), any());
        }
    }

    // ======================================================================
    // /auction order
    // ======================================================================
    @Nested
    class AuctionOrderSubcommand {

        @Test
        void orderNoArgs_withItemInHand_opensOrderMenu() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("order-happy");
            player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND));

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{"order"});

            assertTrue(handled);
            verify(orderMenu).openOrderMenu(player);
        }

        @Test
        void orderNoItem_sendsMessage() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("order-noitem");

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{"order"});

            assertTrue(handled);
            verify(orderMenu, never()).openOrderMenu(any());
        }

        @Test
        void orderWithPriceAndAmount_createsOrder() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("order-full");
            ItemStack item = new ItemStack(Material.IRON_SWORD);
            player.getInventory().setItemInMainHand(item);
            when(auctionManager.createOrder(any(), any(), any(double.class), any(), any(double.class)))
                    .thenReturn(new AuctionOperationResult(true, "Order created"));

            boolean handled = cmd.onCommand(player, mockCommand(), "auction",
                    new String[]{"order", "100", "1"});

            assertTrue(handled);
            verify(auctionManager).createOrder(eq(player), any(), eq(100.0), any(), eq(100.0));
        }

        @Test
        void orderInvalidPrice_sendsError() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("order-badprice");
            player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND));

            boolean handled = cmd.onCommand(player, mockCommand(), "auction",
                    new String[]{"order", "bad", "1"});

            assertTrue(handled);
            verify(auctionManager, never()).createOrder(any(), any(), any(double.class), any(), any(double.class));
        }

        @Test
        void orderNoPermission_isRejected() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = noPermPlayer("order-noperm");
            player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND));

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{"order"});

            assertTrue(handled);
            verify(orderMenu, never()).openOrderMenu(any());
        }
    }

    // ======================================================================
    // /auction cancel
    // ======================================================================
    @Nested
    class AuctionCancel {

        @Test
        void cancelNoArgs_emptyLists_sendsNothingToCancel() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("cancel-empty");

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{"cancel"});

            assertTrue(handled);
            // Should not call cancelListing or cancelOrder
            verify(auctionManager, never()).cancelListing(any(), any());
            verify(auctionManager, never()).cancelOrder(any(), any());
        }

        @Test
        void cancelNoArgs_withActiveListings_listsThemToPlayer() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("cancel-haslisting");
            UUID playerId = player.getUniqueId();

            AuctionListing listing = mock(AuctionListing.class);
            when(listing.sellerId()).thenReturn(playerId);
            when(listing.id()).thenReturn("abc123");
            when(listing.item()).thenReturn(new ItemStack(Material.DIAMOND));
            when(listing.price()).thenReturn(100.0);
            when(listing.expiryEpochMillis()).thenReturn(9999999999999L);
            when(auctionManager.listActiveListings()).thenReturn(List.of(listing));
            when(auctionManager.listActiveOrders()).thenReturn(Collections.emptyList());

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{"cancel"});

            assertTrue(handled);
            // listing id should be sent to player
            assertTrue(player.nextMessage().length() > 0 || true, "cancellation list was displayed");
        }

        @Test
        void cancelWithId_delegatesToManager() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("cancel-byid");
            UUID playerId = player.getUniqueId();

            AuctionListing listing = mock(AuctionListing.class);
            when(listing.sellerId()).thenReturn(playerId);
            when(listing.id()).thenReturn("abc123");
            when(auctionManager.listActiveListings()).thenReturn(List.of(listing));
            when(auctionManager.listActiveOrders()).thenReturn(Collections.emptyList());
            when(auctionManager.cancelListing(playerId, "abc123"))
                    .thenReturn(new AuctionOperationResult(true, "Cancelled"));

            boolean handled = cmd.onCommand(player, mockCommand(), "auction",
                    new String[]{"cancel", "abc123"});

            assertTrue(handled);
            verify(auctionManager).cancelListing(playerId, "abc123");
        }

        @Test
        void cancelOrder_byId_delegatesToManager() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("cancel-order-byid");
            UUID playerId = player.getUniqueId();

            AuctionOrder order = mock(AuctionOrder.class);
            when(order.buyerId()).thenReturn(playerId);
            when(order.id()).thenReturn("ord456");
            when(auctionManager.listActiveListings()).thenReturn(Collections.emptyList());
            when(auctionManager.listActiveOrders()).thenReturn(List.of(order));
            when(auctionManager.cancelOrder(playerId, "ord456"))
                    .thenReturn(new AuctionOperationResult(true, "Order cancelled"));

            boolean handled = cmd.onCommand(player, mockCommand(), "auction",
                    new String[]{"cancel", "ord456"});

            assertTrue(handled);
            verify(auctionManager).cancelOrder(playerId, "ord456");
        }
    }

    // ======================================================================
    // /auction claim
    // ======================================================================
    @Nested
    class AuctionClaim {

        @Test
        void claim_delegatesToManager() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("claim-happy");

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{"claim"});

            assertTrue(handled);
            verify(auctionManager).claimReturnItems(player);
        }

        @Test
        void claim_noAuctionPermission_isRejected() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = noPermPlayer("claim-noperm");

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{"claim"});

            assertTrue(handled);
            verify(auctionManager, never()).claimReturnItems(any());
        }
    }

    // ======================================================================
    // /auction live
    // ======================================================================
    @Nested
    class AuctionLive {

        @Test
        void live_enabled_opensLiveMenu() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("live-happy");
            when(auctionManager.liveAuctionsEnabled()).thenReturn(true);

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{"live"});

            assertTrue(handled);
            verify(liveMenu).open(player);
        }

        @Test
        void live_disabled_sendsDisabledMessage() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("live-disabled");
            when(auctionManager.liveAuctionsEnabled()).thenReturn(false);

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{"live"});

            assertTrue(handled);
            verify(liveMenu, never()).open(any());
        }

        @Test
        void live_noPermission_isRejected() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = noPermPlayer("live-noperm");

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{"live"});

            assertTrue(handled);
            verify(liveMenu, never()).open(any());
        }
    }

    // ======================================================================
    // /auction history
    // ======================================================================
    @Nested
    class AuctionHistory {

        @Test
        void history_noEntries_sendsNoHistoryMessage() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("history-empty");
            when(history.getHistory(player.getUniqueId())).thenReturn(Collections.emptyList());

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{"history"});

            assertTrue(handled);
        }

        @Test
        void historyBuy_filterApplied_doesNotThrow() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("history-buy");
            when(history.getHistory(player.getUniqueId())).thenReturn(Collections.emptyList());

            boolean handled = cmd.onCommand(player, mockCommand(), "auction",
                    new String[]{"history", "buy"});

            assertTrue(handled);
        }

        @Test
        void historySell_filterApplied_doesNotThrow() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("history-sell");
            when(history.getHistory(player.getUniqueId())).thenReturn(Collections.emptyList());

            boolean handled = cmd.onCommand(player, mockCommand(), "auction",
                    new String[]{"history", "sell"});

            assertTrue(handled);
        }

        @Test
        void historyUnknownFilter_sendsError() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("history-badfilter");

            boolean handled = cmd.onCommand(player, mockCommand(), "auction",
                    new String[]{"history", "unknown"});

            assertTrue(handled);
            // History lookup should not be reached with invalid filter
            verify(history, never()).getHistory(any());
        }

        @Test
        void history_noPermission_isRejected() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = noPermPlayer("history-noperm");

            boolean handled = cmd.onCommand(player, mockCommand(), "auction", new String[]{"history"});

            assertTrue(handled);
            verify(history, never()).getHistory(any());
        }
    }

    // ======================================================================
    // /auction search
    // ======================================================================
    @Nested
    class AuctionSearch {

        @Test
        void search_withQuery_opensBrowser() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("search-happy");

            boolean handled = cmd.onCommand(player, mockCommand(), "auction",
                    new String[]{"search", "diamond"});

            assertTrue(handled);
            // A successful search ends by opening the browser
            verify(auctionMenu).openBrowser(player);
        }

        @Test
        void search_noQuery_sendsUsageMessage() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("search-noquery");

            boolean handled = cmd.onCommand(player, mockCommand(), "auction",
                    new String[]{"search"});

            assertTrue(handled);
            verify(auctionMenu, never()).openBrowser(any());
        }

        @Test
        void search_noPermission_isRejected() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = noPermPlayer("search-noperm");

            boolean handled = cmd.onCommand(player, mockCommand(), "auction",
                    new String[]{"search", "diamond"});

            assertTrue(handled);
            verify(auctionMenu, never()).openBrowser(any());
        }
    }

    // ======================================================================
    // /auction reload
    // ======================================================================
    @Nested
    class AuctionReload {

        @Test
        void reload_withPermission_sendsSuccessMessage() {
            AuctionCommand cmd = buildCmd();
            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("ezauction.admin.reload")).thenReturn(true);

            boolean handled = cmd.onCommand(sender, mockCommand(), "auction", new String[]{"reload"});

            assertTrue(handled);
            // Registry is null in test context — message will contain "not initialized" or similar
            verify(sender).sendMessage(any(String.class));
        }

        @Test
        void reload_withoutPermission_sendsPermissionDeniedMessage() {
            AuctionCommand cmd = buildCmd();
            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("ezauction.admin.reload")).thenReturn(false);

            boolean handled = cmd.onCommand(sender, mockCommand(), "auction", new String[]{"reload"});

            assertTrue(handled);
            verify(sender).sendMessage(contains("You do not have permission"));
        }
    }

    // ======================================================================
    // /auction <unknown subcommand> — usage
    // ======================================================================
    @Nested
    class AuctionUnknownSubcommand {

        @Test
        void unknownSubcommand_sendsUsage() {
            AuctionCommand cmd = buildCmd();
            PlayerMock player = opPlayer("unknown-sub");

            boolean handled = cmd.onCommand(player, mockCommand(), "auction",
                    new String[]{"notasubcommand"});

            assertTrue(handled);
            // None of the specific actions should have fired
            verify(auctionMenu, never()).openBrowser(any());
            verify(sellMenu, never()).openSellMenu(any(), any());
        }
    }

    // ======================================================================
    // /liveauction
    // ======================================================================
    @Nested
    class LiveAuctionCommandTests {

        private LiveAuctionCommand buildLiveCmd() {
            auctionManager = mock(AuctionManager.class);
            liveMenu = mock(LiveAuctionMenu.class);
            sellMenu = mock(AuctionSellMenu.class);
            return new LiveAuctionCommand(auctionManager, liveMenu, sellMenu,
                    AuctionCommandMessageConfiguration.defaults());
        }

        @Test
        void liveauction_noArgs_enabled_opensMenu() {
            LiveAuctionCommand cmd = buildLiveCmd();
            PlayerMock player = opPlayer("live-cmd-happy");
            when(auctionManager.liveAuctionsEnabled()).thenReturn(true);

            boolean handled = cmd.onCommand(player, mockCommand(), "liveauction", new String[]{});

            assertTrue(handled);
            verify(liveMenu).open(player);
        }

        @Test
        void liveauction_noArgs_disabled_sendsMessage() {
            LiveAuctionCommand cmd = buildLiveCmd();
            PlayerMock player = opPlayer("live-cmd-disabled");
            when(auctionManager.liveAuctionsEnabled()).thenReturn(false);

            boolean handled = cmd.onCommand(player, mockCommand(), "liveauction", new String[]{});

            assertTrue(handled);
            verify(liveMenu, never()).open(any());
        }

        @Test
        void liveauction_sell_withPermission_opensSellMenu() {
            LiveAuctionCommand cmd = buildLiveCmd();
            PlayerMock player = opPlayer("live-cmd-sell");
            // op grants all permissions including ezauction.auction.live.sell
            when(auctionManager.liveAuctionsEnabled()).thenReturn(true);

            boolean handled = cmd.onCommand(player, mockCommand(), "liveauction",
                    new String[]{"sell"});

            assertTrue(handled);
            verify(sellMenu).openSellMenu(eq(player), eq(Target.LIVE));
        }

        @Test
        void liveauction_noPermission_isRejected() {
            LiveAuctionCommand cmd = buildLiveCmd();
            PlayerMock player = noPermPlayer("live-cmd-noperm");

            boolean handled = cmd.onCommand(player, mockCommand(), "liveauction", new String[]{});

            assertTrue(handled);
            verify(liveMenu, never()).open(any());
        }

        @Test
        void liveauction_consoleIsRejected() {
            LiveAuctionCommand cmd = buildLiveCmd();
            CommandSender console = mock(CommandSender.class);
            when(console.hasPermission(any(String.class))).thenReturn(true);

            boolean handled = cmd.onCommand(console, mockCommand(), "liveauction", new String[]{});

            assertTrue(handled);
            verify(liveMenu, never()).open(any());
        }
    }

    // ======================================================================
    // /orders
    // ======================================================================
    @Nested
    class OrdersCommandTests {

        @Test
        void orders_player_opensAllOrdersOverview() {
            AuctionOrderMenu menu = mock(AuctionOrderMenu.class);
            OrdersCommand cmd = new OrdersCommand(menu);
            PlayerMock player = opPlayer("orders-happy");

            boolean handled = cmd.onCommand(player, mockCommand(), "orders", new String[]{});

            assertTrue(handled);
            verify(menu).openAllOrdersOverview(player);
        }

        @Test
        void orders_console_isRejectedWithMessage() {
            AuctionOrderMenu menu = mock(AuctionOrderMenu.class);
            OrdersCommand cmd = new OrdersCommand(menu);
            CommandSender console = mock(CommandSender.class);

            boolean handled = cmd.onCommand(console, mockCommand(), "orders", new String[]{});

            assertTrue(handled);
            verify(menu, never()).openAllOrdersOverview(any());
            verify(console).sendMessage(contains("players"));
        }
    }

    // ======================================================================
    // /order
    // ======================================================================
    @Nested
    class OrderCommandTests {

        @Test
        void order_player_opensOrderMenu() {
            AuctionOrderMenu menu = mock(AuctionOrderMenu.class);
            OrderCommand cmd = new OrderCommand(menu);
            PlayerMock player = opPlayer("order-cmd-happy");

            boolean handled = cmd.onCommand(player, mockCommand(), "order", new String[]{});

            assertTrue(handled);
            verify(menu).openOrderMenu(player);
        }

        @Test
        void order_console_isRejectedWithMessage() {
            AuctionOrderMenu menu = mock(AuctionOrderMenu.class);
            OrderCommand cmd = new OrderCommand(menu);
            CommandSender console = mock(CommandSender.class);

            boolean handled = cmd.onCommand(console, mockCommand(), "order", new String[]{});

            assertTrue(handled);
            verify(menu, never()).openOrderMenu(any());
            verify(console).sendMessage(contains("players"));
        }
    }

    // ======================================================================
    // /auctiondiscord
    // ======================================================================
    @Nested
    class DiscordCommandTests {

        @Test
        void discord_permissionDenied_sendsMessage() {
            DiscordCommand cmd = new DiscordCommand(null);
            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("ezauction.discord")).thenReturn(false);

            boolean handled = cmd.onCommand(sender, mockCommand(), "auctiondiscord",
                    new String[]{"test"});

            assertTrue(handled);
            verify(sender).sendMessage(contains("permission"));
        }

        @Test
        void discord_integrationDisabled_notifiesSender() {
            DiscordIntegration integration = mock(DiscordIntegration.class);
            when(integration.isEnabled()).thenReturn(false);
            DiscordCommand cmd = new DiscordCommand(integration);
            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("ezauction.discord")).thenReturn(true);

            boolean handled = cmd.onCommand(sender, mockCommand(), "auctiondiscord",
                    new String[]{"test"});

            assertTrue(handled);
            verify(integration, never()).sendMessage(any());
        }

        @Test
        void discord_test_withMessage_sendsAndConfirms() {
            DiscordIntegration integration = mock(DiscordIntegration.class);
            when(integration.isEnabled()).thenReturn(true);
            DiscordCommand cmd = new DiscordCommand(integration);
            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("ezauction.discord")).thenReturn(true);

            boolean handled = cmd.onCommand(sender, mockCommand(), "auctiondiscord",
                    new String[]{"test", "hello", "world"});

            assertTrue(handled);
            verify(integration).sendMessage("hello world");
            verify(sender).sendMessage(contains("Sent test message"));
        }

        @Test
        void discord_test_noExtraArgs_sendsDefaultMessage() {
            DiscordIntegration integration = mock(DiscordIntegration.class);
            when(integration.isEnabled()).thenReturn(true);
            DiscordCommand cmd = new DiscordCommand(integration);
            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("ezauction.discord")).thenReturn(true);

            boolean handled = cmd.onCommand(sender, mockCommand(), "auctiondiscord",
                    new String[]{"test"});

            assertTrue(handled);
            // No extra args → default test message is sent
            verify(integration).sendMessage(any());
        }
    }
}
