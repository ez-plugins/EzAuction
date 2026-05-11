---
title: Commands
nav_order: 1
parent: Reference
---

# Commands

## `/auction`

The main auction house command.

| Sub-command | Usage | Permission | Description |
|-------------|-------|-----------|-------------|
| *(none)* | `/auction` | `ezauction.use` | Open the auction browser GUI |
| `sell <price>` | `/auction sell 100` | `ezauction.sell` | List the held item for sale |
| `cancel` | `/auction cancel` | `ezauction.cancel` | Cancel your active listing |
| `history [player]` | `/auction history` | `ezauction.auction.history` | View your transaction history |
| `history <player>` | `/auction history Steve` | `ezauction.auction.history.others` | View another player's history |
| `team` | `/auction team` | `ezauction.auction.team` | Browse your team's listings |
| `team sell` | `/auction team sell` | `ezauction.auction.team.sell` | Create a team-scoped listing |
| `reload` | `/auction reload` | `ezauction.admin.reload` | Reload all plugin configuration |

---

## `/order`

Create and manage buy orders.

| Sub-command | Usage | Permission | Description |
|-------------|-------|-----------|-------------|
| *(none)* | `/order` | `ezauction.auction.order` | Open the buy orders menu |

---

## `/orders`

Browse all active buy orders.

| Sub-command | Usage | Permission | Description |
|-------------|-------|-----------|-------------|
| *(none)* | `/orders` | `ezauction.auction.order` | Open the buy orders browser |

---

## `/liveauction`

Join the live auction queue.

| Sub-command | Usage | Permission | Description |
|-------------|-------|-----------|-------------|
| *(none)* | `/liveauction` | `ezauction.live` | Open the live auction viewer |

---

## `/auctiondiscord`

Manage the Discord integration at runtime.

| Sub-command | Usage | Permission | Description |
|-------------|-------|-----------|-------------|
| `test [msg]` | `/auctiondiscord test Hi` | `ezauction.discord` | Send a test message |
| `set channel <id>` | `/auctiondiscord set channel 123…` | `ezauction.discord` | Set the Discord channel ID |
| `enable` | `/auctiondiscord enable` | `ezauction.discord` | Enable the integration |
| `disable` | `/auctiondiscord disable` | `ezauction.discord` | Disable the integration |
| `reload` | `/auctiondiscord reload` | `ezauction.discord` | Reload `discord.yml` |
| `role show` | `/auctiondiscord role show` | `ezauction.discord` | Display current role settings |
| `role set id <id>` | `/auctiondiscord role set id 456…` | `ezauction.discord` | Set required role ID |
| `role set name <n>` | `/auctiondiscord role set name Admin` | `ezauction.discord` | Set required role name |
| `role clear` | `/auctiondiscord role clear` | `ezauction.discord` | Clear role settings |
| `role require <bool>` | `/auctiondiscord role require true` | `ezauction.discord` | Toggle role requirement |

---

## Related

- [Permissions](permissions.md) - default values and descriptions
- [Discord Integration](../integrations/discord.md) - full discord.yml reference
