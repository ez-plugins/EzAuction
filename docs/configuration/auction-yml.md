---
title: auction.yml
nav_order: 1
parent: Configuration
---

# auction.yml

The main configuration file. Controls language, listing rules, durations, deposit fees, and feature toggles.

## Language

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `language` | string | `en` | Language code for messages. Supported: `en`, `es`, `nl`, `zh`. |

## Listing rules

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `listing-max-duration` | integer | `72` | Maximum listing duration in hours. |
| `listing-fee` | string | `"0"` | Flat amount (e.g. `100`) or percentage (e.g. `5%`) charged when creating a listing. |
| `max-listings-per-player` | integer | `5` | Maximum active listings per player. Override per-player with permission groups via the API. |
| `auto-remove-expired` | boolean | `true` | Automatically expire and remove old listings. |
| `history-enabled` | boolean | `true` | Track and display transaction history. |
| `notification-sound` | string | `ENTITY_EXPERIENCE_ORB_PICKUP` | Sound played when a listing sells. Use Bukkit sound names. |

## GUI

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `gui-title` | string | `"Auction House"` | Title shown on the browser GUI. |
| `holograms-enabled` | boolean | `false` | Enable hologram displays (requires Minecraft 1.19+). |

## Currency

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `currency-type` | string | `vault` | Economy backend: `vault`, `xp`, or a custom provider. |

## Team auctions

Requires the [TeamsAPI](https://modrinth.com/plugin/teams-api) soft-dependency.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `team-auctions.enabled` | boolean | `true` | Master toggle for team-scoped listings. The GUI toggle button is hidden when TeamsAPI is absent. |

When enabled and TeamsAPI is present:
- A **Team Auctions** toggle appears in the browser GUI (configurable in `menu-layout_*.yml`).
- `/auction team` browses the team's listings.
- `/auction team sell` lists the held item as a team listing.

See [Team Auctions](../features/team-auctions.md) for the full feature guide.

## Example minimal `auction.yml`

```yaml
language: en

listing-max-duration: 48
listing-fee: "2%"
max-listings-per-player: 3
auto-remove-expired: true
history-enabled: true
notification-sound: ENTITY_EXPERIENCE_ORB_PICKUP

gui-title: "Auction House"
holograms-enabled: false

currency-type: vault

team-auctions:
  enabled: true
```
