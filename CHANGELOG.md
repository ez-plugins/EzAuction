# Changelog

All notable changes to EzAuction are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/). Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Release tags use the `v` prefix (e.g. `v2.2.0`).

## [2.3.2] - 2026-08-13

### Fixed

- **Hologram placement & removal** — rewrote the TextDisplay hologram platform to use the typed Paper API directly. Previously a `NamespacedKey(JavaPlugin, String)` lookup threw and was swallowed, so the PDC marker was never written and holograms could not be detected or removed. Holograms are now correctly tagged, re-attached after restarts, and persistent.
- **Hologram removal flow** — `/auctionhologram` now supports `list [radius] [--untagged]`, `clear [radius] [--untagged]` (alias `remove`), and `clearall [radius] [--untagged]`. Tagged `clear` removes the nearest EzAuction hologram immediately; `clearall` and any `--untagged` clear use two-step confirmation. Search uses the player's eye location and the new `removal-radius` config (default 10 blocks) instead of the placement-only `search-radius`.
- **Safer `clear --untagged`** — removing untagged displays (which also match other plugins' TextDisplays) now requires a second run within 15 seconds. The first run reports how many untagged displays are in range; the second removes only the nearest one, so a stray bulk delete of another plugin's holograms is avoided.
- **Placement confirmation shows coordinates** — the success message for `/auctionhologram <type>` now reports the exact spawn coordinates and world (e.g. `Placed … hologram at 12,66,-4 (world)`), matching where the hologram actually landed (the targeted block plus the configured `height-offset`).
- **Existing orphans stay orphaned** — holograms placed before this fix carry no marker; reach them with `--untagged` (also matches other plugins' TextDisplays). Run `list --untagged` before `clear --untagged`.
- **Hardened** — spherical distance filtering for removal, removed a latent `ConcurrentModificationException` in the update loop, and made `max-holograms` enforcement live with a distinct denial message.
- **Buy Orders GUI inventory theft vulnerability** - fixed a critical security issue where players could steal items from the Buy Orders overview. Both the `/orders` command and the Buy Orders tab in `/ah` were unprotected, allowing players to drag items out of the GUI into their inventory, or place items into the GUI (which would be deleted on close). All item interactions in buy order displays are now cancelled, and items are cloned so the backing order data is never directly accessible.

### Changed

- **Folia-safe scheduler shim** — all 23 direct scheduler calls now route through `compat/Scheduler`, which sends sync/delayed/repeating tasks to Folia's global region scheduler (and async tasks to its async scheduler) via reflection, falling back to the regular Bukkit scheduler on Paper/Spigot. This lets the plugin boot on Folia without `IllegalStateException` async-thread-access crashes. FoliaLib was intentionally not added as a dependency.
- **Server boot smoke workflow** — added `.github/workflows/server-boot.yml` which boots the built plugin on real Paper, Folia, and Spigot servers, failing the job on `SEVERE`/`FATAL`/async-access errors or if the plugin never reaches its enabled state. The real `Vault` and `EzEconomy` jars (committed under `plugins/`) are dropped into each server's `plugins/` folder so the full enable chain is exercised; for Folia the workflow injects `folia-supported: true` into a copy of the plugin jar so Folia actually loads it. Paper and Folia are downloaded from PaperMC's Fill API; Spigot is built from source via BuildTools (no prebuilt jar is published). The old in-process `spigot-smoke.yml` YAML round-trip workflow was removed in favour of this real-server boot.

## [2.3.1] - 2026-05-27

### Fixed

- **Live auction escrow safety** - fixed a critical payout edge case in live auction resolution. If the economy plugin fails to credit the seller when an auction ends, the winner's escrowed bid is now refunded immediately and the item is returned to the seller, preventing silent money loss.
- **Live auction regression coverage** - added dedicated end-auction tests for seller-credit failure and success paths to prevent escrow-loss regressions.

## [2.3.0] - 2026-05-25

### Added

- **Live auction overhaul** — complete rewrite of the live auction engine with new interactive features:
  - **`/bid` command** (alias `/livebid`) — dedicated command for all live auction interactions: place bids, join/leave sessions, and manage preferences. Bids can also be placed by clicking the suggestion in chat.
  - **Clickable bid messages** — the bid announcement in chat includes a clickable link that auto-fills `/bid <next-minimum>` for one-click bidding.
  - **Item hover previews** — the item name in chat announcements shows enchantments, potion effects, and shulker box contents on hover.
  - **EzCountdown integration** (optional soft-dependency) — configurable countdown display per-player via `ACTION_BAR`, `BOSS_BAR`, `TITLE`, `SCOREBOARD`, or `CHAT`. Defaults to chat-only reminders when EzCountdown is absent or disabled.
  - **Per-player notification preferences** — players toggle notifications with `/bid notify [on|off]` and choose display types with `/bid notify display <types…>`. Preferences persist across sessions (stored in `live-preferences.yml` or the MySQL table `{prefix}live_player_preferences`).
  - **Live auction picker GUI** — `/liveauction` opens a 54-slot inventory when multiple auctions are running, letting players select which one to join.
  - **Opt-in chat filter** — when `chat-filter-feature-enabled: true`, players can run `/bid mutechat on` to suppress non-auction chat while in an active session.
  - **Bidding configuration** — `minimum-bid-increment-percent` and `minimum-bid-increment-fixed` in `auction.yml` control the minimum raise required per bid.
  - **Five new Bukkit events**: `LiveAuctionStartedEvent` (cancellable), `LiveAuctionBidPlacedEvent` (cancellable), `LiveAuctionEndedEvent`, `LiveAuctionPlayerJoinedEvent`, `LiveAuctionPlayerLeftEvent`.
  - **New permissions**: `ezauction.bid` (default: true), `ezauction.auction.live.notify` (default: true).

![Live auction chat messages](https://i.ibb.co/8LjtSrdM/image.png)

![Live auction shulker contents](https://i.ibb.co/3mDfTnKc/image.png)

![/liveauction queue gui](https://i.ibb.co/3mgZ7nKC/image.png)

- **Live auction seller management** — sellers can enter and manage the live queue directly from the GUI or commands:
  - **Sell button in the Live Auction GUI** — a lit sell button (slot 44) appears in the live browser for players with `ezauction.auction.live.sell`; clicking it opens the sell menu pre-configured for the live queue. A grey button is shown to players without the permission.
  - **`/liveauction sell`** — command shortcut to open the live sell menu without navigating to the GUI first.
  - **Queue position feedback** — on enqueue, the seller receives a chat confirmation with their position (e.g. *"Your Diamond Sword has been added to the live queue at position #2"*). If the queue is disabled the auction starts immediately and a separate instant-start message is shown instead.
  - **Cancel button in the Live Auction GUI** — a cancel button (slot 37) appears only while the viewer has a queued entry; clicking it dequeues the item and refunds the deposit.
  - **`/liveauction cancel`** — removes the sender's own item from the live queue and refunds the deposit.
  - **`/liveauction cancel <id>`** (admin) — force-cancels an active live auction by ID, refunds all bidders, and returns the item to the seller.
  - **Duplicate-entry guard** — players who already have an item queued receive an `already-queued` message and cannot enqueue a second entry until the first is sold or cancelled.
  - **New permissions**: `ezauction.auction.live.sell` (default: true), `ezauction.auction.live.admin` (default: op).

### Fixed

- **JaCoCo Java 25 compatibility** — upgraded `jacoco-maven-plugin` from `0.8.12` to `0.8.13` to resolve `Unsupported class file major version 69` during test instrumentation on JDK 25.
- **Live auction first-bid minimum** — the first bid on a live auction now only requires meeting the starting price. Previously, the minimum bid increment (percent and fixed) was incorrectly applied before any bid existed, making the starting price unreachable.
- **Empty chat input bids the minimum** — during the custom-bid chat prompt, pressing Enter with no input now automatically places a bid at the minimum amount instead of cancelling or erroring.
- **Claim GUI** — `/auction claim` and the **Claims** button in the browser now open a dedicated 54-slot inventory instead of a bare command response. The GUI shows each pending return item with its expiry date in the lore, a **Claim All** button (slot 49), and a **Back** button (slot 45) that returns to the auction browser.
- **Configurable claim expiry** — pending return items now carry an expiry timestamp. New `claim.expiry-days` key in `auction.yml` (default `7`). The expiry is shown in the item lore inside the Claim GUI and resets whenever a new item is stored for the player.
- **Live Auctions shortcut in the browser** — a **Live Auctions** beacon button (slot 47) has been added to the main auction browser GUI. Clicking it opens the live auction picker directly, without needing to type `/liveauction`.

## [2.2.1] - 2026-05-17

### Fixed

- **Spigot compatibility** – `ItemStack` data is now stored as a Base64-encoded byte stream (`item-data` key) via `BukkitObjectOutputStream` instead of YAML-native serialization. This eliminates the `[ERROR] Material cannot be null` log entries that appeared during startup when loading YAML files written by a Paper 1.21+ server on a Spigot server. Existing files are migrated to the new format automatically on the next save cycle; the old `item` key is still read as a fallback during the transition.
- **Smoke test coverage** – Added `YamlAuctionStorageSmokeTest` (8 tests) and a dedicated `spigot-smoke.yml` GitHub Actions workflow to guard this code path in CI.

---

## [2.2.0] - 2026-05-11

### Added

![Team Auctions](https://i.ibb.co/vCYzR3R6/image.png)

- **Team Auctions**: team-scoped listings visible and purchasable only by members of the seller's team.
  - New command: `/auction team` - browse team listings (hidden when disabled or TeamsAPI absent).
  - New command: `/auction team sell` - list held item as a team auction.
  - `team-auctions.enabled` toggle in `auction.yml` (default `false`).
  - `TeamsIntegration` facade - null-safe soft-dependency wrapper around [TeamsAPI 1.4.0](https://github.com/ez-plugins/teams-api).
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
- `SellMenuState.quantity()` - tracks the chosen listing amount, clamped to `[1, item stack size]`.
- `SellMenuInteractionConfiguration.quantityAdjustments` - configurable quantity-step list (default: `±1, ±8, ±16, ±64`).
- `SellMenuLayoutConfiguration.quantityAdjustmentSlots` / `quantityDisplay` layout fields; inventory expanded from 27 to 36 slots to accommodate the extra row.

### Fixed

- Price-adjustment button labels displayed "coins" instead of the configured currency symbol (`$` by default). Labels now use `formatCurrency()` consistently with the rest of the sell menu.
- Chinese (`zh`) translation files: `gui-messages_zh.yml` had a duplicate `activity` preamble block and incorrect 4-space indentation; `messages_zh.yml` had `broadcast` and `team-auctions` merged onto one line and was missing a trailing newline; `orders-only.yml` had Windows CRLF line endings. All three files now pass YAML validation.

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
