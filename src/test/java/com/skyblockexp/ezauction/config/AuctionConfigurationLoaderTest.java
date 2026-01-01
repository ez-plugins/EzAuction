package com.skyblockexp.ezauction.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.skyblockexp.ezauction.config.AuctionBackendMessages;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration.GeneralMessages;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration.HologramMessages;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuctionConfigurationLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadUsesLocalizedCommandMessagesWhenAvailable() throws IOException {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("AuctionConfigurationLoaderTest"));

        Files.createDirectories(tempDir.resolve("messages"));
        Files.writeString(tempDir.resolve("auction.yml"), "language: es\n");

        try (InputStream spanishMessages = getClass().getClassLoader()
                .getResourceAsStream("messages/messages_es.yml")) {
            assertNotNull(spanishMessages, "Spanish message resource should be available");
            Files.copy(spanishMessages, tempDir.resolve("messages/messages_es.yml"));
        }

        try (InputStream spanishGuiMessages = getClass().getClassLoader()
                .getResourceAsStream("messages/gui-messages_es.yml")) {
            assertNotNull(spanishGuiMessages, "Spanish GUI message resource should be available");
            Files.copy(spanishGuiMessages, tempDir.resolve("messages/gui-messages_es.yml"));
        }

        AuctionConfiguration configuration = AuctionConfigurationLoader.load(plugin);
        AuctionCommandMessageConfiguration commandMessages = configuration.commandMessageConfiguration();
        GeneralMessages general = commandMessages.general();
        HologramMessages holograms = commandMessages.holograms();
        AuctionMessageConfiguration guiMessages = configuration.messageConfiguration();

        assertEquals("&cSolo los jugadores pueden usar la casa de subastas.", general.consoleOnly());
        assertEquals("&eUso: /{label} <tipo|clear>", holograms.usage());
        assertEquals("&cNo se pudo encontrar ese listado.", guiMessages.browser().listingNotFound());
    }

    @Test
    void backendMessagesFallbackToEnglishWhenMissing() throws IOException {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("AuctionConfigurationLoaderTest"));

        Files.createDirectories(tempDir.resolve("messages"));
        Files.writeString(tempDir.resolve("auction.yml"), "language: es\n");
        Files.writeString(tempDir.resolve("messages/messages_es.yml"), "");
        Files.writeString(tempDir.resolve("messages/gui-messages_es.yml"), "");

        AuctionConfiguration configuration = AuctionConfigurationLoader.load(plugin);
        AuctionBackendMessages backendMessages = configuration.backendMessages();

        assertEquals(AuctionBackendMessages.defaults().listing().creation().playersOnly(),
                backendMessages.listing().creation().playersOnly());
    }
}
