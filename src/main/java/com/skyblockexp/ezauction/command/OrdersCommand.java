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
 * Handles the /orders command for orders-only mode.
 */
public class OrdersCommand implements CommandExecutor, TabCompleter {
    private final AuctionOrderMenu auctionOrderMenu;

    public OrdersCommand(AuctionOrderMenu auctionOrderMenu) {
        this.auctionOrderMenu = auctionOrderMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        auctionOrderMenu.openAllOrdersOverview(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
