---
title: Customize Messages
nav_order: 3
parent: Step-by-Step Guides
---

# Customize Messages

All player-visible text is stored in `plugins/EzAuction/messages/`. You can change wording, add a translation, or completely rebrand without touching the plugin JAR.

## File layout

Four files per language (replace `en` with `es`, `nl`, or `zh`):

| File | Contains |
|------|---------|
| `messages_en.yml` | Notifications and system messages |
| `gui-messages_en.yml` | GUI item names, lore lines, button labels |
| `menu-interactions_en.yml` | Chat prompts and input instructions |
| `menu-layout_en.yml` | Slot assignments and layout metadata |

## Step 1 - Open the target file

For example, to change the "item listed" success message:

```
plugins/EzAuction/messages/messages_en.yml
```

## Step 2 - Edit the value

Messages use [MiniMessage](https://docs.advntr.dev/minimessage/format.html) formatting:

```yaml
listing-created: "<green>Your item has been listed for <gold>{price}</gold>.</green>"
```

{: .important }
> Do **not** use legacy `§` colour codes. EzAuction uses the Adventure/MiniMessage API exclusively.

## Step 3 - Apply changes

```
/auction reload
```

No restart needed for message changes.

## Translating to a new language

EzAuction ships with `en`, `es`, `nl`, and `zh`. To add a custom language:

1. Copy the four `*_en.yml` files and rename them (e.g. `messages_de.yml`).
2. Translate all values. Keep the keys unchanged.
3. In `auction.yml`, set `language: de`.
4. Run `/auction reload`.

{: .note }
> Translations contributed back to the project are welcome - see [CONTRIBUTING.md](https://github.com/ez-plugins/EzAuction/blob/main/CONTRIBUTING.md).

## Placeholder reference

Common placeholders used across message files:

| Placeholder | Meaning |
|-------------|---------|
| `{player}` | Player display name |
| `{price}` | Formatted price |
| `{item}` | Item description |
| `{quantity}` | Stack size |
| `{duration}` | Human-friendly duration |
| `{listingId}` | Internal listing ID |

## Related

- [Messages configuration](../configuration/messages.md) - file-level reference
- [Configuration overview](../configuration/index.md)
