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

---

## Discord Webhook (standalone — no DiscordSRV required)

EzAuction can post rich embed notifications directly to any Discord channel via a **webhook URL**. This works independently of DiscordSRV: you can use one, the other, or both simultaneously.

### How it works

1. You create a webhook in Discord and copy its URL.
2. You configure the URL in `discord.yml` under the `webhook:` section.
3. On every enabled auction event (start, end, bid, cancel) EzAuction POSTs a colour-coded embed to that channel asynchronously — the server tick thread is never blocked.

### Creating a webhook in Discord

1. Open the target channel → **Edit Channel** → **Integrations** → **Webhooks** → **New Webhook**.
2. Give it a name and optionally an avatar, then click **Copy Webhook URL**.
3. Keep this URL private — treat it like a password. Anyone with it can post to your channel.

### Configuration (`discord.yml`)

```yaml
webhook:
  enabled: true
  url: "https://discord.com/api/webhooks/123456789/ABCDEFGHIJKLMNOP"
  username: "EzAuction"       # Display name in Discord (null = webhook default)
  avatar-url: null            # Avatar URL override (null = webhook default)
  use-embeds: true            # true = rich embed, false = plain text
  events:
    auction_start: true
    auction_end: true
    auction_bid: true
    auction_cancel: true
  embed-colors:
    auction_start: 5763719    # green
    auction_end: 3447003      # blue
    auction_bid: 16776960     # yellow
    auction_cancel: 15158332  # red
```

**Embed colour values** are decimal integers. To convert a hex colour (#RRGGBB) use any online hex-to-decimal tool (e.g. `0x57F287` → `5763719`).

### Admin commands (`/auctiondiscord webhook ...`)

All commands require the `ezauction.discord` permission.

| Command | Description |
|---|---|
| `webhook status` | Show whether the webhook is enabled and the last 10 characters of the URL. |
| `webhook set url <url>` | Set the webhook URL (must start with `https://discord.com/api/webhooks/`), enable the webhook, and reload config. |
| `webhook enable` | Enable the webhook without changing other settings. |
| `webhook disable` | Disable the webhook without changing other settings. |
| `webhook test` | Send a test embed to verify the webhook is working. |

Changes are written to `discord.yml` and take effect immediately — no restart required.

### Event descriptions

| Event key | Triggered when | Embed colour |
|---|---|---|
| `auction_start` | A new auction listing is created | Green |
| `auction_end` | A listing is sold or purchased as an order | Blue |
| `auction_bid` | A bid is placed on a listing | Yellow |
| `auction_cancel` | A seller cancels their listing | Red |

> Expired listings (no buyer found) fire an "Auction Expired" embed using the `auction_end` colour and toggle.

### Error handling

- If the webhook call fails (network error, non-2xx response), a `WARNING` is logged in the server console and gameplay continues unaffected.
- If the configured URL does not start with `https://discord.com/api/webhooks/`, the webhook is silently disabled to prevent misuse.

### Security notes

- Never share your webhook URL publicly — revoke and regenerate it in Discord if it is leaked.
- The URL is validated client-side before any HTTP request is sent.
- Rate limits: Discord allows one message per second per webhook. For busy servers consider disabling less important events (e.g. `auction_bid: false`).

