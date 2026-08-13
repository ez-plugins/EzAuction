package com.skyblockexp.ezauction.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.skyblockexp.ezauction.compat.HologramDisplay;
import com.skyblockexp.ezauction.compat.TextDisplayHologramPlatform;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration.HologramMessages;
import com.skyblockexp.ezauction.config.AuctionHologramConfiguration;
import com.skyblockexp.ezauction.hologram.AuctionHologramManager;
import com.skyblockexp.ezauction.hologram.AuctionHologramType;
import com.skyblockexp.ezauction.service.AuctionManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import java.util.List;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.mockbukkit.mockbukkit.command.MessageTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class AuctionHologramCommandTest {

    private static final String MARKER_KEY = "auction_hologram_type";

    private ServerMock server;
    private HologramMessages messages;
    private TextDisplayHologramPlatform platform;
    private JavaPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.getOrCreateMock();
        plugin = MockBukkit.createMockPlugin("EzAuctionTest");
        platform = new TextDisplayHologramPlatform(plugin);
        messages = HologramMessages.defaults();
    }

    private AuctionHologramManager newManager() {
        AuctionManager auctionManager = mock(AuctionManager.class);
        AuctionTransactionService transactionService = mock(AuctionTransactionService.class);
        AuctionHologramConfiguration configuration = AuctionHologramConfiguration.defaults();
        AuctionHologramManager manager = new AuctionHologramManager(plugin, auctionManager,
                transactionService, configuration, platform);
        manager.enable();
        return manager;
    }

    private Command command() {
        return mock(Command.class);
    }

    @Test
    void nonPlayerSenderGetsPlayersOnly() {
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, newManager(), messages);
        CommandSender console = server.getConsoleSender();
        cmd.onCommand(console, command(), "auctionhologram", new String[]{"list"});
        assertTrue(((MessageTarget) console).nextMessage().contains("Only players may manage auction holograms"));
    }

    @Test
    void nullManagerReportsDisabled() {
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, null, messages);
        PlayerMock player = server.addPlayer();
        cmd.onCommand(player, command(), "auctionhologram", new String[]{"list"});
        assertTrue(player.nextMessage().contains("Auction holograms are disabled"));
    }

    @Test
    void noArgsShowsUsage() {
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, newManager(), messages);
        PlayerMock player = server.addPlayer();
        cmd.onCommand(player, command(), "auctionhologram", new String[0]);
        assertTrue(player.nextMessage().contains("Usage: /auctionhologram"));
    }

    @Test
    void unknownTypeShowsUnknownTypeAndTypeList() {
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, newManager(), messages);
        PlayerMock player = server.addPlayer();
        cmd.onCommand(player, command(), "auctionhologram", new String[]{"bogus"});
        String first = player.nextMessage();
        assertTrue(first.contains("Unknown hologram type 'bogus'"), "got: " + first);
        assertTrue(player.nextMessage().contains("Available hologram types:"));
    }

    @Test
    void clearDelegatesToHandler() {
        AuctionHologramManager manager = newManager();
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, manager, messages);
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        HologramDisplay display = platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));
        platform.setMarker(display, MARKER_KEY, "ACTIVE_LISTINGS");

        cmd.onCommand(player, command(), "auctionhologram", new String[]{"clear"});
        assertTrue(player.nextMessage().contains("Removed"));
        assertEquals(0, manager.findNearby(origin, 10.0D, false).size());
    }

    @Test
    void listDelegatesToHandler() {
        AuctionHologramManager manager = newManager();
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, manager, messages);
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        HologramDisplay display = platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));
        platform.setMarker(display, MARKER_KEY, "ACTIVE_LISTINGS");

        cmd.onCommand(player, command(), "auctionhologram", new String[]{"list"});
        assertTrue(player.nextMessage().contains("Nearby auction holograms"));
        assertTrue(player.nextMessage().contains("active_listings"));
    }

    @Test
    void clearallDelegatesToHandlerTwoStep() {
        AuctionHologramManager manager = newManager();
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, manager, messages);
        PlayerMock player = server.addPlayer();
        Location origin = player.getEyeLocation();
        HologramDisplay display = platform.spawn(origin.clone().add(1.0D, 0.0D, 0.0D));
        platform.setMarker(display, MARKER_KEY, "ACTIVE_LISTINGS");

        cmd.onCommand(player, command(), "auctionhologram", new String[]{"clearall"});
        assertTrue(player.nextMessage().contains("within"));
        cmd.onCommand(player, command(), "auctionhologram", new String[]{"clearall"});
        assertTrue(player.nextMessage().contains("Removed"));
        assertEquals(0, manager.findNearby(origin, 10.0D, false).size());
    }

    @Test
    void placementSuccessReportsCoordinates() {
        AuctionHologramManager manager = newManager();
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, manager, messages);
        PlayerMock player = server.addPlayer();
        Location origin = player.getLocation();

        cmd.onCommand(player, command(), "auctionhologram", new String[]{"listings"});
        String message = player.nextMessage();
        assertTrue(message.contains("Placed"), "got: " + message);
        assertTrue(message.contains(player.getWorld().getName()), "got: " + message);
        assertFalse(manager.findNearby(origin, 8.0D, false).isEmpty());
    }

    @Test
    void placementFailedReportsFailure() {
        AuctionHologramManager manager = mock(AuctionHologramManager.class);
        when(manager.ensureHologram(any(Location.class), any(AuctionHologramType.class))).thenReturn(null);
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, manager, messages);
        PlayerMock player = server.addPlayer();

        cmd.onCommand(player, command(), "auctionhologram", new String[]{"listings"});
        assertTrue(player.nextMessage().contains("Failed to place the auction hologram"));
    }

    @Test
    void translateConvertsAmpersandToColorCode() {
        String translated = AuctionHologramCommand.translate("&cOnly players may manage");
        assertTrue(translated.contains("§c"));
        assertTrue(translated.contains("Only players may manage"));
    }

    @Test
    void translateReturnsNullForNullInput() {
        assertEquals(null, AuctionHologramCommand.translate(null));
    }

    @Test
    void resolvePlacementLocationFallsBackToPlayerWhenNoTarget() {
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, newManager(), messages);
        PlayerMock player = server.addPlayer();
        Location placement = AuctionHologramCommand.resolvePlacementLocation(player);
        assertNotNull(placement);
        assertEquals(player.getLocation().getY() + 1.0D, placement.getY(), 0.001D);
    }

    @Test
    void tabCompleteAtRootListsSubcommandsAndTypes() {
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, newManager(), messages);
        PlayerMock player = server.addPlayer();
        List<String> suggestions = cmd.onTabComplete(player, command(), "auctionhologram", new String[0]);
        assertTrue(suggestions.contains("list"));
        assertTrue(suggestions.contains("clear"));
        assertTrue(suggestions.contains("clearall"));
        assertTrue(suggestions.contains("remove"));
        for (AuctionHologramType type : AuctionHologramType.values()) {
            assertTrue(suggestions.contains(type.name().toLowerCase(Locale.ENGLISH)), "missing type " + type);
        }
    }

    @Test
    void tabCompletePrefixFilters() {
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, newManager(), messages);
        PlayerMock player = server.addPlayer();
        List<String> suggestions = cmd.onTabComplete(player, command(), "auctionhologram", new String[]{"c"});
        assertTrue(suggestions.contains("clear"));
        assertTrue(suggestions.contains("clearall"));
        assertFalse(suggestions.contains("list"));
    }

    @Test
    void tabCompleteRadiusSuggestionsForRemovalSubcommands() {
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, newManager(), messages);
        PlayerMock player = server.addPlayer();
        List<String> suggestions = cmd.onTabComplete(player, command(), "auctionhologram",
                new String[]{"clear", "--"});
        assertTrue(suggestions.contains("--untagged"));
        assertFalse(suggestions.contains("10"));
        assertFalse(suggestions.contains("list"));
    }

    @Test
    void tabCompleteUntaggedFlagAtThirdPosition() {
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, newManager(), messages);
        PlayerMock player = server.addPlayer();
        List<String> suggestions = cmd.onTabComplete(player, command(), "auctionhologram",
                new String[]{"clear", "10", "--un"});
        assertTrue(suggestions.contains("--untagged"));
    }

    @Test
    void tabCompleteUnknownSubcommandYieldsNoSuggestions() {
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, newManager(), messages);
        PlayerMock player = server.addPlayer();
        List<String> suggestions = cmd.onTabComplete(player, command(), "auctionhologram",
                new String[]{"bogus", "1"});
        assertTrue(suggestions.isEmpty());
    }

    @Test
    void tabCompleteBeyondThirdPositionYieldsNoSuggestions() {
        AuctionHologramCommand cmd = new AuctionHologramCommand(plugin, newManager(), messages);
        PlayerMock player = server.addPlayer();
        List<String> suggestions = cmd.onTabComplete(player, command(), "auctionhologram",
                new String[]{"clear", "1", "2", "3"});
        assertTrue(suggestions.isEmpty());
    }
}
