package com.skyblockexp.ezauction.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.gui.AuctionMenu;
import com.skyblockexp.ezauction.gui.AuctionOrderMenu;
import com.skyblockexp.ezauction.gui.AuctionSellMenu;
import org.bukkit.command.Command;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class AuctionCommandSearchTest {

    @Test
    void searchOpensBrowserWithQuery() {
        ServerMock server = MockBukkit.getOrCreateMock();
        PlayerMock player = server.addPlayer("p");
        player.setOp(true);

        AuctionMenu menu = mock(AuctionMenu.class);
        AuctionCommand cmd = new AuctionCommand(
            mock(AuctionManager.class),
            menu,
            mock(AuctionSellMenu.class),
            mock(AuctionOrderMenu.class),
            mock(com.skyblockexp.ezauction.transaction.AuctionTransactionHistory.class),
            mock(com.skyblockexp.ezauction.transaction.AuctionTransactionService.class),
            mock(com.skyblockexp.ezauction.config.AuctionListingRules.class),
            mock(com.skyblockexp.ezauction.gui.LiveAuctionMenu.class),
            null);

        boolean handled = cmd.onCommand(player, mock(Command.class), "auction", new String[]{"search", "diamond"});
        assertTrue(handled);
    }
}
