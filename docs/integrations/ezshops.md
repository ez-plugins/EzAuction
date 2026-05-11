---
title: EzShops
nav_order: 3
parent: Integrations
---

# EzShops Integration

When **EzShops** is installed alongside EzAuction, the sell menu displays a recommended price based on the item's current shop buy/sell price. This helps players price their listings competitively without leaving the GUI.

## Requirements

- EzShops plugin installed (soft-dependency)

## What it does

In the [Sell Menu](../features/sell-menu.md), an additional info slot shows:

- The item's **shop buy price** (what a shop would pay the player)
- The item's **shop sell price** (what a shop charges players)

Players can use these values as a pricing reference when entering their auction price.

## Configuration

Override or disable the price hint display in `auction-values.yml`:

```yaml
# Set to false to hide the EzShops price hint in the sell menu
show-shop-price-hint: true

# Per-item overrides (display name → custom value label)
overrides:
  DIAMOND: "Shop: 100 coins / 150 coins"
```

See [auction-values.yml reference](../configuration/values.md) for full details.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| No price hint shown | Confirm EzShops is installed and the item has a shop price configured |
| Wrong price shown | Check `auction-values.yml` overrides and reload |

## Related

- [Sell Menu](../features/sell-menu.md)
- [auction-values.yml](../configuration/values.md)
