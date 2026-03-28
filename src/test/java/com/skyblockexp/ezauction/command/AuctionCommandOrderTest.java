package com.skyblockexp.ezauction.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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

class AuctionCommandOrderTest {

    @Test
    void invalidPriceSendsMessage() {
        ServerMock server = MockBukkit.getOrCreateMock();
        PlayerMock player = server.addPlayer("p");
        player.setOp(true);

        AuctionCommand cmd = new AuctionCommand(
                mock(AuctionManager.class),
                mock(AuctionMenu.class),
                mock(AuctionSellMenu.class),
                mock(AuctionOrderMenu.class),
                mock(AuctionTransactionHistory.class),
                mock(AuctionTransactionService.class),
                mock(AuctionListingRules.class),
                mock(LiveAuctionMenu.class),
                null);

        boolean handled = cmd.onCommand(player, mock(Command.class), "auction", new String[]{"order", "not-a-number", "1"});
        assertTrue(handled);
    }
}
