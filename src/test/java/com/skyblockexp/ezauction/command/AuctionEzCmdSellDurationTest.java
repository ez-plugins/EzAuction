package com.skyblockexp.ezauction.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.AuctionOperationResult;
import com.skyblockexp.ezauction.config.AuctionListingRules;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration;
import com.skyblockexp.ezauction.gui.AuctionMenu;
import com.skyblockexp.ezauction.gui.AuctionOrderMenu;
import com.skyblockexp.ezauction.gui.AuctionSellMenu;
import com.skyblockexp.ezauction.gui.LiveAuctionMenu;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistory;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import java.time.Duration;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuctionEzCmdSellDurationTest {

    @Test
    void sellParsesDurationAndCreatesListing() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        AuctionManager auctionManager = mock(AuctionManager.class);
        AuctionMenu auctionMenu = mock(AuctionMenu.class);
        AuctionSellMenu auctionSellMenu = mock(AuctionSellMenu.class);
        AuctionOrderMenu auctionOrderMenu = mock(AuctionOrderMenu.class);
        AuctionTransactionHistory transactionHistory = mock(AuctionTransactionHistory.class);
        AuctionTransactionService transactionService = mock(AuctionTransactionService.class);
        AuctionListingRules listingRules = mock(AuctionListingRules.class);
        LiveAuctionMenu liveAuctionMenu = mock(LiveAuctionMenu.class);
        AuctionCommandMessageConfiguration messages = AuctionCommandMessageConfiguration.defaults();

        when(listingRules.minimumPrice()).thenReturn(0.0);
        when(listingRules.defaultDuration()).thenReturn(Duration.ofHours(1));
        when(listingRules.clampDuration(any(Duration.class))).thenAnswer(i -> i.getArgument(0));

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("test");
        when(plugin.getLogger()).thenReturn(logger);
        AuctionEzCmd cmd = new AuctionEzCmd(plugin, "auction", auctionManager, auctionMenu, auctionSellMenu, auctionOrderMenu, transactionHistory, transactionService, listingRules, liveAuctionMenu, messages);

        Player player = mock(Player.class);
        when(player.hasPermission("ezauction.auction")).thenReturn(true);
        when(player.hasPermission("ezauction.auction.sell")).thenReturn(true);
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());

        PlayerInventory inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        org.bukkit.inventory.ItemStack held = mock(org.bukkit.inventory.ItemStack.class);
        when(held.getType()).thenReturn(null);
        when(inv.getItemInMainHand()).thenReturn(held);

        Command bcmd = mock(Command.class);

        AuctionOperationResult ok = mock(AuctionOperationResult.class);
        when(ok.success()).thenReturn(true);
        when(ok.message()).thenReturn("");
        when(auctionManager.createListing(any(Player.class), any(ItemStack.class), anyDouble(), any(Duration.class))).thenReturn(ok);

        boolean handled = cmd.executeCommand(player, bcmd, "auction", new String[]{"sell", "1000", "12h"});
        assertTrue(handled);

        ArgumentCaptor<Duration> cap = ArgumentCaptor.forClass(Duration.class);
        verify(auctionManager).createListing(eq(player), any(ItemStack.class), eq(1000.0), cap.capture());
        Duration captured = cap.getValue();
        assertNotNull(captured);
        assertEquals(12, captured.toHours());
    }
}
