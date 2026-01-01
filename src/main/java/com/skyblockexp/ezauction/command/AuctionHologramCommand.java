package com.skyblockexp.ezauction.command;

import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration;
import com.skyblockexp.ezauction.hologram.AuctionHologramManager;
import com.skyblockexp.ezauction.hologram.AuctionHologramType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Places and removes EzAuction holographic displays.
 */
public final class AuctionHologramCommand implements CommandExecutor, TabCompleter {

    private static final int TARGET_RANGE = 6;

    private final AuctionHologramManager hologramManager;
    private final AuctionCommandMessageConfiguration.HologramMessages messages;

    public AuctionHologramCommand(AuctionHologramManager hologramManager,
            AuctionCommandMessageConfiguration.HologramMessages messages) {
        this.hologramManager = hologramManager;
        this.messages = messages != null ? messages : AuctionCommandMessageConfiguration.HologramMessages.defaults();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, messages.playersOnly());
            return true;
        }
        if (hologramManager == null) {
            sendMessage(sender, messages.disabled());
            return true;
        }
        if (args.length == 0) {
            sendUsage(player, label);
            return true;
        }
        String subcommand = args[0];
        if ("clear".equalsIgnoreCase(subcommand) || "remove".equalsIgnoreCase(subcommand)) {
            return handleClear(player);
        }
        AuctionHologramType type = AuctionHologramType.fromName(subcommand);
        if (type == null) {
            sendMessage(player, messages.unknownType().replace("{type}", subcommand));
            sendTypeList(player);
            return true;
        }
        Location placement = resolvePlacementLocation(player);
        if (placement == null) {
            sendMessage(player, messages.placementRange().replace("{range}", String.valueOf(TARGET_RANGE)));
            return true;
        }
        if (!hologramManager.ensureHologram(placement, type)) {
            sendMessage(player, messages.placementFailed());
            return true;
        }
        sendMessage(player, messages.placementSuccess()
                .replace("{display}", type.displayName())
                .replace("{type}", type.name().toLowerCase(Locale.ENGLISH)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("clear");
            for (AuctionHologramType type : AuctionHologramType.values()) {
                suggestions.add(type.name().toLowerCase(Locale.ENGLISH));
                suggestions.addAll(Arrays.stream(type.aliases())
                        .map(value -> value.toLowerCase(Locale.ENGLISH))
                        .collect(Collectors.toList()));
            }
            String prefix = args[0].toLowerCase(Locale.ENGLISH);
            return suggestions.stream()
                    .filter(value -> value.startsWith(prefix))
                    .distinct()
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private boolean handleClear(Player player) {
        Location location = player.getLocation();
        if (hologramManager.removeNearest(location)) {
            sendMessage(player, messages.cleared());
        } else {
            sendMessage(player, messages.noneFound());
        }
        return true;
    }

    private static Location resolvePlacementLocation(Player player) {
        Block target = player.getTargetBlockExact(TARGET_RANGE);
        Location base;
        if (target != null) {
            base = target.getLocation().add(0.0D, 1.0D, 0.0D);
        } else {
            base = player.getLocation().add(0.0D, 1.0D, 0.0D);
        }
        return base;
    }

    private void sendUsage(CommandSender sender, String label) {
        sendMessage(sender, messages.usage().replace("{label}", label));
        sendTypeList(sender);
    }

    private void sendTypeList(CommandSender sender) {
        if (hologramManager == null) {
            return;
        }
        sendMessage(sender, messages.typesHeading());
        for (AuctionHologramType type : AuctionHologramType.values()) {
            String message = messages.typesEntry()
                    .replace("{name}", type.name().toLowerCase(Locale.ENGLISH))
                    .replace("{display}", type.displayName());
            sendMessage(sender, message);
        }
    }

    private void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
