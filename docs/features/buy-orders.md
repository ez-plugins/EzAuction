---
title: Buy Orders
nav_order: 3
parent: Features
---

# Buy Orders

Buy orders let players post purchase offers for items they want. Other players can browse open orders and fulfil them by providing the requested items.

## Creating a buy order

```
/order <price> <quantity>
/order <price> <quantity> <duration>
```

Hold the item template you want to buy. Requires `ezauction.auction.order`.

Alternatively, open the order menu GUI at `/auction order`.

## Fulfilling an order

Open the order browser (`/auction`, then click the **Orders** toggle). Click an order to see the fulfillment confirmation screen.

Requires `ezauction.auction.fulfill`.

## Orders-only mode

If the server uses orders-only mode, the auction house is hidden and `/orders` (alias `/order`) is the sole command available to players. See [orders-only.yml](../configuration/orders-only.md).

## Order menu GUI layout

The order menu GUI follows the same layout pattern as the sell menu:

- Row 1 - Price-adjust buttons (lime/red concrete)
- Row 2 - Quantity-adjust buttons (glass panes)
- Row 3+ - Item template, price display, quantity display, duration, confirm, cancel

## Cancelling an order

```
/auction cancel <order-id>
```

Tab-completion lists your active order IDs.

## Related

- [Auction House](auction-house.md) - the browser where orders are displayed
- [Permissions](../reference/permissions.md)
- [Commands](../reference/commands.md)
