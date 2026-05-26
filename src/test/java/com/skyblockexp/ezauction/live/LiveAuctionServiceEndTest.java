package com.skyblockexp.ezauction.live;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.skyblockexp.ezauction.model.AuctionListing;
import com.skyblockexp.ezauction.model.AuctionOperationResult;
import com.skyblockexp.ezauction.config.AuctionBackendMessages;
import com.skyblockexp.ezauction.config.LiveAuctionConfiguration;
import com.skyblockexp.ezauction.config.LiveAuctionCountdownConfiguration;
import com.skyblockexp.ezauction.integration.EzCountdownIntegration;
import com.skyblockexp.ezauction.live.model.LiveAuctionState;
import com.skyblockexp.ezauction.live.preferences.PlayerLivePreferencesManager;
import com.skyblockexp.ezauction.live.registry.LiveBidSessionRegistry;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tests for live auction resolution (endAuction) edge cases.
 */
class LiveAuctionServiceEndTest {

    private ServerMock server;
    private JavaPlugin plugin;
    private AuctionTransactionService transactionService;
    private LiveAuctionService service;
    private Map<UUID, List<ItemStack>> pendingReturns;

    @BeforeEach
    void setUp() {
        server = MockBukkit.getOrCreateMock();
        plugin = MockBukkit.createMockPlugin();

        transactionService = mock(AuctionTransactionService.class);
        when(transactionService.formatCurrency(anyDouble()))
                .thenAnswer(inv -> "$" + String.format("%.2f", (double) inv.getArgument(0)));
        when(transactionService.withdrawBuyer(any(), anyDouble()))
                .thenReturn(AuctionOperationResult.success("ok"));
        when(transactionService.creditSeller(any(UUID.class), anyDouble()))
                .thenReturn(AuctionOperationResult.success("ok"));

        pendingReturns = new ConcurrentHashMap<>();

        LiveAuctionCountdownConfiguration countdownCfg = LiveAuctionCountdownConfiguration.defaults();
        LiveAuctionConfiguration cfg = new LiveAuctionConfiguration(true, true, true, 200L, countdownCfg);

        service = new LiveAuctionService(
                plugin, transactionService, cfg,
                AuctionBackendMessages.LiveMessages.defaults(),
                AuctionBackendMessages.FallbackMessages.defaults(),
                new EzCountdownIntegration(plugin, false),
                new LiveBidSessionRegistry(),
                mock(PlayerLivePreferencesManager.class),
                pendingReturns
        );
    }

    @AfterEach
    void tearDown() {
        try { MockBukkit.unmock(); } catch (Throwable ignored) {}
    }

    // ---- helpers ----

    @SuppressWarnings("unchecked")
    private LiveAuctionState injectState(UUID sellerId, double startPrice) throws Exception {
        ItemStack item = new ItemStack(Material.DIAMOND);
        AuctionListing listing = new AuctionListing(
                "auction-1", sellerId, startPrice,
                System.currentTimeMillis() + 60_000L, item, 0.0, null);
        LiveAuctionEntry entry = new LiveAuctionEntry(listing, sellerId, "Seller");
        LiveAuctionState state = new LiveAuctionState(entry, Instant.now().plusSeconds(60));

        Field field = LiveAuctionService.class.getDeclaredField("activeAuctions");
        field.setAccessible(true);
        ((Map<String, LiveAuctionState>) field.get(service)).put(state.id(), state);
        return state;
    }

    private void invokeEndAuction(LiveAuctionState state) throws Exception {
        Method method = LiveAuctionService.class.getDeclaredMethod("endAuction", LiveAuctionState.class);
        method.setAccessible(true);
        method.invoke(service, state);
    }

    // ---- tests ----

    /**
     * When the economy plugin rejects the seller credit, the winner's escrowed funds
     * must be returned to them. Without this fix the money simply vanishes from the economy.
     */
    @Test
    void endAuction_creditSellerFailureRefundsWinnerEscrow() throws Exception {
        UUID sellerId = UUID.randomUUID();
        PlayerMock winner = server.addPlayer("Winner");
        LiveAuctionState state = injectState(sellerId, 100.0);

        // Place a winning bid so state.topBidder() and escrow are populated
        service.bid(winner, "auction-1", 150.0);

        // Economy rejects the seller credit
        when(transactionService.creditSeller(any(UUID.class), anyDouble()))
                .thenReturn(AuctionOperationResult.failure("bank offline"));

        invokeEndAuction(state);

        // Winner's escrow must be refunded since seller could not be credited
        verify(transactionService).refundOrderBuyer(winner.getUniqueId(), 150.0);
    }

    /**
     * When the seller credit succeeds, the winner keeps the item and is NOT refunded.
     */
    @Test
    void endAuction_creditSellerSuccessDoesNotRefundWinner() throws Exception {
        UUID sellerId = UUID.randomUUID();
        PlayerMock winner = server.addPlayer("Winner");
        LiveAuctionState state = injectState(sellerId, 100.0);

        service.bid(winner, "auction-1", 150.0);

        // Credit succeeds (already the default, but be explicit)
        when(transactionService.creditSeller(any(UUID.class), anyDouble()))
                .thenReturn(AuctionOperationResult.success("ok"));

        invokeEndAuction(state);

        // Winner should NOT receive a refund when the payment went through normally
        verify(transactionService, never()).refundOrderBuyer(eq(winner.getUniqueId()), anyDouble());
    }

    /**
     * When the seller credit fails, the item should be returned to the seller (pending returns),
     * not delivered to the winner who was just refunded.
     */
    @Test
    void endAuction_creditSellerFailureReturnsItemToSeller() throws Exception {
        UUID sellerId = UUID.randomUUID();
        PlayerMock winner = server.addPlayer("Winner");
        LiveAuctionState state = injectState(sellerId, 100.0);

        service.bid(winner, "auction-1", 150.0);

        when(transactionService.creditSeller(any(UUID.class), anyDouble()))
                .thenReturn(AuctionOperationResult.failure("bank offline"));

        invokeEndAuction(state);

        // Item must be in the seller's pending returns, not the winner's
        List<ItemStack> sellerItems = pendingReturns.get(sellerId);
        List<ItemStack> winnerItems = pendingReturns.get(winner.getUniqueId());

        assert sellerItems != null && !sellerItems.isEmpty()
                : "seller should have item in pendingReturns when credit fails";
        assert winnerItems == null || winnerItems.isEmpty()
                : "winner should NOT have item in pendingReturns when credit fails";
    }
}
