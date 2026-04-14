package com.skyblockexp.ezauction.feature;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.command.AuctionCommand;
import com.skyblockexp.ezauction.gui.AuctionMenu;
import com.skyblockexp.ezauction.gui.AuctionOrderMenu;
import com.skyblockexp.ezauction.gui.AuctionSellMenu;
import com.skyblockexp.ezauction.gui.LiveAuctionMenu;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistory;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration;
import com.skyblockexp.ezauction.config.AuctionListingRules;
import com.skyblockexp.ezauction.config.AuctionMenuConfiguration;
import com.skyblockexp.ezauction.config.AuctionMessageConfiguration;
import com.skyblockexp.ezauction.config.AuctionValueConfiguration;
import com.skyblockexp.ezauction.compat.LoreItemTagStorage;
import com.skyblockexp.ezauction.util.ItemValueProvider;

public class AuctionCommandGuiIntegrationTest {

    private ServerMock server;

    @BeforeEach
    void setup() {
        try {
            MockBukkit.unmock();
        } catch (IllegalStateException ignored) {
        }
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        try {
            MockBukkit.unmock();
        } catch (Throwable ignored) {
        }
    }

    @Test
    void runningAuctionCommandOpensBrowserGui() {
        PlayerMock player = server.addPlayer("tester-gui");
        // grant operator to satisfy permission checks in command
        player.setOp(true);

        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("EzAuction");

        AuctionManager auctionManager = mock(AuctionManager.class);
        AuctionTransactionService txService = mock(AuctionTransactionService.class);
        AuctionSellMenu sellMenu = mock(AuctionSellMenu.class);
        AuctionOrderMenu orderMenu = mock(AuctionOrderMenu.class);
        AuctionTransactionHistory history = mock(AuctionTransactionHistory.class);
        AuctionListingRules rules = mock(AuctionListingRules.class);
        LiveAuctionMenu liveMenu = mock(LiveAuctionMenu.class);
        AuctionCommandMessageConfiguration messages = AuctionCommandMessageConfiguration.defaults();

        AuctionMenu menu = new AuctionMenu(plugin,
                auctionManager,
                txService,
                AuctionMenuConfiguration.defaults(),
                AuctionMessageConfiguration.BrowserMessages.defaults(),
                AuctionValueConfiguration.defaults(),
                ItemValueProvider.none(),
                ItemValueProvider.none(),
                false,
                new LoreItemTagStorage());

        AuctionCommand cmd = new AuctionCommand(auctionManager, menu, sellMenu, orderMenu, history, txService, rules, liveMenu, messages);

        AtomicReference<InventoryHolder> capturedHolder = new AtomicReference<>();
        AtomicReference<String> capturedTitle = new AtomicReference<>();

        try (MockedStatic<Bukkit> mockedBukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getServer()).thenCallRealMethod();
            mockedBukkit.when(() -> Bukkit.getPluginManager()).thenCallRealMethod();
            mockedBukkit.when(() -> Bukkit.createInventory(org.mockito.ArgumentMatchers.any(InventoryHolder.class), anyInt(), anyString()))
                    .thenAnswer(invocation -> {
                        InventoryHolder holder = invocation.getArgument(0);
                        int size = invocation.getArgument(1);
                        String title = invocation.getArgument(2);
                        capturedHolder.set(holder);
                        capturedTitle.set(title);
                        // Use the MockBukkit Server to create a real inventory instance
                        return server.createInventory(holder, size, title);
                    });
            mockedBukkit.when(() -> Bukkit.getItemFactory()).thenCallRealMethod();

            boolean handled = cmd.onCommand(player, mock(org.bukkit.command.Command.class), "auction", new String[]{});
            assertTrue(handled, "Command should be handled");
        }

        assertNotNull(capturedHolder.get(), "Auction browser inventory should be created");
        assertNotNull(capturedTitle.get(), "Inventory title should be set");
        // Basic sanity: title contains the word 'Auction' (formatted title from defaults)
        assertTrue(capturedTitle.get().toLowerCase().contains("auction"));
    }
}
