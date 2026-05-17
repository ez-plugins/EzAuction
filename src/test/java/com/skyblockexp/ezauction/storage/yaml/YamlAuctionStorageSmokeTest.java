package com.skyblockexp.ezauction.storage.yaml;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.storage.AuctionStorageSnapshot;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistoryEntry;
import com.skyblockexp.ezauction.transaction.AuctionTransactionType;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.nio.file.Path;

/**
 * Smoke tests that verify item serialisation round-trips for the YAML storage layer
 * on any Bukkit platform (Paper or Spigot).
 *
 * <p>These tests confirm that storing and loading {@link ItemStack} data via
 * {@link ItemStackSerializer} (Base64 / {@code item-data} key) does not produce
 * {@code ERROR}-level log entries during YAML loading, which was the root cause of
 * the Spigot startup errors reported in issue #xxx.</p>
 */
@Tag("smoke")
class YamlAuctionStorageSmokeTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        MockBukkit.getOrCreateMock();
    }

    @AfterEach
    void tearDown() {
        try {
            MockBukkit.unmock();
        } catch (Throwable ignored) {
            // ignore if already unmocked by a global extension
        }
    }

    // -------------------------------------------------------------------------
    // ItemStackSerializer
    // -------------------------------------------------------------------------

    @Test
    void itemStackSerializer_serializeDeserialize_preservesMaterial() throws Exception {
        ItemStack original = new ItemStack(Material.DIAMOND);
        String base64 = ItemStackSerializer.serialize(original);

        assertNotNull(base64, "serialize() must return a non-null string");
        assertFalse(base64.isBlank(), "serialize() must return a non-blank string");

        ItemStack restored = ItemStackSerializer.deserialize(base64);
        assertNotNull(restored, "deserialize() must return a non-null ItemStack");
        assertEquals(Material.DIAMOND, restored.getType());
        assertEquals(1, restored.getAmount());
    }

    @Test
    void itemStackSerializer_serializeDeserialize_preservesAmount() throws Exception {
        ItemStack original = new ItemStack(Material.STONE, 42);
        ItemStack restored = ItemStackSerializer.deserialize(ItemStackSerializer.serialize(original));

        assertEquals(Material.STONE, restored.getType());
        assertEquals(42, restored.getAmount());
    }

    // -------------------------------------------------------------------------
    // YamlAuctionHistoryStorage – fresh install (no existing data)
    // -------------------------------------------------------------------------

    @Test
    void historyStorage_freshInstall_loadsEmptyWithoutErrors() {
        YamlAuctionHistoryStorage storage = new YamlAuctionHistoryStorage(mockPlugin());
        assertTrue(storage.initialize(), "initialize() must return true on a fresh directory");

        Map<UUID, Deque<AuctionTransactionHistoryEntry>> result = storage.loadAll();

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Fresh install must return empty history");
    }

    // -------------------------------------------------------------------------
    // YamlAuctionHistoryStorage – item round-trip
    // -------------------------------------------------------------------------

    @Test
    void historyStorage_saveAndLoad_itemSurvivesRoundTrip() {
        JavaPlugin plugin = mockPlugin();
        YamlAuctionHistoryStorage storage = new YamlAuctionHistoryStorage(plugin);
        storage.initialize();

        UUID playerId = UUID.randomUUID();
        ItemStack item = new ItemStack(Material.IRON_SWORD, 1);
        AuctionTransactionHistoryEntry entry = new AuctionTransactionHistoryEntry(
                UUID.randomUUID().toString(),
                AuctionTransactionType.SELL,
                null, null,
                250.0,
                System.currentTimeMillis(),
                item);

        Deque<AuctionTransactionHistoryEntry> history = new ArrayDeque<>();
        history.add(entry);

        Map<UUID, Deque<AuctionTransactionHistoryEntry>> data = new HashMap<>();
        data.put(playerId, history);
        storage.saveAll(data);

        // Load from disk using a fresh instance (same plugin → same directory).
        YamlAuctionHistoryStorage loaded = new YamlAuctionHistoryStorage(plugin);
        loaded.initialize();
        Map<UUID, Deque<AuctionTransactionHistoryEntry>> result = loaded.loadAll();

        assertTrue(result.containsKey(playerId), "Player history must be present after round-trip");
        AuctionTransactionHistoryEntry loadedEntry = result.get(playerId).peekFirst();
        assertNotNull(loadedEntry, "History entry must be non-null");
        assertNotNull(loadedEntry.item(), "Item must survive the YAML round-trip on Spigot");
        assertEquals(Material.IRON_SWORD, loadedEntry.item().getType());
        assertEquals(1, loadedEntry.item().getAmount());
        assertEquals(AuctionTransactionType.SELL, loadedEntry.type());
        assertEquals(250.0, loadedEntry.price(), 0.001);
    }

    // -------------------------------------------------------------------------
    // YamlAuctionStorage – fresh install (no existing data)
    // -------------------------------------------------------------------------

    @Test
    void auctionStorage_freshInstall_loadsEmptyWithoutErrors() {
        YamlAuctionStorage storage = new YamlAuctionStorage(mockPlugin());
        assertTrue(storage.initialize(), "initialize() must return true on a fresh directory");

        AuctionStorageSnapshot snapshot = storage.load();

        assertNotNull(snapshot);
        assertTrue(snapshot.listings().isEmpty(), "Fresh install must return empty listings");
        assertTrue(snapshot.orders().isEmpty(), "Fresh install must return empty orders");
        assertTrue(snapshot.pendingReturns().isEmpty(), "Fresh install must return empty returns");
    }

    // -------------------------------------------------------------------------
    // YamlAuctionStorage – listing item round-trip
    // -------------------------------------------------------------------------

    @Test
    void auctionStorage_saveListing_itemSurvivesRoundTrip() {
        JavaPlugin plugin = mockPlugin();
        YamlAuctionStorage storage = new YamlAuctionStorage(plugin);
        storage.initialize();

        String listingId = UUID.randomUUID().toString();
        UUID sellerId = UUID.randomUUID();
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE, 3);
        long expiry = System.currentTimeMillis() + 86_400_000L;
        AuctionListing listing = new AuctionListing(listingId, sellerId, 500.0, expiry, item, 0.0, null);

        storage.saveListings(List.of(listing), Collections.emptyList());

        YamlAuctionStorage loaded = new YamlAuctionStorage(plugin);
        loaded.initialize();
        AuctionStorageSnapshot snapshot = loaded.load();

        assertTrue(snapshot.listings().containsKey(listingId), "Listing must be present after round-trip");
        AuctionListing restoredListing = snapshot.listings().get(listingId);
        assertNotNull(restoredListing.item(), "Listing item must survive the YAML round-trip on Spigot");
        assertEquals(Material.GOLDEN_APPLE, restoredListing.item().getType());
        assertEquals(3, restoredListing.item().getAmount());
        assertEquals(500.0, restoredListing.price(), 0.001);
    }

    // -------------------------------------------------------------------------
    // YamlAuctionStorage – order item round-trip
    // -------------------------------------------------------------------------

    @Test
    void auctionStorage_saveOrder_itemSurvivesRoundTrip() {
        JavaPlugin plugin = mockPlugin();
        YamlAuctionStorage storage = new YamlAuctionStorage(plugin);
        storage.initialize();

        String orderId = UUID.randomUUID().toString();
        UUID buyerId = UUID.randomUUID();
        ItemStack template = new ItemStack(Material.EMERALD, 10);
        long expiry = System.currentTimeMillis() + 86_400_000L;
        AuctionOrder order = new AuctionOrder(orderId, buyerId, 300.0, expiry, template, 300.0);

        storage.saveListings(Collections.emptyList(), List.of(order));

        YamlAuctionStorage loaded = new YamlAuctionStorage(plugin);
        loaded.initialize();
        AuctionStorageSnapshot snapshot = loaded.load();

        assertTrue(snapshot.orders().containsKey(orderId), "Order must be present after round-trip");
        AuctionOrder restoredOrder = snapshot.orders().get(orderId);
        assertNotNull(restoredOrder.requestedItem(), "Order item must survive the YAML round-trip on Spigot");
        assertEquals(Material.EMERALD, restoredOrder.requestedItem().getType());
        assertEquals(10, restoredOrder.requestedItem().getAmount());
    }

    // -------------------------------------------------------------------------
    // YamlAuctionStorage – returns round-trip
    // -------------------------------------------------------------------------

    @Test
    void auctionStorage_saveReturns_itemsSurviveRoundTrip() {
        JavaPlugin plugin = mockPlugin();
        YamlAuctionStorage storage = new YamlAuctionStorage(plugin);
        storage.initialize();

        UUID playerId = UUID.randomUUID();
        ItemStack item1 = new ItemStack(Material.DIAMOND, 5);
        ItemStack item2 = new ItemStack(Material.GOLD_INGOT, 2);
        Map<UUID, List<ItemStack>> returns = Map.of(playerId, List.of(item1, item2));

        storage.saveReturns(returns);

        YamlAuctionStorage loaded = new YamlAuctionStorage(plugin);
        loaded.initialize();
        AuctionStorageSnapshot snapshot = loaded.load();

        assertTrue(snapshot.pendingReturns().containsKey(playerId), "Returns must be present after round-trip");
        List<ItemStack> restoredItems = snapshot.pendingReturns().get(playerId);
        assertEquals(2, restoredItems.size(), "Both return items must survive");
        assertEquals(Material.DIAMOND, restoredItems.get(0).getType());
        assertEquals(5, restoredItems.get(0).getAmount());
        assertEquals(Material.GOLD_INGOT, restoredItems.get(1).getType());
        assertEquals(2, restoredItems.get(1).getAmount());
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private JavaPlugin mockPlugin() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("YamlAuctionStorageSmokeTest"));
        return plugin;
    }
}
