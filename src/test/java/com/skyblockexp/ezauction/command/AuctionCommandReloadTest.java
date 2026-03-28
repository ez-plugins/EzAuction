package com.skyblockexp.ezauction.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.gui.AuctionMenu;
import com.skyblockexp.ezauction.gui.AuctionSellMenu;
import com.skyblockexp.ezauction.gui.AuctionOrderMenu;

class AuctionCommandReloadTest {

    @Test
    void reloadDeniedWithoutPermission() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezauction.admin.reload")).thenReturn(false);

        AuctionCommand cmd = new AuctionCommand(
            mock(AuctionManager.class),
            mock(AuctionMenu.class),
            mock(AuctionSellMenu.class),
            mock(AuctionOrderMenu.class),
            mock(com.skyblockexp.ezauction.transaction.AuctionTransactionHistory.class),
            mock(com.skyblockexp.ezauction.transaction.AuctionTransactionService.class),
            mock(com.skyblockexp.ezauction.config.AuctionListingRules.class),
            mock(com.skyblockexp.ezauction.gui.LiveAuctionMenu.class),
            null);
        boolean handled = cmd.onCommand(sender, mock(Command.class), "auction", new String[]{"reload"});

        assertTrue(handled);
        verify(sender).sendMessage(org.mockito.ArgumentMatchers.contains("You do not have permission"));
    }
}
