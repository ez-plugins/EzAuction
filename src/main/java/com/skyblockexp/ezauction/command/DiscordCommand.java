package com.skyblockexp.ezauction.command;

import com.skyblockexp.ezauction.bootstrap.PluginRegistry;
import com.skyblockexp.ezauction.integration.DiscordIntegration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiscordCommand implements CommandExecutor, TabCompleter {
    private final DiscordIntegration integration;

    public DiscordCommand(DiscordIntegration integration) {
        this.integration = integration;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ezauction.discord")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /" + label + " test [message]");
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("test".equals(sub)) {
            if (integration == null || !integration.isEnabled()) {
                sender.sendMessage("Discord integration is not enabled or not configured.");
                return true;
            }
            String msg = "EzAuction: Discord test from " + sender.getName();
            if (args.length > 1) {
                msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            }
            integration.sendMessage(msg);
            sender.sendMessage("Sent test message to Discord (if configured).\nMessage: " + msg);
            return true;
        }
        if ("set".equals(sub) && args.length >= 3 && "channel".equalsIgnoreCase(args[1])) {
            // set channel <id>
            String id = args[2];
            try {
                org.bukkit.configuration.file.YamlConfiguration cfg = loadDiscordConfig();
                cfg.set("channel-id", id);
                cfg.set("enabled", true);
                cfg.save(new java.io.File(((org.bukkit.plugin.java.JavaPlugin)org.bukkit.Bukkit.getPluginManager().getPlugin("EzAuction")).getDataFolder(), "discord.yml"));
                // reload integration in registry
                PluginRegistry reg = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry();
                if (reg != null) reg.reloadDiscordIntegrations();
                sender.sendMessage("Discord channel set to " + id + " and enabled.");
            } catch (Exception ex) {
                sender.sendMessage("Failed to write discord.yml: " + ex.getMessage());
            }
            return true;
        }
        if ("enable".equals(sub) || "disable".equals(sub)) {
            boolean enable = "enable".equals(sub);
            try {
                org.bukkit.configuration.file.YamlConfiguration cfg = loadDiscordConfig();
                cfg.set("enabled", enable);
                cfg.save(new java.io.File(((org.bukkit.plugin.java.JavaPlugin)org.bukkit.Bukkit.getPluginManager().getPlugin("EzAuction")).getDataFolder(), "discord.yml"));
                PluginRegistry reg = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry();
                if (reg != null) reg.reloadDiscordIntegrations();
                sender.sendMessage("Discord integration " + (enable ? "enabled" : "disabled") + ".");
            } catch (Exception ex) {
                sender.sendMessage("Failed to update discord.yml: " + ex.getMessage());
            }
            return true;
        }
        if ("reload".equals(sub)) {
            PluginRegistry reg = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry();
            if (reg != null) reg.reloadDiscordIntegrations();
            sender.sendMessage("Reloaded Discord configuration (DiscordSRV + webhook).");
            return true;
        }
        if ("role".equals(sub)) {
            // /auctiondiscord role show
            // /auctiondiscord role set id <id>
            // /auctiondiscord role set name <name>
            // /auctiondiscord role clear
            // /auctiondiscord role require <true|false>
            if (args.length == 1 || "show".equalsIgnoreCase(args[1])) {
                org.bukkit.configuration.file.YamlConfiguration cfg = loadDiscordConfig();
                boolean rr = cfg.getBoolean("role-required", false);
                String rid = cfg.getString("required-role-id", null);
                String rnm = cfg.getString("required-role-name", null);
                sender.sendMessage("Discord role requirement: " + rr + "\nRole ID: " + (rid == null ? "<not set>" : rid) + "\nRole name: " + (rnm == null ? "<not set>" : rnm));
                return true;
            }
            if (args.length >= 4 && "set".equalsIgnoreCase(args[1]) && "id".equalsIgnoreCase(args[2])) {
                String id = args[3];
                try {
                    org.bukkit.configuration.file.YamlConfiguration cfg = loadDiscordConfig();
                    cfg.set("required-role-id", id);
                    cfg.save(new java.io.File(((org.bukkit.plugin.java.JavaPlugin)org.bukkit.Bukkit.getPluginManager().getPlugin("EzAuction")).getDataFolder(), "discord.yml"));
                    PluginRegistry reg = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry();
                    if (reg != null) reg.reloadDiscordIntegrations();
                    sender.sendMessage("Set required role ID to " + id);
                } catch (Exception ex) {
                    sender.sendMessage("Failed to write discord.yml: " + ex.getMessage());
                }
                return true;
            }
            if (args.length >= 4 && "set".equalsIgnoreCase(args[1]) && "name".equalsIgnoreCase(args[2])) {
                String name = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                try {
                    org.bukkit.configuration.file.YamlConfiguration cfg = loadDiscordConfig();
                    cfg.set("required-role-name", name);
                    cfg.save(new java.io.File(((org.bukkit.plugin.java.JavaPlugin)org.bukkit.Bukkit.getPluginManager().getPlugin("EzAuction")).getDataFolder(), "discord.yml"));
                    PluginRegistry reg = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry();
                    if (reg != null) reg.reloadDiscordIntegrations();
                    sender.sendMessage("Set required role name to " + name);
                } catch (Exception ex) {
                    sender.sendMessage("Failed to write discord.yml: " + ex.getMessage());
                }
                return true;
            }
            if (args.length >= 2 && "clear".equalsIgnoreCase(args[1])) {
                try {
                    org.bukkit.configuration.file.YamlConfiguration cfg = loadDiscordConfig();
                    cfg.set("required-role-id", null);
                    cfg.set("required-role-name", null);
                    cfg.save(new java.io.File(((org.bukkit.plugin.java.JavaPlugin)org.bukkit.Bukkit.getPluginManager().getPlugin("EzAuction")).getDataFolder(), "discord.yml"));
                    PluginRegistry reg = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry();
                    if (reg != null) reg.reloadDiscordIntegrations();
                    sender.sendMessage("Cleared required role settings.");
                } catch (Exception ex) {
                    sender.sendMessage("Failed to write discord.yml: " + ex.getMessage());
                }
                return true;
            }
            if (args.length >= 3 && "require".equalsIgnoreCase(args[1])) {
                String v = args[2];
                boolean val = Boolean.parseBoolean(v);
                try {
                    org.bukkit.configuration.file.YamlConfiguration cfg = loadDiscordConfig();
                    cfg.set("role-required", val);
                    cfg.save(new java.io.File(((org.bukkit.plugin.java.JavaPlugin)org.bukkit.Bukkit.getPluginManager().getPlugin("EzAuction")).getDataFolder(), "discord.yml"));
                    PluginRegistry reg = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry();
                    if (reg != null) reg.reloadDiscordIntegrations();
                    sender.sendMessage("Set role-required to " + val);
                } catch (Exception ex) {
                    sender.sendMessage("Failed to write discord.yml: " + ex.getMessage());
                }
                return true;
            }
            sender.sendMessage("Role subcommands: show | set id <id> | set name <name> | clear | require <true|false>");
            return true;
        }
        if ("webhook".equals(sub)) {
            return handleWebhookSubcommand(sender, args);
        }
        sender.sendMessage("Unknown subcommand: " + sub + ". Use /auctiondiscord for help.");
        return true;
    }

    private boolean handleWebhookSubcommand(CommandSender sender, String[] args) {
        String wsub = args.length >= 2 ? args[1].toLowerCase() : "status";
        org.bukkit.plugin.java.JavaPlugin plugin =
                (org.bukkit.plugin.java.JavaPlugin) org.bukkit.Bukkit.getPluginManager().getPlugin("EzAuction");
        if (plugin == null) {
            sender.sendMessage("Could not locate EzAuction plugin instance.");
            return true;
        }
        switch (wsub) {
            case "status" -> {
                org.bukkit.configuration.file.YamlConfiguration cfg = loadDiscordConfig();
                boolean enabled = cfg.getBoolean("webhook.enabled", false);
                String url = cfg.getString("webhook.url", null);
                String maskedUrl = (url != null && url.length() > 10)
                        ? "..." + url.substring(url.length() - 10)
                        : (url != null ? "(set)" : "(not set)");
                sender.sendMessage("Webhook enabled: " + enabled + "\nWebhook URL: " + maskedUrl);
            }
            case "set" -> {
                if (args.length < 4 || !"url".equalsIgnoreCase(args[2])) {
                    sender.sendMessage("Usage: /auctiondiscord webhook set url <webhook-url>");
                    return true;
                }
                String url = args[3];
                if (!url.startsWith("https://discord.com/api/webhooks/")) {
                    sender.sendMessage("Invalid webhook URL. Must start with: https://discord.com/api/webhooks/");
                    return true;
                }
                try {
                    org.bukkit.configuration.file.YamlConfiguration cfg = loadDiscordConfig();
                    cfg.set("webhook.url", url);
                    cfg.set("webhook.enabled", true);
                    cfg.save(new java.io.File(plugin.getDataFolder(), "discord.yml"));
                    PluginRegistry reg = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry();
                    if (reg != null) reg.reloadDiscordIntegrations();
                    sender.sendMessage("Webhook URL configured and enabled.");
                } catch (Exception ex) {
                    sender.sendMessage("Failed to save discord.yml: " + ex.getMessage());
                }
            }
            case "enable", "disable" -> {
                boolean enable = "enable".equals(wsub);
                try {
                    org.bukkit.configuration.file.YamlConfiguration cfg = loadDiscordConfig();
                    cfg.set("webhook.enabled", enable);
                    cfg.save(new java.io.File(plugin.getDataFolder(), "discord.yml"));
                    PluginRegistry reg = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry();
                    if (reg != null) reg.reloadDiscordIntegrations();
                    sender.sendMessage("Discord webhook " + (enable ? "enabled" : "disabled") + ".");
                } catch (Exception ex) {
                    sender.sendMessage("Failed to update discord.yml: " + ex.getMessage());
                }
            }
            case "test" -> {
                com.skyblockexp.ezauction.integration.DiscordWebhookNotifier wh = null;
                PluginRegistry reg = com.skyblockexp.ezauction.EzAuctionPlugin.getStaticRegistry();
                if (reg != null) wh = reg.discordWebhookNotifier;
                if (wh == null || !wh.isEnabled()) {
                    sender.sendMessage("Webhook is not enabled or not configured.");
                    return true;
                }
                wh.sendTestMessage(sender.getName());
                sender.sendMessage("Test message dispatched to Discord webhook.");
            }
            default -> sender.sendMessage(
                    "Webhook subcommands: status | set url <url> | enable | disable | test");
        }
        return true;
    }

    private org.bukkit.configuration.file.YamlConfiguration loadDiscordConfig() {
        org.bukkit.plugin.Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("EzAuction");
        java.io.File f = new java.io.File(((org.bukkit.plugin.java.JavaPlugin)plugin).getDataFolder(), "discord.yml");
        if (f.exists()) return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
        java.io.InputStream stream = ((org.bukkit.plugin.java.JavaPlugin)plugin).getResource("discord.yml");
        if (stream != null) return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(stream));
        return new org.bukkit.configuration.file.YamlConfiguration();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("test", "set", "enable", "disable", "reload", "role", "webhook");
        }
        if (args.length == 2 && "role".equalsIgnoreCase(args[0])) {
            return List.of("show", "set", "clear", "require");
        }
        if (args.length == 3 && "role".equalsIgnoreCase(args[0]) && "set".equalsIgnoreCase(args[1])) {
            return List.of("id", "name");
        }
        if (args.length == 2 && "webhook".equalsIgnoreCase(args[0])) {
            return List.of("status", "set", "enable", "disable", "test");
        }
        if (args.length == 3 && "webhook".equalsIgnoreCase(args[0]) && "set".equalsIgnoreCase(args[1])) {
            return List.of("url");
        }
        return new ArrayList<>();
    }
}
