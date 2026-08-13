---
title: Auction Listings
nav_order: 6
parent: Features
---

# Auction Listings

Everything owners and players need to know about the lifecycle of a standard auction listing - from creating it, through fees and limits, to selling, cancelling, and reclaiming expired or returned items. For the live real-time bidding mode, see [Live Auctions](live-auctions.md). For purchase offers, see [Buy Orders](buy-orders.md).

## Creating a listing

Hold the item to sell, then open the [Sell Menu](sell-menu.md):

```
/auction sell
/auction sell <price>
/auction sell <price> <duration>
```

Players need `ezauction.auction.sell`. The menu lets the player set the price, quantity, and duration, then confirms the listing fee before finalising.

## Durations

Listings accept human-readable durations such as `12h`, `30m`, or `2d`. The maximum allowed duration is controlled by `listing-max-duration` (hours) in `auction.yml`. Durations longer than the limit are rejected.

## Fees, deposits, and limits

| Rule | Config key | Default | Effect |
|------|-----------|---------|--------|
| Listing fee | `listing-fee` | `"0"` | Charged when a listing is created. A flat amount (e.g. `100`) or a percentage of the price (e.g. `5%`). |
| Max duration | `listing-max-duration` | `72` | Maximum listing lifetime in hours. |
| Max active listings | `max-listings-per-player` | `5` | How many listings a player may have at once. Can be raised per-player via permission groups and the API. |
| Auto-expire | `auto-remove-expired` | `true` | Expired, unsold listings are automatically removed. |
| Currency | `currency-type` | `vault` | Economy backend: `vault`, `xp`, or a custom provider. |

The listing fee is taken up front. If the item sells, the seller receives the full sale price. If the listing expires unsold, the item is returned to the seller (see [Claiming returns](#claiming-returns)).

## Buying a listing

1. Open the browser with `/auction`.
2. Click a listing to view the confirmation screen.
3. Click **Confirm** to purchase, or **Cancel** to go back.

Requires `ezauction.auction.buy`. Buyers pay the listed price; the seller is credited (minus any platform fee configured by your economy setup).

## Cancelling a listing

```
/auction cancel
/auction cancel <id>
```

- With no ID, the command lists your own active listings and orders so you can cancel them.
- With an ID, it cancels that specific listing and returns the item to you.

Cancelling early returns the listed item. Whether the up-front listing fee is refunded depends on your configured fee behaviour.

## Expiry

When `auto-remove-expired` is enabled, unsold listings are removed when their duration ends. The item does not disappear - it becomes a pending return that the player must collect.

## Claiming returns

Returned and expired items, plus any items recovered after a failed transaction, are held for the player in the Claim system.

```
/auction claim
```

or click the **Claims / Returns** button in the browser GUI. The Claim GUI shows each pending item with its expiry date and offers a one-click **Claim All** button.

Pending returns are held for `claim.expiry-days` (default `7`, minimum `1`). After that they are considered expired and removed. The remaining time is shown in each item's lore.

## Transaction history

When `history-enabled` is `true` (default), every buy and sell is recorded and viewable:

```
/auction history
/auction history buy
/auction history sell
```

Players see only their own activity. This is useful for disputes and for players tracking their earnings.

## Related

- [Auction House](auction-house.md) - browsing and buying
- [Sell Menu](sell-menu.md) - creating listings
- [Buy Orders](buy-orders.md) - purchase offers
- [Live Auctions](live-auctions.md) - real-time bidding
- [Configuration - auction.yml](../configuration/auction-yml.md)
- [Reference - commands](../reference/commands.md)
- [Reference - permissions](../reference/permissions.md)
