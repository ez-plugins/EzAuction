---
title: Installation
nav_order: 1
parent: Getting Started
---

# Installation

## 1. Download the jar

Download the latest `ezauction-*.jar` from the [Releases](https://modrinth.com/plugin/ezauction) page.

## 2. Install the plugin

Copy the jar into your server's `plugins/` directory:

```text
plugins/
└── ezauction-2.2.0-shaded.jar
```

## 3. Generate default configuration

Start the server once. EzAuction will create:

```text
plugins/EzAuction/
├── auction.yml
├── auction-storage.yml
├── auction-values.yml
├── discord.yml
├── orders-only.yml
└── messages/
    ├── messages_en.yml
    ├── gui-messages_en.yml
    ├── menu-interactions_en.yml
    └── menu-layout_en.yml
```

## 4. Configure storage

Edit `auction-storage.yml`. For most servers the default YAML backend works out of the box. To use MySQL:

```yaml
backend: mysql
mysql:
  host: localhost
  port: 3306
  database: ezauction
  user: root
  password: changeme
```

See [Storage Configuration](../configuration/storage.md) for all options.

## 5. Restart and verify

- Restart the server (a full restart is required after changing the storage backend).
- Check startup logs for any errors.
- Open `/auction` in-game to confirm the GUI loads.

## Optional integrations

The following soft-dependencies are loaded automatically when present - no extra configuration needed to detect them:

| Plugin | Feature |
|--------|---------|
| [TeamsAPI](https://modrinth.com/plugin/teams-api) | Team-scoped auction listings |
| [DiscordSRV](https://github.com/DiscordSRV/DiscordSRV) | Discord webhook notifications |
| Vault | Economy integration (required for currency) |
| EzShops | Shop-price recommendations in sell GUI |

See [Integrations](../integrations/) for setup details.
