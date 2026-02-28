package com.skyblockexp.ezauction.storage.yaml;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.storage.AuctionListingRepository;
import com.skyblockexp.ezauction.storage.AuctionStorageSnapshot;
import com.skyblockexp.ezauction.util.EconomyUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * YAML-backed implementation of {@link AuctionListingRepository}.
 */
public final class YamlAuctionStorage implements AuctionListingRepository {

    private final JavaPlugin plugin;
    private File listingsFile;
    private File returnsFile;

    public YamlAuctionStorage(JavaPlugin plugin) {
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
        listingsFile = new File(dataFolder, "auction-listings.yml");
        returnsFile = new File(dataFolder, "auction-returns.yml");
        try {
            ensureFile(listingsFile);
            ensureFile(returnsFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to initialize YAML storage for " + EzAuctionPlugin.DISPLAY_NAME + '.', ex);
            return false;
        }
        return true;
    }

    @Override
    public AuctionStorageSnapshot load() {
        Map<String, AuctionListing> listings = new HashMap<>();
        Map<String, AuctionOrder> orders = new HashMap<>();
        Map<UUID, List<ItemStack>> returns = new HashMap<>();

        if (listingsFile != null && listingsFile.exists()) {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(listingsFile);
            ConfigurationSection listingsSection = configuration.getConfigurationSection("listings");
            if (listingsSection != null) {
                for (String id : listingsSection.getKeys(false)) {
                    ConfigurationSection listingSection = listingsSection.getConfigurationSection(id);
                    if (listingSection == null) {
                        continue;
                    }
                    AuctionListing listing = loadListing(id, listingSection);
                    if (listing != null) {
                        listings.put(id, listing);
                    }
                }
            }

            ConfigurationSection ordersSection = configuration.getConfigurationSection("orders");
            if (ordersSection != null) {
                for (String id : ordersSection.getKeys(false)) {
                    ConfigurationSection orderSection = ordersSection.getConfigurationSection(id);
                    if (orderSection == null) {
                        continue;
                    }
                    AuctionOrder order = loadOrder(id, orderSection);
                    if (order != null) {
                        orders.put(id, order);
                    }
                }
            }
        }

        if (returnsFile != null && returnsFile.exists()) {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(returnsFile);
            ConfigurationSection section = configuration.getConfigurationSection("returns");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    UUID playerId;
                    try {
                        playerId = UUID.fromString(key);
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().log(Level.WARNING,
                                "Ignoring auction returns entry for invalid player id " + key, ex);
                        continue;
                    }
                    List<ItemStack> items = new ArrayList<>();
                    List<?> serializedItems = section.getList(key);
                    if (serializedItems != null) {
                        for (Object object : serializedItems) {
                            if (object instanceof ItemStack stack && stack.getType() != Material.AIR
                                    && stack.getAmount() > 0) {
                                items.add(stack.clone());
                            }
                        }
                    }
                    if (!items.isEmpty()) {
                        returns.put(playerId, items);
                    }
                }
            }
        }

        return new AuctionStorageSnapshot(listings, orders, returns);
    }

    @Override
    public void saveListings(java.util.Collection<AuctionListing> listings,
            java.util.Collection<AuctionOrder> orders) {
        if (listingsFile == null) {
            return;
        }
        YamlConfiguration configuration = new YamlConfiguration();
        ConfigurationSection listingsSection = configuration.createSection("listings");
        for (AuctionListing listing : listings) {
            ConfigurationSection listingSection = listingsSection.createSection(listing.id());
            listingSection.set("seller", listing.sellerId().toString());
            listingSection.set("price", listing.price());
            listingSection.set("expiry", listing.expiryEpochMillis());
            listingSection.set("deposit", listing.deposit());
            listingSection.set("item", listing.item());
        }
        ConfigurationSection ordersSection = configuration.createSection("orders");
        for (AuctionOrder order : orders) {
            ConfigurationSection orderSection = ordersSection.createSection(order.id());
            orderSection.set("buyer", order.buyerId().toString());
            orderSection.set("price", order.offeredPrice());
            orderSection.set("expiry", order.expiryEpochMillis());
            orderSection.set("reserved", order.reservedAmount());
            orderSection.set("item", order.requestedItem());
        }
        try {
            configuration.save(listingsFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to save " + EzAuctionPlugin.DISPLAY_NAME + " listings file.", ex);
        }
    }

    @Override
    public void saveReturns(Map<UUID, List<ItemStack>> returnsByPlayer) {
        if (returnsFile == null) {
            return;
        }
        YamlConfiguration configuration = new YamlConfiguration();
        ConfigurationSection section = configuration.createSection("returns");
        for (Map.Entry<UUID, List<ItemStack>> entry : returnsByPlayer.entrySet()) {
            List<ItemStack> serialized = new ArrayList<>();
            for (ItemStack stack : entry.getValue()) {
                if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) {
                    continue;
                }
                serialized.add(stack.clone());
            }
            if (!serialized.isEmpty()) {
                section.set(entry.getKey().toString(), serialized);
            }
        }
        try {
            configuration.save(returnsFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to save " + EzAuctionPlugin.DISPLAY_NAME + " returns file.", ex);
        }
    }

    @Override
    public void close() {
        // Nothing to close for YAML storage.
    }

    @Override
    public java.util.Optional<com.skyblockexp.ezauction.AuctionListing> find(String id) throws Exception {
        AuctionStorageSnapshot s = load();
        if (s == null) return java.util.Optional.empty();
        return java.util.Optional.ofNullable(s.listings().get(id));
    }

    @Override
    public java.util.List<com.skyblockexp.ezauction.AuctionListing> findAll() throws Exception {
        AuctionStorageSnapshot s = load();
        if (s == null) return java.util.List.of();
        return new java.util.ArrayList<>(s.listings().values());
    }

    @Override
    public void save(com.skyblockexp.ezauction.AuctionListing entity) throws Exception {
        AuctionStorageSnapshot s = load();
        java.util.Map<String, com.skyblockexp.ezauction.AuctionListing> listings = new java.util.HashMap<>();
        java.util.Map<String, com.skyblockexp.ezauction.AuctionOrder> orders = new java.util.HashMap<>();
        if (s != null) {
            listings.putAll(s.listings());
            orders.putAll(s.orders());
        }
        listings.put(entity.id(), entity);
        saveListings(listings.values(), orders.values());
    }

    @Override
    public void delete(String id) throws Exception {
        AuctionStorageSnapshot s = load();
        java.util.Map<String, com.skyblockexp.ezauction.AuctionListing> listings = new java.util.HashMap<>();
        java.util.Map<String, com.skyblockexp.ezauction.AuctionOrder> orders = new java.util.HashMap<>();
        if (s != null) {
            listings.putAll(s.listings());
            orders.putAll(s.orders());
        }
        listings.remove(id);
        saveListings(listings.values(), orders.values());
    }

    private AuctionListing loadListing(String id, ConfigurationSection section) {
        String sellerRaw = section.getString("seller");
        if (sellerRaw == null || sellerRaw.isEmpty()) {
            plugin.getLogger().warning("Ignoring auction listing " + id + " because the seller id is missing.");
            return null;
        }
        UUID sellerId;
        try {
            sellerId = UUID.fromString(sellerRaw);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING, "Ignoring auction listing " + id + " because the seller id is invalid.", ex);
            return null;
        }
        double price = EconomyUtils.normalizeCurrency(section.getDouble("price"));
        if (price <= 0.0D) {
            plugin.getLogger().warning("Ignoring auction listing " + id + " because the price is invalid.");
            return null;
        }
        long expiry = section.getLong("expiry");
        double deposit = EconomyUtils.normalizeCurrency(section.getDouble("deposit", 0.0D));
        ItemStack item = section.getItemStack("item");
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            plugin.getLogger().warning("Ignoring auction listing " + id + " because the item is invalid.");
            return null;
        }
        return new AuctionListing(id, sellerId, price, expiry, item.clone(), deposit);
    }

    private AuctionOrder loadOrder(String id, ConfigurationSection section) {
        String buyerRaw = section.getString("buyer");
        if (buyerRaw == null || buyerRaw.isEmpty()) {
            plugin.getLogger().warning("Ignoring auction order " + id + " because the buyer id is missing.");
            return null;
        }
        UUID buyerId;
        try {
            buyerId = UUID.fromString(buyerRaw);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING, "Ignoring auction order " + id + " because the buyer id is invalid.", ex);
            return null;
        }
        double price = EconomyUtils.normalizeCurrency(section.getDouble("price"));
        if (price <= 0.0D) {
            plugin.getLogger().warning("Ignoring auction order " + id + " because the price is invalid.");
            return null;
        }
        double reserved = EconomyUtils.normalizeCurrency(section.getDouble("reserved", price));
        if (reserved < price) {
            reserved = price;
        }
        long expiry = section.getLong("expiry");
        ItemStack template = section.getItemStack("item");
        if (template == null || template.getType() == Material.AIR || template.getAmount() <= 0) {
            plugin.getLogger().warning("Ignoring auction order " + id + " because the item template is invalid.");
            return null;
        }
        return new AuctionOrder(id, buyerId, price, expiry, template.clone(), reserved);
    }

    private void ensureFile(File file) throws IOException {
        if (file.exists()) {
            return;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().log(Level.WARNING, "Failed to create directory for {0}.", file);
        }
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Unable to create file " + file);
        }
    }
}
