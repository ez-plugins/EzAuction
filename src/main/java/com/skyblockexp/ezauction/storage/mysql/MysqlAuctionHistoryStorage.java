package com.skyblockexp.ezauction.storage.mysql;

import com.skyblockexp.ezauction.transaction.AuctionTransactionHistoryEntry;
import com.skyblockexp.ezauction.transaction.AuctionTransactionType;
import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.storage.AuctionHistoryStorage;
import com.skyblockexp.ezauction.config.AuctionStorageConfiguration.Mysql;
import com.skyblockexp.ezauction.util.EconomyUtils;
import com.skyblockexp.ezauction.util.ItemStackSerialization;
import java.sql.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;

/**
 * MySQL-based implementation of AuctionHistoryStorage.
 */
public class MysqlAuctionHistoryStorage implements AuctionHistoryStorage {
    private final Logger logger;
    private final Mysql mysql;
    private final String jdbcUrl;
    private final String historyTable;
    private final Lock historyWriteLock = new ReentrantLock(true);
    private boolean driverLoaded = false;

    public MysqlAuctionHistoryStorage(Logger logger, Mysql mysql) {
        this.logger = logger != null ? logger : Logger.getLogger(MysqlAuctionHistoryStorage.class.getName());
        this.mysql = mysql;
        this.jdbcUrl = "jdbc:mysql://" + mysql.host() + ":" + mysql.port() + "/" + mysql.database();
        String prefix = mysql.tablePrefix();
        this.historyTable = sanitize(prefix + "history");
    }

    private boolean isReady() {
        return driverLoaded;
    }

    public boolean initialize() {
        if (driverLoaded) {
            return true;
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            driverLoaded = true;
        } catch (ClassNotFoundException ex) {
            logger.log(Level.SEVERE, "MySQL JDBC driver not found.", ex);
            return false;
        }
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + historyTable + "` ("
                    + "player_uuid CHAR(36) NOT NULL,"
                    + "entry_index INT NOT NULL,"
                    + "transaction_id CHAR(36) NULL,"
                    + "type VARCHAR(16) NOT NULL,"
                    + "timestamp BIGINT NOT NULL,"
                    + "price DOUBLE NOT NULL,"
                    + "counterpart_uuid CHAR(36) NULL,"
                    + "counterpart_name VARCHAR(64) NULL,"
                    + "item LONGTEXT NULL,"
                    + "PRIMARY KEY (player_uuid, entry_index)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            
            // Migration: Add transaction_id column if it doesn't exist (for existing tables)
            try {
                // Check if column exists by querying information_schema
                try (PreparedStatement checkColumn = connection.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = 'transaction_id'")) {
                    checkColumn.setString(1, mysql.database());
                    checkColumn.setString(2, historyTable);
                    try (ResultSet rs = checkColumn.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            // Column doesn't exist, add it
                            statement.executeUpdate("ALTER TABLE `" + historyTable 
                                    + "` ADD COLUMN transaction_id CHAR(36) NULL AFTER entry_index");
                            logger.info("Added transaction_id column to existing " + historyTable + " table.");
                        }
                    }
                }
            } catch (SQLException migrationEx) {
                logger.log(Level.WARNING, 
                        "Could not check/add transaction_id column to " + historyTable + ". "
                        + "If you have an existing table, please add the column manually: "
                        + "ALTER TABLE `" + historyTable + "` ADD COLUMN transaction_id CHAR(36) NULL AFTER entry_index;",
                        migrationEx);
            }
            
            return true;
        } catch (SQLException ex) {
            logger.log(Level.SEVERE,
                    "Failed to create history table in MySQL.", ex);
            return false;
        }
    }

    @Override
    public Map<UUID, Deque<AuctionTransactionHistoryEntry>> loadAll() {
        Map<UUID, Deque<AuctionTransactionHistoryEntry>> result = new HashMap<>();
        if (!isReady()) {
            return result;
        }
        String query = "SELECT player_uuid, entry_index, type, timestamp, price, counterpart_uuid, counterpart_name, item "
                + "FROM `" + historyTable + "` ORDER BY player_uuid, entry_index";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(query);
                ResultSet resultSet = statement.executeQuery()) {
            Map<UUID, Map<Integer, AuctionTransactionHistoryEntry>> grouped = new LinkedHashMap<>();
            while (resultSet.next()) {
                UUID playerId = parseUuid(resultSet.getString("player_uuid"));
                if (playerId == null) {
                    continue;
                }
                int index = resultSet.getInt("entry_index");
                AuctionTransactionType type = parseType(resultSet.getString("type"));
                if (type == null) {
                    continue;
                }
                long timestamp = resultSet.getLong("timestamp");
                double price = EconomyUtils.normalizeCurrency(resultSet.getDouble("price"));
                UUID counterpartId = parseUuid(resultSet.getString("counterpart_uuid"));
                String counterpartName = resultSet.getString("counterpart_name");
                ItemStack item = ItemStackSerialization.deserialize(resultSet.getString("item"), logger);
                // Read transaction ID or generate one for backward compatibility
                String transactionId = resultSet.getString("transaction_id");
                if (transactionId == null || transactionId.isEmpty()) {
                    transactionId = UUID.randomUUID().toString();
                }
                AuctionTransactionHistoryEntry entry = new AuctionTransactionHistoryEntry(transactionId, type, counterpartId,
                        counterpartName, price, timestamp, item);
                grouped.computeIfAbsent(playerId, key -> new LinkedHashMap<>()).put(index, entry);
            }
            for (Map.Entry<UUID, Map<Integer, AuctionTransactionHistoryEntry>> entry : grouped.entrySet()) {
                Map<Integer, AuctionTransactionHistoryEntry> ordered = entry.getValue();
                Deque<AuctionTransactionHistoryEntry> deque = new ArrayDeque<>(ordered.size());
                ordered.entrySet().stream()
                        .sorted(Map.Entry.<Integer, AuctionTransactionHistoryEntry>comparingByKey())
                        .forEachOrdered(e -> deque.addLast(e.getValue()));
                result.put(entry.getKey(), deque);
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE,
                    "Failed to load transaction history from MySQL.", ex);
        }
        return result;
    }

    @Override
    public void saveAll(Map<UUID, Deque<AuctionTransactionHistoryEntry>> history) {
        if (!isReady()) {
            return;
        }
        historyWriteLock.lock();
        String delete = "DELETE FROM `" + historyTable + "`";
        String insert = "INSERT INTO `" + historyTable
                + "` (player_uuid, entry_index, transaction_id, type, timestamp, price, counterpart_uuid, counterpart_name, item)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (Statement deleteStatement = connection.createStatement()) {
                deleteStatement.executeUpdate(delete);
            }
            try (PreparedStatement insertStatement = connection.prepareStatement(insert)) {
                for (Map.Entry<UUID, Deque<AuctionTransactionHistoryEntry>> entry : history.entrySet()) {
                    writeHistoryEntries(insertStatement, entry.getKey(), entry.getValue());
                }
                insertStatement.executeBatch();
            }
            connection.commit();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE,
                    "Failed to save history to MySQL.", ex);
        } finally {
            historyWriteLock.unlock();
        }
    }

    @Override
    public void savePlayerHistory(UUID playerId, Deque<AuctionTransactionHistoryEntry> history) {
        if (!isReady() || playerId == null) {
            return;
        }
        historyWriteLock.lock();
        String delete = "DELETE FROM `" + historyTable + "` WHERE player_uuid = ?";
        String insert = "INSERT INTO `" + historyTable
                + "` (player_uuid, entry_index, transaction_id, type, timestamp, price, counterpart_uuid, counterpart_name, item)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteStatement = connection.prepareStatement(delete)) {
                deleteStatement.setString(1, playerId.toString());
                deleteStatement.executeUpdate();
            }
            try (PreparedStatement insertStatement = connection.prepareStatement(insert)) {
                writeHistoryEntries(insertStatement, playerId, history);
                insertStatement.executeBatch();
            }
            connection.commit();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE,
                    String.format("Failed to save history for player %s to MySQL.", playerId),
                    ex);
        } finally {
            historyWriteLock.unlock();
        }
    }

    @Override
    public void close() {
        // No connection pool or driver to close
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, mysql.username(), mysql.password());
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            logger.log(Level.WARNING,
                    "Invalid UUID encountered in MySQL storage: {0}", raw);
            return null;
        }
    }

    private AuctionTransactionType parseType(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return null;
        }
        try {
            return AuctionTransactionType.valueOf(typeName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            logger.log(Level.WARNING,
                    "Invalid transaction type encountered in MySQL storage: {0}",
                    typeName);
            return null;
        }
    }

    private void writeHistoryEntries(PreparedStatement statement, UUID playerId,
            Deque<AuctionTransactionHistoryEntry> history) throws SQLException {
        if (history == null || history.isEmpty()) {
            return;
        }
        int index = 0;
        for (AuctionTransactionHistoryEntry entry : history) {
            statement.setString(1, playerId.toString());
            statement.setInt(2, index++);
            statement.setString(3, entry.transactionId());
            statement.setString(4, entry.type().name());
            statement.setLong(5, entry.timestamp());
            statement.setDouble(6, entry.price());
            if (entry.counterpartId() != null) {
                statement.setString(7, entry.counterpartId().toString());
            } else {
                statement.setNull(7, java.sql.Types.VARCHAR);
            }
            if (entry.counterpartName() != null && !entry.counterpartName().isEmpty()) {
                statement.setString(8, entry.counterpartName());
            } else {
                statement.setNull(8, java.sql.Types.VARCHAR);
            }
            statement.setString(9, ItemStackSerialization.serialize(entry.item(), logger));
            statement.addBatch();
        }
    }

    private String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "ezauction_table";
        }
        String sanitized = input.replaceAll("[^a-zA-Z0-9_]+", "_");
        if (sanitized.isBlank()) {
            sanitized = "ezauction_table";
        }
        if (sanitized.length() > 64) {
            sanitized = sanitized.substring(0, 64);
        }
        return sanitized;
    }
}
