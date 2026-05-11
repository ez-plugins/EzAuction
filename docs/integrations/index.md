---
title: Integrations
nav_order: 6
has_children: true
---

# Integrations

EzAuction supports optional third-party integrations. All integrations are soft-dependencies - the plugin starts and runs normally if they are absent.

| Integration | Purpose | Required? |
|-------------|---------|-----------|
| [Discord (DiscordSRV)](discord.md) | Forward auction events to a Discord channel | No |
| [TeamsAPI](teamsapi.md) | Enable team-scoped auction listings | No |
| [EzShops](ezshops.md) | Show shop price recommendations in the sell menu | No |

Enable or disable each integration in their respective config files or in `auction.yml`.
