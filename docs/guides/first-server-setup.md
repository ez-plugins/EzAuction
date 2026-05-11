---
title: First Server Setup
nav_order: 1
parent: Step-by-Step Guides
---

# First Server Setup

Follow these steps to install EzAuction on a fresh Paper server.

## Prerequisites

| Requirement | Version |
|-------------|---------|
| Paper | 1.21+ |
| Java | 21+ |
| Economy plugin | Vault + compatible economy (e.g. EssentialsX) |

Optional:
- **TeamsAPI** - enables [Team Auctions](../features/team-auctions.md)
- **DiscordSRV** - enables [Discord Webhooks](../integrations/discord.md)
- **EzShops** - enables item value recommendations in the sell menu

## Step 1 - Download the plugin

Get `EzAuction-x.x.x.jar` from [GitHub Releases](https://modrinth.com/plugin/ezauction).

## Step 2 - Install

1. Drop the JAR into `plugins/`.
2. If using optional integrations, place those plugin JARs in `plugins/` too.
3. Start (or restart) the server once to generate default config files under `plugins/EzAuction/`.

## Step 3 - Review `auction.yml`

Open `plugins/EzAuction/auction.yml`. At minimum, confirm:

```yaml
language: en          # en, es, nl, zh
max-listings: 5       # listings per player
listing-duration: 7d  # default duration
listing-fee: 0.0      # upfront fee (0 = disabled)
```

See [auction.yml reference](../configuration/auction-yml.md) for all keys.

## Step 4 - (Optional) Switch to MySQL

If you plan to use MySQL, edit `plugins/EzAuction/auction-storage.yml` before players start listing items. See [Configure MySQL](configure-mysql.md).

## Step 5 - Set permissions

Grant the default permissions group `ezauction.use`, `ezauction.sell`, and `ezauction.cancel`. Operators inherit `ezauction.admin.*` by default.

See [Permissions](../reference/permissions.md) for a full list.

## Step 6 - Open to players

```
/auction reload
```

Players can now run `/auction` to open the auction house browser.

## Next steps

- [Go-Live Checklist](go-live-checklist.md) - verify everything before announcing
- [Configuration overview](../configuration/index.md) - all config files explained
