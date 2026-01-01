package com.skyblockexp.ezauction.config;

import java.util.Locale;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration describing how auction data should be stored.
 */
public final class AuctionStorageConfiguration {

    private final StorageType type;
    private final Mysql mysql;

    private AuctionStorageConfiguration(StorageType type, Mysql mysql) {
        this.type = type;
        this.mysql = mysql;
    }

    public static AuctionStorageConfiguration yaml() {
        return new AuctionStorageConfiguration(StorageType.YAML, null);
    }

    public static AuctionStorageConfiguration mysql(Mysql mysql) {
        return new AuctionStorageConfiguration(StorageType.MYSQL, mysql);
    }

    public StorageType type() {
        return type;
    }

    public Mysql mysql() {
        return mysql;
    }

    public static AuctionStorageConfiguration from(ConfigurationSection section) {
        if (section == null) {
            return yaml();
        }
        String typeName = section.getString("type", "yaml");
        StorageType storageType = StorageType.from(typeName);
        if (storageType == StorageType.MYSQL) {
            ConfigurationSection mysqlSection = section.getConfigurationSection("mysql");
            if (mysqlSection == null) {
                return yaml();
            }
            Mysql mysql = Mysql.from(mysqlSection);
            return mysql != null ? mysql(mysql) : yaml();
        }
        return yaml();
    }

    @Override
    public String toString() {
        return "AuctionStorageConfiguration{"
                + "type=" + type
                + ", mysql=" + mysql
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuctionStorageConfiguration that)) {
            return false;
        }
        return type == that.type && Objects.equals(mysql, that.mysql);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, mysql);
    }

    /**
     * Supported storage types.
     */
    public enum StorageType {
        YAML,
        MYSQL;

        public static StorageType from(String value) {
            if (value == null || value.isEmpty()) {
                return YAML;
            }
            String normalized = value.trim().toUpperCase(Locale.ENGLISH);
            for (StorageType candidate : values()) {
                if (candidate.name().equals(normalized)) {
                    return candidate;
                }
            }
            return YAML;
        }
    }

    /**
     * Configuration for MySQL storage.
     */
    public record Mysql(
            String host,
            int port,
            String database,
            String username,
            String password,
            boolean useSsl,
            String tablePrefix,
            Pool pool) {

        public Mysql {
            host = valueOrDefault(host, "localhost");
            port = Math.max(1, port);
            database = valueOrDefault(database, "skyblock");
            username = valueOrDefault(username, "root");
            password = password == null ? "" : password;
            useSsl = useSsl;
            tablePrefix = sanitizePrefix(tablePrefix);
            pool = pool == null ? Pool.defaultSettings() : pool;
        }

        public static Mysql from(ConfigurationSection section) {
            if (section == null) {
                return null;
            }
            String host = section.getString("host", "localhost");
            int port = section.getInt("port", 3306);
            String database = section.getString("database", "skyblock");
            String username = section.getString("username", "root");
            String password = section.getString("password", "");
            boolean useSsl = section.getBoolean("use-ssl", true);
            String tablePrefix = section.getString("table-prefix", "ezauction_");
            ConfigurationSection poolSection = section.getConfigurationSection("pool");
            Pool pool = Pool.from(poolSection);
            return new Mysql(host, port, database, username, password, useSsl, tablePrefix, pool);
        }

        private static String valueOrDefault(String value, String fallback) {
            if (value == null) {
                return fallback;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? fallback : trimmed;
        }

        private static String sanitizePrefix(String prefix) {
            if (prefix == null) {
                return "ezauction_";
            }
            String trimmed = prefix.trim();
            if (trimmed.isEmpty()) {
                return "ezauction_";
            }
            String sanitized = trimmed.replaceAll("[^a-zA-Z0-9_]+", "_");
            return sanitized.isEmpty() ? "ezauction_" : sanitized;
        }
    }

    /**
     * Connection pool configuration for MySQL storage.
     */
    public record Pool(int maximumPoolSize, int minimumIdle, long connectionTimeoutMillis, long idleTimeoutMillis,
            long maxLifetimeMillis) {

        public Pool {
            maximumPoolSize = Math.max(1, maximumPoolSize);
            minimumIdle = Math.max(0, Math.min(minimumIdle, maximumPoolSize));
            connectionTimeoutMillis = Math.max(250L, connectionTimeoutMillis);
            idleTimeoutMillis = Math.max(0L, idleTimeoutMillis);
            maxLifetimeMillis = Math.max(30_000L, maxLifetimeMillis);
            if (maxLifetimeMillis > 0L && idleTimeoutMillis >= maxLifetimeMillis) {
                idleTimeoutMillis = Math.max(0L, maxLifetimeMillis - 30_000L);
            }
        }

        public static Pool from(ConfigurationSection section) {
            if (section == null) {
                return defaultSettings();
            }
            int maximumPoolSize = section.getInt("maximum-pool-size", 10);
            int minimumIdle = section.getInt("minimum-idle", 2);
            long connectionTimeout = section.getLong("connection-timeout-millis", 10_000L);
            long idleTimeout = section.getLong("idle-timeout-millis", 600_000L);
            long maxLifetime = section.getLong("max-lifetime-millis", 1_800_000L);
            return new Pool(maximumPoolSize, minimumIdle, connectionTimeout, idleTimeout, maxLifetime);
        }

        public static Pool defaultSettings() {
            return new Pool(10, 2, 10_000L, 600_000L, 1_800_000L);
        }
    }
}
