package com.skyblockexp.ezauction.config;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads the auction configuration from disk.
 */
public final class AuctionConfigurationLoader {

    private AuctionConfigurationLoader() {
    }

    public static AuctionConfiguration load(JavaPlugin plugin) {
        if (plugin == null) {
            return AuctionConfiguration.defaultConfiguration();
        }
        File dataFolder = plugin.getDataFolder();
        YamlConfiguration baseConfiguration = loadConfiguration(dataFolder, "auction.yml");
        String languageCode = normalizeLanguage(baseConfiguration != null ? baseConfiguration.getString("language") : null);

        YamlConfiguration storageConfigurationFile = loadConfiguration(dataFolder, "auction-storage.yml");
        YamlConfiguration menuConfigurationFile = loadLocalizedConfiguration(plugin, dataFolder,
                "messages/menu-layout", languageCode);
        YamlConfiguration menuInteractionConfigurationFile = loadLocalizedConfiguration(plugin, dataFolder,
                "messages/menu-interactions", languageCode);
        YamlConfiguration valueConfigurationFile = loadConfiguration(dataFolder, "auction-values.yml");
        YamlConfiguration messageConfigurationFile = loadLocalizedConfiguration(plugin, dataFolder,
                "messages/gui-messages", languageCode);
        YamlConfiguration commandMessageConfigurationFile = firstNonNull(
                loadLocalizedConfiguration(plugin, dataFolder, "messages/messages", languageCode),
                loadConfiguration(dataFolder, "messages.yml"));

        if (baseConfiguration == null
                && storageConfigurationFile == null
                && menuConfigurationFile == null
                && menuInteractionConfigurationFile == null
                && valueConfigurationFile == null
                && messageConfigurationFile == null
                && commandMessageConfigurationFile == null) {
            return AuctionConfiguration.defaultConfiguration();
        }

        ConfigurationSection listingsSection = getSection(baseConfiguration, "listings");
        int baseLimit = Math.max(0, getInt(baseConfiguration, "listings.max-listings-per-player", 0));
        AuctionListingRules listingRules = AuctionListingRules.from(listingsSection);
        List<Duration> durationOptions = parseDurationOptions(listingsSection);

        ConfigurationSection storageSection = firstNonNull(storageConfigurationFile,
                getSection(baseConfiguration, "storage"));
        AuctionStorageConfiguration storageConfiguration = AuctionStorageConfiguration.from(storageSection);

        ConfigurationSection menuSection = firstNonNull(menuConfigurationFile,
                getSection(baseConfiguration, "menu"));
        AuctionMenuConfiguration menuConfiguration = AuctionMenuConfiguration.from(menuSection);

        ConfigurationSection menusSection = firstNonNull(menuInteractionConfigurationFile,
                getSection(baseConfiguration, "menus"));
        AuctionMenuInteractionConfiguration menuInteractionConfiguration = AuctionMenuInteractionConfiguration
                .from(menusSection);

        ConfigurationSection valuesSection = firstNonNull(valueConfigurationFile,
                getSection(baseConfiguration, "values"));
        AuctionValueConfiguration valueConfiguration = AuctionValueConfiguration.from(valuesSection);

        ConfigurationSection messageSection = firstNonNull(messageConfigurationFile,
                getSection(baseConfiguration, "gui-messages"));
        AuctionMessageConfiguration messageConfiguration = AuctionMessageConfiguration.from(messageSection);

        ConfigurationSection commandMessageSection = firstNonNull(commandMessageConfigurationFile,
                getSection(baseConfiguration, "messages"));
        AuctionCommandMessageConfiguration commandMessageConfiguration = AuctionCommandMessageConfiguration
                .from(commandMessageSection);
        ConfigurationSection backendMessageSection = commandMessageSection != null
                ? commandMessageSection.getConfigurationSection("backend")
                : null;
        AuctionBackendMessages backendMessages = AuctionBackendMessages.from(backendMessageSection);

        ConfigurationSection hologramSection = getSection(baseConfiguration, "holograms");
        AuctionHologramConfiguration hologramConfiguration = AuctionHologramConfiguration.from(hologramSection);

        ConfigurationSection liveAuctionSection = getSection(baseConfiguration, "live-auctions");
        LiveAuctionConfiguration liveAuctionConfiguration = LiveAuctionConfiguration.from(liveAuctionSection);

        AuctionConfiguration result = new AuctionConfiguration(baseLimit, storageConfiguration, listingRules,
                menuConfiguration, menuInteractionConfiguration, valueConfiguration, messageConfiguration,
                backendMessages, commandMessageConfiguration, hologramConfiguration, liveAuctionConfiguration,
                durationOptions,
                baseConfiguration != null && baseConfiguration.isBoolean("debug") ? baseConfiguration.getBoolean("debug") : false);
        plugin.getLogger().log(Level.CONFIG, "Loaded auction configuration: {0}", result);
        return result;
    }

    private static YamlConfiguration loadConfiguration(File dataFolder, String fileName) {
        if (dataFolder == null || fileName == null || fileName.isEmpty()) {
            return null;
        }
        File file = new File(dataFolder, fileName);
        if (!file.exists()) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private static YamlConfiguration loadLocalizedConfiguration(JavaPlugin plugin, File dataFolder, String baseName,
            String languageCode) {
        if (baseName == null || baseName.isEmpty() || languageCode == null || languageCode.isEmpty()) {
            return null;
        }
        String fileName = baseName + '_' + languageCode + ".yml";
        YamlConfiguration localized = loadConfiguration(dataFolder, fileName);
        if (localized != null) {
            return localized;
        }
        if (!"en".equals(languageCode)) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING,
                        "Localized configuration \"{0}\" was not found; falling back to English.", fileName);
            }
            return loadConfiguration(dataFolder, baseName + "_en.yml");
        }
        return null;
    }

    private static ConfigurationSection getSection(YamlConfiguration configuration, String path) {
        if (configuration == null || path == null || path.isEmpty()) {
            return null;
        }
        return configuration.getConfigurationSection(path);
    }

    private static <T> T firstNonNull(T primary, T fallback) {
        return primary != null ? primary : fallback;
    }

    private static int getInt(YamlConfiguration configuration, String path, int fallback) {
        if (configuration == null || path == null || path.isEmpty()) {
            return fallback;
        }
        return configuration.getInt(path, fallback);
    }

    private static String normalizeLanguage(String raw) {
        if (raw == null || raw.isBlank()) {
            return "en";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        String sanitized = normalized.replaceAll("[^a-z0-9_]", "");
        if (sanitized.isEmpty()) {
            return "en";
        }
        return sanitized;
    }

    private static List<Duration> parseDurationOptions(ConfigurationSection listingsSection) {
        if (listingsSection == null) {
            return List.of();
        }
        List<?> rawValues = listingsSection.getList("duration-options");
        if (rawValues == null || rawValues.isEmpty()) {
            return List.of();
        }
        List<Duration> parsed = new ArrayList<>();
        for (Object value : rawValues) {
            Duration duration = parseDurationValue(value);
            if (duration != null) {
                parsed.add(duration);
            }
        }
        if (parsed.isEmpty()) {
            return List.of();
        }
        return List.copyOf(parsed);
    }

    private static Duration parseDurationValue(Object value) {
        if (value instanceof Duration duration) {
            if (!duration.isNegative() && !duration.isZero()) {
                return duration;
            }
            return null;
        }
        if (value instanceof Number number) {
            double minutes = number.doubleValue();
            if (!Double.isFinite(minutes)) {
                return null;
            }
            long rounded = Math.round(minutes);
            if (rounded <= 0L) {
                return null;
            }
            try {
                return Duration.ofMinutes(rounded);
            } catch (ArithmeticException ex) {
                return null;
            }
        }
        if (value instanceof String text) {
            String normalized = text.trim();
            if (normalized.isEmpty()) {
                return null;
            }
            return parseDurationString(normalized);
        }
        return null;
    }

    private static Duration parseDurationString(String input) {
        String normalized = input.trim().toLowerCase(Locale.ENGLISH);
        if (normalized.isEmpty()) {
            return null;
        }
        double multiplier = 1.0D; // minutes
        String numericPortion = normalized;
        if (normalized.endsWith("minutes")) {
            numericPortion = normalized.substring(0, normalized.length() - "minutes".length());
        } else if (normalized.endsWith("minute")) {
            numericPortion = normalized.substring(0, normalized.length() - "minute".length());
        } else if (normalized.endsWith("mins")) {
            numericPortion = normalized.substring(0, normalized.length() - "mins".length());
        } else if (normalized.endsWith("min")) {
            numericPortion = normalized.substring(0, normalized.length() - "min".length());
        } else if (normalized.endsWith("m")) {
            numericPortion = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("hours")) {
            multiplier = 60.0D;
            numericPortion = normalized.substring(0, normalized.length() - "hours".length());
        } else if (normalized.endsWith("hour")) {
            multiplier = 60.0D;
            numericPortion = normalized.substring(0, normalized.length() - "hour".length());
        } else if (normalized.endsWith("hrs")) {
            multiplier = 60.0D;
            numericPortion = normalized.substring(0, normalized.length() - "hrs".length());
        } else if (normalized.endsWith("hr")) {
            multiplier = 60.0D;
            numericPortion = normalized.substring(0, normalized.length() - "hr".length());
        } else if (normalized.endsWith("h")) {
            multiplier = 60.0D;
            numericPortion = normalized.substring(0, normalized.length() - 1);
        }
        double amount;
        try {
            amount = Double.parseDouble(numericPortion.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
        double minutes = amount * multiplier;
        if (!Double.isFinite(minutes) || minutes <= 0.0D) {
            return null;
        }
        long rounded = Math.max(1L, Math.round(minutes));
        try {
            return Duration.ofMinutes(rounded);
        } catch (ArithmeticException ex) {
            return null;
        }
    }
}
