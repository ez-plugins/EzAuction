package com.skyblockexp.ezauction.command;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration;
import com.skyblockexp.ezauction.gui.AuctionSellMenu;
import com.skyblockexp.ezauction.gui.LiveAuctionMenu;
import com.skyblockexp.ezauction.gui.SellMenuHolder.Target;
import com.skyblockexp.ezauction.live.LiveSellContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * /liveauction
 *  - no args: open browser (same as "/auction live")
 *  - sell: open sell menu for live auction flow (same UX as "/auction live sell")
 */
public class LiveAuctionCommand implements CommandExecutor, TabCompleter {

    private final AuctionManager auctionManager;
    private final LiveAuctionMenu liveAuctionMenu;
    private final AuctionSellMenu auctionSellMenu;
    private final AuctionCommandMessageConfiguration messages;

    public LiveAuctionCommand(AuctionManager auctionManager,
                              LiveAuctionMenu liveAuctionMenu,
                              AuctionSellMenu auctionSellMenu,
                              AuctionCommandMessageConfiguration messages) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.liveAuctionMenu = Objects.requireNonNull(liveAuctionMenu, "liveAuctionMenu");
        this.auctionSellMenu = Objects.requireNonNull(auctionSellMenu, "auctionSellMenu");
        this.messages = messages != null ? messages : AuctionCommandMessageConfiguration.defaults();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Console check
        if (!(sender instanceof Player)) {
            sendMessage(sender, messages.general().consoleOnly());
            return true;
        }
        final Player player = (Player) sender;

        // Permission check (matches /auction live)
        if (!player.hasPermission("ezauction.auction.live")) {
            sendMessage(player, messages.live().noPermission());
            return true;
        }

        // Feature toggle
        if (!auctionManager.liveAuctionsEnabled()) {
            sendMessage(player, messages.live().disabled());
            return true;
        }

        // No args -> open live auctions
        if (args.length == 0) {
            liveAuctionMenu.open(player);
            return true;
        }

        final String sub = args[0].toLowerCase(Locale.ENGLISH);
        switch (sub) {
            case "sell" -> {
                if (!player.hasPermission("ezauction.auction.live.sell")) {
                    sendMessage(player, messages.sell().noPermission());
                    return true;
                }
                
                LiveSellContext.mark(player.getUniqueId());
                auctionSellMenu.openSellMenu(player, Target.LIVE);
                return true;
            }
            default -> {
                sendMessage(player, messages.usage().live()
                        .replace("{label}", label.toLowerCase(Locale.ENGLISH)));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("sell");
        }
        return Collections.emptyList();
    }

    private void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) return;
        sender.sendMessage(colorize(message));
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
