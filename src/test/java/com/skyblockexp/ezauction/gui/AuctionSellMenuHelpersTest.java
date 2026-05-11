package com.skyblockexp.ezauction.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.compat.ItemTagStorage;
import com.skyblockexp.ezauction.config.AuctionListingRules;
import com.skyblockexp.ezauction.config.AuctionMenuInteractionConfiguration;
import com.skyblockexp.ezauction.config.AuctionMessageConfiguration;
import com.skyblockexp.ezauction.compat.LoreItemTagStorage;
import com.skyblockexp.ezauction.util.ItemValueProvider;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuctionSellMenuHelpersTest {

    @BeforeAll
    static void initMockBukkit() {
        try { MockBukkit.getOrCreateMock(); } catch (IllegalStateException ignored) {}
    }

    private AuctionSellMenu createMenu() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        AuctionManager manager = mock(AuctionManager.class);
        AuctionTransactionService tx = mock(AuctionTransactionService.class);
        AuctionListingRules rules = mock(AuctionListingRules.class);
        when(rules.minimumPrice()).thenReturn(0.0D);
        when(rules.defaultDuration()).thenReturn(Duration.ofHours(24));
        when(rules.maxDuration()).thenReturn(Duration.ofHours(48));
        when(rules.depositPercent()).thenReturn(0.0D);
        when(rules.depositAmount(org.mockito.ArgumentMatchers.anyDouble())).thenReturn(0.0D);

        return new AuctionSellMenu(plugin, manager, tx, rules, null,
                AuctionMenuInteractionConfiguration.defaults().sellMenu(), ItemValueProvider.none(),
                AuctionMessageConfiguration.SellMessages.defaults(), new LoreItemTagStorage());
    }

    @Test
    void formatDurationPrintsDaysHoursMinutes() throws Exception {
        AuctionSellMenu menu = createMenu();
        Method m = AuctionSellMenu.class.getDeclaredMethod("formatDuration", java.time.Duration.class);
        m.setAccessible(true);

        assertEquals("1d 2h", m.invoke(menu, Duration.ofDays(1).plusHours(2)));
        assertEquals("30m", m.invoke(menu, Duration.ofMinutes(30)));
        assertEquals("0m", m.invoke(menu, Duration.ofMinutes(0)));
    }

    @Test
    void describeItemPrefersDisplayNameOverMaterial() throws Exception {
        AuctionSellMenu menu = createMenu();

        ItemStack named = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = named.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aSpecial Sword"));
        named.setItemMeta(meta);

        Method desc = AuctionSellMenu.class.getDeclaredMethod("describeItem", ItemStack.class);
        desc.setAccessible(true);

        assertEquals("Special Sword", desc.invoke(menu, named));
        Method fmt = AuctionSellMenu.class.getDeclaredMethod("formatMaterialName", Material.class);
        fmt.setAccessible(true);
        assertEquals("Diamond Sword", fmt.invoke(menu, Material.DIAMOND_SWORD));
    }
}
