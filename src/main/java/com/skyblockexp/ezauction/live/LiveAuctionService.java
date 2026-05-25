package com.skyblockexp.ezauction.live;

import com.skyblockexp.ezauction.model.AuctionListing;
import com.skyblockexp.ezauction.model.AuctionOperationResult;
import com.skyblockexp.ezauction.config.AuctionBackendMessages;
import com.skyblockexp.ezauction.config.LiveAuctionConfiguration;
import com.skyblockexp.ezauction.event.LiveAuctionBidPlacedEvent;
import com.skyblockexp.ezauction.event.LiveAuctionEndedEvent;
import com.skyblockexp.ezauction.event.LiveAuctionPlayerJoinedEvent;
import com.skyblockexp.ezauction.event.LiveAuctionPlayerLeftEvent;
import com.skyblockexp.ezauction.event.LiveAuctionStartedEvent;
import com.skyblockexp.ezauction.integration.EzCountdownIntegration;
import com.skyblockexp.ezauction.live.component.LiveAuctionComponentBuilder;
import com.skyblockexp.ezauction.live.model.LiveAuctionState;
import com.skyblockexp.ezauction.live.preferences.PlayerLivePreferences;
import com.skyblockexp.ezauction.live.preferences.PlayerLivePreferencesManager;
import com.skyblockexp.ezauction.live.registry.LiveBidSessionRegistry;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Handles live auction queueing, activation, bidding, and resolution.
 *
 * <p>Each enqueued {@link LiveAuctionEntry} is promoted to an active {@link LiveAuctionState}
 * when the announcement timer fires. Active auctions accept bids, manage player sessions, and
 * optionally drive EzCountdown display notifications. When the countdown expires the auction is
 * resolved: the top bidder's escrow is credited to the seller, all other bidders are refunded,
 * and the item is returned to the seller if there were no bids.</p>
 */
public final class LiveAuctionService {

    private final JavaPlugin plugin;
    private final AuctionTransactionService transactionService;
    private final LiveAuctionConfiguration configuration;
    private final AuctionBackendMessages.LiveMessages messages;
    private final AuctionBackendMessages.FallbackMessages fallbackMessages;
    private final EzCountdownIntegration ezCountdown;
    private final LiveBidSessionRegistry sessionRegistry;
    private final PlayerLivePreferencesManager preferencesManager;
    private final Map<UUID, List<ItemStack>> pendingReturns;

    private final Queue<LiveAuctionEntry> queue = new ConcurrentLinkedQueue<>();
    private final Map<String, LiveAuctionState> activeAuctions = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> endTasks = new ConcurrentHashMap<>();
    private final Map<String, List<BukkitTask>> reminderTasks = new ConcurrentHashMap<>();

    /** Called with the listing ID when a live auction fully resolves (sold or unsold). */
    private Consumer<String> onListingResolved;

    private BukkitTask announcementTask;
    private boolean enabled = false;

    // ---- constructors ----

    public LiveAuctionService(JavaPlugin plugin, AuctionTransactionService transactionService,
            LiveAuctionConfiguration configuration, AuctionBackendMessages.LiveMessages messages,
            AuctionBackendMessages.FallbackMessages fallbackMessages,
            EzCountdownIntegration ezCountdown,
            LiveBidSessionRegistry sessionRegistry,
            PlayerLivePreferencesManager preferencesManager,
            Map<UUID, List<ItemStack>> pendingReturns) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.transactionService = Objects.requireNonNull(transactionService, "transactionService");
        this.configuration = configuration != null ? configuration : LiveAuctionConfiguration.defaults();
        this.messages = messages != null ? messages : AuctionBackendMessages.LiveMessages.defaults();
        this.fallbackMessages = fallbackMessages != null
                ? fallbackMessages : AuctionBackendMessages.FallbackMessages.defaults();
        this.ezCountdown = ezCountdown != null ? ezCountdown : new EzCountdownIntegration(plugin, false);
        this.sessionRegistry = sessionRegistry != null ? sessionRegistry : new LiveBidSessionRegistry();
        this.preferencesManager = preferencesManager;
        this.pendingReturns = pendingReturns != null ? pendingReturns : new ConcurrentHashMap<>();
    }

    /** Backward-compatible constructor; no EzCountdown/session/preferences support. */
    public LiveAuctionService(JavaPlugin plugin, AuctionTransactionService transactionService,
            LiveAuctionConfiguration configuration, AuctionBackendMessages.LiveMessages messages,
            AuctionBackendMessages.FallbackMessages fallbackMessages) {
        this(plugin, transactionService, configuration, messages, fallbackMessages,
                new EzCountdownIntegration(plugin, false),
                new LiveBidSessionRegistry(),
                null,
                new ConcurrentHashMap<>());
    }

    // ---- lifecycle ----

    public void enable() {
        if (!configuration.enabled()) return;
        if (!configuration.queueEnabled() || !configuration.displayInChat()) return;
        long interval = Math.max(1L, configuration.announcementIntervalTicks());
        announcementTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::announceNext, interval, interval);
        enabled = true;
    }

    public void disable() {
        if (announcementTask != null) {
            announcementTask.cancel();
            announcementTask = null;
        }
        // Clean up all active auctions silently on shutdown
        for (LiveAuctionState state : List.copyOf(activeAuctions.values())) {
            cancelTasksForAuction(state.id());
            stopAllHandles(state);
            sessionRegistry.clearAuction(state);
        }
        activeAuctions.clear();
        queue.clear();
        enabled = false;
    }

    // ---- queue management ----

    public boolean enqueue(AuctionListing listing, UUID sellerId, String sellerName) {
        if (!configuration.enabled() || listing == null) return false;
        if (hasQueuedEntry(sellerId)) return false;
        String resolvedName = resolveSellerName(sellerId, sellerName);
        LiveAuctionEntry entry = new LiveAuctionEntry(listing, sellerId, resolvedName);
        if (!configuration.queueEnabled()) {
            startActiveAuction(entry);
            return true;
        }
        queue.offer(entry);
        if (announcementTask == null && configuration.displayInChat()) {
            announceNext();
        }
        return true;
    }

    public List<LiveAuctionEntry> snapshotQueue() {
        return queue.isEmpty() ? List.of() : List.copyOf(new ArrayList<>(queue));
    }

    public LiveAuctionEntry poll() {
        return queue.poll();
    }

    /**
     * Returns {@code true} if the seller already has an entry in the queue.
     */
    public boolean hasQueuedEntry(UUID sellerId) {
        if (sellerId == null) return false;
        return queue.stream().anyMatch(e -> sellerId.equals(e.sellerId()));
    }

    /**
     * Removes and returns the queued entry for the given seller, if present.
     */
    public Optional<LiveAuctionEntry> dequeueForSeller(UUID sellerId) {
        if (sellerId == null) return Optional.empty();
        for (java.util.Iterator<LiveAuctionEntry> it = queue.iterator(); it.hasNext(); ) {
            LiveAuctionEntry e = it.next();
            if (sellerId.equals(e.sellerId())) {
                it.remove();
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the 1-based queue position for the seller.
     * Returns 0 if not found, -1 if queue is disabled.
     */
    public int queuePositionOf(UUID sellerId) {
        if (!configuration.queueEnabled()) return -1;
        if (sellerId == null) return 0;
        int pos = 1;
        for (LiveAuctionEntry e : queue) {
            if (sellerId.equals(e.sellerId())) return pos;
            pos++;
        }
        return 0;
    }

    /**
     * Force-cancels an active live auction by its ID.
     * Refunds all bidders, returns the item to the seller, and broadcasts a cancellation.
     */
    public AuctionOperationResult forceCancel(String auctionId) {
        LiveAuctionState state = activeAuctions.remove(auctionId);
        if (state == null) {
            return AuctionOperationResult.failure(fmt(messages.notFound(), "id", auctionId));
        }
        cancelTasksForAuction(auctionId);
        stopAllHandles(state);
        sessionRegistry.clearAuction(state);

        // Refund all bidders
        for (Map.Entry<UUID, Double> escrowEntry : state.escrowSnapshot().entrySet()) {
            transactionService.refundOrderBuyer(escrowEntry.getKey(), escrowEntry.getValue());
        }

        // Return item to seller
        ItemStack item = state.listing().item();
        if (item != null) {
            UUID sellerId = state.entry().sellerId();
            Player sellerOnline = Bukkit.getPlayer(sellerId);
            if (sellerOnline != null && sellerOnline.isOnline()) {
                Map<Integer, ItemStack> leftover = sellerOnline.getInventory().addItem(item.clone());
                for (ItemStack remainder : leftover.values()) {
                    addToPendingReturns(sellerId, remainder);
                }
            } else {
                addToPendingReturns(sellerId, item.clone());
            }
        }

        Bukkit.getPluginManager().callEvent(new LiveAuctionEndedEvent(state, null));
        broadcastToAll(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(
                fmt(messages.activeCancelled(), "item",
                        LiveAuctionComponentBuilder.describeItem(state.listing().item()))));
        return AuctionOperationResult.success(
                fmt(messages.activeCancelled(), "item",
                        LiveAuctionComponentBuilder.describeItem(state.listing().item())));
    }

    // ---- active auction queries ----

    public Collection<LiveAuctionState> getActiveAuctions() {
        return Collections.unmodifiableCollection(activeAuctions.values());
    }

    public Optional<LiveAuctionState> getActiveAuction(String auctionId) {
        return Optional.ofNullable(activeAuctions.get(auctionId));
    }

    // ---- bidding ----

    /**
     * Places a bid from {@code player} on the auction identified by {@code auctionId}.
     *
     * <p>This method must be called from the main server thread.</p>
     *
     * @return success with a confirmation message, or failure with the reason
     */
    public AuctionOperationResult bid(Player player, String auctionId, double amount) {
        LiveAuctionState state = activeAuctions.get(auctionId);
        if (state == null || state.isExpired()) {
            return AuctionOperationResult.failure(fmt(messages.notFound(), "id", auctionId));
        }

        // Seller cannot bid on their own auction
        if (player.getUniqueId().equals(state.entry().sellerId())) {
            return AuctionOperationResult.failure(colorize(messages.bidSelf()));
        }

        // Amount must meet minimum
        double minimum = state.minimumNextBid(
                configuration.countdownConfiguration().minimumBidIncrementPercent(),
                configuration.countdownConfiguration().minimumBidIncrementFixed());
        if (amount < minimum) {
            return AuctionOperationResult.failure(fmt(messages.bidTooLow(),
                    "amount", transactionService.formatCurrency(amount),
                    "minimum", transactionService.formatCurrency(minimum)));
        }

        // Allow external plugins to cancel the bid before any funds change hands
        LiveAuctionBidPlacedEvent event = new LiveAuctionBidPlacedEvent(state, player, amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return AuctionOperationResult.failure("");

        // Incremental escrow: only charge the delta above what this player already has in escrow
        double existingEscrow = state.getEscrow(player.getUniqueId());
        double delta = amount - existingEscrow;
        if (delta > 0) {
            AuctionOperationResult withdraw = transactionService.withdrawBuyer(player, delta);
            if (!withdraw.success()) {
                return AuctionOperationResult.failure(fmt(messages.bidNoFunds(),
                        "amount", transactionService.formatCurrency(delta)));
            }
        }

        // Commit bid to state
        state.putEscrow(player.getUniqueId(), amount);
        state.updateBid(amount, player.getUniqueId(), player.getName());

        // Auto-join the player as an active participant if they aren't already
        if (!state.isParticipant(player.getUniqueId())) {
            doJoin(player, state);
        }

        double nextMin = state.minimumNextBid(
                configuration.countdownConfiguration().minimumBidIncrementPercent(),
                configuration.countdownConfiguration().minimumBidIncrementFixed());

        // Anti-snipe: if the bid lands within the minimum-finish window, extend the auction timer
        long minFinish = configuration.countdownConfiguration().minTimeBeforeFinishSeconds();
        if (minFinish > 0 && state.secondsRemaining() < minFinish) {
            Instant newExpiry = Instant.now().plusSeconds(minFinish);
            state.extendExpiry(newExpiry);
            BukkitTask oldTask = endTasks.remove(state.id());
            if (oldTask != null) oldTask.cancel();
            BukkitTask newEndTask = plugin.getServer().getScheduler()
                    .runTaskLater(plugin, () -> endAuction(state), minFinish * 20L);
            endTasks.put(state.id(), newEndTask);
        }

        Component bidComponent = LiveAuctionComponentBuilder.buildBidUpdateComponent(
                state, transactionService.formatCurrency(amount),
                transactionService.formatCurrency(nextMin), player.getName(), nextMin);
        broadcastToAll(bidComponent);

        return AuctionOperationResult.success(fmt(messages.bidPlaced(),
                "player", player.getName(),
                "amount", transactionService.formatCurrency(amount),
                "item", LiveAuctionComponentBuilder.describeItem(state.listing().item()),
                "minimum", transactionService.formatCurrency(nextMin)));
    }

    // ---- participant management ----

    public AuctionOperationResult joinAuction(Player player, String auctionId) {
        LiveAuctionState state = activeAuctions.get(auctionId);
        if (state == null || state.isExpired()) {
            return AuctionOperationResult.failure(fmt(messages.notFound(), "id", auctionId));
        }
        if (state.isParticipant(player.getUniqueId())) {
            return AuctionOperationResult.failure(colorize(messages.alreadyIn()));
        }

        doJoin(player, state);
        Bukkit.getPluginManager().callEvent(new LiveAuctionPlayerJoinedEvent(state, player));

        player.sendMessage(fmt(messages.inAuction(),
                "item", LiveAuctionComponentBuilder.describeItem(state.listing().item()),
                "seller", state.entry().sellerName(),
                "bid", transactionService.formatCurrency(state.currentBid())));

        return AuctionOperationResult.success(fmt(messages.joined(),
                "player", player.getName(),
                "item", LiveAuctionComponentBuilder.describeItem(state.listing().item())));
    }

    public void leaveAuction(Player player) {
        Optional<String> sessionAuction = sessionRegistry.leave(player.getUniqueId());
        if (sessionAuction.isEmpty()) return;
        String auctionId = sessionAuction.get();
        LiveAuctionState state = activeAuctions.get(auctionId);
        if (state != null) {
            state.removeParticipant(player.getUniqueId());
            String handle = state.removeNotificationHandle(player.getUniqueId());
            if (handle != null) ezCountdown.stopNotification(handle);
            Bukkit.getPluginManager().callEvent(new LiveAuctionPlayerLeftEvent(state, player));
        }
        player.sendMessage(fmt(messages.left(), "player", player.getName()));
    }

    // ---- feature flags ----

    public boolean isFeatureEnabled() { return configuration.enabled(); }

    public boolean isQueueEnabled() { return configuration.queueEnabled(); }

    public boolean displayInChat() { return configuration.displayInChat(); }

    public boolean isEnabled() { return enabled; }

    /**
     * Registers a callback that is invoked (on the main thread) with the listing ID whenever a
     * live auction fully resolves — whether sold or returned unsold. Use this to remove the
     * corresponding entry from the normal listing store and persist the change.
     */
    public void setListingResolutionCallback(Consumer<String> callback) {
        this.onListingResolved = callback;
    }

    // ---- private: queue and auction lifecycle ----

    private void announceNext() {
        LiveAuctionEntry entry = poll();
        if (entry == null) return;
        startActiveAuction(entry);
    }

    private void startActiveAuction(LiveAuctionEntry entry) {
        if (!configuration.displayInChat()) return;

        long durationSeconds = configuration.countdownConfiguration().durationSeconds();
        Instant expiresAt = Instant.now().plusSeconds(durationSeconds);
        LiveAuctionState state = new LiveAuctionState(entry, expiresAt);

        LiveAuctionStartedEvent event = new LiveAuctionStartedEvent(state);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        activeAuctions.put(state.id(), state);

        // Broadcast rich announcement to all online players
        String formattedPrice = transactionService.formatCurrency(entry.price());
        double minBid = state.minimumNextBid(
                configuration.countdownConfiguration().minimumBidIncrementPercent(),
                configuration.countdownConfiguration().minimumBidIncrementFixed());
        Component announcement = LiveAuctionComponentBuilder.buildAnnouncementComponent(
                state, formattedPrice, transactionService.formatCurrency(minBid), minBid);
        broadcastToAll(announcement);

        // Send a dedicated "your listing is now live" notification to the seller if they're online
        Player seller = Bukkit.getPlayer(entry.sellerId());
        if (seller != null && seller.isOnline()) {
            seller.sendMessage(LiveAuctionComponentBuilder.buildSellerLiveComponent(state));
        }

        // Start EzCountdown for all currently online players who want it
        if (ezCountdown.isAvailable()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                startCountdownForPlayer(p, state, durationSeconds);
            }
        }

        // Schedule end task
        BukkitTask endTask = plugin.getServer().getScheduler()
                .runTaskLater(plugin, () -> endAuction(state), durationSeconds * 20L);
        endTasks.put(state.id(), endTask);

        // Schedule chat reminders
        scheduleReminders(state, durationSeconds);
    }

    private void scheduleReminders(LiveAuctionState state, long durationSeconds) {
        List<BukkitTask> tasks = new ArrayList<>();
        var cfg = configuration.countdownConfiguration();

        // Remind at specific remaining-second marks (e.g. 30s, 10s, 5s left)
        for (long secondsMark : cfg.chatReminderAtSeconds()) {
            if (secondsMark > 0 && secondsMark < durationSeconds) {
                long delayTicks = (durationSeconds - secondsMark) * 20L;
                BukkitTask t = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (activeAuctions.containsKey(state.id())) {
                        sendReminderToAll(state, secondsMark);
                    }
                }, delayTicks);
                tasks.add(t);
            }
        }

        // Repeating interval reminders
        long intervalSeconds = cfg.chatReminderIntervalSeconds();
        if (intervalSeconds > 0) {
            long intervalTicks = intervalSeconds * 20L;
            BukkitTask t = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                if (activeAuctions.containsKey(state.id())) {
                    sendReminderToAll(state, state.secondsRemaining());
                }
            }, intervalTicks, intervalTicks);
            tasks.add(t);
        }

        if (!tasks.isEmpty()) {
            reminderTasks.put(state.id(), tasks);
        }
    }

    private void sendReminderToAll(LiveAuctionState state, long secondsLeft) {
        String formattedBid = transactionService.formatCurrency(state.currentBid());
        Component reminder = LiveAuctionComponentBuilder.buildReminderComponent(state, formattedBid, secondsLeft);
        broadcastToAll(reminder);
    }

    private void endAuction(LiveAuctionState state) {
        // Guard against double-end
        if (activeAuctions.remove(state.id()) == null) return;

        cancelTasksForAuction(state.id());
        stopAllHandles(state);
        sessionRegistry.clearAuction(state);

        UUID winner = state.topBidder();

        if (winner != null) {
            double winAmount = state.getEscrow(winner);
            transactionService.creditSeller(state.entry().sellerId(), winAmount);

            // Refund all losing bidders
            for (Map.Entry<UUID, Double> escrowEntry : state.escrowSnapshot().entrySet()) {
                if (!escrowEntry.getKey().equals(winner)) {
                    transactionService.refundOrderBuyer(escrowEntry.getKey(), escrowEntry.getValue());
                }
            }

            // Deliver item to winner
            ItemStack wonItem = state.listing().item();
            if (wonItem != null) {
                Player winnerPlayer = Bukkit.getPlayer(winner);
                if (winnerPlayer != null && winnerPlayer.isOnline()) {
                    Map<Integer, ItemStack> leftover = winnerPlayer.getInventory().addItem(wonItem.clone());
                    for (ItemStack remainder : leftover.values()) {
                        addToPendingReturns(winner, remainder);
                    }
                    winnerPlayer.sendMessage(fmt(messages.won(),
                            "item", LiveAuctionComponentBuilder.describeItem(wonItem),
                            "price", transactionService.formatCurrency(winAmount)));
                } else {
                    addToPendingReturns(winner, wonItem.clone());
                }
            } else {
                // Notify online winner even when item ref is null (shouldn't happen)
                Player winnerPlayer = Bukkit.getPlayer(winner);
                if (winnerPlayer != null) {
                    winnerPlayer.sendMessage(fmt(messages.won(),
                            "item", LiveAuctionComponentBuilder.describeItem(wonItem),
                            "price", transactionService.formatCurrency(winAmount)));
                }
            }

            Component endMsg = LiveAuctionComponentBuilder.buildEndComponent(
                    state, transactionService.formatCurrency(winAmount), true, state.topBidderName());
            broadcastToAll(endMsg);
        } else {
            // No bids — return item to the seller
            ItemStack item = state.listing().item();
            if (item != null) {
                UUID sellerId = state.entry().sellerId();
                Player sellerOnline = Bukkit.getPlayer(sellerId);
                if (sellerOnline != null && sellerOnline.isOnline()) {
                    Map<Integer, ItemStack> leftover = sellerOnline.getInventory().addItem(item.clone());
                    for (ItemStack remainder : leftover.values()) {
                        addToPendingReturns(sellerId, remainder);
                    }
                } else {
                    addToPendingReturns(sellerId, item.clone());
                }
            }
            Component endMsg = LiveAuctionComponentBuilder.buildEndComponent(state, "", false, "");
            broadcastToAll(endMsg);
        }

        Bukkit.getPluginManager().callEvent(new LiveAuctionEndedEvent(state, winner));

        // Notify the listing store so it can remove this listing and update its slot count
        if (onListingResolved != null) {
            onListingResolved.accept(state.listing().id());
        }
    }

    // ---- private: helpers ----

    private void doJoin(Player player, LiveAuctionState state) {
        sessionRegistry.join(player.getUniqueId(), state);
        if (ezCountdown.isAvailable()) {
            startCountdownForPlayer(player, state, state.secondsRemaining());
        }
    }

    private void startCountdownForPlayer(Player player, LiveAuctionState state, long durationSeconds) {
        if (preferencesManager == null) return;
        PlayerLivePreferences prefs = preferencesManager.getPreferences(player.getUniqueId());
        if (!prefs.wantsCountdownDisplay()) return;

        var cfg = configuration.countdownConfiguration();
        List<String> displayTypes = prefs.displayTypeNames().isEmpty()
                ? cfg.displayTypeNames()
                : prefs.displayTypeNames();

        Optional<String> handle = ezCountdown.startPlayerNotification(
                player, durationSeconds, displayTypes, cfg.formatMessage(), cfg.endMessage());
        handle.ifPresent(h -> state.putNotificationHandle(player.getUniqueId(), h));
    }

    private void stopAllHandles(LiveAuctionState state) {
        state.notificationHandlesSnapshot().values().forEach(ezCountdown::stopNotification);
    }

    private void cancelTasksForAuction(String auctionId) {
        BukkitTask endTask = endTasks.remove(auctionId);
        if (endTask != null) endTask.cancel();
        List<BukkitTask> reminders = reminderTasks.remove(auctionId);
        if (reminders != null) reminders.forEach(BukkitTask::cancel);
    }

    private void addToPendingReturns(UUID ownerId, ItemStack item) {
        pendingReturns.computeIfAbsent(ownerId,
                k -> Collections.synchronizedList(new ArrayList<>())).add(item);
    }

    private void broadcastToAll(Component component) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(component);
        }
    }

    private String resolveSellerName(UUID sellerId, String fallback) {
        if (fallback != null && !fallback.isEmpty()) return fallback;
        if (sellerId != null) {
            Player online = Bukkit.getPlayer(sellerId);
            if (online != null) return online.getName();
            OfflinePlayer offline = Bukkit.getOfflinePlayer(sellerId);
            if (offline.getName() != null) return offline.getName();
        }
        return fallbackMessages.unknownName();
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
}
