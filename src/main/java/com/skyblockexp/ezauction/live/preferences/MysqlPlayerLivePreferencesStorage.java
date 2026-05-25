package com.skyblockexp.ezauction.live.preferences;

import com.skyblockexp.ezauction.config.AuctionStorageConfiguration.Mysql;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL-backed implementation of {@link PlayerLivePreferencesStorage}.
 * Stores player preferences in a single {@code live_player_preferences} table.
 */
public final class MysqlPlayerLivePreferencesStorage implements PlayerLivePreferencesStorage {

    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private final Logger logger;
    private final Mysql mysql;
    private final String jdbcUrl;
    private final String table;

    public MysqlPlayerLivePreferencesStorage(Logger logger, Mysql mysql) {
        this.logger = logger != null ? logger : Logger.getLogger(MysqlPlayerLivePreferencesStorage.class.getName());
        this.mysql = mysql;
        this.jdbcUrl = "jdbc:mysql://" + mysql.host() + ":" + mysql.port() + "/" + mysql.database();
        String prefix = mysql.tablePrefix() != null ? mysql.tablePrefix() : "";
        this.table = sanitize(prefix + "live_player_preferences");
    }

    @Override
    public boolean initialize() {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException ex) {
            logger.log(Level.SEVERE, "[EzAuction] MySQL driver not found for live preferences.", ex);
            return false;
        }
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table + "` ("
                    + "player_uuid CHAR(36) NOT NULL PRIMARY KEY,"
                    + "notifications_enabled TINYINT(1) NOT NULL DEFAULT 1,"
                    + "display_type_names TEXT,"
                    + "mute_other_chat TINYINT(1) NOT NULL DEFAULT 0"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            return true;
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "[EzAuction] Failed to initialise live_player_preferences table.", ex);
            return false;
        }
    }

    @Override
    public PlayerLivePreferences load(UUID playerId) {
        String sql = "SELECT notifications_enabled, display_type_names, mute_other_chat FROM `"
                + table + "` WHERE player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean notif = rs.getBoolean("notifications_enabled");
                    List<String> displayTypes = parseDisplayTypes(rs.getString("display_type_names"));
                    boolean mute = rs.getBoolean("mute_other_chat");
                    return new PlayerLivePreferences(playerId, notif, displayTypes, mute);
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "[EzAuction] Failed to load live preferences for " + playerId, ex);
        }
        return null;
    }

    @Override
    public void save(PlayerLivePreferences preferences) {
        String sql = "INSERT INTO `" + table + "` "
                + "(player_uuid, notifications_enabled, display_type_names, mute_other_chat) "
                + "VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE notifications_enabled = VALUES(notifications_enabled), "
                + "display_type_names = VALUES(display_type_names), "
                + "mute_other_chat = VALUES(mute_other_chat)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, preferences.playerId().toString());
            ps.setBoolean(2, preferences.notificationsEnabled());
            ps.setString(3, String.join(",", preferences.displayTypeNames()));
            ps.setBoolean(4, preferences.muteOtherChatInAuction());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "[EzAuction] Failed to save live preferences for "
                    + preferences.playerId(), ex);
        }
    }

    @Override
    public Collection<PlayerLivePreferences> loadAll() {
        String sql = "SELECT player_uuid, notifications_enabled, display_type_names, mute_other_chat FROM `"
                + table + '`';
        List<PlayerLivePreferences> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    boolean notif = rs.getBoolean("notifications_enabled");
                    List<String> displayTypes = parseDisplayTypes(rs.getString("display_type_names"));
                    boolean mute = rs.getBoolean("mute_other_chat");
                    result.add(new PlayerLivePreferences(playerId, notif, displayTypes, mute));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "[EzAuction] Failed to load all live preferences.", ex);
        }
        return result;
    }

    // ---- helpers ----

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, mysql.username(), mysql.password());
    }

    private static List<String> parseDisplayTypes(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
