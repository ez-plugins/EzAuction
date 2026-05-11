---
title: Upgrading
nav_order: 3
parent: Operations
---

# Upgrading

## General upgrade procedure

1. **Back up** `plugins/EzAuction/` (config, data, messages).
2. Stop the server.
3. Replace `EzAuction-old.jar` with the new JAR in `plugins/`.
4. Start the server. New config keys are added with defaults automatically.
5. Review any new keys added to `auction.yml` and configure them for your server.

{: .warning }
> Never hot-swap the JAR while the server is running. Always stop first.

## 2.1.x to 2.2.x

### New features in 2.2.x

- **Team Auctions** - requires [TeamsAPI](../integrations/teamsapi.md) soft-dependency.
- **Sell Menu GUI improvements** - glass border buttons, quantity-adjust row.
- New `team-auctions` section in `auction.yml` (auto-added with defaults on first start).

### Migration steps

No breaking changes. Standard upgrade procedure applies.

If you use a custom `menu-layout_*.yml`, review the new slot entries for the team toggle button (`toggles.team-listings`, default slot 50) and the quantity row in the sell menu.

## 2.0.x to 2.1.x

No breaking changes. Standard upgrade procedure applies.

## 1.x to 2.x

Version 2.x introduced a completely rewritten storage layer.

### YAML data migration

1.x YAML data files are not compatible with 2.x. Before upgrading:
1. Allow all active listings to expire or manually cancel them.
2. Ensure all players claim their items (`/auction`).
3. Upgrade the JAR.
4. The plugin will create fresh 2.x data files on first start.

### Config migration

| 1.x key | 2.x equivalent |
|---------|---------------|
| `auction.max-listings` | `max-listings` |
| `auction.currency` | `currency` |
| `auction.language` | `language` |

Copy over relevant values manually. The new `auction.yml` has better inline documentation.

### MySQL

If you used MySQL in 1.x, the table schema changed. The plugin will attempt to create new 2.x tables. Old data must be migrated manually or discarded.

## Related

- [First Server Setup](../guides/first-server-setup.md)
- [Troubleshooting](troubleshooting.md)
