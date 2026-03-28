package com.skyblockexp.ezauction.feature;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.skyblockexp.ezauction.command.AuctionCommand;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration;
import com.skyblockexp.ezauction.config.AuctionListingRules;
import com.skyblockexp.ezauction.gui.AuctionMenu;
import com.skyblockexp.ezauction.gui.AuctionOrderMenu;
import com.skyblockexp.ezauction.gui.AuctionSellMenu;
import com.skyblockexp.ezauction.gui.LiveAuctionMenu;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistory;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.AuctionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CommandsMockBukkitFeatureTest {

    private ServerMock server;

    @BeforeEach
    void setup() {
        // Use getOrCreateMock to avoid remocking if the test runner or a global
        // extension already initialized MockBukkit earlier.
        server = MockBukkit.getOrCreateMock();
    }

    @AfterEach
    void tearDown() {
        try {
            MockBukkit.unmock();
        } catch (Throwable ignored) {
            // ignore if already unmocked by global listener/extension
        }
    }

    @Test
    void sellSubcommandOpensSellMenu() {
        PlayerMock player = server.addPlayer("tester");

        AuctionManager auctionManager = mock(AuctionManager.class);
        AuctionMenu auctionMenu = mock(AuctionMenu.class);
        AuctionSellMenu sellMenu = mock(AuctionSellMenu.class);
        AuctionOrderMenu orderMenu = mock(AuctionOrderMenu.class);
        AuctionTransactionHistory history = mock(AuctionTransactionHistory.class);
        AuctionTransactionService txService = mock(AuctionTransactionService.class);
        AuctionListingRules rules = mock(AuctionListingRules.class);
        LiveAuctionMenu liveMenu = mock(LiveAuctionMenu.class);
        AuctionCommandMessageConfiguration messages = AuctionCommandMessageConfiguration.defaults();

        // grant operator to satisfy permission checks in command
        player.setOp(true);
        // give a valid item in hand so the sell subcommand opens the sell menu
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND));

        AuctionCommand cmd = new AuctionCommand(auctionManager, auctionMenu, sellMenu, orderMenu, history, txService, rules, liveMenu, messages);

        boolean handled = cmd.onCommand(player, mock(Command.class), "auction", new String[]{"sell"});
        assertTrue(handled);
        verify(sellMenu).openSellMenu(eq(player), any());
    }

    @Test
    void noArgsOpensBrowser() {
        PlayerMock player = server.addPlayer("tester2");

        AuctionManager auctionManager = mock(AuctionManager.class);
        AuctionMenu auctionMenu = mock(AuctionMenu.class);
        AuctionSellMenu sellMenu = mock(AuctionSellMenu.class);
        AuctionOrderMenu orderMenu = mock(AuctionOrderMenu.class);
        AuctionTransactionHistory history = mock(AuctionTransactionHistory.class);
        AuctionTransactionService txService = mock(AuctionTransactionService.class);
        AuctionListingRules rules = mock(AuctionListingRules.class);
        LiveAuctionMenu liveMenu = mock(LiveAuctionMenu.class);
        AuctionCommandMessageConfiguration messages = AuctionCommandMessageConfiguration.defaults();

        // grant operator to satisfy permission checks in command
        player.setOp(true);

        AuctionCommand cmd = new AuctionCommand(auctionManager, auctionMenu, sellMenu, orderMenu, history, txService, rules, liveMenu, messages);

        boolean handled = cmd.onCommand(player, mock(Command.class), "auction", new String[]{});
        assertTrue(handled);
        verify(auctionMenu).openBrowser(player);
    }
}
