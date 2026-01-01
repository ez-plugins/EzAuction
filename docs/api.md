# EzAuction Plugin API Documentation

This document provides a comprehensive reference for developers integrating with the EzAuction plugin. All public classes, methods, and extension points are documented with usage examples.

## Table of Contents
- [Overview](#overview)
- [Main API Classes](#main-api-classes)
- [Key Methods & Usage](#key-methods--usage)
- [Code Samples](#code-samples)

---

## Overview
EzAuction exposes a robust API for interacting with auctions, listings, and events. Use these APIs to create, query, and manage auctions programmatically.

## Main API Classes
- `AuctionManager` — Central access point for auction operations.
- `AuctionListingService` — Handles creation, cancellation, and purchase of listings.
- `AuctionOrderService` — Manages buy orders.
- `AuctionReturnService` — Handles item returns.
- `AuctionQueryService` — Query and statistics for listings/orders.

## Key Methods & Usage

### Creating a Listing
```java
AuctionOperationResult result = auctionManager.createListing(player, itemStack, price, duration);
if (result.success()) {
    // Listing created
} else {
    player.sendMessage(result.message());
}
```

### Querying Listings
```java
List<AuctionListing> listings = auctionManager.listActiveListings();
for (AuctionListing listing : listings) {
    // Process listing
}
```

### Cancelling a Listing
```java
AuctionOperationResult result = auctionManager.cancelListing(player.getUniqueId(), listingId);
```

## Code Samples

### Registering a Custom Listener
See [docs/events.md](events.md) for event listener code samples.

---
For more details, see the full Javadoc or contact the maintainers.
