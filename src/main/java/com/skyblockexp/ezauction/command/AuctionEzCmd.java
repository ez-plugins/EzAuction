package com.skyblockexp.ezauction.command;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.config.AuctionListingRules;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration;
import com.skyblockexp.ezauction.gui.AuctionMenu;
import com.skyblockexp.ezauction.gui.AuctionOrderMenu;
import com.skyblockexp.ezauction.gui.AuctionSellMenu;
import com.skyblockexp.ezauction.gui.LiveAuctionMenu;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistory;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Backwards-compatible adapter used by older tests and callers. Delegates to {@link AuctionCommand}.
 */
public class AuctionEzCmd {
    private final AuctionCommand delegate;

    public AuctionEzCmd(JavaPlugin plugin, String label, AuctionManager auctionManager, AuctionMenu auctionMenu,
                        AuctionSellMenu auctionSellMenu, AuctionOrderMenu auctionOrderMenu,
                        AuctionTransactionHistory transactionHistory, AuctionTransactionService transactionService,
                        AuctionListingRules listingRules, LiveAuctionMenu liveAuctionMenu,
                        AuctionCommandMessageConfiguration messages) {
        this.delegate = new AuctionCommand(auctionManager, auctionMenu, auctionSellMenu, auctionOrderMenu,
                transactionHistory, transactionService, listingRules, liveAuctionMenu, messages);
    }

    public boolean executeCommand(Player player, Command command, String label, String[] args) {
        return delegate.onCommand(player, command, label, args);
    }
}
