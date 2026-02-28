package com.skyblockexp.ezauction.storage.mysql;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.config.AuctionStorageConfiguration.Mysql;
import com.skyblockexp.ezauction.storage.AuctionListingRepository;
import com.skyblockexp.ezauction.storage.DistributedAuctionListingStorage;
import com.skyblockexp.ezauction.storage.AuctionStorageSnapshot;
import com.skyblockexp.ezauction.util.EconomyUtils;
import com.skyblockexp.ezauction.util.ItemStackSerialization;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * MySQL-based implementation of {@link AuctionListingRepository} and {@link DistributedAuctionListingStorage}.
 */
public class MysqlAuctionListingStorage implements AuctionListingRepository, DistributedAuctionListingStorage {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Mysql mysql;
    private final String jdbcUrl;
    private final String listingsTable;
    private final String ordersTable;
    private final String returnsTable;
    private boolean driverLoaded = false;

    /**
     * Constructs a new MySQL-based auction listing storage implementation.
     *
     * @param plugin the JavaPlugin instance
     * @param mysql  the MySQL configuration
     */
    public MysqlAuctionListingStorage(JavaPlugin plugin, Mysql mysql) {
        this.plugin = plugin;
        this.logger = plugin != null ? plugin.getLogger() : Logger.getLogger(MysqlAuctionListingStorage.class.getName());
        this.mysql = mysql;
        this.jdbcUrl = "jdbc:mysql://" + mysql.host() + ":" + mysql.port() + "/" + mysql.database();
        String prefix = mysql.tablePrefix();
        this.listingsTable = sanitize(prefix + "listings");
        this.ordersTable = sanitize(prefix + "orders");
        this.returnsTable = sanitize(prefix + "returns");
    }

    @Override
    /**
     * Initializes the storage by loading the MySQL driver and creating tables if needed.
     *
     * @return true if initialization succeeded, false otherwise
     */
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
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + listingsTable + "` ("
                    + "id VARCHAR(36) NOT NULL PRIMARY KEY,"
                    + "seller_uuid CHAR(36) NOT NULL,"
                    + "price DOUBLE NOT NULL,"
                    + "expiry BIGINT NOT NULL,"
                    + "deposit DOUBLE NOT NULL,"
                    + "item LONGTEXT NOT NULL"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + ordersTable + "` ("
                    + "id VARCHAR(36) NOT NULL PRIMARY KEY,"
                    + "buyer_uuid CHAR(36) NOT NULL,"
                    + "price DOUBLE NOT NULL,"
                    + "reserved DOUBLE NOT NULL,"
                    + "expiry BIGINT NOT NULL,"
                    + "item LONGTEXT NOT NULL"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + returnsTable + "` ("
                    + "player_uuid CHAR(36) NOT NULL,"
                    + "slot INT NOT NULL,"
                    + "item LONGTEXT NOT NULL,"
                    + "PRIMARY KEY (player_uuid, slot)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            return true;
        } catch (SQLException ex) {
            logger.log(Level.SEVERE,
                    "Failed to create " + EzAuctionPlugin.DISPLAY_NAME + " MySQL tables.", ex);
            return false;
        }
    }

    @Override
    /**
     * Loads all auction listings, orders, and returns from persistent storage.
     *
     * @return a snapshot of all current listings, orders, and returns
     */
    public AuctionStorageSnapshot load() {
        Map<String, AuctionListing> listings = new HashMap<>();
        Map<String, AuctionOrder> orders = new HashMap<>();
        Map<UUID, List<ItemStack>> returns = new HashMap<>();
        if (!isReady()) {
            return AuctionStorageSnapshot.empty();
        }
        String listingsQuery = "SELECT id, seller_uuid, price, expiry, deposit, item FROM `" + listingsTable + "`";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(listingsQuery);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String id = resultSet.getString("id");
                String sellerRaw = resultSet.getString("seller_uuid");
                UUID sellerId = parseUuid(sellerRaw);
                if (sellerId == null) {
                    continue;
                }
                double price = EconomyUtils.normalizeCurrency(resultSet.getDouble("price"));
                if (price <= 0.0D) {
                    continue;
                }
                long expiry = resultSet.getLong("expiry");
                double deposit = EconomyUtils.normalizeCurrency(resultSet.getDouble("deposit"));
                ItemStack item = ItemStackSerialization.deserialize(resultSet.getString("item"), logger);
                if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
                    continue;
                }
                listings.put(id, new AuctionListing(id, sellerId, price, expiry, item, deposit));
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE,
                    "Failed to load " + EzAuctionPlugin.DISPLAY_NAME + " listings from MySQL.", ex);
        }

        String ordersQuery = "SELECT id, buyer_uuid, price, reserved, expiry, item FROM `" + ordersTable + "`";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(ordersQuery);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String id = resultSet.getString("id");
                UUID buyerId = parseUuid(resultSet.getString("buyer_uuid"));
                if (buyerId == null) {
                    continue;
                }
                double price = EconomyUtils.normalizeCurrency(resultSet.getDouble("price"));
                if (price <= 0.0D) {
                    continue;
                }
                double reserved = EconomyUtils.normalizeCurrency(resultSet.getDouble("reserved"));
                if (reserved < price) {
                    reserved = price;
                }
                long expiry = resultSet.getLong("expiry");
                ItemStack item = ItemStackSerialization.deserialize(resultSet.getString("item"), logger);
                if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
                    continue;
                }
                orders.put(id, new AuctionOrder(id, buyerId, price, expiry, item, reserved));
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE,
                    "Failed to load " + EzAuctionPlugin.DISPLAY_NAME + " orders from MySQL.", ex);
        }

        String returnsQuery = "SELECT player_uuid, slot, item FROM `" + returnsTable + "` ORDER BY player_uuid, slot";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(returnsQuery);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                UUID playerId = parseUuid(resultSet.getString("player_uuid"));
                if (playerId == null) {
                    continue;
                }
                ItemStack item = ItemStackSerialization.deserialize(resultSet.getString("item"), logger);
                if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
                    continue;
                }
                returns.computeIfAbsent(playerId, key -> new ArrayList<>()).add(item);
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE,
                    "Failed to load " + EzAuctionPlugin.DISPLAY_NAME + " returns from MySQL.", ex);
        }

        return new AuctionStorageSnapshot(listings, orders, returns);
    }

    @Override
    /**
     * Persists all auction listings and buy orders to storage, replacing any existing data.
     *
     * @param listings the listings to save
     * @param orders   the buy orders to save
     */
    public void saveListings(Collection<AuctionListing> listings, Collection<AuctionOrder> orders) {
        if (!isReady()) {
            return;
        }
        String deleteOrders = "DELETE FROM `" + ordersTable + "`";
        String insertOrder = "INSERT INTO `" + ordersTable
                + "` (id, buyer_uuid, price, reserved, expiry, item) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (Statement deleteStatement = connection.createStatement()) {
                deleteStatement.executeUpdate(deleteOrders);
            }
            try (PreparedStatement insertStatement = connection.prepareStatement(insertOrder)) {
                for (AuctionOrder order : orders) {
                    insertStatement.setString(1, order.id());
                    insertStatement.setString(2, order.buyerId().toString());
                    insertStatement.setDouble(3, order.offeredPrice());
                    insertStatement.setDouble(4, order.reservedAmount());
                    insertStatement.setLong(5, order.expiryEpochMillis());
                    insertStatement.setString(6, ItemStackSerialization.serialize(order.requestedItem(), logger));
                    insertStatement.addBatch();
                }
                insertStatement.executeBatch();
            }
            connection.commit();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE,
                    "Failed to save " + EzAuctionPlugin.DISPLAY_NAME + " orders to MySQL.", ex);
        }
    }

    @Override
    public java.util.Optional<AuctionListing> find(String id) throws Exception {
        AuctionStorageSnapshot s = load();
        if (s == null) return java.util.Optional.empty();
        return java.util.Optional.ofNullable(s.listings().get(id));
    }

    @Override
    public java.util.List<AuctionListing> findAll() throws Exception {
        AuctionStorageSnapshot s = load();
        if (s == null) return java.util.List.of();
        return new java.util.ArrayList<>(s.listings().values());
    }

    @Override
    public void save(AuctionListing entity) throws Exception {
        insertListing(entity);
    }

    @Override
    public void delete(String id) throws Exception {
        deleteListing(id);
    }

    @Override
    public void close() {
        // No pooled resources to close in this implementation.
    }

    @Override
    /**
     * Persists a single auction listing to storage, making it visible to other servers.
     *
     * @param listing the listing to insert
     */
    public void insertListing(AuctionListing listing) {
        if (!isReady() || listing == null) {
            return;
        }
        String insert = "INSERT INTO `" + listingsTable
                + "` (id, seller_uuid, price, expiry, deposit, item) VALUES (?, ?, ?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE seller_uuid = VALUES(seller_uuid),"
                + " price = VALUES(price),"
                + " expiry = VALUES(expiry),"
                + " deposit = VALUES(deposit),"
                + " item = VALUES(item)";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setString(1, listing.id());
            statement.setString(2, listing.sellerId().toString());
            statement.setDouble(3, listing.price());
            statement.setLong(4, listing.expiryEpochMillis());
            statement.setDouble(5, listing.deposit());
            statement.setString(6, ItemStackSerialization.serialize(listing.item(), logger));
            statement.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE,
                    String.format("Failed to persist %s listing %s to MySQL.",
                            EzAuctionPlugin.DISPLAY_NAME, listing.id()),
                    ex);
        }
    }

    @Override
    /**
     * Attempts to claim (remove) a listing from storage as part of an exclusive claim.
     *
     * @param listingId the listing identifier
     * @return true if the listing was removed, false otherwise
     */
    public boolean tryClaimListing(String listingId) {
        if (!isReady() || listingId == null || listingId.isEmpty()) {
            return false;
        }
        return deleteListingInternal(listingId, true);
    }

    @Override
    /**
     * Removes a listing from storage without requiring knowledge of claim status.
     *
     * @param listingId the listing identifier
     */
    public void deleteListing(String listingId) {
        if (!isReady() || listingId == null || listingId.isEmpty()) {
            return;
        }
        deleteListingInternal(listingId, false);
    }

    @Override
    /**
     * Persists all pending return items for players to storage.
     *
     * @param returnsByPlayer a map of player UUIDs to their pending return items
     */
    public void saveReturns(Map<UUID, List<ItemStack>> returnsByPlayer) {
        if (!isReady()) {
            return;
        }
        String deleteReturns = "DELETE FROM `" + returnsTable + "`";
        String insertReturn = "INSERT INTO `" + returnsTable
                + "` (player_uuid, slot, item) VALUES (?, ?, ?)";
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (Statement deleteStatement = connection.createStatement()) {
                deleteStatement.executeUpdate(deleteReturns);
            }
            try (PreparedStatement insertStatement = connection.prepareStatement(insertReturn)) {
                for (Map.Entry<UUID, List<ItemStack>> entry : returnsByPlayer.entrySet()) {
                    List<ItemStack> items = entry.getValue();
                    if (items == null || items.isEmpty()) {
                        continue;
                    }
                    int slot = 0;
                    for (ItemStack stack : items) {
                        if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) {
                            continue;
                        }
                        insertStatement.setString(1, entry.getKey().toString());
                        insertStatement.setInt(2, slot++);
                        insertStatement.setString(3, ItemStackSerialization.serialize(stack, logger));
                        insertStatement.addBatch();
                    }
                }
                insertStatement.executeBatch();
            }
            connection.commit();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE,
                    "Failed to save " + EzAuctionPlugin.DISPLAY_NAME + " returns to MySQL.", ex);
        }
    }

    /**
     * Removes a listing from storage, optionally returning whether it was present.
     *
     * @param listingId    the listing identifier
     * @param returnResult if true, return whether the listing was present
     * @return true if the listing was present and removed, false otherwise
     */
    private boolean deleteListingInternal(String listingId, boolean returnResult) {
        String delete = "DELETE FROM `" + listingsTable + "` WHERE id = ?";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(delete)) {
            statement.setString(1, listingId);
            int affected = statement.executeUpdate();
            return returnResult ? affected > 0 : true;
        } catch (SQLException ex) {
            logger.log(Level.SEVERE,
                    String.format("Failed to delete %s listing %s from MySQL.",
                            EzAuctionPlugin.DISPLAY_NAME, listingId),
                    ex);
            return false;
        }
    }

    /**
     * Obtains a new JDBC connection to the configured MySQL database.
     *
     * @return a new JDBC connection
     * @throws SQLException if a connection cannot be established
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, mysql.username(), mysql.password());
    }

    /**
     * Returns whether the storage is ready for use (driver loaded).
     *
     * @return true if ready, false otherwise
     */
    private boolean isReady() {
        return driverLoaded;
    }

    /**
     * Parses a UUID from a string, logging a warning if invalid.
     *
     * @param raw the string to parse
     * @return the parsed UUID, or null if invalid
     */
    private UUID parseUuid(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            logger.log(Level.WARNING,
                    "Invalid UUID encountered in " + EzAuctionPlugin.DISPLAY_NAME + " MySQL storage: {0}", raw);
            return null;
        }
    }

    /**
     * Sanitizes a table name prefix for use in MySQL table names.
     *
     * @param input the input string
     * @return a sanitized table name prefix
     */
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
    
    /**
     * Closes any resources held by this storage implementation.
     * This implementation does not maintain persistent connections or resources,
     * so this method is a no-op. Required by the {@link AutoCloseable} interface.
     */
    @Override
    public void close() {
        // No resources to close in this implementation.
    }
}
