package com.skyblockexp.ezauction.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.skyblockexp.ezauction.config.AuctionValueConfiguration;
import com.skyblockexp.shop.api.ShopPriceService;
import java.util.Collections;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EzShopsItemValueProviderTest {

    private JavaPlugin plugin;
    private Server server;
    private ServicesManager servicesManager;
    private RegisteredServiceProvider<ShopPriceService> registration;
    private ShopPriceService priceService;

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
        server = mock(Server.class);
        servicesManager = mock(ServicesManager.class);
        priceService = mock(ShopPriceService.class);
        registration =
                new RegisteredServiceProvider<>(
                        ShopPriceService.class, priceService, ServicePriority.Normal, plugin);

        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(server.getServicesManager()).thenReturn(servicesManager);
        when(servicesManager.getKnownServices()).thenReturn(Set.of(ShopPriceService.class));
        when(servicesManager.getRegistration(ShopPriceService.class)).thenReturn(registration);
    }

    @Test
    void createReturnsProviderWhenServiceIsRegistered() {
        ItemValueProvider provider =
                EzShopsItemValueProvider.create(plugin, AuctionValueConfiguration.Mode.EZSHOPS_BUY);
        assertNotNull(provider);

        ItemStack itemStack = mock(ItemStack.class);
        OptionalDouble expected = OptionalDouble.of(48.5D);
        when(priceService.findBuyPrice(itemStack)).thenReturn(expected);

        OptionalDouble actual = provider.estimate(itemStack);
        assertTrue(actual.isPresent());
        assertEquals(expected.getAsDouble(), actual.getAsDouble(), 0.0001D);
    }

    @Test
    void estimateUsesSellPricesWhenConfiguredForSellMode() {
        ItemValueProvider provider =
                EzShopsItemValueProvider.create(plugin, AuctionValueConfiguration.Mode.EZSHOPS_SELL);
        assertNotNull(provider);

        ItemStack itemStack = mock(ItemStack.class);
        OptionalDouble expected = OptionalDouble.of(15.25D);
        when(priceService.findSellPrice(itemStack)).thenReturn(expected);

        OptionalDouble actual = provider.estimate(itemStack);
        assertTrue(actual.isPresent());
        assertEquals(expected.getAsDouble(), actual.getAsDouble(), 0.0001D);
    }

    @Test
    void createReturnsNullWhenServiceUnavailable() {
        when(servicesManager.getRegistration(ShopPriceService.class)).thenReturn(null);
        assertNull(EzShopsItemValueProvider.create(plugin, AuctionValueConfiguration.Mode.EZSHOPS_BUY));
    }

    @Test
    void createReturnsNullWhenServiceClassNotRegistered() {
        when(servicesManager.getKnownServices()).thenReturn(Collections.emptySet());
        assertNull(EzShopsItemValueProvider.create(plugin, AuctionValueConfiguration.Mode.EZSHOPS_BUY));
    }
}
