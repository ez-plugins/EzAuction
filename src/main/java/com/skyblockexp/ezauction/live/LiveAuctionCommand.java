package com.skyblockexp.ezauction.live;

import com.skyblockexp.ezauction.service.AuctionManager;
import com.skyblockexp.ezauction.gui.menu.AuctionSellMenu;
import com.skyblockexp.ezauction.gui.menu.LiveAuctionMenu;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration;
import com.skyblockexp.ezauction.util.DateUtil;
import com.skyblockexp.ezauction.util.NumberShortcutParser;
import java.time.Duration;
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
 * @author Shadow48402
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
        if (args[0].equalsIgnoreCase("sell")) {
            // Optional prefill: /liveauction sell [stackSize] [value]
            int prefillSize = -1;
            double prefillPrice = -1.0;

            if (args.length >= 2) {
                try {
                    prefillSize = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Stack size must be a whole number.");
                    return true;
                }
                if (prefillSize < 1) {
                    player.sendMessage(ChatColor.RED + "Stack size must be at least 1.");
                    return true;
                }
            }
            if (args.length >= 3) {
                try {
                    prefillPrice = NumberShortcutParser.parse(args[2]);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Invalid price. Use a number or shorthand like 5k, 1.5m, 2b.");
                    return true;
                }
                if (prefillPrice <= 0) {
                    player.sendMessage(ChatColor.RED + "Price must be positive.");
                    return true;
                }
            }

            // Show queue info and auction duration before opening the menu
            long durationSeconds = auctionManager.getConfiguration()
                    .liveAuctionConfiguration().countdownConfiguration().durationSeconds();
            int queueSize = auctionManager.listQueuedLiveAuctions().size();
            String durationText = DateUtil.formatDuration(Duration.ofSeconds(durationSeconds));
            String queueText = queueSize == 0 ? "empty" : queueSize + " in queue";
            player.sendMessage(ChatColor.GOLD + "[Live Auction] " + ChatColor.GRAY
                    + "Duration: " + ChatColor.YELLOW + durationText
                    + ChatColor.GRAY + " | Queue: " + ChatColor.YELLOW + queueText);

            auctionSellMenu.openLive(player, prefillSize, prefillPrice);
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
        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            // Suggest common stack sizes
            List<String> completions = new ArrayList<>();
            for (String size : List.of("1", "16", "32", "64")) {
                if (size.startsWith(args[1])) completions.add(size);
            }
            return completions;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("sell")) {
            // Suggest common price shorthands
            List<String> completions = new ArrayList<>();
            for (String price : List.of("100", "500", "1000", "5k", "10k", "50k", "100k", "1m", "10m")) {
                if (price.startsWith(args[2].toLowerCase())) completions.add(price);
            }
            return completions;
        }
        return Collections.emptyList();
    }

}
