package com.skyblockexp.ezauction.command;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class DiscordCommandTest {

    @Test
    void permissionDeniedSendsMessage() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezauction.discord")).thenReturn(false);
        DiscordCommand cmd = new DiscordCommand(null);

        boolean handled = cmd.onCommand(sender, mock(Command.class), "discord", new String[]{"test"});
        assertTrue(handled);
        verify(sender).sendMessage("You do not have permission to use this command.");
    }

    @Test
    void testSubWhenIntegrationDisabledNotifiesSender() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezauction.discord")).thenReturn(true);

        com.skyblockexp.ezauction.integration.DiscordIntegration integration = mock(com.skyblockexp.ezauction.integration.DiscordIntegration.class);
        when(integration.isEnabled()).thenReturn(false);

        DiscordCommand cmd = new DiscordCommand(integration);

        boolean handled = cmd.onCommand(sender, mock(Command.class), "discord", new String[]{"test"});
        assertTrue(handled);
        verify(sender).sendMessage("Discord integration is not enabled or not configured.");
    }

    @Test
    void testSubSendsMessageViaIntegration() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezauction.discord")).thenReturn(true);

        com.skyblockexp.ezauction.integration.DiscordIntegration integration = mock(com.skyblockexp.ezauction.integration.DiscordIntegration.class);
        when(integration.isEnabled()).thenReturn(true);

        DiscordCommand cmd = new DiscordCommand(integration);

        boolean handled = cmd.onCommand(sender, mock(Command.class), "discord", new String[]{"test", "hello", "world"});
        assertTrue(handled);

        verify(integration).sendMessage("hello world");
        verify(sender).sendMessage(startsWith("Sent test message to Discord"));
    }
}
