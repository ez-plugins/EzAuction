package com.skyblockexp.ezauction.hologram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.skyblockexp.ezauction.compat.HologramDisplay;
import com.skyblockexp.ezauction.compat.HologramPlatform;
import com.skyblockexp.ezauction.compat.TextDisplayHologramPlatform;
import com.skyblockexp.ezauction.config.AuctionHologramConfiguration;
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

class AuctionHologramManagerRemovalTest {

    private static final String MARKER_KEY = "auction_hologram_type";

    private ServerMock server;
    private AuctionHologramManager manager;
    private HologramPlatform platform;

    @BeforeEach
    void setUp() {
        server = MockBukkit.getOrCreateMock();
        AuctionManager auctionManager = mock(AuctionManager.class);
        AuctionTransactionService transactionService = mock(AuctionTransactionService.class);
        JavaPlugin plugin = MockBukkit.createMockPlugin("EzAuctionTest");
        platform = new TextDisplayHologramPlatform(plugin);
        AuctionHologramConfiguration configuration = AuctionHologramConfiguration.defaults();
        manager = new AuctionHologramManager(plugin, auctionManager, transactionService, configuration, platform);
        manager.enable();
    }

    @Test
    void findNearbyReturnsTaggedSortedAndExcludesUntaggedByDefault() {
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        HologramDisplay taggedNear = platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));
        platform.setMarker(taggedNear, MARKER_KEY, AuctionHologramType.ACTIVE_LISTINGS.name());
        HologramDisplay untagged = platform.spawn(origin.clone().add(2.0D, 0.0D, 0.0D));

        List<AuctionHologramManager.HologramMatch> matches = manager.findNearby(origin, 10.0D, false);
        assertEquals(1, matches.size());
        assertEquals(AuctionHologramType.ACTIVE_LISTINGS, matches.get(0).type());

        List<AuctionHologramManager.HologramMatch> withUntagged = manager.findNearby(origin, 10.0D, true);
        assertEquals(2, withUntagged.size());
        assertTrue(withUntagged.stream().anyMatch(m -> m.type() == null
                && m.display().uniqueId().equals(untagged.uniqueId())));
    }

    @Test
    void removeRemovesNearestAndUntracks() {
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        HologramDisplay display = platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));
        platform.setMarker(display, MARKER_KEY, AuctionHologramType.ACTIVE_ORDERS.name());

        List<AuctionHologramManager.HologramMatch> matches = manager.findNearby(origin, 10.0D, false);
        assertEquals(1, matches.size());
        assertTrue(manager.remove(matches.get(0)));

        List<AuctionHologramManager.HologramMatch> after = manager.findNearby(origin, 10.0D, false);
        assertEquals(0, after.size());
    }

    @Test
    void removeAllRemovesEveryMatch() {
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        HologramDisplay a = platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));
        HologramDisplay b = platform.spawn(origin.clone().add(2.0D, 0.0D, 0.0D));
        platform.setMarker(a, MARKER_KEY, AuctionHologramType.ACTIVE_LISTINGS.name());
        platform.setMarker(b, MARKER_KEY, AuctionHologramType.ACTIVE_ORDERS.name());

        List<AuctionHologramManager.HologramMatch> matches = manager.findNearby(origin, 10.0D, false);
        int removed = manager.removeAll(matches);
        assertEquals(2, removed);
    }

    @Test
    void ensureHologramReturnsSpawnLocationOnSuccess() {
        PlayerMock player = server.addPlayer();
        Location base = new Location(player.getWorld(), 1000.0D, 64.0D, 1000.0D).add(0.0D, 1.0D, 0.0D);

        Location spawned = manager.ensureHologram(base, AuctionHologramType.ACTIVE_LISTINGS);
        assertTrue(spawned != null, "ensureHologram should return the spawn location on success");
        assertEquals(base.getBlockX(), spawned.getBlockX());
        assertEquals(0.5D, spawned.getX() - spawned.getBlockX(), 0.001D);
        assertEquals(base.getBlockY() + (int) AuctionHologramConfiguration.defaults().heightOffset(), spawned.getBlockY());
        assertEquals(base.getBlockZ(), spawned.getBlockZ());
        manager.findNearby(spawned, 1.0D, false).forEach(manager::remove);
    }

    @Test
    void ensureHologramReturnsNullOnNullInput() {
        assertTrue(manager.ensureHologram(null, AuctionHologramType.ACTIVE_LISTINGS) == null);
        assertTrue(manager.ensureHologram(new org.bukkit.Location(null, 0, 0, 0),
                AuctionHologramType.ACTIVE_LISTINGS) == null);
    }

    @Test
    void nothingInRangeReturnsEmpty() {
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        List<AuctionHologramManager.HologramMatch> matches = manager.findNearby(origin, 10.0D, false);
        assertTrue(matches.isEmpty());
    }
}
