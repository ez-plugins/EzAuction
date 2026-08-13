---
title: Auction Holograms
nav_order: 7
parent: Features
---

# Auction Holograms

EzAuction can display live auction stats in the world as floating `TextDisplay` holograms. They are a visual overview of your auction house - useful outside spawn, at shops, or in hubs.

Requires **Minecraft 1.19+** (TextDisplay entities) and a server running **Paper 1.21+**.

## Enabling

Holograms are off by default. In `auction.yml`:

```yaml
holograms-enabled: true
```

Then under the top-level `holograms:` block:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `holograms.enabled` | boolean | `true` | Master toggle for in-world hologram displays. |
| `holograms.update-interval-ticks` | long | `100` | How often hologram text refreshes (20 ticks = 1 second). |
| `holograms.search-radius` | double | `2.5` | Horizontal radius used to re-attach an existing hologram when placing a new one at the same spot. |
| `holograms.height-offset` | double | `1.75` | Vertical offset added above the targeted block when spawning. |
| `holograms.removal-radius` | double | `10.0` | Default search radius for `list`, `clear`, and `clearall`. |
| `holograms.max-removal-radius` | double | `64.0` | Hard cap on the radius a player may pass to those commands. |
| `holograms.allow-untagged-removal` | boolean | `true` | Allow the `--untagged` flag (see [Removing](#removing-holograms)). Use with care. |
| `holograms.max-holograms` | int | `100` | Maximum number of tracked holograms; placement is denied beyond this. |

The `--untagged` flag and `allow-untagged-removal` also match other plugins' `TextDisplay` entities, so keep that flag disabled unless staff need to clean up stray displays.

## Placing a hologram

Look at a block (within 6 blocks) and run:

```
/auctionhologram <type>
```

| Type | Alias | Shows |
|------|-------|-------|
| `ACTIVE_LISTINGS` | `listings` | Current active auction listings |
| `ACTIVE_ORDERS` | `orders` | Current active buy orders |

The hologram spawns at the centre of the targeted block, raised by `height-offset` (default `1.75`). On success the command reports the exact spawn coordinates and world, e.g. `Placed Active Listings hologram at 12,66,-4 (world)`, so you know exactly where it landed.

If you place a hologram where one of the same type already exists, the existing one is re-used (re-attached) instead of creating a duplicate.

## Managing holograms

All hologram commands require `ezauction.hologram`. See the [Commands reference](../reference/commands.md#auctionhologram) for the full table.

### Listing nearby holograms

```
/auctionhologram list [radius] [--untagged]
```

Lists matching displays nearest-first, one line per match with type, world, coordinates, and distance. Add `--untagged` to also show unmarked `TextDisplay` entities (e.g. from other plugins).

### Removing holograms

```
/auctionhologram clear [radius]
/auctionhologram clear [radius] --untagged      (alias: remove)
/auctionhologram clearall [radius] [--untagged]
```

- **`clear`** (tagged) - removes the single nearest EzAuction hologram immediately.
- **`clear --untagged`** - two-step, for safety. The first run reports how many untagged displays are in range (these may include other plugins' holograms); running the command again within **15 seconds** removes only the nearest one. This prevents an accidental bulk delete of another plugin's displays.
- **`clearall`** - removes every matching hologram in range, also behind a two-step confirmation (run again within 15 seconds).

Radius defaults to `removal-radius` and is capped at `max-removal-radius`.

### Reaching orphaned holograms

Holograms placed by older builds may carry no marker. Reach them with `--untagged` (which also matches other plugins' `TextDisplay` entities). Always run `list --untagged` first to see what is in range before clearing.

## Persistence

Holograms are tagged via the Paper Persistent Data Container, so they survive restarts and are re-attached automatically on server start. Tracked counts are enforced against `max-holograms`.

## Related

- [Configuration - auction.yml](../configuration/auction-yml.md)
- [Commands reference](../reference/commands.md#auctionhologram)
- [Permissions reference](../reference/permissions.md)
