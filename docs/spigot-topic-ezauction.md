[CENTER][SIZE=6][B]EzAuction[/B][/SIZE]
[SIZE=3]Menu-first marketplace with listings, buy orders, and async safety nets[/SIZE]
[SIZE=2]Paper/Purpur 1.21+ • Vault economy • YAML fallback + MySQL/HikariCP • English/Dutch/Spanish/Chinese menus included • PlaceholderAPI stats
[COLOR=#ff6600]Requires Vault and a compatible economy plugin to process currency.[/COLOR][/SIZE][/CENTER]

[SIZE=4][B]Why EzAuction?[/B][/SIZE]
[LIST]
[*][B]Menu-first trading[/B] – Paginated browsers, sell/order editors, and confirmation popups keep every transaction guided and click-friendly.
[IMG]https://i.ibb.co/ks3WjqQ2/ez-auction-auction-house.png[/IMG]
[*][B]Listings + buy orders[/B] – Run timed sales, reserve balance for buy requests, and fulfill either side straight from the GUI or command flow.
[IMG]https://i.ibb.co/27hNgZgx/ez-auction-buy-orders.png[/IMG]
[IMG]https://i.ibb.co/KxyrpHQk/ez-auction-listing.png[/IMG]
[*][B]Async persistence & fallbacks[/B] – Listings and history write through dedicated executors, automatically falling back to YAML storage if MySQL is unreachable.
[*][B]Offline-safe returns[/B] – Expired or undeliverable items go to a claimable stash with login reminders and `/auction claim` recovery.
[*][B]Rank-aware limits[/B] – Plug in a custom `AuctionListingLimitResolver` service to scale slot caps with ranks, islands, or progression data.
[*][B]Smart value overlays[/B] – Surface configured price hints or pipe in [URL='https://www.spigotmc.org/resources/1-21-%E2%9A%A0%EF%B8%8F-ezshops-%E2%9A%A0%EF%B8%8F-dynamic-pricing-gui-player-shops-sell-hand-inv.129780/']EzShops [/URL]buy/sell estimates directly into menu lore.[IMG]https://i.ibb.co/fzcXWVjq/ez-auction-shop-pricing.png[/IMG]
[/LIST]
[SIZE=4][B]Feature Highlights[/B][/SIZE]
[LIST]
[*][B]Configurable menus & translations[/B] – Override browser/confirm layouts, filler glass, and button text across the bundled English, Dutch, Spanish, and Chinese menu files.
[*][B]Flexible storage[/B] – Start on lightweight YAML or flip to production-ready MySQL with SSL flags, table prefixes, and shaded HikariCP pooling.
[*][B]Asynchronous history[/B] – Background save queues batch player history writes to keep gameplay responsive, even during heavy auction traffic.
[*][B]Player protections[/B] – Deposit refunds, balance checks, expiry sweeps, and inventory-aware delivery prevent dupes and lost currency.
[*][B]PlaceholderAPI hooks[/B] – Drop `%ezauction_*%` placeholders into scoreboards or holograms to surface active listings, orders, limits, or pending returns.
[*][B]Integration-ready API[/B] – Register Bukkit services to override listing caps or provide custom value providers without forking the plugin.
[*][B]Enchant-aware search[/B] – Type queries like [icode]sharpness v[/icode], [icode]fortune 3[/icode], or [icode]minecraft:looting[/icode] to surface listings and buy orders carrying the matching enchantments, including stored book enchants. Matching works with roman numerals, numeric levels, namespaced keys, and underscore/space differences so players can zero in on the exact enchant stack they need.
[/LIST]
[SIZE=4][B]Hologram Features[/B][/SIZE]
[LIST]
[*][B]Auction holograms[/B] – Place interactive holograms in the world to display live auction stats, top listings, or buy order summaries. Supports click/touch interaction for quick info and future actions.
[*][B]Performance optimized[/B] – Configurable max hologram count and batch update options ensure smooth gameplay even with many holograms active.
[*][B]Permission and proximity controls[/B] – Optionally restrict hologram visibility and interaction by permission node or player proximity (distance).
[*][B]Configurable types[/B] – Choose which auction stats or listings to display via hologram type selection.
[/LIST]

[spoiler="Hologram Configuration"]
[code=yml]
holograms:
  enabled: true
  update-interval-ticks: 100
  search-radius: 2.5
  height-offset: 1.75
  max-holograms: 100
  batch-update: true
  require-permission: false # Set true to require a permission node
  view-permission: ezauction.hologram.view # Permission node for viewing/interacting
  proximity-limit: false # Set true to restrict by distance
  proximity-distance: 32.0 # Max distance for interaction
[/code]
[/spoiler]

[SIZE=4][B]Quick Start[/B][/SIZE]
[LIST]
[*]Upload `EzAuction.jar`, restart your Paper or Purpur server, and confirm Vault plus an economy plugin are active.
[*]Edit [icode]plugins/EzAuction/auction.yml[/icode] for pricing rules, durations, and GUI slots, adjust [icode]auction-storage.yml[/icode] for YAML/MySQL persistence, and update [icode]auction-values.yml[/icode] or [icode]messages/menu-interactions_*.yml[/icode] as needed.
[*]Choose between YAML or MySQL storage, then grant players [icode]ezauction.auction[/icode] (and optional sell/order/history nodes) so they can browse and trade.
[*]Encourage `/auction history` so buyers and sellers can audit their latest activity right from chat.
[/LIST]

[SIZE=4][B]Live Auctions[/B][/SIZE]
[LIST]
[*][B]Queue the spotlight listings[/B] – Flip [icode]live-auctions.enabled[/icode] on (and [icode]queue-enabled[/icode] if you want a rolling lineup) so fresh sales are staged for broadcast and visible through [icode]/auction live[/icode].
[*][B]Hype them in chat[/B] – Keep [icode]display-in-chat[/icode] enabled to announce the next queued listing every [icode]announcement-interval-ticks[/icode] ticks using your Vault currency formatting.
[*][B]Curate from the live menu[/B] – The Live Auctions GUI shows queue position, seller names, price summaries, and quick actions to refresh or jump back into the main browser.
[/LIST]

[B]Commands & Permissions[/B]
[table]
[tr][th]Command[/th][th]Description[/th][th]Permission[/th][/tr]
[tr][td]/auction[/td][td]Open the auction browser, claim items, and cancel listings.[/td][td]ezauction.auction[/td][/tr]
[tr][td]/auction sell <price> [duration][/td][td]List the held item instantly or open the sell GUI.[/td][td]ezauction.auction.sell[/td][/tr]
[tr][td]/auction order <price> <amount> [duration][/td][td]Create a buy order with reserved funds.[/td][td]ezauction.auction.order[/td][/tr]
[tr][td]/auction cancel [id][/td][td]Review and cancel active listings or buy orders.[/td][td]ezauction.auction[/td][/tr]
[tr][td]/auction history [buy|sell][/td][td]Show the latest transactions for each category.[/td][td]ezauction.auction.history[/td][/tr]
[tr][td]/auction live[/td][td]Preview upcoming live auction broadcasts and refresh the queue.[/td][td]ezauction.auction.live[/td][/tr]
[tr][td]/auction claim[/td][td]Withdraw stored return items.[/td][td]ezauction.auction[/td][/tr]
[tr][td]/auction search <query>[/td][td]Open the auction browser filtered by item name, enchantment, or keyword.[/td][td]ezauction.auction.search[/td][/tr]
[tr][td]/auctionhologram <create|remove|list> [args][/td][td]Manage auction holograms: create, remove, or list in-world auction displays.[/td][td]ezauction.hologram.manage[/td][/tr]
[/table]

[SIZE=4][B]Setup Guide[/B][/SIZE]
[spoiler="Installation & Configuration"]
[B]Requirements[/B]
[table]
[tr][th]Requirement[/th][th]Notes[/th][/tr]
[tr][td]Java 17+[/td][td]Matches the Paper 1.21.4 API target used by EzAuction.[/td][/tr]
[tr][td]Paper or Purpur 1.21+[/td][td]The plugin relies on modern Paper server APIs.[/td][/tr]
[tr][td]Vault[/td][td]Provides the economy bridge for deposits, buy orders, and payouts.[/td][/tr]
[tr][td]Economy plugin (recommended: [URL='https://www.spigotmc.org/resources/1-21-ezeconomy-modern-vault-economy-plugin-for-minecraft-servers.130975/']EzEconomy[/URL])[/td][td]A Vault-compatible provider is required. EzEconomy is recommended for best compatibility and modern features.[/td][/tr]
[tr][td]Optional: [URL='https://www.spigotmc.org/resources/1-21-%E2%9A%A0%EF%B8%8F-ezshops-%E2%9A%A0%EF%B8%8F-dynamic-shops-gui-player-shops-sell-hand-inv.129780/']EzShops[/URL][/td][td]Unlock live value overlays via [icode]values.mode[/icode] = [icode]ezshops-buy[/icode] or [icode]ezshops-sell[/icode].[/td][/tr]
[tr][td]Optional: Custom limit resolver[/td][td]Expose [icode]AuctionListingLimitResolver[/icode] as a Bukkit service to scale listing caps.[/td][/tr]
[tr][td]Optional: Menu localisation overrides[/td][td]Duplicate the bundled `_nl`, `_es`, or `_zh` menu files to keep translations in sync with your branding.[/td][/tr]
[/table]

[B]Configuration Overview[/B]
[LIST]
[*][B]Storage selection[/B] – Toggle [icode]storage.type[/icode] between [icode]yaml[/icode] and [icode]mysql[/icode]; MySQL connections support SSL flags and pool tuning.
[*][B]Menus[/B] – [icode]menu.browser[/icode] and [icode]menu.confirm[/icode] control inventory sizes, filler panes, and navigation slots across all language variants.
[*][B]Sell & order editors[/B] – Configure default price suggestions and click adjustments for both listing and buy-order editors.
[*][B]Listing rules[/B] – Clamp durations, enforce minimum pricing, schedule custom duration options, and reserve refundable deposits, with automatic executor-based expiry sweeps.
[*][B]Menu interactions[/B] – Tweak click increments, confirmation buttons, and chat prompts through [icode]messages/menu-interactions_*.yml[/icode] per locale.
[*][B]Live auctions[/B] – Manage [icode]live-auctions.enabled[/icode], [icode]queue-enabled[/icode], [icode]display-in-chat[/icode], and [icode]announcement-interval-ticks[/icode] to stage listings for announcements or GUI previews.
[*][B]Value display[/B] – Display configured material prices or EzShops-sourced estimates, templated through [icode]values.format[/icode].
[/LIST]

[B]Optional Integrations[/B]
[LIST]
[*]Expose a custom [icode]AuctionListingLimitResolver[/icode] service to scale listing caps with ranks, islands, or other progression data.
[*]Install EzShops so the auction browser can display buy or sell prices directly in listing lore when configured.
[*]Install PlaceholderAPI to unlock `%ezauction_*%` placeholders for leaderboards, scoreboards, or Discord relays.
[/LIST]

[B]Network Setup (Bungee/Velocity)[/B]
[LIST=1]
[*]Switch [icode]storage.type[/icode] to [icode]mysql[/icode] and point each Paper/Purpur backend to the same credentials so listings, buy orders, and returns are stored centrally. The bundled MySQL driver implements [icode]DistributedAuctionListingStorage[/icode] so inserts and claims remain atomic across servers.
[*]Drop the same [icode]EzAuction.jar[/icode] on every gameplay server behind your proxy, then copy a single set of tuned configs ([icode]auction.yml[/icode], [icode]auction-storage.yml[/icode], [icode]messages/*.yml[/icode]) to keep menus, limits, and language consistent network-wide.
[*]Restart each backend (avoid /reload) after editing the configs so every instance warms its cache from MySQL before players join. The manager only serves listings once storage is marked ready.
[*]If a lobby should not show auctions, leave EzAuction off that node or point it at a different [icode]table-prefix[/icode] so it maintains a separate pool.
[*]Confirm every server shares the same Vault economy provider or synchronised balance backend. Currency withdrawals happen locally, so mismatched economy data across nodes can desync deposits and reserved funds.
[/LIST]
[/spoiler]

[spoiler="Bundled menu translations"]
[LIST]
[*]EzAuction ships with [icode]menu-layout_*.yml[/icode] and [icode]menu-interactions_*.yml[/icode] bundles for English, Dutch, Spanish, and Chinese (`_en`, `_nl`, `_es`, `_zh`).
[*]Copy any bundle to another locale tag (for example `_de`) to introduce custom language support without touching code.
[*]Keep button text consistent across languages so navigation slots and confirmation buttons remain aligned with the GUI configuration.
[/LIST]
[/spoiler]

[spoiler="Custom listing limit resolver example"]
[LIST=1]
[*]Create a lightweight helper plugin that loads before EzAuction (set [icode]loadbefore: [EzAuction][/icode] in its [icode]plugin.yml[/icode]).
[*]Implement [icode]com.skyblockexp.ezauction.api.AuctionListingLimitResolver[/icode] to compute the final slot total.
[*]Register the resolver with Bukkit's [icode]ServicesManager[/icode] during [icode]onEnable()[/icode] so EzAuction can discover it at startup.
[/LIST]
[code=java]
package com.example.auctionlimits;

import com.skyblockexp.ezauction.api.AuctionListingLimitResolver;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuctionLimitPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        AuctionListingLimitResolver resolver = (sellerId, baseLimit) -> {
            int bonus = getConfig().getInt("extra-slots." + sellerId.toString(), 0);
            return Math.max(0, baseLimit + bonus);
        };

        Bukkit.getServicesManager().register(
                AuctionListingLimitResolver.class,
                resolver,
                this,
                ServicePriority.Normal
        );
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
    }
}
[/code]

[code=yml]
name: AuctionLimitPlugin
version: 1.0.0
main: com.example.auctionlimits.AuctionLimitPlugin
loadbefore: [EzAuction]
[/code]

[code=yml]
# plugins/AuctionLimitPlugin/config.yml
extra-slots:
  123e4567-e89b-12d3-a456-426614174000: 2
  2a2f4982-0c7a-4f13-86a1-4f85c4a94cf7: 4
[/code]

This companion plugin registers before EzAuction enables and adds any configured bonus slots per player UUID on top of EzAuction's base limit. Update the UUID keys or compute the bonus however you like to reflect your network's progression rules.
[/spoiler]
[SIZE=4]
[B]Configuration at a Glance[/B][/SIZE]
[spoiler="auction.yml"]
[code=yml]
storage:
  type: yaml # or mysql
  mysql:
    host: localhost
    port: 3306
    database: skyblock
    username: root
    password: secret
    use-ssl: true
    table-prefix: ezauction_
    pool:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout-millis: 10000
      idle-timeout-millis: 600000
      max-lifetime-millis: 1800000
menu:
  browser:
    title: "&2Auction House &7({page}/{total_pages})"
    size: 54
    filler:
      material: GRAY_STAINED_GLASS_PANE
      display-name: "&8 "
    navigation:
      previous-slot: 45
      close-slot: 49
      next-slot: 53
    empty-listing-slot: 22
  confirm:
    title: "&2Confirm Purchase"
    size: 27
    filler:
      material: GRAY_STAINED_GLASS_PANE
      display-name: "&8 "
    confirm-slot: 11
    listing-slot: 13
    cancel-slot: 15
listings:
  default-duration-hours: 24
  max-duration-hours: 72
  minimum-price: 10.0
  listing-deposit-percent: 5.0
  max-listings-per-player: 3
live-auctions:
  enabled: false
  queue-enabled: true
  display-in-chat: true
  announcement-interval-ticks: 200
values:
  enabled: false
  format: "&7Value: &6{value}"
  materials:
    diamond: 2500.0
[/code]
[/spoiler]
[spoiler="auction-storage.yml"]
[code=yml]
type: yaml # or mysql
mysql:
  host: localhost
  port: 3306
  database: skyblock
  username: root
  password: secret
  use-ssl: true
  table-prefix: ezauction_
  pool:
    maximum-pool-size: 10
    minimum-idle: 2
    connection-timeout-millis: 10000
    idle-timeout-millis: 600000
    max-lifetime-millis: 1800000
[/code]
[/spoiler]
[spoiler="auction-values.yml"]
[code=yml]
enabled: false
mode: configured # or ezshops-buy / ezshops-sell
format: "&7Value: &6{value}"
materials:
  diamond: 2500.0
[/code]
[/spoiler]
[SIZE=4]
[B]Support & Links[/B][/SIZE]
[LIST]
[*]Need help? Route players to your Discord, website, or ticket bot and mention EzAuction for faster triage.
[*]Share update logs, bug reports, or feature requests on your resource discussion thread to keep the roadmap flowing.
[*]Document any custom listing-limit resolvers so staff know which ranks or island levels unlock extra slots.
[*]Direct support: [URL='https://discord.gg/yWP95XfmBS']Join our Discord server[/URL].
[*][URL='https://www.spigotmc.org/resources/1-21-ezeconomy-modern-vault-economy-plugin-for-minecraft-servers.130975/']EzEconomy: Modern Vault Economy Plugin[/URL]
[*][URL='https://www.spigotmc.org/resources/1-21-%E2%9A%A0%EF%B8%8F-ezshops-%E2%9A%A0%EF%B8%8F-dynamic-shops-gui-player-shops-sell-hand-inv.129780/']EzShops: Dynamic Player Shops & GUI[/URL]
[/LIST]

[CENTER][SIZE=4][B]Ready to modernize your marketplace?[/B]
Deploy EzAuction today and give your players a polished, automated trading hub![/SIZE][/CENTER]