# EzAuction Permissions

This document lists all permissions available in the EzAuction plugin, their default values, and a description of what each permission allows.

## Permission Nodes

| Permission Node                | Default   | Description                                      |
|-------------------------------|-----------|--------------------------------------------------|
| `ezauction.use`               | true      | Allows access to the auction GUI and commands.    |
| `ezauction.sell`              | true      | Allows listing items for sale in the auction.     |
| `ezauction.cancel`            | true      | Allows canceling own auction listings.            |
| `ezauction.admin.reload`      | op        | Allows reloading the plugin configuration.        |
| `ezauction.admin.*`           | op        | Grants all admin permissions for EzAuction.       |
| `ezauction.bypass.limit`      | op        | Bypass max listings per player limit.             |
| `ezauction.bypass.fee`        | op        | Bypass listing fee when creating auctions.        |
| `ezauction.bypass.duration`   | op        | Bypass max listing duration restriction.          |
| `ezauction.hologram`          | true      | Allows use of auction hologram commands.          |
| `ezauction.live`              | true      | Allows use of live auction commands.              |

## Permission Details

- **`ezauction.use`**: Required to open the auction browser GUI and use basic auction commands.
- **`ezauction.sell`**: Required to list items for sale in the auction house.
- **`ezauction.cancel`**: Required to cancel your own active listings.
- **`ezauction.admin.reload`**: Allows reloading the plugin configuration via command.
- **`ezauction.admin.*`**: Wildcard for all admin permissions (reload, force cancel, etc.).
- **`ezauction.bypass.limit`**: Allows a player to exceed the configured maximum number of active listings.
- **`ezauction.bypass.fee`**: Allows a player to create listings without paying the listing fee.
- **`ezauction.bypass.duration`**: Allows a player to set listing durations beyond the configured maximum.
- **`ezauction.hologram`**: Allows use of commands related to auction holograms (if enabled).
- **`ezauction.live`**: Allows use of live auction features and commands.

## Example Usage

To grant a player all auction permissions:

```
/pex user <player> add ezauction.*
```

To grant only basic auction access:

```
/pex user <player> add ezauction.use
/pex user <player> add ezauction.sell
/pex user <player> add ezauction.cancel
```

For more information, see the main README or the plugin's in-game help.
