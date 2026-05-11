---
title: TeamsAPI
nav_order: 2
parent: Integrations
---

# TeamsAPI Integration

[TeamsAPI](https://modrinth.com/plugin/teams-api) is a soft-dependency that unlocks the [Team Auctions](../features/team-auctions.md) feature. Without it, EzAuction runs normally - the team toggle button is simply hidden and `/auction team` is disabled.

## Installation

1. Download `TeamsAPI.jar` from [Releases](https://modrinth.com/plugin/teams-api).
2. Drop it into `plugins/`.
3. Restart the server.
4. Verify the startup log shows:

```
[EzAuction] TeamsAPI detected - team auction features enabled.
```

## Enabling team auctions

In `auction.yml`:

```yaml
team-auctions:
  enabled: true
```

This is the default. Set to `false` to disable the feature even when TeamsAPI is present.

## Permissions

| Node | Default | Description |
|------|---------|-------------|
| `ezauction.auction.team` | true | Browse team-scoped listings |
| `ezauction.auction.team.sell` | true | Create team-scoped listings |

## How the integration works

EzAuction looks up a player's team membership via TeamsAPI at listing creation and browse time. Listings are tagged with the team ID. Only players in the same team see those listings in the browser.

If TeamsAPI is removed at runtime, EzAuction falls back gracefully - team-tagged listings become invisible to all players until the plugin is restarted with TeamsAPI present again.

## Related

- [Team Auctions feature guide](../features/team-auctions.md)
- [Permissions reference](../reference/permissions.md)
