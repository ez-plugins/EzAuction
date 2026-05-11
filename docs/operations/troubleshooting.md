---
title: Troubleshooting
nav_order: 2
parent: Operations
---

# Troubleshooting

## Plugin does not load

**Symptom:** `EzAuction` is missing from `/plugins` or shown in red.

| Cause | Fix |
|-------|-----|
| Wrong server software | EzAuction requires Paper 1.21+. Spigot is not supported. |
| Wrong Java version | Requires Java 21. Run `java -version` to check. |
| Missing Vault | Install Vault and a compatible economy plugin (e.g. EssentialsX). |
| JAR conflict | Check `logs/latest.log` for the exact error. |

## Economy not working / prices show as 0

1. Confirm Vault is installed and shows green in `/plugins`.
2. Confirm an economy plugin (EssentialsX, CMI, etc.) is registered with Vault.
3. Run `/eco give <player> 0` to test if Vault can resolve the economy provider.

## Listings not saving (YAML)

- Check that the server process has write permission to `plugins/EzAuction/`.
- Look for `IOException` in `logs/latest.log`.
- Check available disk space.

## MySQL connection errors

| Error message | Fix |
|---------------|-----|
| `Communications link failure` | Check host/port and firewall rules |
| `Access denied for user` | Verify username and password in `auction-storage.yml` |
| `Unknown database` | Create the database: `CREATE DATABASE ezauction;` |
| `Too many connections` | Reduce `pool-size` in `auction-storage.yml` |

After fixing, always **restart** the server (not just `/auction reload`).

## Team auctions button not showing

1. Confirm TeamsAPI is installed and shows green in `/plugins`.
2. Confirm `team-auctions.enabled: true` in `auction.yml`.
3. Confirm the player has `ezauction.auction.team` permission.
4. Run `/auction reload`.

## Discord messages not posting

See the [Discord integration troubleshooting table](../integrations/discord.md#troubleshooting).

## GUI items show as raw YAML keys

Messages failed to load. Check:
1. `language:` in `auction.yml` matches an available locale (`en`, `es`, `nl`, `zh`).
2. The messages files exist in `plugins/EzAuction/messages/`.
3. Run `/auction reload` and check the console for YAML parse errors.

## Config changes not taking effect

- For most files: run `/auction reload`.
- For `auction-storage.yml`: restart the server.
- For `discord.yml`: run `/auctiondiscord reload`.

## Checking logs

```bash
grep -i "ezauction\|auction" logs/latest.log
```

All EzAuction log messages are prefixed with `[EzAuction]`.

## Related

- [Reloading](reloading.md)
- [Configure MySQL](../guides/configure-mysql.md)
- [Discord Integration](../integrations/discord.md)
