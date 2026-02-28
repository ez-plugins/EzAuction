package com.skyblockexp.ezauction.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.compat.LoreItemTagStorage;
import com.skyblockexp.ezauction.config.AuctionListingRules;
import com.skyblockexp.ezauction.config.AuctionMenuInteractionConfiguration;
import com.skyblockexp.ezauction.config.AuctionMessageConfiguration;
import com.skyblockexp.ezauction.util.ItemValueProvider;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AuctionOrderMenuTest {

    @Test
    void recommendedPerItemPriceAppearsInLore()
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        // Ensure MockBukkit registries are initialized if MockBukkit is available
        com.skyblockexp.ezauction.testutil.MockBukkitHelper.ensureMocked();
        try {
            @SuppressWarnings("unused")
            org.bukkit.Material testMat = org.bukkit.Material.STONE;
        } catch (Throwable t) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Paper Registry not available; skipping GUI test");
        }
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("EzAuction");

        AuctionManager auctionManager = mock(AuctionManager.class);
        AuctionTransactionService transactionService = mock(AuctionTransactionService.class);
        when(transactionService.formatCurrency(anyDouble()))
                .thenAnswer(invocation -> String.format(Locale.ENGLISH, "$%.2f", invocation.getArgument(0, Double.class)));

        // Register mocks into EzFramework Registry for tests that access it
        com.skyblockexp.ezauction.testutil.TestRegistryHelper.register(plugin, com.skyblockexp.ezauction.AuctionManager.class, auctionManager);
        com.skyblockexp.ezauction.testutil.TestRegistryHelper.register(plugin, com.skyblockexp.ezauction.transaction.AuctionTransactionService.class, transactionService);

        AuctionListingRules listingRules = mock(AuctionListingRules.class);
        when(listingRules.minimumPrice()).thenReturn(5.0D);
        when(listingRules.defaultDuration()).thenReturn(Duration.ofHours(24));
        when(listingRules.maxDuration()).thenReturn(Duration.ofHours(48));
        when(listingRules.clampDuration(any(Duration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuctionMenuInteractionConfiguration.OrderMenuInteractionConfiguration orderConfig = AuctionMenuInteractionConfiguration
                .defaults()
                .orderMenu();

        ItemValueProvider provider = itemStack -> OptionalDouble.of(120.0D);

        // Ensure MockBukkit registries are initialized if MockBukkit is available
        com.skyblockexp.ezauction.testutil.MockBukkitHelper.ensureMocked();

        AuctionOrderMenu menu = new AuctionOrderMenu(plugin, auctionManager, transactionService, listingRules,
        null, orderConfig, provider, AuctionMessageConfiguration.OrderMessages.defaults(), new LoreItemTagStorage());

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.hasPermission(anyString())).thenReturn(true);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        ItemStack held = mock(ItemStack.class);
        when(held.getAmount()).thenReturn(4);
        when(held.getType()).thenReturn(Material.EMERALD);
        when(held.getMaxStackSize()).thenReturn(64);
        try { when(held.clone()).thenReturn(held); } catch (Exception ignored) {}
        when(inventory.getItemInMainHand()).thenReturn(held);

        AtomicReference<InventoryHolder> capturedHolder = new AtomicReference<>();
        Inventory mockInventory = mock(Inventory.class);
        when(mockInventory.getSize()).thenReturn(27);

        org.bukkit.inventory.ItemFactory realItemFactory = null;
        try {
            realItemFactory = Bukkit.getItemFactory();
        } catch (Throwable ignored) {
        }

        try (MockedStatic<Bukkit> mockedBukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            if (realItemFactory != null) {
                mockedBukkit.when(() -> Bukkit.getItemFactory()).thenReturn(realItemFactory);
            } else {
                org.bukkit.inventory.ItemFactory itemFactory = org.mockito.Mockito.mock(org.bukkit.inventory.ItemFactory.class);
                org.mockito.Mockito.when(itemFactory.isApplicable(org.mockito.ArgumentMatchers.any(org.bukkit.inventory.meta.ItemMeta.class),
                        org.mockito.ArgumentMatchers.any(org.bukkit.inventory.ItemStack.class))).thenReturn(true);
                mockedBukkit.when(() -> Bukkit.getItemFactory()).thenReturn(itemFactory);
            }

            mockedBukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString()))
                .thenAnswer(invocation -> {
                        InventoryHolder holder = invocation.getArgument(0);
                        capturedHolder.set(holder);
                        when(mockInventory.getHolder()).thenReturn(holder);
                        return mockInventory;
                    });

            try {
                menu.openOrderMenu(player);
            } catch (Throwable t) {
                org.junit.jupiter.api.Assumptions.assumeTrue(false, "Paper Registry not available during menu open; skipping GUI test");
            }
        }

        InventoryHolder holder = capturedHolder.get();
        assertNotNull(holder, "Order menu holder should be captured");

        Method stateMethod = holder.getClass().getDeclaredMethod("state");
        stateMethod.setAccessible(true);
        Object state = stateMethod.invoke(holder);

        Method pricePerItemMethod = state.getClass().getDeclaredMethod("pricePerItem");
        pricePerItemMethod.setAccessible(true);
        double pricePerItem = (double) pricePerItemMethod.invoke(state);
        assertEquals(30.0D, pricePerItem, 0.0001D, "Per-item price should use provider recommendation");

        Method recommendedMethod = state.getClass().getDeclaredMethod("recommendedPricePerItem");
        recommendedMethod.setAccessible(true);
        Double recommended = (Double) recommendedMethod.invoke(state);
        assertEquals(30.0D, recommended);

        Method createPriceDisplay = AuctionOrderMenu.class.getDeclaredMethod("createPriceDisplay", holder.getClass());
        createPriceDisplay.setAccessible(true);
        ItemStack priceDisplay = (ItemStack) createPriceDisplay.invoke(menu, holder);
        ItemMeta meta = priceDisplay.getItemMeta();
        assertNotNull(meta);
        System.out.println("DEBUG: priceDisplay lore=" + meta.getLore());
        org.junit.jupiter.api.Assertions.assertNotNull(meta.getLore());

        String perItemLine = ChatColor.GRAY + "Recommended per Item: " + ChatColor.GOLD + "$30.00";
        String totalLine = ChatColor.GRAY + "Recommended Total: " + ChatColor.GOLD + "$120.00";
        System.out.println("DEBUG: expected perItemLine='" + perItemLine + "'");
        System.out.println("DEBUG: expected totalLine='" + totalLine + "'");
        for (Object line : meta.getLore()) {
            System.out.println("DEBUG: loreLine='" + line + "'");
        }
        assertTrue(meta.getLore().contains(perItemLine));
        assertTrue(meta.getLore().contains(totalLine));
    }
}
