package com.skyblockexp.ezauction.command;

import com.skyblockexp.ezauction.service.AuctionManager;
import com.skyblockexp.ezauction.model.AuctionOperationResult;
import com.skyblockexp.ezauction.config.AuctionBackendMessages;
import com.skyblockexp.ezauction.gui.menu.BidMenu;
import com.skyblockexp.ezauction.gui.menu.LiveAuctionPickerMenu;
import com.skyblockexp.ezauction.live.LiveAuctionService;
import com.skyblockexp.ezauction.live.model.LiveAuctionState;
import com.skyblockexp.ezauction.live.preferences.PlayerLivePreferences;
import com.skyblockexp.ezauction.live.preferences.PlayerLivePreferencesManager;
import com.skyblockexp.ezauction.live.registry.LiveBidSessionRegistry;
import com.skyblockexp.ezauction.util.NumberShortcutParser;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handles the {@code /bid} command and all its sub-commands.
 *
 * <p>Sub-command overview:</p>
 * <ul>
 *   <li>{@code /bid} — shows status (active session) or lists active auctions</li>
 *   <li>{@code /bid <amount>} — places a bid on the player's currently selected auction</li>
 *   <li>{@code /bid select <id>} — joins a live auction by its ID</li>
 *   <li>{@code /bid leave} — leaves the current live auction session</li>
 *   <li>{@code /bid notify [on|off]} — toggles notification preferences</li>
 *   <li>{@code /bid notify display <types...>} — sets EzCountdown display types</li>
 *   <li>{@code /bid mutechat [on|off]} — toggles chat filter while in auction</li>
 * </ul>
 */
public final class BidCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "ezauction.bid";

    private final AuctionManager auctionManager;
    private final LiveAuctionService liveAuctionService;
    private final LiveBidSessionRegistry sessionRegistry;
    private final PlayerLivePreferencesManager preferencesManager;
    private final LiveAuctionPickerMenu pickerMenu;
    private final BidMenu bidMenu;
    private final AuctionBackendMessages.LiveMessages messages;

    public BidCommand(AuctionManager auctionManager,
            LiveAuctionService liveAuctionService,
            LiveBidSessionRegistry sessionRegistry,
            PlayerLivePreferencesManager preferencesManager,
            LiveAuctionPickerMenu pickerMenu,
            BidMenu bidMenu,
            AuctionBackendMessages.LiveMessages messages) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.liveAuctionService = Objects.requireNonNull(liveAuctionService, "liveAuctionService");
        this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry");
        this.preferencesManager = Objects.requireNonNull(preferencesManager, "preferencesManager");
        this.pickerMenu = Objects.requireNonNull(pickerMenu, "pickerMenu");
        this.bidMenu = Objects.requireNonNull(bidMenu, "bidMenu");
        this.messages = messages != null ? messages : AuctionBackendMessages.LiveMessages.defaults();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command is for players only.");
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (!auctionManager.liveAuctionsEnabled()) {
            player.sendMessage(ChatColor.RED + "Live auctions are not enabled on this server.");
            return true;
        }

        // /bid with no arguments
        if (args.length == 0) {
            handleNoArgs(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ENGLISH);

        switch (sub) {
            case "select" -> handleSelect(player, args);
            case "leave" -> handleLeave(player);
            case "notify" -> handleNotify(player, args);
            case "mutechat" -> handleMuteChat(player, args);
            default -> handleBid(player, args[0]);
        }
        return true;
    }

    // ---- sub-command handlers ----

    private void handleNoArgs(Player player) {
        Optional<String> activeId = sessionRegistry.getActiveAuctionId(player.getUniqueId());
        if (activeId.isPresent()) {
            // Open the bid GUI for the current session
            liveAuctionService.getActiveAuction(activeId.get()).ifPresentOrElse(
                    state -> bidMenu.open(player, activeId.get()),
                    () -> {
                        // Session stale — auto-clean
                        sessionRegistry.leave(player.getUniqueId());
                        openPickerOrList(player);
                    });
        } else {
            openPickerOrList(player);
        }
    }

    private void openPickerOrList(Player player) {
        Collection<LiveAuctionState> active = liveAuctionService.getActiveAuctions();
        if (active.isEmpty()) {
            player.sendMessage(colorize(messages.noActive()));
            return;
        }
        // Open GUI picker
        pickerMenu.open(player);
    }

    private void handleBid(Player player, String amountStr) {
        double amount;
        try {
            amount = NumberShortcutParser.parse(amountStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(colorize(messages.bidUsage()));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(colorize(messages.bidUsage()));
            return;
        }

        // Determine which auction to bid on
        String auctionId = resolveAuctionId(player);
        if (auctionId == null) {
            Collection<LiveAuctionState> active = liveAuctionService.getActiveAuctions();
            if (active.isEmpty()) {
                player.sendMessage(colorize(messages.noActive()));
                return;
            }
            if (active.size() == 1) {
                auctionId = active.iterator().next().id();
            } else {
                // Multiple auctions — player must select one first
                player.sendMessage(colorize(messages.selectPrompt()));
                listAuctions(player, active);
                return;
            }
        }

        AuctionOperationResult result = liveAuctionService.bid(player, auctionId, amount);
        if (!result.message().isEmpty()) {
            player.sendMessage(result.message());
        }
    }

    private void handleSelect(Player player, String[] args) {
        if (args.length < 2) {
            // No ID given — open picker
            openPickerOrList(player);
            return;
        }
        String auctionId = args[1];
        AuctionOperationResult result = liveAuctionService.joinAuction(player, auctionId);
        if (!result.message().isEmpty()) {
            player.sendMessage(result.message());
        }
    }

    private void handleLeave(Player player) {
        liveAuctionService.leaveAuction(player);
    }

    private void handleNotify(Player player, String[] args) {
        if (args.length >= 3 && args[1].equalsIgnoreCase("display")) {
            // /bid notify display <TYPE1> [TYPE2 ...]
            List<String> types = Arrays.stream(args, 2, args.length)
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());
            PlayerLivePreferences prefs = preferencesManager.getPreferences(player.getUniqueId());
            prefs.setDisplayTypeNames(types);
            preferencesManager.savePreferences(prefs);
            player.sendMessage(fmt(messages.displayUpdated(), "types", String.join(", ", types)));
            return;
        }

        // /bid notify [on|off]
        PlayerLivePreferences prefs = preferencesManager.getPreferences(player.getUniqueId());
        boolean enable;
        if (args.length >= 2) {
            enable = args[1].equalsIgnoreCase("on");
        } else {
            enable = !prefs.notificationsEnabled();
        }
        prefs.setNotificationsEnabled(enable);
        preferencesManager.savePreferences(prefs);
        player.sendMessage(colorize(enable ? messages.notificationOn() : messages.notificationOff()));
    }

    private void handleMuteChat(Player player, String[] args) {
        PlayerLivePreferences prefs = preferencesManager.getPreferences(player.getUniqueId());
        boolean enable;
        if (args.length >= 2) {
            enable = args[1].equalsIgnoreCase("on");
        } else {
            enable = !prefs.muteOtherChatInAuction();
        }
        prefs.setMuteOtherChatInAuction(enable);
        preferencesManager.savePreferences(prefs);
        player.sendMessage(colorize(enable ? messages.chatFilterOn() : messages.chatFilterOff()));
    }

    // ---- tab completion ----

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return List.of();
        if (!player.hasPermission(PERMISSION)) return List.of();

        if (args.length == 1) {
            return filterStart(args[0], List.of("select", "leave", "notify", "mutechat"));
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ENGLISH)) {
                case "select" -> liveAuctionService.getActiveAuctions().stream()
                        .map(LiveAuctionState::id)
                        .filter(id -> id.startsWith(args[1]))
                        .collect(Collectors.toList());
                case "notify" -> filterStart(args[1], List.of("on", "off", "display"));
                case "mutechat" -> filterStart(args[1], List.of("on", "off"));
                default -> List.of();
            };
        }
        if (args.length >= 3 && args[0].equalsIgnoreCase("notify") && args[1].equalsIgnoreCase("display")) {
            return filterStart(args[args.length - 1],
                    List.of("ACTION_BAR", "BOSS_BAR", "CHAT", "TITLE", "SCOREBOARD"));
        }
        return List.of();
    }

    // ---- helpers ----

    private void listAuctions(Player player, Collection<LiveAuctionState> auctions) {
        for (LiveAuctionState state : auctions) {
            String entry = fmt(messages.selectEntry(),
                    "id", state.id(),
                    "item", com.skyblockexp.ezauction.live.component.LiveAuctionComponentBuilder
                            .describeItem(state.listing().item()),
                    "seller", state.entry().sellerName(),
                    "bid", String.valueOf(state.currentBid()),
                    "seconds", String.valueOf(state.secondsRemaining()));
            player.sendMessage(entry);
        }
    }

    /** Returns the auction ID the player is currently in, or null if they're not in one. */
    private String resolveAuctionId(Player player) {
        return sessionRegistry.getActiveAuctionId(player.getUniqueId()).orElse(null);
    }

    private String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String fmt(String template, String... kvPairs) {
        String result = template;
        for (int i = 0; i + 1 < kvPairs.length; i += 2) {
            result = result.replace("{" + kvPairs[i] + "}", kvPairs[i + 1] != null ? kvPairs[i + 1] : "");
        }
        return colorize(result);
    }

    private static List<String> filterStart(String prefix, List<String> options) {
        String lower = prefix.toLowerCase(Locale.ENGLISH);
        return options.stream()
                .filter(s -> s.toLowerCase(Locale.ENGLISH).startsWith(lower))
                .collect(Collectors.toList());
    }
}
