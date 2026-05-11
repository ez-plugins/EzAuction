package com.skyblockexp.ezauction.integration;

import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.model.Team;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;

/**
 * Null-safe facade for the optional TeamsAPI soft-dependency.
 *
 * <p>All methods guard against {@code TeamsAPI} being absent on the classpath or not yet
 * initialised. Any {@link Throwable} thrown by the underlying API is caught and treated as
 * "not available" so the plugin degrades gracefully.</p>
 */
public class TeamsIntegration {

    private final JavaPlugin plugin;

    public TeamsIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns {@code true} if TeamsAPI is present on the classpath and its service is available.
     */
    public boolean isAvailable() {
        try {
            return TeamsAPI.isAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Returns the {@link Team} the given player belongs to, or an empty {@link Optional} if the
     * player has no team or TeamsAPI is unavailable.
     *
     * @param playerUUID the player's UUID
     * @return an {@link Optional} containing the player's team, or empty
     */
    public Optional<Team> getPlayerTeam(UUID playerUUID) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            return TeamsAPI.getService().getPlayerTeam(playerUUID);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    /**
     * Returns the UUID of the team the given player belongs to, or an empty {@link Optional}.
     *
     * @param playerUUID the player's UUID
     * @return an {@link Optional} containing the team UUID, or empty
     */
    public Optional<UUID> getTeamId(UUID playerUUID) {
        return getPlayerTeam(playerUUID).map(Team::getId);
    }

    /**
     * Returns {@code true} if both players are members of the same team.
     *
     * @param player1 UUID of the first player
     * @param player2 UUID of the second player
     * @return {@code true} if both share a team
     */
    public boolean isSameTeam(UUID player1, UUID player2) {
        if (!isAvailable()) {
            return false;
        }
        try {
            Optional<Team> team = TeamsAPI.getService().getPlayerTeam(player1);
            return team.map(t -> t.isMember(player2)).orElse(false);
        } catch (Throwable t) {
            return false;
        }
    }
}
