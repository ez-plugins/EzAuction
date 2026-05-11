---
title: orders-only.yml
nav_order: 5
parent: Configuration
---

# orders-only.yml

Restricts the plugin to buy-orders mode only - the auction house browser, sell GUI, and live auctions are all disabled.

## Enable orders-only mode

```yaml
orders-only-mode: true
```

When enabled:
- `/orders` (alias `/order`) is the only available command for players.
- All other `/auction` subcommands are disabled.
- The browser GUI, sell menu, and live auction queue are hidden.

## Use case

Orders-only mode is useful for servers that want a pure buy-order marketplace without public item listings.

## Related

- [Buy Orders](../features/buy-orders.md) - full buy-order feature guide
- [Commands](../reference/commands.md) - available commands in each mode
