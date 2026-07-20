# New or Edited Plugin Checklist

Leave an unchecked box with a short explanation when a gate is not complete; do not silently remove
inapplicable checks.

- Plugin name: `Wild Weather Update`
- Slug: `wild-weather-update`
- Owner: `Carmelo Santana`
- Target version: `1.0.2` (patch; preceded by `1.0.1`)
- Paper version: `26.1.2 build 74`
- Java version: `25`
- Updater destination: `wild-weather-update.jar`
- External services: `none`
- Status: `active`
- Autonomy: `autonomous`

Maven `artifactId`: `wild-weather-update`. `plugin.yml` name: `WildWeatherUpdate`.
Releasable JAR: `wild-weather-update-<version>.jar`.

This file was created retroactively for an already-shipped, already-released plugin. It records the
**current real state**, not a plan for new work. Only the `1.0.2` bug fix described below was
performed in this pass; gates not exercised by that fix are left unchecked with a note.

## 1. Scope

- [x] Status is explicitly recorded as active.
- [x] Scope of this change is defined: fix Floodgate/Bedrock player-name resolution in
      `/weather trigger` and `/weather force`.
- [x] Known limitations recorded (see below).

### What changed in 1.0.2

`WeatherCommand.handleTriggerCommand` and `WeatherCommand.handleForceCommand` resolved a typed
player name with `Bukkit.getPlayer(String)`. Floodgate joins a Bedrock account under a `.`-prefixed
Java-side username (a player who calls themself `carm` is `.acarm`), and `getPlayer` prefix-matches
the *start* of the name, so `getPlayer("carm")` never matched `.acarm`. Both sites now call
`PlayerLookup.resolveAllowingPartial`, which tries the bare name, then the `.`-prefixed form, then a
case-insensitive sweep, then Bukkit's own partial match as a final tier (preserving the pre-existing
partial-matching behaviour). Failure now reports who is online via `PlayerLookup.noSuchPlayerMessage`.

`WeatherManager` lines 144/159 also call `Bukkit.getPlayer`, but take a name from a third-party
plugin through the public `WeatherAPI` rather than from user input. **Deliberately out of scope and
unchanged.**

### Known limitations

- The Floodgate prefix is hardcoded to Floodgate's `.` default rather than read from config. A
  server that reconfigured it still resolves through the case-insensitive sweep.
- Tab completion in `onTabComplete` still prefix-matches on `player.getName()`, so it offers the
  prefixed name only once the operator types the leading `.`. Not changed in this pass; Bedrock
  clients receive no command suggestions from Geyser regardless, which is why the failure message
  lists online names.

## 2. Repository

- [x] Own git repository, `main` branch, clean tree at the start of this change.
- [ ] Remote/SSH `origin` configuration not inspected in this pass.

## 3. Metadata

- [x] AGPL-3.0-or-later `LICENSE` and Maven license metadata present and consistent.
- [x] `https://xpfarm.org` URL metadata present in `pom.xml`; `plugin.yml` author `xpfarm.org`.
- [x] `org.xpfarm` Maven group.
- [x] Slug, artifact, and `plugin.yml` name are consistent.
- [x] No secrets introduced by this change.
- [ ] Full history secret scan not performed in this pass.

Note: no source file in this repository carried a license header before this change. The new
`PlayerLookup.java` and `PlayerLookupTest.java` carry the ecosystem-standard AGPL header, copied in
style from the sibling `electric-furnace` repository since no in-repo example existed. Existing
files were left as they were.

## 4. Compatibility

- [x] Java 25 / Paper 26.1.2 build 74 compile succeeds — `mvn clean verify` green (see §6).
- [x] No dependency changes: the fix adds no hard or soft dependency. `floodgate` is **not** added
      as a `softdepend` — the fix is pure string handling and never touches `FloodgateApi`.
- [ ] Geyser/Floodgate/ViaVersion behavioural review not re-run on a live stack this pass. The
      change is specifically a Bedrock-facing fix but was verified only by unit test.

## 5. External services

- [x] No external integrations exist in this plugin. Not applicable.

## 6. Tests and build

- [x] Unit tests cover the separable logic. **6 tests added — the repository had none.**
      `PlayerLookupTest` covers `targetNameCandidates` and `noSuchPlayerMessage`.
- [x] `mvn --batch-mode --no-transfer-progress clean verify` succeeds:
      `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`, producing
      `wild-weather-update-1.0.2.jar`.
- [x] The releasable JAR and embedded `plugin.yml` were inspected; `original-*` JARs are excluded.
      Verified by unzipping the built JAR. Embedded `plugin.yml` reads `version: '1.0.2'`,
      `api-version: '1.21'`, `main: org.xpfarm.wildweather.WildWeatherPlugin`. Bytecode major
      version of the first `.class` entry is **69 (Java 25)**, matching the ecosystem standard.

      **Exclusion is at the CI release-asset step, not at build time.** `target/` contains both
      `wild-weather-update-1.0.2.jar` and `original-wild-weather-update-1.0.2.jar` — the
      `original-*` JAR *is* still produced locally. It is excluded from released assets by
      `.github/workflows/build.yml`, which filters `! -name 'original-*'` on both the SHA256SUMS
      step and the `gh release upload` step (and excludes `!target/original-*.jar` from the
      uploaded build artifact). So no `original-*` JAR can reach a release, but one does exist on
      disk after a local build.

      `maven-shade-plugin` is a **no-op** here: every dependency is `provided`/`test` scope, so it
      shades nothing and exists only to rename the untouched jar, which is what creates the
      `original-*` file. `agua-de-florida` resolved this by removing shading entirely; doing the
      same here is out of scope for this change.

Test infrastructure was created as part of this change: the repository had no `src/test/java` and
its only test dependency was JUnit 4 with no surefire declared, so no test could have run. JUnit 4
was verified unreferenced by any source and removed; `org.junit.jupiter:junit-jupiter` 5.10.0 and
`maven-surefire-plugin` 3.5.2 were added, matching `electric-furnace`.

### Test coverage note — what is and is not covered

`PlayerLookup.resolve` and `PlayerLookup.resolveAllowingPartial` are **not** unit-tested. Both call
`Bukkit` statics that cannot be constructed headlessly and no MockBukkit dependency exists or was
added. Only the pure string functions are pinned. The tiering logic inside `resolve` — exact before
case-insensitive before partial — is therefore unverified by test.

## 7. Matrix

### 7a — single-plugin runtime verification (`1.0.2`) — PARTIAL

Evidence below comes from a **single disposable Legendary stack run on 2026-07-20**
(image `05jchambers/legendary-minecraft-geyser-floodgate:latest`) with **all six fixed plugin
JARs mounted together**. The same run backs the gate 7a note in all six repositories.

- [x] Paper, Geyser, Floodgate, and ViaVersion start successfully together. **Verified.** Paper
      reached `Done (18.178s)! For help, type "help"`. The Java port answered a real Minecraft
      protocol handshake — not merely a TCP connect — reporting `Paper 26.1.2 | protocol 775` and
      `PLAYERS: 0 / 20`. `/plugins` reported 9 plugins, all green/enabled: AguaDeFlorida, floodgate,
      Geyser-Spigot, GlutenFreeBread, StarterPack, TheCurse, ViaVersion, WildWeatherUpdate,
      WorldCRUD. Companion versions observed: floodgate v2.2.5-SNAPSHOT (b138-fc99cfc),
      Geyser-Spigot v2.11.0-SNAPSHOT (Geyser 2.11.0-b1200), ViaVersion present; Geyser started on
      UDP port 19200. Each plugin enabled at its new version with **zero exceptions, errors, or
      SEVERE lines attributable to any of the six** — including `Enabling WildWeatherUpdate v1.0.2`.
- [ ] Java and Bedrock smoke tests cover joins plus affected commands, events, permissions,
      persistence, and reloads. **PARTIAL — the Java side was exercised, the Bedrock side was not.
      Left unchecked deliberately.**

      *What was exercised.* The **Floodgate prefix assumption was confirmed empirically, not merely
      from documentation**: reading `/minecraft/plugins/floodgate/config.yml` inside the running
      container on the Floodgate 2.2.5 build showed `username-prefix: "."` and
      `replace-spaces: true`, alongside the shipped comment "Floodgate prepends a prefix to bedrock
      usernames to avoid conflicts". The `.` prefix this fix depends on is now **observed on the
      actual runtime, not assumed** — the single most important upgrade to the evidence.

      The **new failure path was then exercised end-to-end over RCON on the live server** for every
      fixed command across all six plugins — `/aguadeflorida give carm`, `/curse start carm`,
      `/curse book carm`, `/worldcrud listpermissions carm`, `/starterpack give carm`,
      `/gfbread clear carm`, and `/weather trigger rain carm` — and each returned the new
      message with no exception: exactly `No player matches 'carm'; no players are online.` This proves that
      `PlayerLookup.resolve` / `resolveAllowingPartial` / `onlineNames` / `noSuchPlayerMessage`
      actually execute correctly against real Bukkit APIs, that command dispatch reaches them, and
      that the message renders — none of which the unit tests could show.

      *What remains unverified.* **The positive match is still unproven.** No real Bedrock client
      was available, so no player with a `.`-prefixed Java-side username ever joined. What is
      verified is that the resolution path runs without error and that the not-found branch is
      correct; that `/weather trigger rain carm` actually **finds** a Bedrock player named `.acarm` has
      **not** been observed. Only the empty-online-list branch of `noSuchPlayerMessage` was
      exercised; the branch that lists online player names was not. The operator will verify live on
      the dev server with helpers. `resolve` / `resolveAllowingPartial` still have **no unit-test
      coverage** (Bukkit statics, no MockBukkit).

### 7b — ten-plugin ecosystem matrix — NOT RUN

- [ ] Out-of-band; no updater manifest entry or dependency changed.

## 8. CI/CD

- [x] Standard plugin Actions workflow present at `.github/workflows/build.yml` from `1.0.0`.
- [ ] Successful main Actions run for this commit — not applicable yet; the change sits on
      `fix/floodgate-name-resolution` and has not been pushed.

## 9. Release

- [ ] Not released. `pom.xml` is at `1.0.2` and `CHANGELOG.md` has its entry, but no tag, no push,
      no GitHub release.

## 10. Updater

- [x] Already enrolled from a prior release; this patch changes no manifest field.
- [ ] Updater install/upgrade behaviours not re-verified for `1.0.2`.

## 11. Deployment

- [ ] Not deployed. `1.0.1` remains the deployed version.

## 12. Handoff

- [ ] `CURRENT_STATE.md` not updated for `1.0.2`.

**Follow-up owner:** whoever releases `1.0.2` must run gate 7a with an actual Bedrock client and
confirm `/weather trigger <bare-name>` and `/weather force <event> <bare-name>` reach a
Floodgate-prefixed player. That is the one claim this change cannot make from the build alone.
