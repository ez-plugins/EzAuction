# Changelog

All notable changes to EzAuction are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/). Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Release tags use the `v` prefix (e.g. `v2.2.0`).

## [Unreleased]

---

## [2.2.0] - 2026-05-11

### Added

- **Team Auctions**: team-scoped listings visible and purchasable only by members of the seller's team.
  - New command: `/auction team` — browse team listings (hidden when disabled or TeamsAPI absent).
  - New command: `/auction team sell` — list held item as a team auction.
  - `team-auctions.enabled` toggle in `auction.yml` (default `false`).
  - `TeamsIntegration` facade — null-safe soft-dependency wrapper around [TeamsAPI 1.4.0](https://github.com/ez-plugins/teams-api).
  - `AuctionListing.teamId` field persisted to both YAML and MySQL backends.
  - `AuctionListingService` team scope and `AuctionQueryService` team query, both guarded by the config flag.
  - `AuctionManager.createTeamListing()` and `listActiveTeamListings()`.
  - Team browse tab in the auction browser GUI; tab button hidden when team auctions are disabled or TeamsAPI is not installed.
  - `TeamsAPI` added to `softdepend` in `plugin.yml`.
  - Full message keys added across all four language files (`en`, `es`, `nl`, `zh`).
  - New permissions: `ezauction.auction.team`, `ezauction.auction.team.sell`.

### Added (sell menu)

- Price-adjustment buttons replaced with coloured glass panes (lime = increase, red = decrease).
- Second row of quantity-adjustment buttons (same glass style) to select how many of a stack to list.
- `SellMenuState.quantity()` — tracks the chosen listing amount, clamped to `[1, item stack size]`.
- `SellMenuInteractionConfiguration.quantityAdjustments` — configurable quantity-step list (default: `±1, ±8, ±16, ±64`).
- `SellMenuLayoutConfiguration.quantityAdjustmentSlots` / `quantityDisplay` layout fields; inventory expanded from 27 to 36 slots to accommodate the extra row.

### Fixed

- Price-adjustment button labels displayed "coins" instead of the configured currency symbol (`$` by default). Labels now use `formatCurrency()` consistently with the rest of the sell menu.

---

## [2.0.1] - 2025-10-05

### Added

- **Orders-Only Mode**: set `orders-only-mode: true` in `orders-only.yml` to disable all auction-house features and expose only the `/orders` and `/order` buy-order commands.

---

## [2.0.0] - 2025-08-20

### Added

- Enhanced GUI navigation with Back buttons and search tips.
- Quick access to pending returns via a dedicated Claims button.
- Consolidated "My Activity" menu combining listings and history.
- Low-price warning in listing confirmation dialogs.
- EzShops 2.0.0+ integration support.

### Changed

- Full rewrite of GUI layer using Bukkit inventory-based menus with PDC-backed action keys.
- Configuration split into focused files: `auction.yml`, `auction-storage.yml`, `auction-values.yml`, `discord.yml`, `orders-only.yml`.
- Storage backend abstracted behind `AuctionStorage` interface; YAML and MySQL both supported.
- Messages migrated to MiniMessage / Kyori Adventure components; legacy `§` colour codes removed.
- Multi-language support formalised (`en`, `es`, `nl`, `zh`) via `language:` setting in `auction.yml`.
