---
title: Reloading
nav_order: 1
parent: Operations
---

# Reloading

Most EzAuction configuration can be reloaded at runtime without restarting the server.

## Command

```
/auction reload
```

Requires `ezauction.admin.reload` (default: op).

## What is reloaded

| File | Reloaded? | Notes |
|------|-----------|-------|
| `auction.yml` | ✅ | All core settings including language, limits, durations |
| `auction-storage.yml` | ❌ | Requires full server restart |
| `auction-values.yml` | ✅ | Item value overrides |
| `discord.yml` | ✅ via `/auctiondiscord reload` | Use the dedicated command |
| `orders-only.yml` | ✅ | Mode toggle applied immediately |
| `messages/*.yml` | ✅ | All language files |

{: .warning }
> **Storage backend changes** (`auction-storage.yml`) require a full server restart. Reload does not reconnect or migrate the database.

## Discord config reload

To reload only the Discord integration without touching other config:

```
/auctiondiscord reload
```

## When to restart instead

- Changing the storage backend (YAML ↔ MySQL)
- Adding or removing soft-dependency plugins (TeamsAPI, DiscordSRV)
- Installing a new version of EzAuction

## Related

- [Commands](../reference/commands.md)
- [Troubleshooting](troubleshooting.md)
