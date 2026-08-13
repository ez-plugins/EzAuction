package com.skyblockexp.ezauction.live.model;

import com.skyblockexp.ezauction.model.AuctionListing;
import com.skyblockexp.ezauction.live.LiveAuctionEntry;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutable runtime state for a live auction that is currently being announced.
 *
 * <p>Instances are created when a queued {@link LiveAuctionEntry} is promoted to an active slot
 * and discarded when the auction ends. Thread-safe via volatile fields and concurrent maps.</p>
 */
public final class LiveAuctionState {

    private final String id;
    private final LiveAuctionEntry entry;
    private volatile Instant expiresAt;

    /** Accumulated escrow per bidder. Key = bidder UUID, value = total amount withheld. */
    private final Map<UUID, Double> escrow = new ConcurrentHashMap<>();

    /** Players who have explicitly joined via /bid select or the clickable announcement. */
    private final Set<UUID> participants = ConcurrentHashMap.newKeySet();

    /**
     * Per-player EzCountdown handles, used to stop notifications when players leave or the
     * auction ends. Both passive observers (preference-based) and active participants may have handles.
     */
    private final Map<UUID, String> notificationHandles = new ConcurrentHashMap<>();

    private volatile double currentBid;
    private volatile UUID topBidder;
    private volatile String topBidderName;

    public LiveAuctionState(LiveAuctionEntry entry, Instant expiresAt) {
        this.entry = entry;
        this.id = entry.listing().id();
        this.expiresAt = expiresAt;
        this.currentBid = entry.listing().price();
        this.topBidder = null;
        this.topBidderName = null;
    }

    // ---- identity ----

    public String id() { return id; }

    public LiveAuctionEntry entry() { return entry; }

    public AuctionListing listing() { return entry.listing(); }

    public Instant expiresAt() { return expiresAt; }

    // ---- bid state ----

    public double currentBid() { return currentBid; }

    public UUID topBidder() { return topBidder; }

    public String topBidderName() { return topBidderName; }

    public boolean hasBids() { return topBidder != null; }

    /**
     * Updates the current bid. Must only be called while holding appropriate external locks or from a
     * single-threaded context (e.g. the main server thread via Bukkit scheduler).
     */
    public void updateBid(double amount, UUID bidder, String bidderName) {
        this.currentBid = amount;
        this.topBidder = bidder;
        this.topBidderName = bidderName;
    }

    // ---- escrow ----

    public double getEscrow(UUID bidder) {
        return escrow.getOrDefault(bidder, 0.0);
    }

    /** Replaces (upserts) the escrowed amount for this bidder. */
    public void putEscrow(UUID bidder, double amount) {
        escrow.put(bidder, amount);
    }

    public void removeEscrow(UUID bidder) {
        escrow.remove(bidder);
    }

    /** Returns a snapshot of all bidders and their escrowed amounts. */
    public Map<UUID, Double> escrowSnapshot() {
        return Collections.unmodifiableMap(new java.util.HashMap<>(escrow));
    }

    // ---- participants ----

    public void addParticipant(UUID player) { participants.add(player); }

    public void removeParticipant(UUID player) { participants.remove(player); }

    public boolean isParticipant(UUID player) { return participants.contains(player); }

    public Set<UUID> participants() { return Collections.unmodifiableSet(participants); }

    // ---- EzCountdown notification handles ----

    public void putNotificationHandle(UUID player, String handle) {
        notificationHandles.put(player, handle);
    }

    public String removeNotificationHandle(UUID player) {
        return notificationHandles.remove(player);
    }

    public Map<UUID, String> notificationHandlesSnapshot() {
        return Collections.unmodifiableMap(new java.util.HashMap<>(notificationHandles));
    }

    // ---- helpers ----

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Returns the minimum acceptable next bid given the configured increments.
     *
     * @param pct   minimum increment as a percentage of the current bid (0 = disabled)
     * @param fixed minimum fixed increment (0 = disabled)
     * @return minimum next bid (always strictly greater than currentBid when any increment is set)
     */
    public double minimumNextBid(double pct, double fixed) {
        // First bid only needs to meet the starting price - no increment required.
        if (!hasBids()) return currentBid;
        double base = currentBid;
        double byPercent = pct > 0 ? base * (1.0 + pct / 100.0) : base;
        double byFixed = fixed > 0 ? base + fixed : base;
        double minimum = Math.max(byPercent, byFixed);
        // must be strictly greater than current when increments are set
        if ((pct > 0 || fixed > 0) && minimum <= base) {
            minimum = base + Math.max(fixed, 0.01);
        }
        return minimum;
    }

    // ---- seconds remaining ----

    public long secondsRemaining() {
        long seconds = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0L, seconds);
    }

    /** Extends the auction expiry to the given instant. Used for anti-snipe timer resets. */
    public void extendExpiry(Instant newExpiry) {
        this.expiresAt = newExpiry;
    }

    @Override
    public String toString() {
        return "LiveAuctionState{id='" + id + "', currentBid=" + currentBid
                + ", topBidder=" + topBidderName + ", expires=" + expiresAt + '}';
    }
}
