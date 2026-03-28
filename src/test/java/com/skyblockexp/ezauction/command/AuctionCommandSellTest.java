package com.skyblockexp.ezauction.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.bukkit.Material;
import org.bukkit.command.Command;
import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.gui.AuctionMenu;
import com.skyblockexp.ezauction.gui.AuctionOrderMenu;
import com.skyblockexp.ezauction.gui.AuctionSellMenu;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class AuctionCommandSellTest {

    private ServerMock server;

    @BeforeEach
    void setup() {
        server = MockBukkit.getOrCreateMock();
    }

    @AfterEach
    void tearDown() {
        try {
            MockBukkit.unmock();
        } catch (Throwable ignored) {}
    }

    @Test
    void sellOpOpensSellMenu() {
        PlayerMock player = server.addPlayer("seller");
        player.setOp(true);
        player.getInventory().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.DIAMOND));

        AuctionMenu menu = mock(AuctionMenu.class);
        AuctionSellMenu sell = mock(AuctionSellMenu.class);
        AuctionCommand cmd = new AuctionCommand(mock(AuctionManager.class), menu, sell, mock(AuctionOrderMenu.class), mock(), mock(), mock(), mock(), null);

        boolean handled = cmd.onCommand(player, mock(Command.class), "auction", new String[]{"sell"});
        assertTrue(handled);
        verify(sell).openSellMenu(org.mockito.Mockito.eq(player), org.mockito.ArgumentMatchers.any());
    }
}
