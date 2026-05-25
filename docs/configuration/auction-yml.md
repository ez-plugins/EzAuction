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

## Live auctions — countdown

Controls the countdown timer displayed during live auctions. Requires the [EzCountdown](https://modrinth.com/plugin/ezcountdown) soft-dependency for non-chat displays.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `live-auctions.countdown.ezcountdown-enabled` | boolean | `false` | Use EzCountdown for rich countdown displays. Requires EzCountdown to be installed. When `false`, only chat reminders are sent. |
| `live-auctions.countdown.display-types` | list | `[ACTION_BAR]` | EzCountdown display types. Valid values: `ACTION_BAR`, `BOSS_BAR`, `CHAT`, `TITLE`, `SCOREBOARD`. |
| `live-auctions.countdown.format-message` | string | `"{formatted}"` | Countdown text format. `{formatted}` is replaced with the default HH:MM:SS string from EzCountdown. |
| `live-auctions.countdown.start-message` | string | `""` | Message shown when the countdown starts (blank = none). |
| `live-auctions.countdown.end-message` | string | `"Auction ended!"` | Message shown when the countdown reaches zero. |
| `live-auctions.countdown.duration-seconds` | integer | `60` | How long (seconds) each live auction runs. |
| `live-auctions.countdown.chat-reminder-interval-seconds` | integer | `0` | Send a chat reminder every N seconds. `0` = disabled. |
| `live-auctions.countdown.chat-reminder-at-seconds` | list | `[30, 10, 5]` | Send a chat reminder when this many seconds remain. Empty list = disabled. |

## Live auctions — bidding

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `live-auctions.bidding.minimum-bid-increment-percent` | double | `5.0` | Minimum bid must be at least this % above the current bid. |
| `live-auctions.bidding.minimum-bid-increment-fixed` | double | `10.0` | Minimum bid must be at least this amount above the current bid. Both percent and fixed are evaluated; the higher result wins. |

## Live auctions — chat filter

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `live-auctions.chat-filter-feature-enabled` | boolean | `false` | Enable the opt-in chat filter feature. When `true`, players can use `/bid mutechat on` to suppress non-auction chat while in a live auction session. |

## Claim expiry

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `claim.expiry-days` | integer | `7` | Number of days a pending return item is held before it is considered expired. Shown in the item lore inside the Claim GUI. Minimum effective value is `1`. |

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
