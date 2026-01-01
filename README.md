


# EzAuction

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Build](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/Gyvex/ezauction/actions)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://adoptium.net/)

**EzAuction** is a modern, extensible Minecraft auction plugin for Bukkit/Spigot servers. It features a user-friendly GUI, robust API, and comprehensive event system for both server owners and plugin developers.

---

## 📑 Table of Contents
- [Requirements](#-requirements)
- [Installation](#-installation)
- [Features](#-features)
- [Configuration](#-configuration)
- [Permissions & Commands](#-permissions--commands)
- [Documentation](#-documentation)
- [Usage Examples](#-usage-examples)
- [Contributing](#-contributing)
- [Support & Community](#-support--community)
- [License](#-license)

---

## 📦 Requirements
- Java 17 or higher ([Adoptium](https://adoptium.net/))
- Bukkit/Spigot/Paper server (1.17+ recommended)

## 🚀 Installation
1. Download the latest EzAuction JAR from the [releases page](https://github.com/Gyvex/ezauction/releases).
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
- Actively maintained and open source

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

**Main Commands:**

| Command                | Description                        | Permission                |
|------------------------|------------------------------------|---------------------------|
| `/auction`             | Open the auction browser GUI       | `ezauction.use`           |
| `/auction sell <price>`| List held item for sale            | `ezauction.sell`          |
| `/auction cancel`      | Cancel your active listing         | `ezauction.cancel`        |
| `/auction reload`      | Reload plugin configuration        | `ezauction.admin.reload`  |

**Key Permissions:**

- `ezauction.use`: Access the auction GUI
- `ezauction.sell`: List items for sale
- `ezauction.cancel`: Cancel own listings
- `ezauction.admin.*`: All admin permissions

See the [docs/permissions.md](../docs/permissions.md) for a full list.

## 📚 Documentation
- [API Reference](docs/api.md): Public classes, methods, and code samples
- [Event Reference](docs/events.md): All plugin events, listener registration, and event payloads
- [Configuration Guide](docs/configuration.md): All config options explained
- [Permissions](docs/permissions.md): All permissions and their usage
- [Full Documentation](docs/): Guides, advanced usage, and troubleshooting

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
