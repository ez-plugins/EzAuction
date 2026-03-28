package com.skyblockexp.ezauction.integration;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates DiscordSRV integration and configuration loading from discord.yml.
 */
public final class DiscordIntegration {
    private final JavaPlugin plugin;
    private final boolean enabled;
    private final String channelId;
    private final Map<String, Boolean> events;
    private final Map<String, String> templates;
    private final boolean roleRequired;
    private final String requiredRoleId;
    private final String requiredRoleName;

    public DiscordIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
        Map<String, Boolean> ev = new HashMap<>();
        Map<String, String> tm = new HashMap<>();
        boolean en = false;
        String cid = null;
        try {
            java.io.File dataFolder = plugin.getDataFolder();
            org.bukkit.configuration.file.YamlConfiguration cfg = null;
            java.io.File discordFile = new java.io.File(dataFolder, "discord.yml");
            if (discordFile.exists()) {
                cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(discordFile);
            } else {
                java.io.InputStream stream = plugin.getResource("discord.yml");
                if (stream != null) {
                    cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(stream));
                }
            }
            if (cfg != null) {
                en = cfg.getBoolean("enabled", false);
                cid = cfg.getString("channel-id", null);
                org.bukkit.configuration.ConfigurationSection evsec = cfg.getConfigurationSection("events");
                if (evsec != null) {
                    for (String k : evsec.getKeys(false)) {
                        ev.put(k, evsec.getBoolean(k, false));
                    }
                }
                org.bukkit.configuration.ConfigurationSection mf = cfg.getConfigurationSection("message-format");
                if (mf != null) {
                    for (String k : mf.getKeys(false)) {
                        tm.put(k, mf.getString(k, ""));
                    }
                }
                roleRequired = cfg.getBoolean("role-required", false);
                requiredRoleId = cfg.getString("required-role-id", null);
                requiredRoleName = cfg.getString("required-role-name", null);
            } else {
                roleRequired = false;
                requiredRoleId = null;
                requiredRoleName = null;
            }
        } catch (Throwable ex) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to load discord.yml for DiscordSRV integration.", ex);
        }
        this.enabled = en;
        this.channelId = cid;
        this.events = java.util.Collections.unmodifiableMap(ev);
        this.templates = java.util.Collections.unmodifiableMap(tm);
        this.roleRequired = roleRequired;
        this.requiredRoleId = requiredRoleId;
        this.requiredRoleName = requiredRoleName;
    }

    public boolean isEnabled() {
        return enabled && channelId != null && !channelId.isBlank();
    }

    public boolean isEventEnabled(String event) {
        return events.getOrDefault(event, false);
    }

    public String getTemplate(String event) {
        return templates.get(event);
    }

    public String getChannelId() { return channelId; }

    public boolean isRoleRequired() { return roleRequired; }
    public String getRequiredRoleId() { return requiredRoleId; }
    public String getRequiredRoleName() { return requiredRoleName; }

    /**
     * Returns true if the player (by UUID) is linked to a Discord account which has the required role.
     * If role requirement is disabled, returns true.
     * Falls back to allowing when DiscordSRV is not present or on reflection errors.
     */
    public boolean isPlayerAllowed(java.util.UUID playerUuid) {
        if (!roleRequired) return true;
        if (playerUuid == null) return false;
        try {
            Class<?> apiClass = Class.forName("com.discordsrv.api.DiscordSRV");
            java.lang.reflect.Method getPlugin = apiClass.getMethod("getPlugin");
            Object apiPlugin = getPlugin.invoke(null);
            if (apiPlugin == null) return true;
            Object accountLinkManager = null;
            try {
                accountLinkManager = apiPlugin.getClass().getMethod("getAccountLinkManager").invoke(apiPlugin);
            } catch (Throwable ignored) {}
            String discordId = null;
            if (accountLinkManager != null) {
                try {
                    java.lang.reflect.Method m = accountLinkManager.getClass().getMethod("getDiscordId", java.util.UUID.class);
                    Object o = m.invoke(accountLinkManager, playerUuid);
                    if (o != null) discordId = String.valueOf(o);
                } catch (Throwable ex) {
                    try {
                        java.lang.reflect.Method m2 = accountLinkManager.getClass().getMethod("getDiscordId", String.class);
                        Object o2 = m2.invoke(accountLinkManager, playerUuid.toString());
                        if (o2 != null) discordId = String.valueOf(o2);
                    } catch (Throwable ignored) {}
                }
            }
            if (discordId == null) return false;
            // Obtain JDA
            Object jda = null;
            try {
                java.lang.reflect.Method getJda = apiPlugin.getClass().getMethod("getJDA");
                jda = getJda.invoke(apiPlugin);
            } catch (Throwable ignored) {}
            if (jda == null) return true;
            java.lang.reflect.Method getGuilds = jda.getClass().getMethod("getGuilds");
            java.util.List<?> guilds = (java.util.List<?>) getGuilds.invoke(jda);
            if (guilds == null) return true;
            for (Object guild : guilds) {
                try {
                    java.lang.reflect.Method getMemberById = guild.getClass().getMethod("getMemberById", java.lang.String.class);
                    Object member = getMemberById.invoke(guild, discordId);
                    if (member == null) continue;
                    java.util.List<?> roles = (java.util.List<?>) member.getClass().getMethod("getRoles").invoke(member);
                    if (roles == null) continue;
                    for (Object role : roles) {
                        try {
                            String rid = (String) role.getClass().getMethod("getId").invoke(role);
                            String rname = (String) role.getClass().getMethod("getName").invoke(role);
                            if (requiredRoleId != null && !requiredRoleId.isBlank() && requiredRoleId.equals(rid)) return true;
                            if (requiredRoleName != null && !requiredRoleName.isBlank() && requiredRoleName.equalsIgnoreCase(rname)) return true;
                        } catch (Throwable ignored) {}
                    }
                } catch (Throwable ignored) {}
            }
            return false;
        } catch (ClassNotFoundException cnf) {
            // DiscordSRV not present - allow by default
            return true;
        } catch (Throwable ex) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to check Discord role for player.", ex);
            return true;
        }
    }

    public void sendMessageIfAllowed(java.util.UUID playerUuid, String message) {
        if (!isPlayerAllowed(playerUuid)) return;
        sendMessage(message);
    }

    public void sendMessage(String message) {
        if (!isEnabled() || message == null || message.isBlank()) return;
        try {
            org.bukkit.plugin.Plugin p = plugin.getServer().getPluginManager().getPlugin("DiscordSRV");
            if (p == null) return;
            // Try common API access patterns
            try {
                Class<?> apiClass = Class.forName("com.discordsrv.api.DiscordSRV");
                java.lang.reflect.Method getPlugin = apiClass.getMethod("getPlugin");
                Object apiPlugin = getPlugin.invoke(null);
                if (apiPlugin != null) {
                    try {
                        java.lang.reflect.Method getJda = apiPlugin.getClass().getMethod("getJDA");
                        Object jda = getJda.invoke(apiPlugin);
                        if (jda != null) {
                            java.lang.reflect.Method getTextChannelById = jda.getClass().getMethod("getTextChannelById", java.lang.String.class);
                            Object channel = getTextChannelById.invoke(jda, channelId);
                            if (channel != null) {
                                java.lang.reflect.Method sendMessage = channel.getClass().getMethod("sendMessage", CharSequence.class);
                                Object action = sendMessage.invoke(channel, message);
                                try { java.lang.reflect.Method queue = action.getClass().getMethod("queue"); queue.invoke(action); } catch (NoSuchMethodException ignored) {}
                                return;
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (ClassNotFoundException ignored) {}
            // Fallback: try to find a getJDA method on the plugin instance
            try {
                java.lang.reflect.Method getJda = p.getClass().getMethod("getJDA");
                Object jda = getJda.invoke(p);
                if (jda != null) {
                    java.lang.reflect.Method getTextChannelById = jda.getClass().getMethod("getTextChannelById", java.lang.String.class);
                    Object channel = getTextChannelById.invoke(jda, channelId);
                    if (channel != null) {
                        java.lang.reflect.Method sendMessage = channel.getClass().getMethod("sendMessage", CharSequence.class);
                        Object action = sendMessage.invoke(channel, message);
                        try { java.lang.reflect.Method queue = action.getClass().getMethod("queue"); queue.invoke(action); } catch (NoSuchMethodException ignored) {}
                        return;
                    }
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ex) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to send DiscordSRV message.", ex);
        }
    }
}
