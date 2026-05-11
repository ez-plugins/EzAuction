---
title: Go-Live Checklist
nav_order: 5
parent: Step-by-Step Guides
---

# Go-Live Checklist

Run through this checklist before announcing EzAuction to your players.

## Installation

- [ ] `EzAuction-x.x.x.jar` is in `plugins/` and loaded (`/plugins` shows it green)
- [ ] Vault and a compatible economy plugin are installed and working
- [ ] Optional integrations (TeamsAPI, DiscordSRV, EzShops) are installed if desired

## Configuration

- [ ] `language:` in `auction.yml` matches your server's locale
- [ ] `max-listings:` is set to a reasonable per-player cap
- [ ] `listing-duration:` default and max are appropriate for your economy
- [ ] `listing-fee:` and `deposit:` are configured (or set to `0` to disable)
- [ ] Storage backend is correct: `yaml` for small servers, `mysql` for larger ones  
  → [Configure MySQL guide](configure-mysql.md)
- [ ] Discord webhooks configured and tested (if using)  
  → [`/auctiondiscord test`](../integrations/discord.md)

## Permissions

- [ ] Default group has `ezauction.use`, `ezauction.sell`, `ezauction.cancel`
- [ ] Staff/admin group has `ezauction.admin.*`
- [ ] VIP group has `ezauction.bypass.limit` (if applicable)
- [ ] Team auction permissions set (`ezauction.auction.team` / `ezauction.auction.team.sell`)

## Testing

- [ ] `/auction` opens the browser with no errors
- [ ] Listing an item via `/auction sell <price>` works
- [ ] Buying an item from the browser works and money transfers correctly
- [ ] `/order` and `/orders` work (if buy orders are enabled)
- [ ] `/auction reload` completes without errors in the console

## Final

- [ ] No errors in `logs/latest.log` relating to EzAuction
- [ ] Backup of `plugins/EzAuction/` taken before opening to players

---

{: .tip }
> Keep a copy of your `plugins/EzAuction/` folder before each plugin update so you can roll back if needed.
