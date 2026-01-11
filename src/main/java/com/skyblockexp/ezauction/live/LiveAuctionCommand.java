package com.skyblockexp.ezauction.live;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.gui.AuctionSellMenu;
import com.skyblockexp.ezauction.gui.LiveAuctionMenu;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles the /liveauction command for opening the live auction browser or sell menu.
 * Provides tab completion for subcommands.
 *
 * @author SkyblockExperience Team
 * @since 1.1.0
 */
public class LiveAuctionCommand implements CommandExecutor, TabCompleter {
    private final AuctionManager auctionManager;
    private final LiveAuctionMenu liveAuctionMenu;
    private final AuctionSellMenu auctionSellMenu;
    private final AuctionCommandMessageConfiguration messages;

    /**
     * Constructs a new LiveAuctionCommand.
     * @param auctionManager the auction manager
     * @param liveAuctionMenu the live auction menu
     * @param auctionSellMenu the auction sell menu
     * @param messages the command message configuration
     */
    public LiveAuctionCommand(AuctionManager auctionManager, LiveAuctionMenu liveAuctionMenu, AuctionSellMenu auctionSellMenu, AuctionCommandMessageConfiguration messages) {
        this.auctionManager = auctionManager;
        this.liveAuctionMenu = liveAuctionMenu;
        this.auctionSellMenu = auctionSellMenu;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            // Open live auction browser
            liveAuctionMenu.open(player);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("sell")) {
            // Open live auction sell menu
            auctionSellMenu.openLive(player);
            return true;
        }
        player.sendMessage(ChatColor.RED + messages.usage().live());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if ("sell".startsWith(args[0].toLowerCase())) {
                completions.add("sell");
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
