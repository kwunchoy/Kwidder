# Changelog

## 2026-07-23

### Added

- Added multiple line-item frequency cap rules, with one rule each for day, ISO week, and calendar month.
- Added persistent per-identity bid-response counters that reset when each configured calendar window changes.
- Added a UI frequency-cap editor with add and remove controls and Day, Week, and Month options.
- Added tests for daily, weekly, and monthly reset boundaries and bidder enforcement.

### Changed

- Updated line-item eligibility so every configured frequency cap must allow a bid.
- Preserved compatibility with line items that use the earlier lifetime frequency cap.
- Updated the README with frequency-cap window and reset behavior.
