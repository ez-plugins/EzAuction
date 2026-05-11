---
title: Events
nav_order: 3
parent: Reference
---

# Events

EzAuction fires Bukkit events for all major auction actions, allowing external plugins to react, modify, or cancel operations.

All events are in the `com.skyblockexp.ezauction.event` package.

## Event summary

| Event | Cancellable | Fired when |
|-------|-------------|-----------|
| `AuctionListingCreateEvent` | ✅ | A player attempts to create a new listing |
| `AuctionListingSellEvent` | ✅ | A listing is about to be sold |
| `AuctionListingSoldEvent` | ❌ | A listing has been successfully sold |
| `AuctionOrderCreateEvent` | ✅ | A buy order is created |
| `AuctionOrderFulfillEvent` | ❌ | A buy order is fulfilled |

---

## `AuctionListingCreateEvent`

Fired before a new auction listing is finalized. Allows modification of item, price, and player.

```java
public AuctionListingCreateEvent(Player player, ItemStack item, double price)
public AuctionListingCreateEvent(AuctionListing listing, Player player, ItemStack item, double price)

// Getters / setters
Player getPlayer() / void setPlayer(Player player)
ItemStack getItem() / void setItem(ItemStack item)
double getPrice()  / void setPrice(double price)
boolean isCancelled() / void setCancelled(boolean cancel)
```

---

## `AuctionListingSellEvent`

Fired before a listing is sold. Cancel to prevent the purchase.

```java
public AuctionListingSellEvent(AuctionListing listing)

boolean isCancelled() / void setCancelled(boolean cancel)
```

---

## `AuctionListingSoldEvent`

Fired after a listing is successfully sold. Not cancellable.

```java
public AuctionListingSoldEvent(AuctionListing listing)
```

---

## `AuctionOrderCreateEvent`

Fired when a buy order is created. Cancellable.

---

## `AuctionOrderFulfillEvent`

Fired when a buy order is fulfilled. Not cancellable.

---

## Listener registration

```java
@EventHandler
public void onAuctionCreate(AuctionListingCreateEvent event) {
    // Block listings under 1 coin
    if (event.getPrice() < 1.0) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("Minimum price is 1 coin.");
    }
}
```

Register the listener in your plugin's `onEnable()`:

```java
getServer().getPluginManager().registerEvents(new MyAuctionListener(), this);
```

## Code samples

### Block specific items from being listed

```java
@EventHandler
public void onAuctionCreate(AuctionListingCreateEvent event) {
    if (event.getItem().getType() == Material.DIRT) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("Dirt cannot be auctioned.");
    }
}
```

### Enforce a minimum price

```java
@EventHandler
public void onAuctionCreate(AuctionListingCreateEvent event) {
    if (event.getPrice() < 10.0) {
        event.setPrice(10.0);
    }
}
```

---

## Related

- [Developer API](api.md) — programmatic access
- [CONTRIBUTING.md](https://github.com/ez-plugins/EzAuction/blob/main/CONTRIBUTING.md) — adding new events
