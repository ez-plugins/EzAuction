package com.skyblockexp.ezauction.storage.yaml;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

/**
 * Encodes and decodes {@link ItemStack} instances to and from a Base64 byte string
 * using {@link BukkitObjectOutputStream}.
 *
 * <p>Unlike storing items directly in a {@link org.bukkit.configuration.file.YamlConfiguration}
 * via {@code section.set("item", itemStack)}, this format is stored as a plain YAML string
 * scalar. Bukkit's {@code YamlConstructor} therefore does <em>not</em> attempt to
 * auto-deserialize it during file loading, which eliminates the
 * {@code Material cannot be null} {@code ERROR} log entries that occur when item data was
 * written on a Paper server and later loaded on a Spigot server (or vice-versa).
 *
 * <p>Requires a running Bukkit server (i.e. must not be called before {@code onEnable}).
 */
final class ItemStackSerializer {

    private ItemStackSerializer() {}

    /**
     * Serialises {@code item} to a Base64 string.
     *
     * @param item the item to serialise; must not be {@code null}
     * @return a non-null, non-empty Base64 string
     * @throws IOException if the item cannot be serialised
     */
    static String serialize(ItemStack item) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeObject(item);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    /**
     * Deserialises an {@link ItemStack} from a Base64 string produced by
     * {@link #serialize(ItemStack)}.
     *
     * @param base64 the Base64 string to deserialise
     * @return the restored {@link ItemStack}
     * @throws IOException            if the byte data cannot be read
     * @throws ClassNotFoundException if the item class cannot be resolved
     */
    static ItemStack deserialize(String base64) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(base64);
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
            return (ItemStack) in.readObject();
        }
    }

    /**
     * Loads a YAML {@link File} safely by stripping
     * {@code ==: org.bukkit.inventory.ItemStack} type-tag lines from the raw text
     * before handing it to {@link YamlConfiguration#loadFromString}.
     *
     * <p>Bukkit's {@code YamlConstructor} auto-deserializes any map node tagged
     * {@code ==: org.bukkit.inventory.ItemStack} during file loading. On Spigot this
     * fails with {@code Material cannot be null} whenever the data was originally
     * written by a Paper 1.21+ server. Removing those lines turns the node into a
     * plain map; {@code getItemStack()} then returns {@code null} (handled gracefully
     * by the callers), and no ERROR is logged.
     *
     * @param file the YAML file to load; need not exist
     * @return a populated {@link YamlConfiguration}, or an empty one if the file does
     *         not exist or cannot be parsed
     */
    static YamlConfiguration loadSafe(File file) {
        YamlConfiguration config = new YamlConfiguration();
        if (file == null || !file.exists()) {
            return config;
        }
        try {
            String raw = Files.readString(file.toPath());
            // Replace Bukkit YAML type-tag lines with a YAML comment so the
            // YamlConstructor never attempts platform-specific deserialization.
            raw = raw.replace("==: org.bukkit.inventory.ItemStack", "# legacy-item-tag stripped");
            config.loadFromString(raw);
        } catch (IOException | InvalidConfigurationException ignored) {
            // Return the empty config; callers treat missing sections as empty data.
        }
        return config;
    }
}
