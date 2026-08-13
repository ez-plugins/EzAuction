package com.skyblockexp.ezauction.command;

import com.skyblockexp.ezauction.command.hologram.HologramRemovalHandler;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration.HologramMessages;
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
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Places and removes EzAuction holographic displays.
 */
public final class AuctionHologramCommand implements CommandExecutor, TabCompleter {

    private static final int TARGET_RANGE = 6;

    private final AuctionHologramManager hologramManager;
    private final HologramMessages messages;
    private final HologramRemovalHandler removalHandler;

    public AuctionHologramCommand(JavaPlugin plugin, AuctionHologramManager hologramManager,
            HologramMessages messages) {
        this.hologramManager = hologramManager;
        this.messages = messages != null ? messages : HologramMessages.defaults();
        this.removalHandler = hologramManager != null ? new HologramRemovalHandler(plugin, hologramManager, this.messages) : null;
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
        switch (subcommand.toLowerCase(Locale.ENGLISH)) {
            case "clear", "remove" -> {
                removalHandler.clear(player, rest(args));
                return true;
            }
            case "list" -> {
                removalHandler.list(player, rest(args));
                return true;
            }
            case "clearall" -> {
                removalHandler.clearAll(player, rest(args));
                return true;
            }
            default -> {
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
                Location spawned = hologramManager.ensureHologram(placement, type);
                if (spawned == null) {
                    sendMessage(player, messages.placementFailed());
                    return true;
                }
                String worldName = spawned.getWorld() != null ? spawned.getWorld().getName() : "?";
                sendMessage(player, messages.placementSuccess()
                        .replace("{display}", type.displayName())
                        .replace("{type}", type.name().toLowerCase(Locale.ENGLISH))
                        .replace("{x}", String.valueOf(spawned.getBlockX()))
                        .replace("{y}", String.valueOf(spawned.getBlockY()))
                        .replace("{z}", String.valueOf(spawned.getBlockZ()))
                        .replace("{world}", worldName));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("list");
            suggestions.add("clear");
            suggestions.add("clearall");
            suggestions.add("remove");
            for (AuctionHologramType type : AuctionHologramType.values()) {
                suggestions.add(type.name().toLowerCase(Locale.ENGLISH));
                suggestions.addAll(Arrays.stream(type.aliases())
                        .map(value -> value.toLowerCase(Locale.ENGLISH))
                        .collect(Collectors.toList()));
            }
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ENGLISH);
            return suggestions.stream()
                    .filter(value -> value.startsWith(prefix))
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ENGLISH);
            if (sub.equals("list") || sub.equals("clear") || sub.equals("clearall") || sub.equals("remove")) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("10");
                suggestions.add("32");
                suggestions.add("64");
                suggestions.add("--untagged");
                String prefix = args[1].toLowerCase(Locale.ENGLISH);
                return suggestions.stream()
                        .filter(value -> value.startsWith(prefix))
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ENGLISH);
            if (sub.equals("list") || sub.equals("clear") || sub.equals("clearall") || sub.equals("remove")) {
                String prefix = args[2].toLowerCase(Locale.ENGLISH);
                return List.of("--untagged").stream()
                        .filter(value -> value.startsWith(prefix))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    public static String translate(String message) {
        if (message == null) {
            return null;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static List<String> rest(String[] args) {
        if (args.length <= 1) {
            return List.of();
        }
        List<String> rest = new ArrayList<>(args.length - 1);
        for (int i = 1; i < args.length; i++) {
            rest.add(args[i]);
        }
        return rest;
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
        sender.sendMessage(translate(message));
    }

    static Location resolvePlacementLocation(Player player) {
        Block target = player.getTargetBlockExact(TARGET_RANGE);
        Location base;
        if (target != null) {
            base = target.getLocation().add(0.0D, 1.0D, 0.0D);
        } else {
            base = player.getLocation().add(0.0D, 1.0D, 0.0D);
        }
        return base;
    }
}
