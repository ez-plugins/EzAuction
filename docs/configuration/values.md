---
title: auction-values.yml
nav_order: 3
parent: Configuration
---

# auction-values.yml

Controls item value display and shop-price recommendation behaviour in the sell GUI.

## Shop price recommendations

When EzShops is installed and the `shop-price` section is configured, the sell GUI shows a recommended price and a warning when the listing price is below 50% of that value.

```yaml
shop-price:
  enabled: true
  format: "${amount}"
```

## Per-item value overrides

Override the displayed value for specific materials:

```yaml
item-values:
  NETHERITE_INGOT:
    display-value: 5000
  DRAGON_EGG:
    display-value: 100000
```

## Minimum / maximum prices

```yaml
min-price: 1
max-price: 1000000
```

## Related

- [Sell Menu](../features/sell-menu.md) - how value recommendations appear in the GUI
- [EzShops Integration](../integrations/ezshops.md)
