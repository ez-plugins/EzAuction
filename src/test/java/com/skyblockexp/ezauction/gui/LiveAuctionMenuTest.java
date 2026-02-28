package com.skyblockexp.ezauction.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.compat.LoreItemTagStorage;
import com.skyblockexp.ezauction.config.AuctionMessageConfiguration;
import com.skyblockexp.ezauction.config.AuctionValueConfiguration;
import com.skyblockexp.ezauction.live.LiveAuctionService;
import com.skyblockexp.ezauction.util.ItemValueProvider;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LiveAuctionMenuTest {

    private JavaPlugin plugin;
    private AuctionManager auctionManager;
    private AuctionTransactionService transactionService;
    private AuctionMenu auctionMenu;
    private LiveAuctionService liveAuctionService;

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
        auctionManager = mock(AuctionManager.class);
        transactionService = mock(AuctionTransactionService.class);
        auctionMenu = mock(AuctionMenu.class);
        liveAuctionService = mock(LiveAuctionService.class);

        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(server.getScheduler()).thenReturn(scheduler);
        when(plugin.getName()).thenReturn("EzAuction");
        when(liveAuctionService.isFeatureEnabled()).thenReturn(true);
        when(liveAuctionService.isQueueEnabled()).thenReturn(true);
    }

    @Test
    void appendShopPriceAddsLoreWhenProviderReturnsValue() throws Exception {
        // Ensure MockBukkit registries are initialized if MockBukkit is available
        com.skyblockexp.ezauction.testutil.MockBukkitHelper.ensureMocked();

        try {
            @SuppressWarnings("unused")
            org.bukkit.Material testMat = org.bukkit.Material.STONE;
        } catch (Throwable t) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Paper Registry not available; skipping GUI test");
        }

        ItemValueProvider shopPriceProvider = mock(ItemValueProvider.class);
        AuctionValueConfiguration valueConfiguration = AuctionValueConfiguration.defaults();
        LiveAuctionMenu menu = new LiveAuctionMenu(plugin, auctionManager, transactionService, auctionMenu,
                liveAuctionService, AuctionMessageConfiguration.LiveMessages.defaults(), valueConfiguration,
                shopPriceProvider, true, new LoreItemTagStorage());

        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.DIAMOND);
        List<String> lore = new ArrayList<>();
        OptionalDouble price = OptionalDouble.of(64.0D);
        when(shopPriceProvider.estimate(item)).thenReturn(price);
        when(transactionService.formatCurrency(price.getAsDouble())).thenReturn("64.00");

        Method method = LiveAuctionMenu.class.getDeclaredMethod("appendShopPrice", ItemStack.class, List.class);
        method.setAccessible(true);
        method.invoke(menu, item, lore);

        assertEquals(1, lore.size());
        assertEquals(ChatColor.GRAY + "Shop Price: " + ChatColor.GOLD + "64.00", lore.get(0));
    }

    @Test
    void appendShopPriceSkipsWhenDisplayDisabled() throws Exception {
        ItemValueProvider shopPriceProvider = mock(ItemValueProvider.class);
        AuctionValueConfiguration valueConfiguration = AuctionValueConfiguration.defaults();
        LiveAuctionMenu menu = new LiveAuctionMenu(plugin, auctionManager, transactionService, auctionMenu,
                liveAuctionService, AuctionMessageConfiguration.LiveMessages.defaults(), valueConfiguration,
                shopPriceProvider, false, new LoreItemTagStorage());

        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.DIAMOND);
        List<String> lore = new ArrayList<>();

        Method method = LiveAuctionMenu.class.getDeclaredMethod("appendShopPrice", ItemStack.class, List.class);
        method.setAccessible(true);
        method.invoke(menu, item, lore);

        assertTrue(lore.isEmpty());
        verifyNoInteractions(shopPriceProvider);
    }
}
