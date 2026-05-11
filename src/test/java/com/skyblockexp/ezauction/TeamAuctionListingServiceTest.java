package com.skyblockexp.ezauction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezauction.integration.TeamsIntegration;
import com.skyblockexp.ezauction.service.AuctionQueryService;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeamAuctionListingServiceTest {

    private TeamsIntegration teamsIntegration;

    @BeforeEach
    void setUp() {
        teamsIntegration = mock(TeamsIntegration.class);
    }

    private AuctionQueryService buildQueryService(Map<String, AuctionListing> listings) {
        AuctionConfiguration configuration = AuctionManagerTestUtils.mockAuctionConfiguration();
        when(configuration.teamAuctionsEnabled()).thenReturn(true);
        return new AuctionQueryService(listings, new HashMap<>(), null, configuration, teamsIntegration);
    }

    private AuctionQueryService buildQueryServiceTeamDisabled(Map<String, AuctionListing> listings) {
        AuctionConfiguration configuration = AuctionManagerTestUtils.mockAuctionConfiguration();
        when(configuration.teamAuctionsEnabled()).thenReturn(false);
        return new AuctionQueryService(listings, new HashMap<>(), null, configuration, teamsIntegration);
    }

    @Test
    void listActiveListingsFiltersOutTeamListings() {
        UUID sellerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        long expiry = System.currentTimeMillis() + Duration.ofHours(1).toMillis();
        ItemStack item = new ItemStack(Material.DIAMOND, 1);

        AuctionListing globalListing = new AuctionListing("global-1", sellerId, 50.0D, expiry, item, 0.0D, null);
        AuctionListing teamListing = new AuctionListing("team-1", sellerId, 75.0D, expiry, item, 0.0D, teamId);

        Map<String, AuctionListing> listings = new HashMap<>();
        listings.put(globalListing.id(), globalListing);
        listings.put(teamListing.id(), teamListing);

        when(teamsIntegration.isAvailable()).thenReturn(true);
        AuctionQueryService qs = buildQueryService(listings);

        List<AuctionListing> active = qs.listActiveListings();

        assertTrue(active.stream().anyMatch(l -> l.id().equals("global-1")),
                "Global listing should be visible in the global view");
        assertFalse(active.stream().anyMatch(l -> l.id().equals("team-1")),
                "Team listing should be filtered out of the global view");
    }

    @Test
    void listActiveTeamListingsReturnsOnlyMatchingTeam() {
        UUID sellerId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID otherTeamId = UUID.randomUUID();
        long expiry = System.currentTimeMillis() + Duration.ofHours(1).toMillis();
        ItemStack item = new ItemStack(Material.DIAMOND, 1);

        AuctionListing ownTeamListing = new AuctionListing("team-own", sellerId, 50.0D, expiry, item, 0.0D, teamId);
        AuctionListing otherTeamListing = new AuctionListing("team-other", sellerId, 75.0D, expiry, item, 0.0D, otherTeamId);
        AuctionListing globalListing = new AuctionListing("global-1", sellerId, 10.0D, expiry, item, 0.0D, null);

        Map<String, AuctionListing> listings = new HashMap<>();
        listings.put(ownTeamListing.id(), ownTeamListing);
        listings.put(otherTeamListing.id(), otherTeamListing);
        listings.put(globalListing.id(), globalListing);

        when(teamsIntegration.isAvailable()).thenReturn(true);
        when(teamsIntegration.getTeamId(viewerId)).thenReturn(Optional.of(teamId));

        AuctionQueryService qs = buildQueryService(listings);
        List<AuctionListing> teamActive = qs.listActiveTeamListings(viewerId);

        assertTrue(teamActive.stream().anyMatch(l -> l.id().equals("team-own")),
                "Own-team listing should appear in team view");
        assertFalse(teamActive.stream().anyMatch(l -> l.id().equals("team-other")),
                "Other-team listing should not appear in team view");
        assertFalse(teamActive.stream().anyMatch(l -> l.id().equals("global-1")),
                "Global listing should not appear in team view");
    }

    @Test
    void listActiveTeamListingsReturnsEmptyWhenTeamsUnavailable() {
        UUID viewerId = UUID.randomUUID();
        when(teamsIntegration.isAvailable()).thenReturn(false);
        AuctionQueryService qs = buildQueryService(new HashMap<>());
        List<AuctionListing> result = qs.listActiveTeamListings(viewerId);
        assertTrue(result.isEmpty(), "Should return empty list when TeamsAPI is unavailable");
    }

    @Test
    void listActiveTeamListingsReturnsEmptyWhenTeamAuctionsDisabledInConfig() {
        UUID viewerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        long expiry = System.currentTimeMillis() + Duration.ofHours(1).toMillis();
        ItemStack item = new ItemStack(Material.DIAMOND, 1);
        AuctionListing teamListing = new AuctionListing("team-cfg", UUID.randomUUID(), 50.0D, expiry, item, 0.0D, teamId);
        Map<String, AuctionListing> listings = new HashMap<>();
        listings.put(teamListing.id(), teamListing);

        when(teamsIntegration.isAvailable()).thenReturn(true);
        when(teamsIntegration.getTeamId(viewerId)).thenReturn(Optional.of(teamId));
        AuctionQueryService qs = buildQueryServiceTeamDisabled(listings);

        List<AuctionListing> result = qs.listActiveTeamListings(viewerId);
        assertTrue(result.isEmpty(), "Should return empty list when team-auctions.enabled=false in config");
    }

    @Test
    void listActiveTeamListingsReturnsEmptyWhenViewerNotInTeam() {
        UUID viewerId = UUID.randomUUID();
        when(teamsIntegration.isAvailable()).thenReturn(true);
        when(teamsIntegration.getTeamId(viewerId)).thenReturn(Optional.empty());
        AuctionQueryService qs = buildQueryService(new HashMap<>());
        List<AuctionListing> result = qs.listActiveTeamListings(viewerId);
        assertTrue(result.isEmpty(), "Should return empty list when viewer is not in a team");
    }

    @Test
    void findListingByIdSearchesAllListings() {
        UUID sellerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        long expiry = System.currentTimeMillis() + Duration.ofHours(1).toMillis();
        ItemStack item = new ItemStack(Material.DIAMOND, 1);

        AuctionListing teamListing = new AuctionListing("team-find", sellerId, 50.0D, expiry, item, 0.0D, teamId);
        Map<String, AuctionListing> listings = new HashMap<>();
        listings.put(teamListing.id(), teamListing);

        when(teamsIntegration.isAvailable()).thenReturn(true);
        AuctionQueryService qs = buildQueryService(listings);

        AuctionListing found = qs.findListingById("team-find");
        assertNotNull(found, "findListingById should return team listing regardless of scope");
    }

    @Test
    void findListingByIdReturnsNullForUnknownId() {
        when(teamsIntegration.isAvailable()).thenReturn(true);
        AuctionQueryService qs = buildQueryService(new HashMap<>());
        assertNull(qs.findListingById("nonexistent"));
    }

    @Test
    void auctionListingIsTeamListingReturnsTrueForTeamListing() {
        UUID teamId = UUID.randomUUID();
        AuctionListing teamListing = new AuctionListing("t", UUID.randomUUID(), 1.0D,
                System.currentTimeMillis() + 60_000L, new ItemStack(Material.STONE), 0.0D, teamId);
        assertTrue(teamListing.isTeamListing());
    }

    @Test
    void auctionListingIsTeamListingReturnsFalseForGlobalListing() {
        AuctionListing globalListing = new AuctionListing("g", UUID.randomUUID(), 1.0D,
                System.currentTimeMillis() + 60_000L, new ItemStack(Material.STONE), 0.0D, null);
        assertFalse(globalListing.isTeamListing());
    }

    @Test
    void auctionListingGlobalFactoryProducesNullTeamId() {
        AuctionListing listing = AuctionListing.global("g", UUID.randomUUID(), 1.0D,
                System.currentTimeMillis() + 60_000L, new ItemStack(Material.STONE), 0.0D);
        assertFalse(listing.isTeamListing());
    }
}
