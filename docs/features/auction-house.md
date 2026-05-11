---
title: Auction House
nav_order: 1
parent: Features
---

# Auction House

The core feature of EzAuction: a paginated GUI browser where players can search, sort, and buy active listings.

## Opening the browser

```
/auction
```

No arguments. Players with `ezauction.auction` can open the GUI.

## Browser GUI controls

| Button | Description |
|--------|-------------|
| Previous / Next | Navigate listing pages |
| Search | Enter a search query in chat. Supports item name, enchantment name, enchantment level, and material ID. |
| Sort | Cycle through sort modes (price, time, name) |
| Listings toggle | Filter to active auction listings |
| Orders toggle | Switch to the buy-orders view |
| Team Auctions toggle | View team-scoped listings (requires TeamsAPI) |
| Claims / Returns | Quick-open the pending returns menu |
| My Activity | View your own listings and orders |

## Search patterns

| Pattern | Example | Matches |
|---------|---------|---------|
| Item name | `fortune` | Any item with "fortune" in its display name or enchantments |
| With level | `fortune 3` | Items with Fortune III specifically |
| Roman numerals | `sharpness v` | Items with Sharpness V |
| Material ID | `minecraft:diamond_sword` | Exact material match |

## Buying a listing

1. Click any listing in the browser to open the confirmation screen.
2. Review the item, price, and seller.
3. Click **Confirm** to purchase or **Cancel** to return.

Requires `ezauction.auction.buy`.

## Sorting listings

Click the sort button to cycle through available sort modes. The active sort is remembered per-player for the session.

## Related

- [Sell Menu](sell-menu.md) - listing items for sale
- [Commands reference](../reference/commands.md)
- [Permissions reference](../reference/permissions.md)
