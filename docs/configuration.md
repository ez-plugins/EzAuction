# EzAuction Configuration Guide

This document provides a comprehensive overview of all configuration options available in EzAuction. Use this guide to customize the plugin to fit your server's needs.

---

## Location

Configuration files are generated in the `plugins/EzAuction/` directory on first run. The main configuration file is `config.yml`. Additional files include language files and auction value settings.

---


## Orders-Only Mode (`orders-only.yml`)

To enable only the Orders feature (disabling all auction house functionality), create or edit `orders-only.yml`:

```yaml
orders-only-mode: true
```

When enabled, the `/orders` command and its `/order` alias are available for players to create and manage buy orders. All other auction commands and GUIs are disabled.

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
| `holograms-enabled`         | boolean   | false           | Enable/disable auction hologram displays. <br>**Compatibility:** Hologram features require Minecraft 1.19+ and a supported server implementation. On legacy servers, hologram features will be disabled automatically and a warning will be logged. |
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


- **Hologram Compatibility:**
  - Hologram features use the TextDisplay entity, available only on Minecraft 1.19 and newer. On older server versions, hologram features will be disabled and a warning will be shown in console.
  - The plugin uses runtime checks and reflection to ensure compatibility. If your server does not support TextDisplay, all hologram-related commands and displays will be gracefully disabled.
  - For legacy support, the plugin falls back to a no-op implementation, ensuring no errors or crashes occur on unsupported versions.
  - For more details, see the [compatibility documentation](../docs/paper-topic-ezauction.md).

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

## Version 2.0.0 Updates

### New GUI Features

Version 2.0.0 introduces several new buttons and navigation improvements. These are configured in `messages/menu-layout_en.yml`:

#### Search Tips Button
Displays helpful search patterns to users:
```yaml
search-tips:
  slot: 50
  material: KNOWLEDGE_BOOK
  display-name: "&eSearch Tips"
  lore:
    - "&7Supported patterns:"
    - "&e• &7Item name: &ffortune"
    - "&e• &7With level: &ffortune 3"
    - "&e• &7Roman numerals: &fsharpness v"
    - "&e• &7Material ID: &fminecraft:diamond_sword"
```

#### Claims/Returns Button
Quick access to pending item returns:
```yaml
claims:
  slot: 47
  material: ENDER_CHEST
  display-name: "&6Pending Returns"
  lore:
    - "&7Click to claim items."
```

#### Sort Button Relocated
The sort button has moved from slot 50 to slot 51 to accommodate the search tips button. Update your custom layouts if needed.

### EzShops Integration

Version 2.0.0 adds support for EzShops 2.0.0+ while maintaining backwards compatibility with 1.x:

- **Automatic Detection:** The plugin detects which EzShops version is installed
- **Price Recommendations:** Shop prices appear in confirmation dialogs when enabled
- **Low-Price Warnings:** Listings below 50% of recommended price show warnings
- **Configuration:** Set up in `auction-values.yml` under `shop-price` section

### Migration from 1.x

**Good news:** No configuration changes are required! Version 2.0.0 is fully backwards compatible.

**Optional:** If you have custom `menu-layout_en.yml` configurations, consider adding the new button definitions to take advantage of new features.

---

For further help, see the README or open an issue on GitHub.