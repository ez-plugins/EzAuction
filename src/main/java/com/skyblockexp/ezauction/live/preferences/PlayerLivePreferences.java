package com.skyblockexp.ezauction.live.preferences;

import java.util.List;
import java.util.UUID;

/**
 * Stores a player's personal live auction notification preferences.
 *
 * <p>Instances are mutable to allow incremental updates from the {@code /bid notify} and
 * {@code /bid mutechat} sub-commands.</p>
 */
public final class PlayerLivePreferences {

    private final UUID playerId;
    private boolean notificationsEnabled;
    /** Display type names for EzCountdown (e.g. "ACTION_BAR"). Empty = chat-only. */
    private List<String> displayTypeNames;
    private boolean muteOtherChatInAuction;

    public PlayerLivePreferences(UUID playerId,
            boolean notificationsEnabled,
            List<String> displayTypeNames,
            boolean muteOtherChatInAuction) {
        this.playerId = playerId;
        this.notificationsEnabled = notificationsEnabled;
        this.displayTypeNames = displayTypeNames != null ? List.copyOf(displayTypeNames) : List.of();
        this.muteOtherChatInAuction = muteOtherChatInAuction;
    }

    /** Creates default preferences (notifications on, chat-only display, no chat filter). */
    public static PlayerLivePreferences defaults(UUID playerId) {
        return new PlayerLivePreferences(playerId, true, List.of(), false);
    }

    public UUID playerId() { return playerId; }

    public boolean notificationsEnabled() { return notificationsEnabled; }

    public void setNotificationsEnabled(boolean enabled) { this.notificationsEnabled = enabled; }

    public List<String> displayTypeNames() { return displayTypeNames; }

    public void setDisplayTypeNames(List<String> names) {
        this.displayTypeNames = names != null ? List.copyOf(names) : List.of();
    }

    public boolean muteOtherChatInAuction() { return muteOtherChatInAuction; }

    public void setMuteOtherChatInAuction(boolean mute) { this.muteOtherChatInAuction = mute; }

    /**
     * Returns true if the player wants EzCountdown display notifications
     * (requires {@link #notificationsEnabled()} AND at least one display type configured).
     */
    public boolean wantsCountdownDisplay() {
        return notificationsEnabled && !displayTypeNames.isEmpty();
    }

    @Override
    public String toString() {
        return "PlayerLivePreferences{playerId=" + playerId
                + ", notificationsEnabled=" + notificationsEnabled
                + ", displayTypeNames=" + displayTypeNames
                + ", muteOtherChatInAuction=" + muteOtherChatInAuction
                + '}';
    }
}
