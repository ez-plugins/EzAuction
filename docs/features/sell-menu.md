---
title: Sell Menu
nav_order: 2
parent: Features
---

# Sell Menu

The sell menu is an interactive GUI that guides players through creating a new auction listing.

## Opening the sell menu

```
/auction sell
```

Hold the item you want to list. You can also pass an initial price:

```
/auction sell 5000
/auction sell 5000 24h
```

Requires `ezauction.auction.sell`.

## GUI layout (36-slot inventory)

| Row | Contents |
|-----|---------|
| Row 1 (slots 0–8) | Price-adjust buttons (green = increase, red = decrease) |
| Row 2 (slots 9–17) | Amount-adjust buttons (cyan = increase, blue = decrease) |
| Row 3 (slots 18–26) | Listing item preview, price display, quantity display |
| Row 4 (slots 27–35) | Duration display, custom price button, confirm, cancel |

## Price buttons

Clicking a price button adjusts the listing price up or down by the displayed amount. Prices are shown in currency format (e.g. `+$1,000.00`). Glass pane colour indicates direction:

- **Lime glass** - increase price
- **Red glass** - decrease price

## Amount buttons

Clicking an amount button adjusts how many of the held item stack you want to list:

- **Cyan glass** - increase amount
- **Blue glass** - decrease amount

The quantity is clamped between 1 and the item's stack size.

## Price display

Shows:
- Current listing price
- Recommended price from EzShops (if available)
- A warning if the price is below 50% of the recommended value

## Duration

Click the duration button to cycle through available durations. Configured via `auction.yml`.

## Custom price

Click the custom price button to type a price in chat.

## Confirm

The confirm button summarises the full listing (item, amount, price, duration, listing fee) before finalising.

## Related

- [Auction House](auction-house.md) - browsing and buying
- [Configuration - auction.yml](../configuration/auction-yml.md)
- [Configuration - auction-values.yml](../configuration/values.md)
