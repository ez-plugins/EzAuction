---
title: Discord (DiscordSRV)
nav_order: 1
parent: Integrations
---

# Discord Integration

A lightweight optional integration that forwards auction events to a Discord channel via DiscordSRV. It is safe to leave disabled if you do not use DiscordSRV.

## Requirements

- **DiscordSRV** installed on the server (soft-dependency - EzAuction still starts without it)
- A Discord bot with **Send Messages** permission in the target channel
- Developer Mode enabled in Discord to copy channel/role snowflakes

## Quick setup

1. Install DiscordSRV and configure it normally.
2. In `plugins/EzAuction/discord.yml`, set `enabled: true` and paste your channel ID:

```yaml
enabled: true
channel-id: "123456789012345678"
```

3. Restart the server or run `/auctiondiscord reload`.
4. Test the connection:

```
/auctiondiscord test Hello from EzAuction!
```

## Full `discord.yml` reference

```yaml
enabled: true
channel-id: "123456789012345678"

events:
  auction_start: true
  auction_end: true
  auction_bid: true
  auction_cancel: true

role-required: false
required-role-id: null
required-role-name: null

message-format:
  auction_start: "<green>Auction started: {item} ({quantity}) listed by {seller} for {price}</green>"
  auction_end:   "<yellow>Auction ended: {item} sold to {buyer} for {price}</yellow>"
  auction_bid:   "<aqua>{bidder} bid {amount} on {item}</aqua>"
  auction_cancel: "<red>Auction cancelled: {item} by {seller}</red>"
```

## Template placeholders

| Placeholder | Value |
|-------------|-------|
| `{item}` | Item description (e.g. "3× Diamond Sword") |
| `{price}` | Formatted sale price |
| `{seller}` | Seller username |
| `{buyer}` | Buyer username |
| `{bidder}` | Bidder username |
| `{amount}` | Bid amount (formatted) |
| `{listingId}` | Internal listing ID |
| `{quantity}` | Stack size |
| `{duration}` | Human-friendly duration (start events only) |

## Role-check behavior

When `role-required: true`, the plugin checks DiscordSRV's linked-account mapping and verifies the player's linked Discord account holds the required role (ID preferred over name). If DiscordSRV is absent, the check is skipped and messages are sent anyway to avoid hard failure.

## Admin commands (`/auctiondiscord`)

Requires `ezauction.discord`.

| Sub-command | Action |
|-------------|--------|
| `test [message]` | Send a test message to the configured channel |
| `set channel <id>` | Set `channel-id` and enable the integration |
| `enable` / `disable` | Toggle `enabled` at runtime |
| `reload` | Reload `discord.yml` without a restart |
| `role show` | Display current role settings |
| `role set id <id>` | Set `required-role-id` |
| `role set name <name>` | Set `required-role-name` |
| `role clear` | Clear role ID and name |
| `role require <true\|false>` | Toggle `role-required` |

## Finding channel and role IDs

1. In Discord, open **User Settings → Advanced** and enable **Developer Mode**.
2. Right-click a channel → **Copy ID** for the channel snowflake.
3. In **Server Settings → Roles**, right-click a role → **Copy ID** for the role snowflake.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| No messages posted | Confirm `enabled: true`, correct `channel-id`, bot has Send Messages permission |
| Role check never passes | Prefer `required-role-id` (stable); ensure players have linked via DiscordSRV |
| Config changes not applied | Run `/auctiondiscord reload` or restart |
