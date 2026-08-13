package com.skyblockexp.ezauction.command.hologram;

import com.skyblockexp.ezauction.command.AuctionHologramCommand;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration.HologramMessages;
import com.skyblockexp.ezauction.hologram.AuctionHologramManager;
import com.skyblockexp.ezauction.hologram.AuctionHologramManager.HologramMatch;
import com.skyblockexp.ezauction.hologram.AuctionHologramType;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles the {@code list}, {@code clear} and {@code clearall} subcommands of {@code /auctionhologram}.
 *
 * <p>Search origin is the player's eye location so the vertical placement offset of holograms is
 * covered. Removal radii default to the configured {@code removal-radius} and are clamped to
 * {@code max-removal-radius}.
 */
public final class HologramRemovalHandler {

    private static final long CONFIRM_WINDOW_MILLIS = 15_000L;

    private final JavaPlugin plugin;
    private final AuctionHologramManager manager;
    private final HologramMessages messages;
    private final Map<UUID, PendingClearAll> pendingConfirmations = new ConcurrentHashMap<>();

    public HologramRemovalHandler(JavaPlugin plugin, AuctionHologramManager manager, HologramMessages messages) {
        this.plugin = plugin;
        this.manager = manager;
        this.messages = messages != null ? messages : HologramMessages.defaults();
    }

    /**
     * Lists nearby holograms, nearest-first, one line per match.
     */
    public void list(Player player, List<String> args) {
        ParseResult parsed = parse(args, player);
        if (parsed.denied() != null) {
            player.sendMessage(parsed.denied());
            return;
        }
        Location eye = player.getEyeLocation();
        List<HologramMatch> matches = manager.findNearby(eye, parsed.radius(), parsed.includeUntagged());
        if (matches.isEmpty()) {
            player.sendMessage(AuctionHologramCommand.translate(messages.listEmpty()));
            return;
        }
        player.sendMessage(AuctionHologramCommand.translate(messages.listHeading()));
        int index = 0;
        for (HologramMatch match : matches) {
            index++;
            Location loc = match.display().location();
            String typeLabel = match.type() != null ? match.type().name().toLowerCase(Locale.ENGLISH)
                    : messages.unknownTypeLabel();
            String worldName = loc != null && loc.getWorld() != null ? loc.getWorld().getName() : "?";
            double x = loc != null ? loc.getX() : 0.0D;
            double y = loc != null ? loc.getY() : 0.0D;
            double z = loc != null ? loc.getZ() : 0.0D;
            String line = messages.listEntry()
                    .replace("{index}", String.valueOf(index))
                    .replace("{type}", typeLabel)
                    .replace("{world}", worldName)
                    .replace("{x}", formatCoord(x))
                    .replace("{y}", formatCoord(y))
                    .replace("{z}", formatCoord(z))
                    .replace("{distance}", String.format(Locale.US, "%.1f", match.distance()));
            player.sendMessage(AuctionHologramCommand.translate(line));
        }
    }

    /**
     * Removes the single nearest hologram. When untagged displays are included, the removal becomes a
     * two-step flow: the first invocation reports the count and arms a confirmation, the second within
     * the window removes only the nearest untagged display.
     */
    public void clear(Player player, List<String> args) {
        ParseResult parsed = parse(args, player);
        if (parsed.denied() != null) {
            player.sendMessage(parsed.denied());
            return;
        }
        if (parsed.includeUntagged()) {
            clearUntagged(player, parsed);
            return;
        }
        Location eye = player.getEyeLocation();
        List<HologramMatch> matches = manager.findNearby(eye, parsed.radius(), parsed.includeUntagged());
        if (matches.isEmpty()) {
            player.sendMessage(AuctionHologramCommand.translate(messages.noneFound()));
            return;
        }
        HologramMatch nearest = matches.get(0);
        manager.remove(nearest);
        String typeLabel = nearest.type() != null ? nearest.type().name().toLowerCase(Locale.ENGLISH)
                : messages.unknownTypeLabel();
        String message = messages.clearedDetailed()
                .replace("{type}", typeLabel)
                .replace("{distance}", String.format(Locale.US, "%.1f", nearest.distance()));
        player.sendMessage(AuctionHologramCommand.translate(message));
    }

    private void clearUntagged(Player player, ParseResult parsed) {
        UUID id = player.getUniqueId();
        PendingClearAll pending = pendingConfirmations.get(id);
        if (pending != null && !pending.isExpired()) {
            pendingConfirmations.remove(id);
            List<HologramMatch> matches = pending.matches();
            if (matches.isEmpty()) {
                player.sendMessage(AuctionHologramCommand.translate(messages.noneFound()));
                return;
            }
            HologramMatch nearest = matches.get(0);
            manager.remove(nearest);
            String typeLabel = nearest.type() != null ? nearest.type().name().toLowerCase(Locale.ENGLISH)
                    : messages.unknownTypeLabel();
            String message = messages.clearedDetailed()
                    .replace("{type}", typeLabel)
                    .replace("{distance}", String.format(Locale.US, "%.1f", nearest.distance()));
            player.sendMessage(AuctionHologramCommand.translate(message));
            return;
        }
        Location eye = player.getEyeLocation();
        List<HologramMatch> matches = manager.findNearby(eye, parsed.radius(), true);
        if (matches.isEmpty()) {
            player.sendMessage(AuctionHologramCommand.translate(messages.noneFound()));
            return;
        }
        pendingConfirmations.put(id, new PendingClearAll(matches));
        String message = messages.untaggedClearConfirm()
                .replace("{count}", String.valueOf(matches.size()))
                .replace("{radius}", String.format(Locale.US, "%.1f", parsed.radius()));
        player.sendMessage(AuctionHologramCommand.translate(message));
    }

    /**
     * Two-step removal: first invocation reports the count, second within the window removes them.
     */
    public void clearAll(Player player, List<String> args) {
        ParseResult parsed = parse(args, player);
        if (parsed.denied() != null) {
            player.sendMessage(parsed.denied());
            return;
        }
        UUID id = player.getUniqueId();
        PendingClearAll pending = pendingConfirmations.get(id);
        if (pending != null && !pending.isExpired()) {
            pendingConfirmations.remove(id);
            int removed = manager.removeAll(pending.matches());
            String message = messages.clearallDone().replace("{count}", String.valueOf(removed));
            player.sendMessage(AuctionHologramCommand.translate(message));
            return;
        }
        Location eye = player.getEyeLocation();
        List<HologramMatch> matches = manager.findNearby(eye, parsed.radius(), parsed.includeUntagged());
        if (matches.isEmpty()) {
            player.sendMessage(AuctionHologramCommand.translate(messages.noneFound()));
            return;
        }
        pendingConfirmations.put(id, new PendingClearAll(matches));
        String message = messages.clearallConfirm()
                .replace("{count}", String.valueOf(matches.size()))
                .replace("{radius}", String.format(Locale.US, "%.1f", parsed.radius()))
                .replace("{label}", player.getName());
        player.sendMessage(AuctionHologramCommand.translate(message));
    }

    /**
     * Clears any pending confirmation state when a player quits.
     */
    public void onPlayerQuit(Player player) {
        if (player != null) {
            pendingConfirmations.remove(player.getUniqueId());
        }
    }

    /**
     * Expires stale confirmation entries. Call periodically from the plugin.
     */
    public void expireStale() {
        long now = System.currentTimeMillis();
        pendingConfirmations.entrySet().removeIf(entry -> entry.getValue().createdAt() + CONFIRM_WINDOW_MILLIS < now);
    }

    private ParseResult parse(List<String> args, Player player) {
        boolean includeUntagged = false;
        Double radius = null;
        for (String arg : args) {
            if ("--untagged".equalsIgnoreCase(arg)) {
                includeUntagged = true;
                continue;
            }
            try {
                radius = Double.parseDouble(arg);
            } catch (NumberFormatException ex) {
                return ParseResult.denied(messages.usage().replace("{label}", player.getName()));
            }
        }
        if (includeUntagged && !manager.allowUntaggedRemoval()) {
            return ParseResult.denied(messages.untaggedDisabled());
        }
        double maxRadius = manager.maxRemovalRadius();
        if (radius != null && radius > maxRadius) {
            return ParseResult.denied(messages.invalidRadius()
                    .replace("{input}", String.valueOf(radius))
                    .replace("{max}", String.format(Locale.US, "%.1f", maxRadius)));
        }
        if (radius != null && radius <= 0.0D) {
            return ParseResult.denied(messages.invalidRadius()
                    .replace("{input}", String.valueOf(radius))
                    .replace("{max}", String.format(Locale.US, "%.1f", maxRadius)));
        }
        double effectiveRadius = radius != null ? radius : manager.removalRadius();
        return ParseResult.allowed(effectiveRadius, includeUntagged);
    }

    private static String formatCoord(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private record ParseResult(boolean allowed, double radius, boolean includeUntagged, String denied) {
        static ParseResult allowed(double radius, boolean includeUntagged) {
            return new ParseResult(true, radius, includeUntagged, null);
        }

        static ParseResult denied(String message) {
            return new ParseResult(false, 0.0D, false, message);
        }
    }

    private static final class PendingClearAll {
        private final List<HologramMatch> matches;
        private final long createdAt;

        private PendingClearAll(List<HologramMatch> matches) {
            this.matches = matches;
            this.createdAt = System.currentTimeMillis();
        }

        private List<HologramMatch> matches() {
            return matches;
        }

        private long createdAt() {
            return createdAt;
        }

        private boolean isExpired() {
            return createdAt + CONFIRM_WINDOW_MILLIS < System.currentTimeMillis();
        }
    }
}
