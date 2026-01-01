package com.skyblockexp.ezauction.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import com.skyblockexp.ezauction.EzAuctionPlugin;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

/**
 * Utility methods for serializing ItemStack objects.
 */
public final class ItemStackSerialization {

    private ItemStackSerialization() {
    }

    public static String serialize(ItemStack item, Logger logger) {
        if (item == null) {
            return null;
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(item.clone());
            dataOutput.flush();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException ex) {
            if (logger != null) {
                logger.log(Level.SEVERE,
                        "Failed to serialize ItemStack for " + EzAuctionPlugin.DISPLAY_NAME + " storage.", ex);
            }
            return null;
        }
    }

    public static ItemStack deserialize(String data, Logger logger) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException ex) {
            if (logger != null) {
                logger.log(Level.WARNING,
                        "Failed to decode base64 ItemStack data for " + EzAuctionPlugin.DISPLAY_NAME + " storage.", ex);
            }
            return null;
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            Object object = dataInput.readObject();
            if (object instanceof ItemStack stack) {
                return stack.clone();
            }
        } catch (ClassNotFoundException | IOException ex) {
            if (logger != null) {
                logger.log(Level.SEVERE,
                        "Failed to deserialize ItemStack for " + EzAuctionPlugin.DISPLAY_NAME + " storage.", ex);
            }
        }
        return null;
    }
}
