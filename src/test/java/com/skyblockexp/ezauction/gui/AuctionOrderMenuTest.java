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
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("EzAuction");

        AuctionManager auctionManager = mock(AuctionManager.class);
        AuctionTransactionService transactionService = mock(AuctionTransactionService.class);
        when(transactionService.formatCurrency(anyDouble()))
                .thenAnswer(invocation -> String.format(Locale.ENGLISH, "$%.2f", invocation.getArgument(0, Double.class)));

        AuctionListingRules listingRules = mock(AuctionListingRules.class);
        when(listingRules.minimumPrice()).thenReturn(5.0D);
        when(listingRules.defaultDuration()).thenReturn(Duration.ofHours(24));
        when(listingRules.maxDuration()).thenReturn(Duration.ofHours(48));
        when(listingRules.clampDuration(any(Duration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuctionMenuInteractionConfiguration.OrderMenuInteractionConfiguration orderConfig = AuctionMenuInteractionConfiguration
                .defaults()
                .orderMenu();

        ItemValueProvider provider = itemStack -> OptionalDouble.of(120.0D);

        AuctionOrderMenu menu = new AuctionOrderMenu(plugin, auctionManager, transactionService, listingRules,
                null, orderConfig, provider, AuctionMessageConfiguration.OrderMessages.defaults(), new LoreItemTagStorage());

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.hasPermission(anyString())).thenReturn(true);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        ItemStack held = new ItemStack(Material.EMERALD, 4);
        when(inventory.getItemInMainHand()).thenReturn(held);

        AtomicReference<InventoryHolder> capturedHolder = new AtomicReference<>();
        Inventory mockInventory = mock(Inventory.class);
        when(mockInventory.getSize()).thenReturn(27);

        try (MockedStatic<Bukkit> mockedBukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString()))
                    .thenAnswer(invocation -> {
                        InventoryHolder holder = invocation.getArgument(0);
                        capturedHolder.set(holder);
                        when(mockInventory.getHolder()).thenReturn(holder);
                        return mockInventory;
                    });

            menu.openOrderMenu(player);
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
        assertTrue(meta.hasLore());

        String perItemLine = ChatColor.GRAY + "Recommended per Item: " + ChatColor.GOLD + "$30.00";
        String totalLine = ChatColor.GRAY + "Recommended Total: " + ChatColor.GOLD + "$120.00";
        assertTrue(meta.getLore().contains(perItemLine));
        assertTrue(meta.getLore().contains(totalLine));
    }
}
