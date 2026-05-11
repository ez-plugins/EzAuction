---
title: Live Auctions
nav_order: 5
parent: Features
---

# Live Auctions

Live auctions are a real-time bidding queue. Items are presented one at a time and players submit bids within a time window.

## Accessing the live auction

```
/liveauction
```

Or open via the **Live** button in the main auction browser.

Requires `ezauction.auction.live`.

## Creating a live listing

```
/auction sell live
```

Or open the sell menu normally and select the **Live Auction** target.

Requires `ezauction.auction.live.sell`.

## How it works

1. A seller submits an item into the live queue via the sell menu.
2. The item becomes active when it reaches the front of the queue.
3. Players open `/liveauction` to view and bid on the current item.
4. At expiry or when bidding ends, the item goes to the highest bidder.
5. Unsold items are returned to the seller's claim queue.

## Configuration

Live auction settings are in `auction.yml` under the `live-auctions` section:

```yaml
live-auctions:
  enabled: true
  default-duration: 5m
  max-queue-size: 20
```

## Related

- [Sell Menu](sell-menu.md) - creating listings
- [Permissions](../reference/permissions.md) - `ezauction.auction.live` and `ezauction.auction.live.sell`
