package com.skyblockexp.ezauction.live;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.skyblockexp.ezauction.model.AuctionListing;
import com.skyblockexp.ezauction.live.model.LiveAuctionState;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.time.Instant;
import java.util.UUID;

class LiveAuctionStateTest {

    @BeforeEach
    void setUp() {
        MockBukkit.getOrCreateMock();
    }

    @AfterEach
    void tearDown() {
        try { MockBukkit.unmock(); } catch (Throwable ignored) {}
    }

    private LiveAuctionState makeState(double startPrice) {
        ItemStack item = new ItemStack(Material.DIAMOND);
        AuctionListing listing = new AuctionListing(
                "test-id", UUID.randomUUID(), startPrice,
                System.currentTimeMillis() + 60_000L, item, 0.0, null);
        LiveAuctionEntry entry = new LiveAuctionEntry(listing, UUID.randomUUID(), "Seller");
        return new LiveAuctionState(entry, Instant.now().plusSeconds(60));
    }

    @Test
    void noBidsInitially() {
        LiveAuctionState state = makeState(100.0);
        assertFalse(state.hasBids());
        assertNull(state.topBidder());
        assertEquals(100.0, state.currentBid());
    }

    @Test
    void updateBidSetTopBidder() {
        LiveAuctionState state = makeState(100.0);
        UUID bidder = UUID.randomUUID();
        state.updateBid(150.0, bidder, "Alice");

        assertTrue(state.hasBids());
        assertEquals(150.0, state.currentBid());
        assertEquals(bidder, state.topBidder());
        assertEquals("Alice", state.topBidderName());
    }

    @Test
    void minimumNextBidByPercent() {
        LiveAuctionState state = makeState(100.0);
        // No bids yet: first bid only needs to meet the starting price
        assertEquals(100.0, state.minimumNextBid(10.0, 0), 0.001);
    }

    @Test
    void minimumNextBidByFixed() {
        LiveAuctionState state = makeState(100.0);
        // No bids yet: first bid only needs to meet the starting price
        assertEquals(100.0, state.minimumNextBid(0, 25.0), 0.001);
    }

    @Test
    void minimumNextBidHigherWins() {
        LiveAuctionState state = makeState(200.0);
        // No bids yet: first bid only needs to meet the starting price
        assertEquals(200.0, state.minimumNextBid(5.0, 50.0), 0.001);
    }

    @Test
    void minimumNextBidAfterFirstBid_ByPercent() {
        LiveAuctionState state = makeState(100.0);
        state.updateBid(100.0, UUID.randomUUID(), "Bidder");
        // Bid placed at 100; 10% increment → minimum = 110
        assertEquals(110.0, state.minimumNextBid(10.0, 0), 0.001);
    }

    @Test
    void minimumNextBidAfterFirstBid_ByFixed() {
        LiveAuctionState state = makeState(100.0);
        state.updateBid(100.0, UUID.randomUUID(), "Bidder");
        // Bid placed at 100; fixed 25 → minimum = 125
        assertEquals(125.0, state.minimumNextBid(0, 25.0), 0.001);
    }

    @Test
    void minimumNextBidAfterFirstBid_HigherWins() {
        LiveAuctionState state = makeState(200.0);
        state.updateBid(200.0, UUID.randomUUID(), "Bidder");
        // percent: 200 * 1.05 = 210; fixed: 200 + 50 = 250 → 250 wins
        assertEquals(250.0, state.minimumNextBid(5.0, 50.0), 0.001);
    }

    @Test
    void minimumNextBidPercent_NoIncrement_ReturnsCurrentBid() {
        LiveAuctionState state = makeState(100.0);
        // Both zero — minimum = current bid (no requirement)
        assertEquals(100.0, state.minimumNextBid(0, 0), 0.001);
    }

    @Test
    void escrowAccumulates() {
        LiveAuctionState state = makeState(100.0);
        UUID bidder = UUID.randomUUID();
        state.putEscrow(bidder, 200.0);
        assertEquals(200.0, state.getEscrow(bidder), 0.001);
        state.putEscrow(bidder, 300.0);
        assertEquals(300.0, state.getEscrow(bidder), 0.001);
    }

    @Test
    void participantTracking() {
        LiveAuctionState state = makeState(100.0);
        UUID player = UUID.randomUUID();
        assertFalse(state.isParticipant(player));
        state.addParticipant(player);
        assertTrue(state.isParticipant(player));
        state.removeParticipant(player);
        assertFalse(state.isParticipant(player));
    }

    @Test
    void isNotExpiredWhenFuture() {
        LiveAuctionState state = makeState(100.0);
        assertFalse(state.isExpired());
    }
}
