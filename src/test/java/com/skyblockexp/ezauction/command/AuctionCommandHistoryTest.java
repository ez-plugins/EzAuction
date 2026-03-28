package com.skyblockexp.ezauction.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistory;
import com.skyblockexp.ezauction.gui.AuctionMenu;
import com.skyblockexp.ezauction.gui.AuctionOrderMenu;
import com.skyblockexp.ezauction.gui.AuctionSellMenu;
import org.bukkit.command.Command;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class AuctionCommandHistoryTest {

    @Test
    void historyNoPermissionHandledForPlayerWithoutPermission() {
        ServerMock server = MockBukkit.getOrCreateMock();
        PlayerMock player = server.addPlayer("p");
        // ensure no extra permissions

        AuctionTransactionHistory history = mock(AuctionTransactionHistory.class);
        AuctionCommand cmd = new AuctionCommand(mock(AuctionManager.class), mock(AuctionMenu.class), mock(AuctionSellMenu.class), mock(AuctionOrderMenu.class), history, mock(), mock(), mock(), null);
        boolean handled = cmd.onCommand(player, mock(Command.class), "auction", new String[]{"history"});
        assertTrue(handled);
    }
}
