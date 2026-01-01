package com.skyblockexp.ezauction;

import static org.mockito.Mockito.mock;
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezauction.api.AuctionListingLimitResolver;

public class AuctionManagerTestUtils {
    public static AuctionConfiguration mockAuctionConfiguration() {
        return mock(AuctionConfiguration.class);
    }
    public static AuctionListingLimitResolver mockAuctionListingLimitResolver() {
        return mock(AuctionListingLimitResolver.class);
    }
}
