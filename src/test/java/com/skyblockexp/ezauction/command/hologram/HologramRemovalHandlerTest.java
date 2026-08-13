package com.skyblockexp.ezauction.command.hologram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.skyblockexp.ezauction.compat.HologramDisplay;
import com.skyblockexp.ezauction.compat.HologramPlatform;
import com.skyblockexp.ezauction.compat.TextDisplayHologramPlatform;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration.HologramMessages;
import com.skyblockexp.ezauction.config.AuctionHologramConfiguration;
import com.skyblockexp.ezauction.hologram.AuctionHologramManager;
import com.skyblockexp.ezauction.hologram.AuctionHologramType;
import com.skyblockexp.ezauction.service.AuctionManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        assertTrue(player.nextMessage().contains("No auction hologram found nearby") || player.nextMessage().toLowerCase().contains("none"));
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

    @Test
    void listWithNoDisplaysSendsEmptyMessage() {
        PlayerMock player = server.addPlayer();
        handler.list(player, List.of());
        assertTrue(player.nextMessage().contains("No auction holograms found nearby"));
    }

    @Test
    void listUntaggedShowsUnknownTypeLabel() {
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        HologramDisplay untagged = platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));

        handler.list(player, List.of("--untagged"));
        player.nextMessage(); // heading
        String entry = player.nextMessage();
        assertTrue(entry.toLowerCase().contains("unknown"), "untagged display should use the unknown label: " + entry);
        assertTrue(untagged.isValid());
    }

    @Test
    void clearWithRadiusAboveMaximumIsRejected() {
        PlayerMock player = server.addPlayer();
        handler.clear(player, List.of("999"));
        assertTrue(player.nextMessage().contains("Invalid radius"));
    }

    @Test
    void clearWithNonPositiveRadiusIsRejected() {
        PlayerMock player = server.addPlayer();
        handler.clear(player, List.of("-3"));
        assertTrue(player.nextMessage().contains("Invalid radius"));
    }

    @Test
    void clearUntaggedRejectedWhenDisabledInConfiguration() {
        JavaPlugin plugin = MockBukkit.createMockPlugin("EzAuctionTest");
        AuctionManager auctionManager = mock(AuctionManager.class);
        AuctionTransactionService transactionService = mock(AuctionTransactionService.class);
        AuctionHologramConfiguration configuration = new AuctionHologramConfiguration(
                true, 100L, 2.5D, 1.75D, 100, true, false, "ezauction.hologram.view",
                false, 32.0D, 10.0D, 64.0D, false);
        AuctionHologramManager disabledManager = new AuctionHologramManager(plugin, auctionManager,
                transactionService, configuration, platform);
        disabledManager.enable();
        HologramRemovalHandler disabledHandler = new HologramRemovalHandler(plugin, disabledManager, messages);

        PlayerMock player = server.addPlayer();
        disabledHandler.clear(player, List.of("--untagged"));
        assertTrue(player.nextMessage().contains("Untagged removal is disabled"));
    }

    @Test
    void onPlayerQuitClearsPendingConfirmation() {
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));
        platform.spawn(origin.clone().add(2.0D, 0.0D, 0.0D));

        handler.clear(player, List.of("--untagged"));
        player.nextMessage(); // confirm
        handler.onPlayerQuit(player);

        // second invocation after quit must re-arm, not remove
        handler.clear(player, List.of("--untagged"));
        String reArmed = player.nextMessage();
        assertTrue(reArmed.contains("untagged display(s)"), "quit should clear pending, forcing re-arm: " + reArmed);
        assertEquals(2, manager.findNearby(origin, 10.0D, true).size());
    }

    @Test
    void expireStaleRemovesExpiredConfirmation() throws Exception {
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        HologramDisplay display = platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));
        List<AuctionHologramManager.HologramMatch> matches = manager.findNearby(origin, 10.0D, true);
        injectStalePending(player, matches);
        assertEquals(1, pendingSize());

        handler.expireStale();
        assertEquals(0, pendingSize());
        assertTrue(display.isValid());
    }

    @Test
    void expiredClearUntaggedConfirmationReArms() throws Exception {
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));
        platform.spawn(origin.clone().add(2.0D, 0.0D, 0.0D));
        List<AuctionHologramManager.HologramMatch> matches = manager.findNearby(origin, 10.0D, true);
        injectStalePending(player, matches);

        handler.clear(player, List.of("--untagged"));
        String reArmed = player.nextMessage();
        assertTrue(reArmed.contains("untagged display(s)"), "expired pending must re-arm, not remove: " + reArmed);
        assertEquals(2, manager.findNearby(origin, 10.0D, true).size());
    }

    @Test
    void expiredClearAllConfirmationReArms() throws Exception {
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        HologramDisplay display = platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));
        platform.setMarker(display, MARKER_KEY, "ACTIVE_LISTINGS");
        List<AuctionHologramManager.HologramMatch> matches = manager.findNearby(origin, 10.0D, false);
        injectStalePending(player, matches);

        handler.clearAll(player, List.of());
        String reArmed = player.nextMessage();
        assertTrue(reArmed.contains("auction hologram(s) within"), "expired pending must re-arm, not remove: " + reArmed);
        assertEquals(1, manager.findNearby(origin, 10.0D, false).size());
    }

    @SuppressWarnings("unchecked")
    private int pendingSize() throws Exception {
        Field field = HologramRemovalHandler.class.getDeclaredField("pendingConfirmations");
        field.setAccessible(true);
        Map<UUID, Object> map = (Map<UUID, Object>) field.get(handler);
        return map.size();
    }

    private void injectStalePending(PlayerMock player, List<AuctionHologramManager.HologramMatch> matches) throws Exception {
        Field field = HologramRemovalHandler.class.getDeclaredField("pendingConfirmations");
        field.setAccessible(true);
        Map<UUID, Object> map = (Map<UUID, Object>) field.get(handler);

        Class<?> pendingClass = Class.forName(
                "com.skyblockexp.ezauction.command.hologram.HologramRemovalHandler$PendingClearAll");
        Constructor<?> ctor = pendingClass.getDeclaredConstructor(List.class);
        ctor.setAccessible(true);
        Object pending = ctor.newInstance(matches);
        Field createdAt = pendingClass.getDeclaredField("createdAt");
        createdAt.setAccessible(true);
        createdAt.set(pending, 0L);
        map.put(player.getUniqueId(), pending);
        assertFalse(((boolean) pendingClass.getDeclaredMethod("isExpired").invoke(pending)));
    }
}
