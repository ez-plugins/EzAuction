---
title: Permissions
nav_order: 2
parent: Reference
---

# Permissions

## Permission nodes

| Permission | Default | Description |
|-----------|---------|-------------|
| `ezauction.use` | true | Open the auction browser GUI and use basic commands |
| `ezauction.sell` | true | List items for sale in the auction house |
| `ezauction.cancel` | true | Cancel own active listings |
| `ezauction.hologram` | true | Use auction hologram commands |
| `ezauction.live` | true | Use live auction features and commands |
| `ezauction.auction.order` | true | Open buy-orders menu and browser |
| `ezauction.auction.history` | true | View own transaction history |
| `ezauction.auction.history.others` | op | View another player's transaction history |
| `ezauction.auction.team` | true | Browse team-scoped auction listings |
| `ezauction.auction.team.sell` | true | Create team-scoped listings |
| `ezauction.discord` | op | Manage Discord integration via `/auctiondiscord` |
| `ezauction.bypass.limit` | op | Exceed the configured max-listings-per-player cap |
| `ezauction.bypass.fee` | op | Create listings without paying the listing fee |
| `ezauction.bypass.duration` | op | Set listing durations beyond the configured maximum |
| `ezauction.admin.reload` | op | Reload plugin configuration via `/auction reload` |
| `ezauction.admin.*` | op | Wildcard — all admin permissions |

## Permission groups (example)

### Default players

```
ezauction.use
ezauction.sell
ezauction.cancel
ezauction.live
ezauction.auction.order
ezauction.auction.history
ezauction.auction.team
ezauction.auction.team.sell
```

### VIP players

```
# Everything above, plus:
ezauction.bypass.limit
```

### Staff / admins

```
ezauction.admin.*
ezauction.discord
```

## Related

- [Commands](commands.md) — command-level permission requirements
- [Team Auctions](../features/team-auctions.md) — team permission details
