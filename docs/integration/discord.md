# DiscordSRV integration

A lightweight, optional integration that forwards auction events to Discord via DiscordSRV (JDA). It is safe to leave disabled if you do not use DiscordSRV.

## Quick links
- DiscordSRV: https://github.com/DiscordSRV/DiscordSRV
- Finding IDs (Developer Mode): https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-

## Requirements
- (Optional) Install DiscordSRV on your server to enable account linking and role checks.
- Ensure the plugin data folder contains `discord.yml`. A default file is bundled with the plugin.

## Configuration
Edit `discord.yml` in the plugin data folder (defaults are in `src/main/resources/discord.yml`). Key options:

- `enabled` (boolean) — master switch.
- `channel-id` (string) — Discord channel ID (snowflake) for posts.
- `events` (map) — which events to publish: `auction_start`, `auction_end`, `auction_bid`, `auction_cancel`.
- `message-format` (map) — templates per event (placeholders below). Supports Minecraft color codes using `&` for in-game messages; color codes are stripped when sending to Discord.
- `role-required` (boolean) — when true, the plugin will only send messages if the involved player has a linked Discord account that holds the required role.
- `required-role-id` (string) — role snowflake (recommended).
- `required-role-name` (string) — role name fallback.

### Example `discord.yml` snippet
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
  auction_start: "&aAuction started: {item} ({quantity}) listed by {seller} for {price} (id:{listingId})"
```

## Template placeholders
- `{item}` — item description (e.g. "3x Diamond Sword")
- `{price}` — formatted price
- `{seller}` — seller username
- `{buyer}` — buyer username
- `{bidder}` — bidder username
- `{amount}` — bid amount (formatted)
- `{listingId}` — internal listing id
- `{quantity}` — item stack quantity
- `{duration}` — human-friendly duration (for start events)

## Runtime admin commands (`/auctiondiscord`)
- `test [message]` — send a test message to the configured channel.
- `set channel <id>` — set `channel-id` and enable integration.
- `enable` / `disable` — toggle `enabled` in `discord.yml`.
- `reload` — reload `discord.yml` at runtime.
- `role show` — display current role settings.
- `role set id <id>` — set `required-role-id`.
- `role set name <name>` — set `required-role-name`.
- `role clear` — clear role id/name.
- `role require <true|false>` — set `role-required`.

Permission: `ezauction.discord` is required to run these commands.

## Role-check behavior
- If `role-required` is enabled the plugin checks DiscordSRV's linked-account mapping and tests whether the linked Discord account has the configured role (by ID first, then by name).
- If DiscordSRV is not installed or reflection fails, the plugin falls back to allowing messages by default (to avoid hard failure).

## Finding channel and role IDs (quick)
1. In Discord, open **User Settings → Advanced** and enable **Developer Mode**.
2. Right-click a channel and choose **Copy ID** to get the channel snowflake.
3. Right-click a role in Server Settings → Roles and choose **Copy ID** to get the role snowflake.

## Troubleshooting
- No messages: confirm `enabled: true`, `channel-id` is correct, and the bot (DiscordSRV) has send permission in that channel.
- Role checks not working: prefer `required-role-id` (stable) over name; ensure players have linked their Minecraft account to DiscordSRV.
- After editing `discord.yml`, run `/auctiondiscord reload` or restart the server.

## Notes for server owners
- The integration uses reflection to avoid a hard dependency on DiscordSRV; nothing breaks if DiscordSRV is absent.
- If you want, I can add step-by-step screenshots or a short admin checklist to this doc.

