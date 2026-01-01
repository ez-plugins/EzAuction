# EzAuction Configuration Guide

This document provides a comprehensive overview of all configuration options available in EzAuction. Use this guide to customize the plugin to fit your server's needs.

---

## Location

Configuration files are generated in the `plugins/EzAuction/` directory on first run. The main configuration file is `config.yml`. Additional files include language files and auction value settings.

---

## Main Configuration (`config.yml`)

| Option                      | Type      | Default         | Description                                                                 |
|-----------------------------|-----------|-----------------|-----------------------------------------------------------------------------|
| `listing-max-duration`      | integer   | 72              | Maximum hours a listing can stay active.                                    |
| `listing-fee`               | string    | "0"            | Fee for creating a listing. Supports flat (e.g., `100`) or percent (`5%`).  |
| `currency-type`             | string    | "vault"        | Economy type: `vault`, `xp`, or custom.                                     |
| `max-listings-per-player`   | integer   | 5               | Maximum active listings per player.                                         |
| `allow-bidding`             | boolean   | false           | Enable/disable bidding (if supported).                                      |
| `gui-title`                 | string    | "Auction House"| Title displayed on the auction GUI.                                         |
| `language`                  | string    | "en"           | Language code for plugin messages.                                          |
| `auto-remove-expired`       | boolean   | true            | Automatically remove expired listings.                                      |
| `history-enabled`           | boolean   | true            | Enable/disable auction transaction history.                                 |
| `holograms-enabled`         | boolean   | false           | Enable/disable auction hologram displays.                                   |
| `notification-sound`        | string    | "ENTITY_EXPERIENCE_ORB_PICKUP" | Sound played on auction notifications. |

---

## Language & Messages

- The `language` option selects the language file from `plugins/EzAuction/messages/`.
- You can customize all messages and GUI text by editing the relevant YAML files.
- Supported languages: English (`en`), Spanish (`es`), Dutch (`nl`), Chinese (`zh`).

---

## Auction Values (`auction-values.yml`)

- Configure minimum/maximum prices, default durations, and item-specific rules.
- Example:
  ```yaml
  min-price: 100
  max-price: 100000
  default-duration: 24
  item-rules:
    DIAMOND_SWORD:
      min-price: 500
      max-price: 50000
  ```

---

## Storage & Persistence

- `auction-storage.yml` controls storage backend and options.
- Supported backends: `yaml` (default), `mysql` (for advanced setups).
- Example:
  ```yaml
  backend: yaml
  mysql:
    host: localhost
    port: 3306
    database: ezauction
    user: root
    password: changeme
  ```

---

## Reloading Configuration

- Use `/auction reload` to apply changes without restarting the server.
- Some changes (e.g., storage backend) may require a full server restart.

---

## Advanced Options

- See inline comments in each config file for further details.
- For troubleshooting and advanced usage, refer to the [full documentation](../docs/).

---

## Example: Minimal `config.yml`

```yaml
listing-max-duration: 48
listing-fee: "2%"
currency-type: "vault"
max-listings-per-player: 3
allow-bidding: false
gui-title: "Auction House"
language: "en"
auto-remove-expired: true
history-enabled: true
holograms-enabled: false
notification-sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
```

---

For further help, see the README or open an issue on GitHub.