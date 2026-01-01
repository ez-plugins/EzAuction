package com.skyblockexp.ezauction.storage.yaml;

import com.skyblockexp.ezauction.transaction.AuctionTransactionHistoryEntry;
import com.skyblockexp.ezauction.transaction.AuctionTransactionType;
import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.storage.AuctionHistoryStorage;
import com.skyblockexp.ezauction.util.EconomyUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * YAML implementation of {@link AuctionHistoryStorage}.
 */
public final class YamlAuctionHistoryStorage implements AuctionHistoryStorage {

    private final JavaPlugin plugin;
    private File historyFile;
    private final Lock historyLock = new ReentrantLock(true);

    public YamlAuctionHistoryStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean initialize() {
        if (plugin == null) {
            return false;
        }
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to create " + EzAuctionPlugin.DISPLAY_NAME + " data folder at {0}.", dataFolder);
        }
        historyFile = new File(dataFolder, "auction-history.yml");
        if (historyFile.exists()) {
            return true;
        }
        try {
            File parent = historyFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().log(Level.WARNING, "Failed to create directory for {0}.", historyFile);
            }
            if (!historyFile.exists() && !historyFile.createNewFile()) {
                throw new IOException("Unable to create file " + historyFile);
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize auction history YAML storage.", ex);
            return false;
        }
        return true;
    }

    @Override
    public Map<UUID, Deque<AuctionTransactionHistoryEntry>> loadAll() {
        historyLock.lock();
        try {
            return loadAllUnlocked();
        } finally {
            historyLock.unlock();
        }
    }

    @Override
    public void saveAll(Map<UUID, Deque<AuctionTransactionHistoryEntry>> history) {
        if (historyFile == null) {
            return;
        }
        historyLock.lock();
        try {
            writeAllLocked(history);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save auction history YAML file.", ex);
        } finally {
            historyLock.unlock();
        }
    }

    @Override
    public void savePlayerHistory(UUID playerId, Deque<AuctionTransactionHistoryEntry> history) {
        if (playerId == null || historyFile == null) {
            return;
        }
        historyLock.lock();
        try {
            Map<UUID, Deque<AuctionTransactionHistoryEntry>> existing = loadAllUnlocked();
            Deque<AuctionTransactionHistoryEntry> copy = history != null
                    ? new ArrayDeque<>(history)
                    : new ArrayDeque<>();
            existing.put(playerId, copy);
            writeAllLocked(existing);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to save auction history YAML file for player " + playerId + '.', ex);
        } finally {
            historyLock.unlock();
        }
    }

    @Override
    public void close() {
        // Nothing to close for YAML storage.
    }

    private Map<UUID, Deque<AuctionTransactionHistoryEntry>> loadAllUnlocked() {
        Map<UUID, Deque<AuctionTransactionHistoryEntry>> entries = new HashMap<>();
        if (historyFile == null || !historyFile.exists()) {
            return entries;
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(historyFile);
        ConfigurationSection historySection = configuration.getConfigurationSection("history");
        if (historySection == null) {
            return entries;
        }
        Set<String> playerKeys = historySection.getKeys(false);
        for (String playerKey : playerKeys) {
            UUID ownerId;
            try {
                ownerId = UUID.fromString(playerKey);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().log(Level.WARNING,
                        "Ignoring auction history entry for invalid player id " + playerKey, ex);
                continue;
            }
            ConfigurationSection playerSection = historySection.getConfigurationSection(playerKey);
            if (playerSection == null) {
                continue;
            }
            List<AuctionTransactionHistoryEntry> playerEntries = new ArrayList<>();
            for (String entryKey : playerSection.getKeys(false)) {
                ConfigurationSection entrySection = playerSection.getConfigurationSection(entryKey);
                if (entrySection == null) {
                    continue;
                }
                AuctionTransactionHistoryEntry entry = loadEntry(entrySection);
                if (entry != null) {
                    playerEntries.add(entry);
                }
            }
            if (!playerEntries.isEmpty()) {
                playerEntries.sort(Comparator.comparingLong(AuctionTransactionHistoryEntry::timestamp).reversed());
                Deque<AuctionTransactionHistoryEntry> deque = new ArrayDeque<>(playerEntries);
                entries.put(ownerId, deque);
            }
        }
        return entries;
    }

    private void writeAllLocked(Map<UUID, Deque<AuctionTransactionHistoryEntry>> history) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        ConfigurationSection historySection = configuration.createSection("history");
        for (Map.Entry<UUID, Deque<AuctionTransactionHistoryEntry>> entry : history.entrySet()) {
            ConfigurationSection playerSection = historySection.createSection(entry.getKey().toString());
            Deque<AuctionTransactionHistoryEntry> deque = entry.getValue();
            if (deque == null || deque.isEmpty()) {
                continue;
            }
            int index = 0;
            for (AuctionTransactionHistoryEntry historyEntry : deque) {
                ConfigurationSection entrySection = playerSection.createSection(Integer.toString(index++));
                writeEntry(historyEntry, entrySection);
            }
        }
        configuration.save(historyFile);
    }

    private AuctionTransactionHistoryEntry loadEntry(ConfigurationSection section) {
        String typeName = section.getString("type");
        AuctionTransactionType type = parseType(typeName);
        if (type == null) {
            return null;
        }
        long timestamp = section.getLong("timestamp", 0L);
        double price = EconomyUtils.normalizeCurrency(section.getDouble("price", 0.0D));
        String counterpartIdRaw = section.getString("counterpart-id");
        UUID counterpartId = null;
        if (counterpartIdRaw != null && !counterpartIdRaw.isEmpty()) {
            try {
                counterpartId = UUID.fromString(counterpartIdRaw);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().log(Level.WARNING,
                        "Ignoring invalid counterpart id in auction history entry.", ex);
            }
        }
        String counterpartName = section.getString("counterpart-name");
        ItemStack item = section.getItemStack("item");
        if (item != null) {
            item = item.clone();
        }
        return new AuctionTransactionHistoryEntry(type, counterpartId, counterpartName, price, timestamp, item);
    }

    private void writeEntry(AuctionTransactionHistoryEntry entry, ConfigurationSection section) {
        section.set("type", entry.type().name());
        section.set("timestamp", entry.timestamp());
        section.set("price", entry.price());
        if (entry.counterpartId() != null) {
            section.set("counterpart-id", entry.counterpartId().toString());
        }
        if (entry.counterpartName() != null && !entry.counterpartName().isEmpty()) {
            section.set("counterpart-name", entry.counterpartName());
        }
        if (entry.item() != null) {
            section.set("item", entry.item());
        }
    }

    private AuctionTransactionType parseType(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return null;
        }
        String normalized = typeName.trim().toUpperCase(Locale.ENGLISH);
        for (AuctionTransactionType type : AuctionTransactionType.values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
