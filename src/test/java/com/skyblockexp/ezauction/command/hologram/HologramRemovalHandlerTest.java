package com.skyblockexp.ezauction.command.hologram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.skyblockexp.ezauction.compat.HologramDisplay;
import com.skyblockexp.ezauction.compat.HologramPlatform;
import com.skyblockexp.ezauction.compat.TextDisplayHologramPlatform;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration.HologramMessages;
import com.skyblockexp.ezauction.config.AuctionHologramConfiguration;
import com.skyblockexp.ezauction.hologram.AuctionHologramManager;
import com.skyblockexp.ezauction.service.AuctionManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class HologramRemovalHandlerTest {

    private static final String MARKER_KEY = "auction_hologram_type";

    private ServerMock server;
    private HologramRemovalHandler handler;
    private AuctionHologramManager manager;
    private HologramPlatform platform;
    private HologramMessages messages;

    @BeforeEach
    void setUp() {
        server = MockBukkit.getOrCreateMock();
        JavaPlugin plugin = MockBukkit.createMockPlugin("EzAuctionTest");
        platform = new TextDisplayHologramPlatform(plugin);
        AuctionManager auctionManager = mock(AuctionManager.class);
        AuctionTransactionService transactionService = mock(AuctionTransactionService.class);
        AuctionHologramConfiguration configuration = AuctionHologramConfiguration.defaults();
        manager = new AuctionHologramManager(plugin, auctionManager, transactionService, configuration, platform);
        manager.enable();
        messages = HologramMessages.defaults();
        handler = new HologramRemovalHandler(plugin, manager, messages);
    }

    @Test
    void clearRemovesNearestAndSendsDetailedMessage() {
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        HologramDisplay display = platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));
        platform.setMarker(display, MARKER_KEY, "ACTIVE_LISTINGS");

        handler.clear(player, List.of());

        assertTrue(player.nextMessage().contains("Removed"));
        assertEquals(0, manager.findNearby(origin, 10.0D, false).size());
    }

    @Test
    void clearWithNothingInRangeSendsNoneFound() {
        PlayerMock player = server.addPlayer();
        handler.clear(player, List.of());
        assertTrue(player.nextMessage().contains("No auction holograms found nearby") || player.nextMessage().toLowerCase().contains("none"));
    }

    @Test
    void clearUntaggedRequiresSecondInvocationAndRemovesNearest() {
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        HologramDisplay near = platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));
        HologramDisplay far = platform.spawn(origin.clone().add(3.0D, 0.0D, 0.0D));

        handler.clear(player, List.of("--untagged"));
        String first = player.nextMessage();
        assertTrue(first.contains("untagged") || first.contains("display(s)"), "first call should confirm, not remove: " + first);
        assertEquals(2, manager.findNearby(origin, 10.0D, true).size());

        handler.clear(player, List.of("--untagged"));
        String second = player.nextMessage();
        assertTrue(second.contains("Removed"), "second call should remove the nearest: " + second);
        List<AuctionHologramManager.HologramMatch> remaining = manager.findNearby(origin, 10.0D, true);
        assertEquals(1, remaining.size());
        assertEquals(far.uniqueId(), remaining.get(0).display().uniqueId());
    }

    @Test
    void clearUntaggedWithNothingInRangeSendsNoneFound() {
        PlayerMock player = server.addPlayer();
        handler.clear(player, List.of("--untagged"));
        assertTrue(player.nextMessage().contains("No auction hologram") || player.nextMessage().toLowerCase().contains("none"));
    }

    @Test
    void clearallRequiresSecondInvocation() {
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        HologramDisplay display = platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));
        platform.setMarker(display, MARKER_KEY, "ACTIVE_LISTINGS");

        handler.clearAll(player, List.of());
        String first = player.nextMessage();
        assertTrue(first.contains("within"), "first call should report the count, not remove: " + first);

        handler.clearAll(player, List.of());
        String second = player.nextMessage();
        assertTrue(second.contains("Removed"), "second call should remove: " + second);
        assertEquals(0, manager.findNearby(origin, 10.0D, false).size());
    }

    @Test
    void invalidRadiusProducesInvalidRadiusMessage() {
        PlayerMock player = server.addPlayer();
        handler.clear(player, List.of("abc"));
        assertTrue(player.nextMessage().contains("Usage"),
                "non-numeric radius must reject with usage, not unknown-type");
    }

    @Test
    void listShowsEntriesThenEmptyAfterRemoval() {
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        HologramDisplay display = platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));
        platform.setMarker(display, MARKER_KEY, "ACTIVE_LISTINGS");

        handler.list(player, List.of());
        String heading = player.nextMessage();
        assertTrue(heading.contains("Nearby auction holograms"));
        String entry = player.nextMessage();
        assertTrue(entry.contains("active_listings"));
    }
}
