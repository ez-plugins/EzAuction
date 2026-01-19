package com.skyblockexp.ezauction.command;

import com.skyblockexp.ezauction.gui.AuctionOrderMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.Collections;
import java.util.List;

/**
 * Handles the /order command to place a new order.
 */
public class OrderCommand implements CommandExecutor, TabCompleter {
    private final AuctionOrderMenu auctionOrderMenu;

    public OrderCommand(AuctionOrderMenu auctionOrderMenu) {
        this.auctionOrderMenu = auctionOrderMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        auctionOrderMenu.openOrderMenu(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
