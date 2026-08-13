package com.skyblockexp.ezauction.live;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
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
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class LiveAuctionServiceBidTest {

    private ServerMock server;
    private JavaPlugin plugin;
    private AuctionTransactionService transactionService;
    private LiveAuctionService service;
    private LiveBidSessionRegistry sessionRegistry;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        server = MockBukkit.getOrCreateMock();
        plugin = MockBukkit.createMockPlugin();

        transactionService = mock(AuctionTransactionService.class);
        when(transactionService.formatCurrency(anyDouble()))
                .thenAnswer(inv -> "$" + String.format("%.2f", (double) inv.getArgument(0)));
        when(transactionService.withdrawBuyer(any(), anyDouble()))
                .thenReturn(AuctionOperationResult.success("ok"));

        sessionRegistry = new LiveBidSessionRegistry();

        LiveAuctionCountdownConfiguration countdownCfg = LiveAuctionCountdownConfiguration.defaults();
        LiveAuctionConfiguration cfg = new LiveAuctionConfiguration(true, true, true, 200L, countdownCfg);

        service = new LiveAuctionService(
                plugin, transactionService, cfg,
                AuctionBackendMessages.LiveMessages.defaults(),
                AuctionBackendMessages.FallbackMessages.defaults(),
                new EzCountdownIntegration(plugin, false),
                sessionRegistry,
                mock(PlayerLivePreferencesManager.class),
                new ConcurrentHashMap<>()
        );
    }

    @AfterEach
    void tearDown() {
        try { MockBukkit.unmock(); } catch (Throwable ignored) {}
    }

    // ---- helpers ----

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

    // ---- tests ----

    @Test
    void bid_sellerCannotBidOnOwnAuction() throws Exception {
        PlayerMock seller = server.addPlayer("Seller");
        injectState(seller.getUniqueId(), 100.0);

        AuctionOperationResult result = service.bid(seller, "auction-1", 150.0);

        assertFalse(result.success());
        verify(transactionService, never()).withdrawBuyer(any(), anyDouble());
    }

    @Test
    void bid_belowStartingPriceIsRejected() throws Exception {
        PlayerMock bidder = server.addPlayer("Bidder");
        injectState(UUID.randomUUID(), 100.0);

        // First bid must meet the starting price; bidding below it should be rejected
        AuctionOperationResult result = service.bid(bidder, "auction-1", 90.0);

        assertFalse(result.success());
        verify(transactionService, never()).withdrawBuyer(any(), anyDouble());
    }

    @Test
    void bid_belowMinimumAfterExistingBidIsRejected() throws Exception {
        PlayerMock firstBidder = server.addPlayer("FirstBidder");
        PlayerMock secondBidder = server.addPlayer("SecondBidder");
        LiveAuctionState state = injectState(UUID.randomUUID(), 100.0);

        // First bid at starting price succeeds
        service.bid(firstBidder, "auction-1", 100.0);

        // Defaults: 5% + fixed 10 → minimum after 100 bid = max(105, 110) = 110
        AuctionOperationResult result = service.bid(secondBidder, "auction-1", 105.0);

        assertFalse(result.success());
    }

    @Test
    void bid_successfulBidWithdrawsDelta() throws Exception {
        PlayerMock bidder = server.addPlayer("Bidder");
        injectState(UUID.randomUUID(), 100.0);

        AuctionOperationResult result = service.bid(bidder, "auction-1", 115.0);

        assertTrue(result.success());
        // Full amount withdrawn on first bid (no prior escrow)
        verify(transactionService).withdrawBuyer(bidder, 115.0);
    }

    @Test
    void bid_secondBidOnlyWithdrawsDelta() throws Exception {
        PlayerMock bidder = server.addPlayer("Bidder");
        LiveAuctionState state = injectState(UUID.randomUUID(), 100.0);

        // First bid
        service.bid(bidder, "auction-1", 115.0);
        reset(transactionService);
        when(transactionService.formatCurrency(anyDouble()))
                .thenAnswer(inv -> "$" + String.format("%.2f", (double) inv.getArgument(0)));
        when(transactionService.withdrawBuyer(any(), anyDouble()))
                .thenReturn(AuctionOperationResult.success("ok"));

        // Second bid - only delta (140 - 115 = 25) should be withdrawn
        service.bid(bidder, "auction-1", 140.0);

        verify(transactionService).withdrawBuyer(bidder, 25.0);
    }

    @Test
    void bid_noFundsReturnsFailure() throws Exception {
        PlayerMock bidder = server.addPlayer("Bidder");
        injectState(UUID.randomUUID(), 100.0);

        when(transactionService.withdrawBuyer(any(), anyDouble()))
                .thenReturn(AuctionOperationResult.failure("not enough"));

        AuctionOperationResult result = service.bid(bidder, "auction-1", 115.0);

        assertFalse(result.success());
    }

    @Test
    void bid_autoJoinsPlayerAsParticipant() throws Exception {
        PlayerMock bidder = server.addPlayer("Bidder");
        LiveAuctionState state = injectState(UUID.randomUUID(), 100.0);

        assertFalse(state.isParticipant(bidder.getUniqueId()));

        service.bid(bidder, "auction-1", 115.0);

        assertTrue(state.isParticipant(bidder.getUniqueId()));
        assertTrue(sessionRegistry.isParticipating(bidder.getUniqueId()));
    }

    @Test
    void bid_unknownAuctionReturnsFailure() {
        PlayerMock bidder = server.addPlayer("Bidder");

        AuctionOperationResult result = service.bid(bidder, "nonexistent", 150.0);

        assertFalse(result.success());
    }

    @Test
    void joinAuction_alreadyInReturnsFail() throws Exception {
        PlayerMock bidder = server.addPlayer("Bidder");
        injectState(UUID.randomUUID(), 100.0);

        service.joinAuction(bidder, "auction-1");
        AuctionOperationResult result = service.joinAuction(bidder, "auction-1");

        assertFalse(result.success());
    }
}
