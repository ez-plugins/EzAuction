---
title: Developer API
nav_order: 4
parent: Reference
---

# Developer API

EzAuction exposes a stable public API for plugin developers to create, query, cancel, and monitor auction listings and buy orders.

## Dependency

Add EzAuction as a soft (or hard) dependency in `plugin.yml`:

```yaml
softdepend: [EzAuction]
```

Then obtain the `AuctionManager` instance via the service registry or by casting the plugin:

```java
Plugin plugin = Bukkit.getPluginManager().getPlugin("EzAuction");
if (plugin instanceof EzAuctionPlugin ezAuction) {
    AuctionManager manager = ezAuction.getAuctionManager();
}
```

## Main API classes

| Class | Package | Role |
|-------|---------|------|
| `AuctionManager` | `com.skyblockexp.ezauction` | Central access point |
| `AuctionListingService` | `com.skyblockexp.ezauction.service` | Create, cancel, and purchase listings |
| `AuctionOrderService` | `com.skyblockexp.ezauction.service` | Manage buy orders |
| `AuctionReturnService` | `com.skyblockexp.ezauction.claim` | Handle item returns |
| `AuctionQueryService` | `com.skyblockexp.ezauction.service` | Query listings and statistics |

## Common operations

### Create a listing

```java
AuctionOperationResult result = manager.createListing(player, itemStack, price, duration);
if (result.success()) {
    // listing created
} else {
    player.sendMessage(result.message());
}
```

### Query active listings

```java
List<AuctionListing> listings = manager.listActiveListings();
for (AuctionListing listing : listings) {
    String seller = listing.getSellerName();
    double price  = listing.getPrice();
}
```

### Cancel a listing

```java
AuctionOperationResult result = manager.cancelListing(player.getUniqueId(), listingId);
```

### Listen for events

See the [Events reference](events.md) for all cancellable and observable events.

## `AuctionListing` fields

| Method | Returns | Description |
|--------|---------|-------------|
| `getId()` | `UUID` | Unique listing identifier |
| `getSellerUUID()` | `UUID` | Seller's UUID |
| `getSellerName()` | `String` | Seller's display name |
| `getItem()` | `ItemStack` | The listed item |
| `getPrice()` | `double` | Listing price |
| `getExpiry()` | `Instant` | Expiry timestamp |
| `isTeamListing()` | `boolean` | Whether this is a team-scoped listing |
| `getTeamId()` | `String` or `null` | Team identifier (if team listing) |

## `AuctionOperationResult`

```java
boolean success()   // true if operation succeeded
String message()    // player-facing result message (MiniMessage)
```

## Custom listing-limit resolver

Implement `ListingLimitResolver` in the `com.skyblockexp.ezauction.api` package to supply per-player limits based on custom logic (e.g. rank, playtime):

```java
public class MyLimitResolver implements ListingLimitResolver {
    @Override
    public int getLimit(Player player) {
        return player.hasPermission("myserver.vip") ? 20 : 5;
    }
}
```

Register it via `EzAuctionPlugin#setListingLimitResolver(resolver)`.

## Related

- [Events](events.md) — all custom Bukkit events
- [CONTRIBUTING.md](https://github.com/ez-plugins/EzAuction/blob/main/CONTRIBUTING.md)
