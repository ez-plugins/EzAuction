---
title: Messages & Language
nav_order: 4
parent: Configuration
---

# Messages & Language

All player-visible text is stored in `plugins/EzAuction/messages/`, split into four files per language.

## Supported languages

| Code | Language |
|------|----------|
| `en` | English (default) |
| `es` | Spanish |
| `nl` | Dutch |
| `zh` | Chinese |

Set the active language in [`auction.yml`](auction-yml.md):

```yaml
language: en
```

## File layout

| File | Contains |
|------|---------|
| `messages_<lang>.yml` | General plugin notifications and chat messages |
| `gui-messages_<lang>.yml` | GUI item names, lore, and button labels |
| `menu-interactions_<lang>.yml` | Chat prompts and input instructions |
| `menu-layout_<lang>.yml` | Slot assignments and layout metadata |

## Editing messages

1. Open the relevant file in `plugins/EzAuction/messages/`.
2. Edit the value for any key - do not change the key itself.
3. Run `/auction reload` to apply changes without restarting.

## Color codes

Messages use [MiniMessage / Kyori Adventure](https://docs.advntr.net/minimessage/format.html) tags, **not** legacy `§` codes.

```yaml
# Correct
no-permission: "<red>You do not have permission to do that.</red>"

# Incorrect - do not use
no-permission: "§cYou do not have permission to do that."
```

## Adding a custom language

1. Copy all four `*_en.yml` files and rename them with your language code (e.g. `_fr`).
2. Translate every value. **Keys must match exactly** across all four files.
3. Set `language: fr` in `auction.yml` and reload.
