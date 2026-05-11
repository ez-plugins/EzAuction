---
title: Orders-Only Setup
nav_order: 4
parent: Step-by-Step Guides
---

# Orders-Only Setup

Orders-only mode disables the full auction house and leaves only the buy-orders system active. Useful for economy servers that want a want-list / marketplace without live item listings.

## What changes

| Feature | Normal mode | Orders-only mode |
|---------|-------------|-----------------|
| Auction browser (`/auction`) | ✅ Active | ❌ Disabled |
| Sell listings | ✅ Active | ❌ Disabled |
| Buy orders (`/order`) | ✅ Active | ✅ Active |
| Buy orders browser (`/orders`) | ✅ Active | ✅ Active |

## Step 1 - Edit `orders-only.yml`

```yaml
orders-only-mode: true
```

## Step 2 - Reload or restart

```
/auction reload
```

The `/auction` command and all sell/listing commands will be silently disabled. Players will see the orders commands only.

## Step 3 - Set permissions

In orders-only mode, you typically want to grant:

```
ezauction.auction.order   # browse buy orders
```

And revoke or leave unconfigured:
```
ezauction.use             # no longer needed
ezauction.sell            # no longer needed
```

## Reverting

Set `orders-only-mode: false` and reload. Existing buy orders are preserved.

## Related

- [Buy Orders feature](../features/buy-orders.md)
- [orders-only.yml reference](../configuration/orders-only.md)
