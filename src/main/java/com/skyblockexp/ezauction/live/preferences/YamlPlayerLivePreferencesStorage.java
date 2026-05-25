package com.skyblockexp.ezauction.live.preferences;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * YAML-backed implementation of {@link PlayerLivePreferencesStorage}.
 * All preferences are stored in {@code live-preferences.yml} in the plugin's data folder.
 */
public final class YamlPlayerLivePreferencesStorage implements PlayerLivePreferencesStorage {

    private static final String FILE_NAME = "live-preferences.yml";

    private final JavaPlugin plugin;
    private File file;

    public YamlPlayerLivePreferencesStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean initialize() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("[EzAuction] Could not create data folder for live preferences.");
        }
        file = new File(dataFolder, FILE_NAME);
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning("[EzAuction] Could not create " + FILE_NAME + '.');
                    return false;
                }
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "[EzAuction] Failed to create " + FILE_NAME, ex);
                return false;
            }
        }
        return true;
    }

    @Override
    public PlayerLivePreferences load(UUID playerId) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String key = playerId.toString();
        if (!yaml.contains(key)) return null;
        ConfigurationSection section = yaml.getConfigurationSection(key);
        if (section == null) return null;
        return fromSection(playerId, section);
    }

    @Override
    public void save(PlayerLivePreferences preferences) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String key = preferences.playerId().toString();
        yaml.set(key + ".notifications-enabled", preferences.notificationsEnabled());
        yaml.set(key + ".display-type-names", preferences.displayTypeNames());
        yaml.set(key + ".mute-other-chat", preferences.muteOtherChatInAuction());
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "[EzAuction] Failed to save live preferences.", ex);
        }
    }

    @Override
    public Collection<PlayerLivePreferences> loadAll() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<PlayerLivePreferences> result = new ArrayList<>();
        for (String key : yaml.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                ConfigurationSection section = yaml.getConfigurationSection(key);
                if (section != null) {
                    result.add(fromSection(playerId, section));
                }
            } catch (IllegalArgumentException ignored) {
                // not a UUID key
            }
        }
        return result;
    }

    private static PlayerLivePreferences fromSection(UUID playerId, ConfigurationSection section) {
        boolean notifEnabled = section.getBoolean("notifications-enabled", true);
        List<String> displayTypes = section.getStringList("display-type-names");
        boolean mute = section.getBoolean("mute-other-chat", false);
        return new PlayerLivePreferences(playerId, notifEnabled, displayTypes, mute);
    }
}
