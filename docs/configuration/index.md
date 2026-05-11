---
title: Configuration
nav_order: 3
has_children: true
---

# Configuration

EzAuction configuration is split across focused YAML files, all located in `plugins/EzAuction/` after the first server start.

| File | Purpose |
|------|---------|
| [`auction.yml`](auction-yml.md) | Core settings: language, durations, limits, deposits, feature flags |
| [`auction-storage.yml`](storage.md) | Storage backend (YAML or MySQL) and connection pool |
| [`auction-values.yml`](values.md) | Item value display and shop-price recommendation overrides |
| [`discord.yml`](../integrations/discord.md) | Discord webhook URLs and event toggles |
| [`orders-only.yml`](orders-only.md) | Restrict the plugin to buy-orders-only mode |

## Reloading

Use `/auction reload` to apply most changes at runtime. A full server restart is required when changing the **storage backend**.

## Messages and language

All player-visible text is in `plugins/EzAuction/messages/`. See [Messages & Language](messages.md) for the full guide.
