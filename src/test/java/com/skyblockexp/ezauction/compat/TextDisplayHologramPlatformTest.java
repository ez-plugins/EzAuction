package com.skyblockexp.ezauction.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class TextDisplayHologramPlatformTest {

    private ServerMock server;
    private TextDisplayHologramPlatform platform;

    @BeforeEach
    void setUp() {
        server = MockBukkit.getOrCreateMock();
        platform = new TextDisplayHologramPlatform(MockBukkit.createMockPlugin("EzAuctionTest"));
    }

    @Test
    void isSupportedReturnsTrue() {
        assertTrue(platform.isSupported());
    }

    @Test
    void markerRoundTripReturnsTypeName() {
        PlayerMock player = server.addPlayer();
        Location location = player.getLocation();
        HologramDisplay display = platform.spawn(location);
        assertNotNull(display, "spawn should produce a TextDisplay-backed hologram");

        platform.setMarker(display, "auction_hologram_type", "ACTIVE_LISTINGS");
        String marker = platform.getMarker(display, "auction_hologram_type");

        assertEquals("ACTIVE_LISTINGS", marker, "marker must round-trip through the PDC");
    }

    @Test
    void untaggedDisplayHasNoMarker() {
        PlayerMock player = server.addPlayer();
        HologramDisplay display = platform.spawn(player.getLocation());
        assertNull(platform.getMarker(display, "auction_hologram_type"));
    }

    @Test
    void findNearbyReturnsSpawnedDisplaySortedByDistance() {
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        HologramDisplay near = platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));
        platform.setMarker(near, "auction_hologram_type", "ACTIVE_LISTINGS");
        platform.spawn(origin.clone().add(5.0D, 0.0D, 0.0D));

        var nearby = platform.findNearby(origin, 10.0D);
        assertEquals(2, nearby.size());
        assertTrue(nearby.stream().anyMatch(d -> d.uniqueId().equals(near.uniqueId())));
    }
}
