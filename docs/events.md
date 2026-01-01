
# EzAuction Event Reference

This document provides a comprehensive, professional reference for all public events fired by the EzAuction plugin. It includes class-level documentation, method signatures, and code samples for plugin developers.

---

## Table of Contents
- [Overview](#overview)
- [Event Class Reference](#event-class-reference)
  - [AuctionListingEvent](#auctionlistingevent)
  - [AuctionListingCreateEvent](#auctionlistingcreateevent)
  - [AuctionListingSellEvent](#auctionlistingsellevent)
  - [AuctionListingSoldEvent](#auctionlistingsoldevent)
  - [AuctionOrderCreateEvent](#auctionordercreateevent)
  - [AuctionOrderFulfillEvent](#auctionorderfulfillevent)
- [Listener Registration](#listener-registration)
- [Code Samples](#code-samples)

---

## Overview
EzAuction fires Bukkit events for all major auction actions. These events allow plugin developers to extend, monitor, or restrict auction activity in a type-safe, event-driven manner.

---

## Event Class Reference

### AuctionListingEvent (abstract)
**Package:** `com.skyblockexp.ezauction.event`

Base class for all auction listing events. Exposes the `AuctionListing` object and Bukkit event handler methods.

**Fields:**
- `protected final AuctionListing listing`

**Methods:**
```java
public AuctionListing getListing()
public HandlerList getHandlers()
public static HandlerList getHandlerList()
```

---

### AuctionListingCreateEvent
**Extends:** `AuctionListingEvent`  
**Implements:** `Cancellable`

Fired when a player attempts to create a new auction listing. This event is cancellable and allows modification of the item, price, and player before the listing is finalized.

**Constructors:**
```java
public AuctionListingCreateEvent(Player player, ItemStack item, double price)
public AuctionListingCreateEvent(AuctionListing listing, Player player, ItemStack item, double price)
```

**Methods:**
```java
public Player getPlayer()
public void setPlayer(Player player)
public ItemStack getItem()
public void setItem(ItemStack item)
public double getPrice()
public void setPrice(double price)
public boolean isCancelled()
public void setCancelled(boolean cancel)
```

---

### AuctionListingSellEvent
**Extends:** `AuctionListingEvent`  
**Implements:** `Cancellable`

Fired before an auction listing is sold. This event is cancellable, allowing plugins to prevent a sale.

**Constructor:**
```java
public AuctionListingSellEvent(AuctionListing listing)
```

**Methods:**
```java
public boolean isCancelled()
public void setCancelled(boolean cancel)
```

---

### AuctionListingSoldEvent
**Extends:** `AuctionListingEvent`

Fired after an auction listing is successfully sold. This event is not cancellable.

**Constructor:**
```java
public AuctionListingSoldEvent(AuctionListing listing)
```

---

### AuctionOrderCreateEvent
**Extends:** `Event`  
**Implements:** `Cancellable`

Fired when a buy order is created. (See API docs for full class reference.)

---

### AuctionOrderFulfillEvent
**Extends:** `Event`

Fired when a buy order is fulfilled. (See API docs for full class reference.)

---

## Listener Registration
Register your event listeners as standard Bukkit listeners:
```java
@EventHandler
public void onAuctionListingCreate(AuctionListingCreateEvent event) {
    if (event.getPrice() < 1.0) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("Minimum price is 1.0!");
    }
}
```

---

## Code Samples

### Cancelling a Listing Creation
```java
@EventHandler
public void onAuctionListingCreate(AuctionListingCreateEvent event) {
    if (event.getItem().getType() == Material.DIRT) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("You cannot auction dirt!");
    }
}
```

### Modifying Listing Details
```java
@EventHandler
public void onAuctionListingCreate(AuctionListingCreateEvent event) {
    // Set a minimum price
    if (event.getPrice() < 10.0) {
        event.setPrice(10.0);
        event.getPlayer().sendMessage("Price raised to minimum: 10.0");
    }
    // Change the item if needed
    // event.setItem(new ItemStack(Material.DIAMOND));
}
```

---
For more event details, see the [API docs](api.md) or contact the maintainers.
