# EzAuction

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Build](https://github.com/Gyvex/ezauction/actions/workflows/ci.yml/badge.svg)](https://github.com/Gyvex/ezauction/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Gyvex/ezauction/graph/badge.svg)](https://codecov.io/gh/Gyvex/ezauction)
[![Java](https://img.shields.io/badge/java-21%2B-blue)](https://adoptium.net/)

**EzAuction** is a modern, extensible Minecraft auction plugin for Bukkit/Spigot servers. It features a user-friendly GUI, robust API, and comprehensive event system for both server owners and plugin developers.

**Version 2.0.0** introduced enhanced GUI navigation, detailed confirmation dialogs, and full compatibility with EzShops 2.0.0+. **Version 2.2.0** adds Team Auctions, team-scoped listings visible and purchasable only by your team, powered by the optional [TeamsAPI](https://modrinth.com/plugin/teams-api) soft-dependency.

---

## 📑 Table of Contents
- [EzAuction](#ezauction)
  - [📑 Table of Contents](#-table-of-contents)
  - [📦 Requirements](#-requirements)
  - [🚀 Installation](#-installation)
    - [New Installation](#new-installation)
  - [✨ Features](#-features)
  - [⚙️ Configuration](#️-configuration)
  - [🛡️ Permissions \& Commands](#️-permissions--commands)
  - [📚 Documentation](#-documentation)
  - [🛠️ Usage Examples](#️-usage-examples)
    - [Creating a Listing](#creating-a-listing)
    - [Listening for Events](#listening-for-events)
  - [🤝 Contributing](#-contributing)
  - [🛡️ Support \& Community](#️-support--community)
  - [📄 License](#-license)

---

## 📦 Requirements
- Java 21 or higher ([Adoptium](https://adoptium.net/))
- Paper server 1.21+ (Bukkit/Spigot compatible)

## 🚀 Installation

### New Installation
1. Download the latest EzAuction JAR from the [releases page](https://github.com/ez-plugins/ezauction/releases).
2. Place the JAR in your server's `plugins/` directory.
3. Start or reload your server.
4. Configure the plugin as needed (see below).

## ✨ Features
- Intuitive auction GUI for browsing, buying, and selling
- Buy/sell items with in-game currency
- Configurable listing rules, durations, and permissions
- Search, filter, and sort auction listings
- Expiry and auto-removal of old listings
- Customizable messages and GUI appearance
- Full event and API support for developers
- Multi-language support (via config)
- **NEW in 2.0**: EzShops 2.0.0+ integration support
- **NEW in 2.0**: Enhanced navigation with Back buttons and search tips
- **NEW in 2.0**: Quick access to pending returns via Claims button
- **NEW in 2.0**: Low-price warnings in confirmation dialogs
- **NEW in 2.0**: Consolidated "My Activity" menu
- Actively maintained and open source
- **NEW in 2.0.1** Orders-Only Mode
  If you only want the Orders feature (buy orders, no auction house), set `orders-only-mode: true` in `orders-only.yml`. This disables all auction house features and enables the `/orders` and `/order` commands for players to create and manage buy orders.
- **NEW in 2.2.0**: Team Auctions — team-scoped listings visible and purchasable only by members of the seller's team. Requires the optional [TeamsAPI](https://github.com/ez-plugins/teams-api) soft-dependency. Toggle with `team-auctions.enabled` in `auction.yml`.

## ⚙️ Configuration
Default configuration files are generated on first run in `plugins/EzAuction/`.

**Key configuration options in `config.yml`:**

- `listing-max-duration`: Maximum time (in hours) a listing can stay active
- `listing-fee`: Flat or percentage fee for creating a listing
- `currency-type`: Vault, XP, or custom economy
- `max-listings-per-player`: Limit the number of active listings per player
- `allow-bidding`: Enable/disable bidding (if supported)
- `gui-title`: Customize the auction GUI title
- `language`: Set the plugin language (see `lang/` folder)

After editing, reload the plugin or restart the server to apply changes.

## 🛡️ Permissions & Commands

**Orders-Only Mode Command:**

| Command   | Description                        | Permission                |
|-----------|------------------------------------|---------------------------|
| `/orders` | Open the Orders (buy orders) menu  | `ezauction.auction.order` |

When `orders-only-mode` is enabled, all other auction commands are disabled.

**Main Commands:**

| Command                | Description                        | Permission                |
|------------------------|------------------------------------|---------------------------|
| `/auction`             | Open the auction browser GUI       | `ezauction.use`           |
| `/auction sell <price>`| List held item for sale            | `ezauction.sell`          |
| `/auction cancel`      | Cancel your active listing         | `ezauction.cancel`        |
| `/auction reload`      | Reload plugin configuration        | `ezauction.admin.reload`  |
| `/auction history [player]` | View your auction history (or another player's, if permitted) | `ezauction.auction.history` / `ezauction.auction.history.others` |
| `/auction team`             | Browse team-scoped auction listings                           | `ezauction.auction.team`                                          |
| `/auction team sell`        | List held item as a team auction                              | `ezauction.auction.team.sell`                                     |

**Key Permissions:**

- `ezauction.use`: Access the auction GUI
- `ezauction.sell`: List items for sale
- `ezauction.cancel`: Cancel own listings
- `ezauction.admin.*`: All admin permissions

- `ezauction.auction.history`: View your own auction history in the GUI
- `ezauction.auction.history.others`: View other players' auction history (if permitted)
- `ezauction.auction.team`: Browse team-scoped auction listings (hidden when disabled or TeamsAPI absent)
- `ezauction.auction.team.sell`: Create team-scoped listings via `/auction team sell`

See the [docs/permissions.md](docs/permissions.md) for a full list.

## 📚 Documentation

Full documentation is available at **[ez-plugins.github.io/ezauction](https://ez-plugins.github.io/ezauction)**.

- [Getting Started](https://ez-plugins.github.io/ezauction/getting-started) - Installation and first setup
- [Configuration](https://ez-plugins.github.io/ezauction/configuration) - All config files explained
- [Commands & Permissions](https://ez-plugins.github.io/ezauction/reference/commands) - Full command and permission reference
- [Developer API & Events](https://ez-plugins.github.io/ezauction/reference/api) - Public API and custom Bukkit events
- [Integrations](https://ez-plugins.github.io/ezauction/integrations) - Discord, TeamsAPI, EzShops
- [Changelog](CHANGELOG.md) - Version history and notable changes

## 🛠️ Usage Examples

### Creating a Listing
```java
AuctionOperationResult result = auctionManager.createListing(player, itemStack, price, duration);
if (result.success()) {
    // Listing created
} else {
    player.sendMessage(result.message());
}
```

### Listening for Events
```java
@EventHandler
public void onAuctionListingCreate(AuctionListingCreateEvent event) {
    // Custom logic here
}
```

---

## 🤝 Contributing
Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines, or open an issue/PR.

## 🛡️ Support & Community
- For help, open an issue on GitHub or contact the maintainers.
- Feature requests and bug reports are encouraged.

## 📄 License
EzAuction is licensed under the [MIT License](LICENSE). Copyright (c) 2026 Gyvex (63536625).

---

For full documentation, see the [docs/](docs/) folder. For support, open an issue or contact the maintainers.
