---
title: Team Auctions
nav_order: 4
parent: Features
---

# Team Auctions

Team Auctions let members of the same team list items exclusively for their teammates. Listings are hidden from the global auction browser and only visible to players who are in the seller's team.

## Requirements

- **TeamsAPI** soft-dependency must be installed on the server (`plugins/TeamsAPI.jar`).  
  Without it, the feature degrades gracefully - the GUI button is hidden and all team commands are silently disabled.
- The `team-auctions.enabled` flag must be `true` in `auction.yml` (default: `true`).

## Commands

| Command | Description | Permission |
|---------|-------------|-----------|
| `/auction team` | Browse your team's listings | `ezauction.auction.team` |
| `/auction team sell` | Open sell menu for a team listing | `ezauction.auction.team.sell` |

## GUI toggle

When TeamsAPI is present and the feature is enabled, a **Team Auctions** toggle button appears in the auction browser (default slot 50). Click it to filter the listing view to your team only and back.

The slot can be adjusted in `menu-layout_*.yml` under `toggles.team-listings`.

## How listings work

1. The seller uses `/auction team sell` (or presses the sell button while the team filter is active).
2. The listing is tagged with the seller's team ID.
3. The listing is invisible to players outside that team.
4. Purchase, cancellation, and expiry behave identically to normal listings.

## Configuration

In `auction.yml`:

```yaml
team-auctions:
  enabled: true   # Master toggle. Also hidden if TeamsAPI is absent at runtime.
```

Set `enabled: false` to disable the feature server-wide without removing the TeamsAPI plugin.

## Related

- [TeamsAPI Integration](../integrations/teamsapi.md) - soft-dependency details
- [Permissions](../reference/permissions.md) - `ezauction.auction.team` and `ezauction.auction.team.sell`
- [Commands](../reference/commands.md) - full command reference
