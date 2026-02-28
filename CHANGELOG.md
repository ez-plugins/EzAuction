# Changelog

## Unreleased

- Refactor: migrate storage implementations to `AuctionListingRepository` and `AuctionHistoryRepository` interfaces.
- Remove legacy `AuctionStorage`/`AuctionHistoryStorage` interfaces and adapter classes.
- Update components to register and consume repository-typed services via EzFramework `Registry`.
- Internal: cleanup and tests; all unit tests pass.
