package com.skyblockexp.ezauction.integration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Sends auction event notifications to a Discord channel via a configurable
 * webhook URL. No dependency on DiscordSRV is required.
 *
 * <p>All HTTP calls are dispatched asynchronously so they never block the main
 * server thread. Transient failures (network errors, non-2xx responses) are
 * logged at WARNING level and otherwise ignored so they cannot affect gameplay.
 */
public final class DiscordWebhookNotifier {

    // Accepted URL prefix – only genuine Discord webhook URLs are allowed.
    private static final String DISCORD_WEBHOOK_PREFIX = "https://discord.com/api/webhooks/";

    private final JavaPlugin plugin;
    private final boolean enabled;
    private final String webhookUrl;
    private final String username;
    private final String avatarUrl;
    private final boolean useEmbeds;
    private final Map<String, Boolean> events;
    private final Map<String, Integer> embedColors;

    /** Shared HTTP client – created once, reused for every webhook call. */
    private final HttpClient httpClient;

    public DiscordWebhookNotifier(JavaPlugin plugin) {
        this.plugin = plugin;

        boolean resolvedEnabled = false;
        String resolvedUrl = null;
        String resolvedUsername = "EzAuction";
        String resolvedAvatarUrl = null;
        boolean resolvedUseEmbeds = true;
        Map<String, Boolean> resolvedEvents = new HashMap<>();
        Map<String, Integer> resolvedColors = new HashMap<>();

        try {
            YamlConfiguration cfg = loadConfig(plugin);
            if (cfg != null) {
                ConfigurationSection wh = cfg.getConfigurationSection("webhook");
                if (wh != null) {
                    resolvedEnabled = wh.getBoolean("enabled", false);
                    resolvedUrl = wh.getString("url", null);
                    resolvedUsername = wh.getString("username", "EzAuction");
                    resolvedAvatarUrl = wh.getString("avatar-url", null);
                    resolvedUseEmbeds = wh.getBoolean("use-embeds", true);

                    ConfigurationSection evSec = wh.getConfigurationSection("events");
                    if (evSec != null) {
                        for (String k : evSec.getKeys(false)) {
                            resolvedEvents.put(k, evSec.getBoolean(k, false));
                        }
                    }

                    ConfigurationSection colorSec = wh.getConfigurationSection("embed-colors");
                    if (colorSec != null) {
                        for (String k : colorSec.getKeys(false)) {
                            resolvedColors.put(k, colorSec.getInt(k, 0));
                        }
                    }
                }
            }
        } catch (Throwable ex) {
            plugin.getLogger().log(Level.WARNING, "[EzAuction] Failed to load webhook configuration from discord.yml.", ex);
        }

        this.enabled = resolvedEnabled;
        this.webhookUrl = resolvedUrl;
        this.username = resolvedUsername;
        this.avatarUrl = resolvedAvatarUrl;
        this.useEmbeds = resolvedUseEmbeds;
        this.events = Collections.unmodifiableMap(resolvedEvents);
        this.embedColors = Collections.unmodifiableMap(resolvedColors);
        this.httpClient = HttpClient.newHttpClient();
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} when the webhook is enabled and the configured URL
     * is a valid Discord webhook URL.
     */
    public boolean isEnabled() {
        return enabled && webhookUrl != null && webhookUrl.startsWith(DISCORD_WEBHOOK_PREFIX);
    }

    /** Returns {@code true} when the given event key is toggled on in config. */
    public boolean isEventEnabled(String event) {
        return events.getOrDefault(event, false);
    }

    public void sendAuctionStart(String item, String quantity, String seller, String price,
                                 String duration, String listingId) {
        if (useEmbeds) {
            String json = buildEmbedPayload(
                    "New Auction Listed",
                    null,
                    colorFor("auction_start"),
                    field("Item", item, true),
                    field("Seller", seller, true),
                    field("Starting Price", price, true),
                    field("Duration", duration, true),
                    field("Quantity", quantity, true),
                    field("Listing ID", listingId, true)
            );
            dispatch(json);
        } else {
            dispatch(buildPlainPayload(
                    "**New Auction Listed** — " + item + " (" + quantity + ") by **" + seller
                            + "** for " + price + " | Duration: " + duration + " | ID: " + listingId
            ));
        }
    }

    public void sendAuctionEnd(String item, String seller, String price, String listingId) {
        if (useEmbeds) {
            String json = buildEmbedPayload(
                    "Auction Ended",
                    null,
                    colorFor("auction_end"),
                    field("Item", item, true),
                    field("Seller", seller, true),
                    field("Sale Price", price, true),
                    field("Listing ID", listingId, true)
            );
            dispatch(json);
        } else {
            dispatch(buildPlainPayload(
                    "**Auction Ended** — " + item + " by **" + seller + "** sold for " + price
                            + " | ID: " + listingId
            ));
        }
    }

    public void sendAuctionBid(String item, String bidder, String amount, String listingId) {
        if (useEmbeds) {
            String json = buildEmbedPayload(
                    "New Bid Placed",
                    null,
                    colorFor("auction_bid"),
                    field("Item", item, true),
                    field("Bidder", bidder, true),
                    field("Bid Amount", amount, true),
                    field("Listing ID", listingId, true)
            );
            dispatch(json);
        } else {
            dispatch(buildPlainPayload(
                    "**New Bid** — **" + bidder + "** bid " + amount + " on " + item
                            + " | ID: " + listingId
            ));
        }
    }

    public void sendAuctionCancel(String item, String seller, String listingId) {
        if (useEmbeds) {
            String json = buildEmbedPayload(
                    "Auction Cancelled",
                    null,
                    colorFor("auction_cancel"),
                    field("Item", item, true),
                    field("Seller", seller, true),
                    field("Listing ID", listingId, true)
            );
            dispatch(json);
        } else {
            dispatch(buildPlainPayload(
                    "**Auction Cancelled** — " + item + " by **" + seller + "** | ID: " + listingId
            ));
        }
    }

    public void sendAuctionExpiry(String item, String seller, String listingId) {
        if (useEmbeds) {
            String json = buildEmbedPayload(
                    "Auction Expired",
                    "No buyers were found before the listing expired.",
                    colorFor("auction_end"),
                    field("Item", item, true),
                    field("Seller", seller, true),
                    field("Listing ID", listingId, true)
            );
            dispatch(json);
        } else {
            dispatch(buildPlainPayload(
                    "**Auction Expired** — " + item + " by **" + seller + "** (no buyer) | ID: " + listingId
            ));
        }
    }

    /** Sends a labelled test embed to verify the webhook is working. */
    public void sendTestMessage(String initiatedBy) {
        if (useEmbeds) {
            String json = buildEmbedPayload(
                    "EzAuction — Webhook Test",
                    "This test message was triggered by **" + escapeJson(initiatedBy) + "**. "
                            + "If you can see this, your webhook is configured correctly!",
                    5793266, // #588FEF soft blue
                    field("Status", "OK", true),
                    field("Time", Instant.now().toString(), true)
            );
            dispatch(json);
        } else {
            dispatch(buildPlainPayload(
                    "EzAuction webhook test initiated by " + initiatedBy + " — webhook is working."
            ));
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private int colorFor(String event) {
        return embedColors.getOrDefault(event, 0);
    }

    /** Returns a JSON fragment representing a single embed field. */
    private static String field(String name, String value, boolean inline) {
        return "{\"name\":\"" + escapeJson(name) + "\","
                + "\"value\":\"" + escapeJson(value != null ? value : "—") + "\","
                + "\"inline\":" + inline + "}";
    }

    /**
     * Builds a Discord embed payload JSON string.
     *
     * @param title       embed title
     * @param description optional embed description (nullable)
     * @param color       embed left-bar colour (decimal integer)
     * @param fields      pre-built field JSON fragments
     * @return full webhook JSON body
     */
    private String buildEmbedPayload(String title, String description, int color,
                                     String... fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        if (username != null && !username.isBlank()) {
            sb.append("\"username\":\"").append(escapeJson(username)).append("\",");
        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            sb.append("\"avatar_url\":\"").append(escapeJson(avatarUrl)).append("\",");
        }

        sb.append("\"embeds\":[{");
        sb.append("\"title\":\"").append(escapeJson(title)).append("\",");

        if (description != null && !description.isBlank()) {
            sb.append("\"description\":\"").append(escapeJson(description)).append("\",");
        }

        sb.append("\"color\":").append(color).append(",");
        sb.append("\"timestamp\":\"").append(Instant.now().toString()).append("\",");

        if (fields.length > 0) {
            sb.append("\"fields\":[");
            for (int i = 0; i < fields.length; i++) {
                sb.append(fields[i]);
                if (i < fields.length - 1) sb.append(",");
            }
            sb.append("],");
        }

        // Footer with branding
        sb.append("\"footer\":{\"text\":\"EzAuction\"}");

        sb.append("}]}");
        return sb.toString();
    }

    private String buildPlainPayload(String content) {
        StringBuilder sb = new StringBuilder("{");
        if (username != null && !username.isBlank()) {
            sb.append("\"username\":\"").append(escapeJson(username)).append("\",");
        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            sb.append("\"avatar_url\":\"").append(escapeJson(avatarUrl)).append("\",");
        }
        sb.append("\"content\":\"").append(escapeJson(content)).append("\"}");
        return sb.toString();
    }

    /**
     * POSTs {@code jsonPayload} to the configured webhook URL asynchronously.
     * Any failure is logged at WARNING level: it never propagates to callers.
     */
    private void dispatch(String jsonPayload) {
        if (!isEnabled()) return;

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING,
                    "[EzAuction] Invalid webhook URL – could not dispatch notification.", ex);
            return;
        }

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {
                    if (error != null) {
                        plugin.getLogger().log(Level.WARNING,
                                "[EzAuction] Webhook delivery failed: " + error.getMessage());
                    } else if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        plugin.getLogger().warning(
                                "[EzAuction] Webhook returned HTTP " + response.statusCode()
                                        + " — check that the webhook URL is valid and the bot has"
                                        + " permission to post in the channel.");
                    }
                });
    }

    /**
     * Escapes a string for safe inclusion inside a JSON string literal.
     * Only the characters required by RFC 8259 §7 are escaped.
     */
    static String escapeJson(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length() + 8);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Config loading
    // -----------------------------------------------------------------------

    private static YamlConfiguration loadConfig(JavaPlugin plugin) throws IOException {
        File dataFolder = plugin.getDataFolder();
        File discordFile = new File(dataFolder, "discord.yml");
        if (discordFile.exists()) {
            return YamlConfiguration.loadConfiguration(discordFile);
        }
        InputStream stream = plugin.getResource("discord.yml");
        if (stream != null) {
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
        }
        return null;
    }
}
