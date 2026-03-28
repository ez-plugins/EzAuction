package com.skyblockexp.ezauction.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.gui.AuctionMenu;
import com.skyblockexp.ezauction.gui.AuctionOrderMenu;
import com.skyblockexp.ezauction.gui.AuctionSellMenu;
import org.bukkit.command.Command;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class AuctionCommandCancelTest {

    @Test
    void noArgsSendsNothingToCancelWhenNoListings() {
        ServerMock server = MockBukkit.getOrCreateMock();
        PlayerMock player = server.addPlayer("p");
        player.setOp(true);

        AuctionManager manager = mock(AuctionManager.class);
        when(manager.listActiveListings()).thenReturn(java.util.Collections.emptyList());
        when(manager.listActiveOrders()).thenReturn(java.util.Collections.emptyList());

        AuctionCommand cmd = new AuctionCommand(
            manager,
            mock(AuctionMenu.class),
            mock(AuctionSellMenu.class),
            mock(AuctionOrderMenu.class),
            mock(com.skyblockexp.ezauction.transaction.AuctionTransactionHistory.class),
            mock(com.skyblockexp.ezauction.transaction.AuctionTransactionService.class),
            mock(com.skyblockexp.ezauction.config.AuctionListingRules.class),
            mock(com.skyblockexp.ezauction.gui.LiveAuctionMenu.class),
            null);
        boolean handled = cmd.onCommand(player, mock(Command.class), "auction", new String[]{"cancel"});
        assertTrue(handled);
    }
}
