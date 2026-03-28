package com.skyblockexp.ezauction.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.gui.AuctionMenu;
import com.skyblockexp.ezauction.gui.AuctionOrderMenu;
import com.skyblockexp.ezauction.gui.AuctionSellMenu;
import com.skyblockexp.ezauction.gui.LiveAuctionMenu;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistory;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.config.AuctionListingRules;
import org.bukkit.command.Command;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class AuctionCommandLiveTest {

    @Test
    void liveDisabledShowsMessage() {
        ServerMock server = MockBukkit.getOrCreateMock();
        PlayerMock player = server.addPlayer("p");
        AuctionManager manager = mock(AuctionManager.class);
        when(manager.liveAuctionsEnabled()).thenReturn(false);

        AuctionCommand cmd = new AuctionCommand(
                manager,
                mock(AuctionMenu.class),
                mock(AuctionSellMenu.class),
                mock(AuctionOrderMenu.class),
                mock(AuctionTransactionHistory.class),
                mock(AuctionTransactionService.class),
                mock(AuctionListingRules.class),
                mock(LiveAuctionMenu.class),
                null);
        boolean handled = cmd.onCommand(player, mock(Command.class), "auction", new String[]{"live"});
        assertTrue(handled);
    }
}
