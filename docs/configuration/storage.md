---
title: auction-storage.yml
nav_order: 2
parent: Configuration
---

# auction-storage.yml

Controls the storage backend that persists auction listings, orders, and transaction history.

## Backend selection

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `backend` | string | `yaml` | Storage engine: `yaml` (file-based) or `mysql`. |

{: .note }
Changing the backend requires a full server restart. Data is **not** migrated automatically between backends.

## YAML backend

No additional configuration is needed. Data is stored in `plugins/EzAuction/data/`.

## MySQL backend

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `mysql.host` | string | `localhost` | Database hostname. |
| `mysql.port` | integer | `3306` | Database port. |
| `mysql.database` | string | `ezauction` | Database/schema name. |
| `mysql.user` | string | `root` | Database username. |
| `mysql.password` | string | `changeme` | Database password. |
| `mysql.pool-size` | integer | `10` | HikariCP connection pool size. |

## Example: MySQL

```yaml
backend: mysql
mysql:
  host: db.example.com
  port: 3306
  database: ezauction
  user: ezauction_user
  password: s3cr3t
  pool-size: 10
```

## Security note

Never commit `auction-storage.yml` with real credentials to version control. Use environment variable substitution or a secrets manager for production deployments.
