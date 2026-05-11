---
title: Getting Started
nav_order: 2
has_children: true
---

# Getting Started

## Requirements

| Requirement | Version |
|-------------|---------|
| Paper | 1.21+ |
| Java | 21+ |

## First-run checklist

1. Confirm Java 21+ and Paper 1.21+ are installed.
2. Drop the EzAuction jar into `plugins/`.
3. Start the server once - default configuration files are generated.
4. Configure `auction.yml`, `auction-storage.yml`, and `auction-values.yml`.
5. Restart the server and check the startup logs.
6. Run `/auction` in-game as a quick sanity check.

## Recommended setup order

- Start with the default YAML storage backend. Switch to MySQL later if needed.
- Leave optional integrations (Discord, TeamsAPI) disabled until core functionality is verified.
- Customise messages after confirming the plugin is working.

## Next step

Continue with [Installation](installation.md).
