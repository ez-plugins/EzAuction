---
title: Live Auctions
nav_order: 5
parent: Features
---

# Live Auctions

Live auctions are a real-time bidding experience. Items are presented one at a time and players submit bids within a configurable time window. A countdown tracks the remaining time, bids are clickable in chat, and item details (enchantments, potion effects, shulker contents) are visible on hover.

## Accessing a live auction

```
/liveauction
```

Or open via the **Live** button in the main auction browser. This opens the auction picker GUI when multiple live auctions are active, or auto-joins when only one is running.

You can also join directly via `/bid select <id>`.

Requires `ezauction.live`.

## Creating a live listing

```
/liveauction sell
```

Or click the **Sell** button (slot 44) in the Live Auction browser GUI. Both open the sell menu pre-configured for the live queue.

Legacy paths also work: `/auction sell live` and the sell menu reached from the main browser.

Requires `ezauction.auction.live.sell`.

## How it works

1. A seller uses `/liveauction sell` (or the GUI sell button) to queue an item. A chat message confirms the queue position, or an instant-start message appears if the queue is disabled.
2. The item becomes active when it reaches the front of the queue.
3. Players open `/liveauction` or `/bid select <id>` to join and view the current item.
4. Bids are placed with `/bid <amount>`. Clickable bid suggestions appear in chat.
5. At expiry the item goes to the highest bidder; unsold items are returned to the seller.

## Cancelling a live listing

```
/liveauction cancel
```

Removes your own item from the live queue and refunds the deposit. A **Cancel** button (slot 37) also appears in the Live Auction GUI while you have a queued entry.

Requires `ezauction.auction.live.sell`.

### Admin force-cancel

```
/liveauction cancel <id>
```

Force-cancels an *active* live auction (currently running, not just queued). All bidders are refunded and the item is returned to the seller.

Requires `ezauction.auction.live.admin`.

## The `/bid` command

| Sub-command | Description |
|-------------|-------------|
| `/bid` | Show your session status or list active auctions |
| `/bid <amount>` | Place a bid |
| `/bid select <id>` | Join a specific live auction |
| `/bid leave` | Leave your current session |
| `/bid notify [on\|off]` | Toggle countdown notifications |
| `/bid notify display <types…>` | Set EzCountdown display types (e.g. `ACTION_BAR BOSS_BAR`) |
| `/bid mutechat [on\|off]` | Silence other chat while in a session (opt-in, see below) |

## Item hover previews

When an item is announced in chat, hovering over the item name shows:

- **Enchantments** — all applied enchantments and their level
- **Potion effects** — active effects and duration for potions/tipped arrows
- **Shulker contents** — preview of the items stored inside a shulker box

## Countdown notifications

By default players see timed chat reminders as the auction approaches its end. Optionally, players can enable richer countdowns via `/bid notify on` (requires EzCountdown).

Per-player preferences are saved and restored across sessions. Players can choose which display types to use (`ACTION_BAR`, `BOSS_BAR`, `TITLE`, etc.).

### Server-wide EzCountdown setup

In `auction.yml`:

```yaml
live-auctions:
  countdown:
    ezcountdown-enabled: true          # requires EzCountdown
    display-types: [ACTION_BAR]        # default display for all players
    duration-seconds: 60
    chat-reminder-at-seconds: [30, 10, 5]
```

See [auction.yml configuration](../configuration/auction-yml.md#live-auctions--countdown) for all keys.

## Placing a bid

Bids are placed with `/bid <amount>`. Clickable bid suggestions appear in chat.

The **first bid** on any live auction only needs to meet the starting price — no increment is required on the opening bid. Subsequent bids must exceed the current highest bid by at least the configured minimum increment (`bid-increment.percent` or `bid-increment.fixed`, whichever is larger).

Typing `/bid` with no amount (or sending an empty response in the chat prompt) will automatically place a bid at the current minimum.

## Opt-in chat filter

When `chat-filter-feature-enabled: true` is set in `auction.yml`, players can run `/bid mutechat on` to suppress all non-auction chat messages while they are in an active session. This is entirely opt-in and off by default.

## Configuration

All live auction settings are in `auction.yml` under `live-auctions:`.

See the [auction.yml reference](../configuration/auction-yml.md) for full details.

## Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `ezauction.live` | true | Use `/liveauction` |
| `ezauction.auction.live.sell` | true | Queue items for live auction and cancel own entries |
| `ezauction.auction.live.admin` | op | Force-cancel active live auctions |
| `ezauction.bid` | true | Use `/bid` |
| `ezauction.auction.live.notify` | true | Toggle countdown notifications |

## Related

- [Sell Menu](sell-menu.md) - creating listings
- [Commands](../reference/commands.md) - `/bid` full reference
- [Configuration](../configuration/auction-yml.md) - `live-auctions` section
- [Permissions](../reference/permissions.md) - all permission nodes
