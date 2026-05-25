package com.skyblockexp.ezauction.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

/**
 * Null-safe facade for the optional EzCountdown soft-dependency.
 *
 * <p>All public methods guard against EzCountdown being absent or not yet initialised.
 * Any {@link Throwable} thrown by the underlying API is caught and treated as "not available".
 * EzCountdown types are only referenced inside method bodies — never in field declarations or
 * method signatures — so this class loads safely even when EzCountdown is not installed.</p>
 */
public final class EzCountdownIntegration {

    private final JavaPlugin plugin;
    private final boolean configEnabled;
    private final boolean autoDetect;

    /**
     * @param plugin        the owning plugin
     * @param configEnabled whether {@code ezcountdown-enabled} is set to true in config
     * @param autoDetect    when {@code true}, EzCountdown is used automatically if present on the server
     *                      regardless of {@code configEnabled}
     */
    public EzCountdownIntegration(JavaPlugin plugin, boolean configEnabled, boolean autoDetect) {
        this.plugin = plugin;
        this.configEnabled = configEnabled;
        this.autoDetect = autoDetect;
    }

    /** Convenience constructor — auto-detect defaults to {@code false} for backward compatibility. */
    public EzCountdownIntegration(JavaPlugin plugin, boolean configEnabled) {
        this(plugin, configEnabled, false);
    }

    /**
     * Returns {@code true} if EzCountdown should be used.
     * This is the case when:
     * <ul>
     *   <li>{@code ezcountdown-enabled} is {@code true} in config, <b>or</b></li>
     *   <li>{@code ezcountdown-auto-detect} is {@code true} (default) and the plugin is installed.</li>
     * </ul>
     */
    public boolean isAvailable() {
        if (!configEnabled && !autoDetect) return false;
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("EzCountdown")) return false;
            RegisteredServiceProvider<?> rsp =
                    Bukkit.getServicesManager().getRegistration(
                            Class.forName("com.skyblockexp.ezcountdown.api.EzCountdownApi"));
            return rsp != null && rsp.getProvider() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Starts an ephemeral countdown notification targeted at a single player.
     *
     * @param player           the target player
     * @param durationSeconds  countdown length in seconds
     * @param displayTypeNames display type names (e.g. "ACTION_BAR", "BOSS_BAR")
     * @param formatMessage    countdown text format; use {@code {formatted}} for the default HH:MM:SS
     * @param endMessage       message shown when the countdown reaches zero (null or blank = none)
     * @return an opaque handle that can be passed to {@link #stopNotification(String)}, or empty
     */
    public Optional<String> startPlayerNotification(Player player, long durationSeconds,
            List<String> displayTypeNames, String formatMessage, String endMessage) {
        if (!isAvailable() || player == null || durationSeconds <= 0) {
            return Optional.empty();
        }
        try {
            com.skyblockexp.ezcountdown.api.EzCountdownApi api = resolveApi();
            if (api == null) return Optional.empty();

            java.util.EnumSet<com.skyblockexp.ezcountdown.display.DisplayType> displays =
                    parseDisplayTypes(displayTypeNames);

            com.skyblockexp.ezcountdown.api.model.NotificationBuilder builder =
                    com.skyblockexp.ezcountdown.api.model.Notification.builder()
                            .duration(durationSeconds)
                            .displays(displays)
                            .players(List.of(player));

            if (formatMessage != null && !formatMessage.isBlank()) {
                builder.message(formatMessage);
            }
            if (endMessage != null && !endMessage.isBlank()) {
                builder.endMessage(endMessage);
            }

            return api.sendNotification(builder.build());
        } catch (Throwable t) {
            plugin.getLogger().fine("[EzCountdown] Failed to start notification: " + t.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Stops an active countdown by its handle returned from {@link #startPlayerNotification}.
     *
     * @param handle opaque handle; null or blank values are silently ignored
     */
    public void stopNotification(String handle) {
        if (handle == null || handle.isBlank()) return;
        try {
            com.skyblockexp.ezcountdown.api.EzCountdownApi api = resolveApi();
            if (api != null) {
                api.stopCountdown(handle);
            }
        } catch (Throwable t) {
            plugin.getLogger().fine("[EzCountdown] Failed to stop notification: " + t.getMessage());
        }
    }

    // ---- private helpers ----

    @SuppressWarnings("unchecked")
    private com.skyblockexp.ezcountdown.api.EzCountdownApi resolveApi() {
        try {
            Class<?> apiClass = Class.forName("com.skyblockexp.ezcountdown.api.EzCountdownApi");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(
                    (Class<Object>) apiClass);
            return rsp != null ? (com.skyblockexp.ezcountdown.api.EzCountdownApi) rsp.getProvider() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static java.util.EnumSet<com.skyblockexp.ezcountdown.display.DisplayType> parseDisplayTypes(
            List<String> names) {
        java.util.EnumSet<com.skyblockexp.ezcountdown.display.DisplayType> result =
                java.util.EnumSet.noneOf(com.skyblockexp.ezcountdown.display.DisplayType.class);
        if (names != null) {
            for (String name : names) {
                try {
                    result.add(com.skyblockexp.ezcountdown.display.DisplayType.valueOf(
                            name.toUpperCase(java.util.Locale.ENGLISH)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (result.isEmpty()) {
            result.add(com.skyblockexp.ezcountdown.display.DisplayType.ACTION_BAR);
        }
        return result;
    }
}
