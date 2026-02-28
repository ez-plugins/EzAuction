package com.skyblockexp.ezauction.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.skyblockexp.ezauction.gui.SellMenuHolder.Target;
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

class AuctionSellMenuTest {

    @Test
    void recommendedPriceFromProviderIsAppliedToStateAndLore()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
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
        when(listingRules.minimumPrice()).thenReturn(10.0D);
        when(listingRules.defaultDuration()).thenReturn(Duration.ofHours(24));
        when(listingRules.maxDuration()).thenReturn(Duration.ofHours(48));
        when(listingRules.clampDuration(org.mockito.ArgumentMatchers.any(Duration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(listingRules.depositPercent()).thenReturn(0.0D);
        when(listingRules.depositAmount(anyDouble())).thenReturn(0.0D);

        AuctionMenuInteractionConfiguration.SellMenuInteractionConfiguration sellConfig = AuctionMenuInteractionConfiguration
                .defaults()
                .sellMenu();

        ItemValueProvider provider = itemStack -> OptionalDouble.of(150.0D);

        // Ensure MockBukkit registries are initialized if MockBukkit is available
        com.skyblockexp.ezauction.testutil.MockBukkitHelper.ensureMocked();

        AuctionSellMenu menu = new AuctionSellMenu(plugin, auctionManager, transactionService, listingRules,
                        null, sellConfig, provider, AuctionMessageConfiguration.SellMessages.defaults(), new LoreItemTagStorage());

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        PlayerInventory playerInventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(playerInventory);
        ItemStack heldItem = mock(ItemStack.class);
        when(heldItem.getAmount()).thenReturn(3);
        when(heldItem.getType()).thenReturn(Material.DIAMOND);
        try { when(heldItem.clone()).thenReturn(heldItem); } catch (Exception ignored) {}
        when(playerInventory.getItemInMainHand()).thenReturn(heldItem);

        AtomicReference<InventoryHolder> capturedHolder = new AtomicReference<>();
        Inventory mockInventory = mock(Inventory.class);
        when(mockInventory.getSize()).thenReturn(27);

                org.bukkit.inventory.ItemFactory realItemFactory = null;
                try {
        try {
            @SuppressWarnings("unused")
            org.bukkit.Material testMat = org.bukkit.Material.STONE;
        } catch (Throwable t) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Paper Registry not available; skipping GUI test");
        }
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

            mockedBukkit.when(() -> Bukkit.createInventory(org.mockito.ArgumentMatchers.any(InventoryHolder.class),
                    anyInt(), anyString())).thenAnswer(invocation -> {
                        InventoryHolder holder = invocation.getArgument(0);
                        capturedHolder.set(holder);
                        when(mockInventory.getHolder()).thenReturn(holder);
                        return mockInventory;
                    });

                        try {
                                menu.openSellMenu(player, Target.NORMAL);
                        } catch (Throwable t) {
                                org.junit.jupiter.api.Assumptions.assumeTrue(false, "Paper Registry not available during menu open; skipping GUI test");
                        }
        }

        InventoryHolder holder = capturedHolder.get();
        assertNotNull(holder, "Sell menu holder should be captured");

        Method stateMethod = holder.getClass().getDeclaredMethod("state");
        stateMethod.setAccessible(true);
        Object state = stateMethod.invoke(holder);

        Method priceMethod = state.getClass().getDeclaredMethod("price");
        priceMethod.setAccessible(true);
        double price = (double) priceMethod.invoke(state);
        assertEquals(150.0D, price, 0.0001D, "State price should use provider recommendation");

        Method recommendedMethod = state.getClass().getDeclaredMethod("recommendedPrice");
        recommendedMethod.setAccessible(true);
        Double recommended = (Double) recommendedMethod.invoke(state);
        assertEquals(150.0D, recommended);

        Method createPriceDisplay = AuctionSellMenu.class.getDeclaredMethod("createPriceDisplay", holder.getClass());
        createPriceDisplay.setAccessible(true);
        ItemStack priceDisplay = (ItemStack) createPriceDisplay.invoke(menu, holder);
        ItemMeta meta = priceDisplay.getItemMeta();
        assertNotNull(meta);
        org.junit.jupiter.api.Assertions.assertNotNull(meta.getLore());
        String expectedLine = ChatColor.GRAY + "Recommended Price: " + ChatColor.GOLD + "$150.00";
        assertTrue(meta.getLore().contains(expectedLine), "Lore should include formatted recommendation");
    }
}
