---
title: Configure MySQL
nav_order: 2
parent: Step-by-Step Guides
---

# Configure MySQL

EzAuction defaults to a file-based YAML backend. Switch to MySQL for better performance on servers with many players or listings.

## When to switch

- More than ~50 concurrent players browsing/selling
- You want cross-server data sharing
- You want robust backup and query capabilities

## Step 1 - Create a database

```sql
CREATE DATABASE ezauction CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ezauction'@'%' IDENTIFIED BY 'strongpassword';
GRANT ALL PRIVILEGES ON ezauction.* TO 'ezauction'@'%';
FLUSH PRIVILEGES;
```

## Step 2 - Edit `auction-storage.yml`

```yaml
storage:
  backend: mysql         # change from yaml to mysql

mysql:
  host: localhost
  port: 3306
  database: ezauction
  username: ezauction
  password: strongpassword
  pool-size: 10
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
```

{: .warning }
> Migrate **before** players start listing items, or run a data migration to avoid losing existing YAML listings.

## Step 3 - Restart

Restart the server (not just `/auction reload`). EzAuction will create the required tables automatically on first startup.

## Step 4 - Verify

Check the startup log for:

```
[EzAuction] Connected to MySQL database.
[EzAuction] Tables initialised.
```

If you see a connection error, double-check host/port, firewall rules, and credentials.

## Connection pool tuning

| Key | Recommendation |
|-----|---------------|
| `pool-size` | 2× CPU cores, or 10 for small servers |
| `connection-timeout` | 30 000 ms |
| `idle-timeout` | 600 000 ms (10 min) |
| `max-lifetime` | 1 800 000 ms (30 min) |

## Related

- [Storage configuration reference](../configuration/storage.md)
- [Troubleshooting](../operations/troubleshooting.md)
