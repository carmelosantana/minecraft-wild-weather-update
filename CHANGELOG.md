# Changelog

All notable changes to Wild Weather Update are documented here.

## 1.0.2 - 2026-07-20

### Fixed

- `/weather trigger` and `/weather force` now resolve Bedrock players who joined
  through Floodgate. Floodgate prefixes a Bedrock account's Java-side username with
  `.`, and `Bukkit.getPlayer` matches the start of the name, so typing the bare name
  never found the prefixed account. Both sites now try the prefixed form as well.
- The "player not found" message now lists who is currently online, which is the only
  way a Bedrock player can discover the prefixed username -- Geyser sends no
  command-suggestion packets, so Bedrock clients get no tab completion at all.

### Changed

- Test dependency migrated from JUnit 4 to JUnit 5 (`junit-jupiter` 5.10.0) and
  `maven-surefire-plugin` 3.5.2 declared, so tests actually run during `verify`.

## 1.0.1 - 2026-07-19

### Fixed

- SHA256SUMS.txt now records bare JAR filenames instead of the build-time
  `target/` path, so `sha256sum --check` works against downloaded release assets.

## 1.0.0 - 2026-07-13

### Changed

- Updated the build baseline to Paper 26.1.2 and Java 25.
- Updated Maven compiler and shading plugins for Java 25 bytecode.
- Added GitHub Actions for tests, release JARs, SHA-256 checksums, and tagged releases.
- Verified plugin startup and command registration on the current server stack.

### Tested

- Paper 26.1.2 build 74
- Geyser 2.11.0
- Floodgate 2.2.5 build 138
- ViaVersion 5.11.0
