# EzAuction — Agent Guidelines

A Paper Minecraft plugin providing a fully-featured auction house with GUI menus, live auctions, buy orders, Discord webhooks, hologram displays, and MySQL or YAML storage backends.

## Architecture

| Layer | Package | Role |
|-------|---------|------|
| Entrypoint | `bootstrap/` | Plugin lifecycle, component wiring |
| Config | `config/` | Typed config objects loaded from YML files |
| Commands | `command/` | `/auction`, `/order`, `/liveauction`, `/auctiondiscord`, etc. |
| GUI | `gui/` | Inventory-based menus (browser, sell, history, orders) |
| Services | `service/`, `claim/`, `live/`, `history/` | Business logic, no Bukkit I/O |
| Storage | `storage/` — `yaml/`, `mysql/` | Persistence behind `AuctionStorage` interface |
| Events | `event/` | Bukkit custom events for external plugin hooks |
| Integration | `integration/` | Optional Discord webhook notifier |
| Compat | `compat/` | HologramPlatform abstraction, PDC/Lore item tag strategies |
| API | `api/` | Public surface for listing-limit resolvers |

See [docs/api.md](docs/api.md), [docs/events.md](docs/events.md), [docs/permissions.md](docs/permissions.md), [docs/configuration.md](docs/configuration.md).

## Configuration

All user-facing settings live in resource YML files — never hardcode values.

| File | Purpose |
|------|---------|
| `auction.yml` | Core settings: language, durations, limits, deposits, feature flags |
| `auction-storage.yml` | Storage backend (YAML or MySQL) and connection pool |
| `auction-values.yml` | Item value display overrides |
| `discord.yml` | Webhook URLs and event toggles |
| `orders-only.yml` | Restrict plugin to buy-orders-only mode |

**Every new feature must have a toggle in `auction.yml`** under a clearly named section. Default values must be set so the plugin works out of the box on first install.

## Messages & Internationalisation

Messages live in `src/main/resources/messages/`, split into four per-language sets:

| File pattern | Contains |
|---|---|
| `messages_*.yml` | General plugin messages and notifications |
| `gui-messages_*.yml` | GUI item names, lore, and button labels |
| `menu-interactions_*.yml` | Chat prompts and input instructions |
| `menu-layout_*.yml` | Slot assignments and layout metadata |

**Supported languages:** `en` (default), `es`, `nl`, `zh` — set via `language:` in `auction.yml`.

Rules:
- Every new player-visible string **must** be added to all four language files.
- Use MiniMessage / Kyori Adventure components — no legacy `§` colour codes.
- Keys must match exactly across all language files.
- Never concatenate message fragments; use placeholders within a single key.

## Code Conventions

- Java 21; target Paper 1.21+ — use Paper/Adventure APIs, not Bukkit-legacy equivalents.
- One responsibility per class. Split large classes; do not exceed ~300 lines without good reason.
- Config objects are **immutable records or final-field POJOs** populated at load time by `AuctionConfigurationLoader`.
- Feature entry points belong in dedicated packages (`live/`, `history/`, `integration/`, …). Do not add new feature logic to `EzAuctionPlugin` or `AuctionManager` directly.
- Custom Bukkit events (`event/`) must be fired wherever an external plugin may need to react.
- Prefer dependency injection via constructor over static singletons.
- Follow existing Javadoc on public API classes and interfaces.

See [CONTRIBUTING.md](CONTRIBUTING.md) for PR and style guidelines.

## Build & Test

```bash
mvn verify          # compile, test, package
mvn test            # unit tests only
mvn package -DskipTests   # build JAR without tests
```

- Tests use **JUnit 5 + Mockito + MockBukkit**. All new features need corresponding unit tests.
- Keep test resources in `src/test/resources/`.
- Do not commit failing tests.

## Documentation

- Keep `docs/` in sync with every change to config keys, events, permissions, or the API.
- `docs/configuration.md` — updated when any YML key is added, removed, or changed.
- `docs/events.md` — updated when a new custom event is added.
- `docs/permissions.md` — updated when a permission node changes.
- `docs/api.md` — updated when the public API changes.
- Changelog entries go in `README.md` under the relevant version heading.
