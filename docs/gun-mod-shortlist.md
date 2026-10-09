# Gun mod shortlist (issue #3)

> **Status: APPROVED for prototyping (2026-10-09).** The owner approved the unofficial TaCZ NeoForge port as the
> gun mod to prototype, with Superb Warfare as the fallback. That approves a direction to test. It does not show
> the port works on NeoForge 21.1.256, and it grants no permission to publish or redistribute it. No mod has
> been installed and `pack/` is unchanged.

## Decision

- **Approved:** prototype with the unofficial TaCZ NeoForge port (`tacz-1.21.1`, 1.1.8-hotfix-r7 [S5]). Fall
  back to Superb Warfare (0.8.9.2 [S1]) if the port fails the load experiment, the license and export check,
  or the owner's playtest.
- **Approval:** the owner, 2026-10-09, in the Orca coordinator conversation for issue #3: "Let's do TacZ
  fashooo". The coordinator passed it to the closeout session.
- **Not covered by the approval:** tested compatibility, a place in `pack/`, publishing or redistributing the
  port or its assets, and gun art.
- **Evidence kept:** the comparison, Ghost Core analysis, experiment plan and sources below are the evidence
  for the recommendation and stay as written. Edits since approval: this section and the status; "Proposed"
  changed to "Approved" in the owner summary; the owner's answers; issue links in place of the F1–F7
  placeholders, with tracking notes; and E4 and the downed-lock row reworded as open questions rather than
  expected failures.
- **Still to verify:**
  - [#9] recorded owner limits on license, asset and export terms (R9, R10), with route verification
    and no-default-pack-resource proof still required before publishing;
  - [#10] load and integration experiment (E1–E6);
  - [#14] owner playtest;
  - [#15] server performance.
- **Gates:** [#9] and a passing E1 in [#10] gate pack integration ([#12], which also needs the [#4] baseline)
  and gun art ([#16]). [#11] is needed only if E4 in [#10] finds a downed player can fire.
- **R9/R10 research checkpoint (2026-10-09):** see [TaCZ license, assets and export terms](tacz-license-terms.md).
  The separate CurseForge port project and r7 file are verified [S24]. Code license, asset statements,
  source dependencies and format-specific exports are documented; exact asset scope, explicit author
  modpack permission and a fully independent custom pack remain unanswered. Two owner answers were recorded
  on 2026-10-09: accept documented terms/unknowns for packwiz or Modrinth download references, defer bundled
  server ZIPs, and approve original files only with no default-pack resource dependency proved before publishing.
- **Centrifuge brain record:** prepared, not yet recorded. The brain writer refused it because its map census
  does not list a `lazarus-clause` project yet; details are in the closeout PR.

Lazarus Clause needs a gun mod for its "shooter half" (see the pillars in `README.md`). The target is
Minecraft 1.21.1 on NeoForge 21.1.256. This document compares the gun mods whose Modrinth metadata lists
a NeoForge 1.21.1 build, checks how each would interact with Ghost Core (resurrection, the downed state,
scans, transmat), and proposes one direction plus the experiment that would test it.

Method: a `mod-scout` pass (Haiku, high) pulled version lists from the Modrinth API with NeoForge + 1.21.1
filters on 2026-10-09 and spot-checked CurseForge. That metadata shows a build is *listed*, not that it
works on NeoForge 21.1.256; no candidate has been loaded or tested. A judgment pass (Opus 5.5, xhigh) then
read Ghost Core's code and the NeoForge 21.1.256 sources. Facts from the scout carry a source tag like [S5]
(the list is at the end). Facts from local code carry a file and line. Anything without either is marked
**unverified**.

---

## Owner summary

**Approved prototype direction: the unofficial TaCZ NeoForge port (`tacz-1.21.1`, 1.1.8-hotfix-r7,
2026-10-01 [S5]).** Treat it as a gun platform and fill it with our own Company-branded sci-fi gun pack.
Its NeoForge 1.21.1 build is listed on Modrinth; it has not been loaded on NeoForge 21.1.256 yet.

- It is the only candidate verified to take new guns as data packs (`.zip`, JSON) rather than as mods [S6],
  and a third-party gun pack (a Fallout port) already exists for it [S20]. That would make original sci-fi
  guns a content job rather than a fork, which suits the pack's "original art, no Bungie assets" rule.
- It lists no mandatory dependencies [S6], has public GPL-3.0 code [S7] we can read and hook, and lists
  attachments with workbench crafting [S6].
- Tradeoffs: it is an **unofficial port** (upstream TaCZ is Forge 1.20.1 only [S8][S9]). The approved r7
  release is listed on Modrinth [S5] and the separate CurseForge port project [S24]; cross-host byte
  identity is unchecked. The imported original asset statement and default-pack metadata say
  **CC BY-NC-ND 4.0**. [R9/R10 findings](tacz-license-terms.md) separate platform inclusion guidance,
  reference/download exports, bundled server ZIPs and unresolved asset scope. The owner's 2026-10-09
  acceptance/direction is limited to packwiz or Modrinth download references and original files with
  no default-pack resource dependency proved before publishing; bundled server ZIPs are deferred.
  This report draws no legal conclusion and records no authors' permission.

**Approved fallback: Superb Warfare (0.8.9.2, 2026-09-30 [S1]).** It is listed on both Modrinth and
CurseForge, with GPL-3.0 code and a perk system [S2][S4]. It ranks second because its asset metadata says
All Rights Reserved [S2], it lists about six dependencies [S22], block destruction is on by default [S2],
and its 1.21.1 source is not confirmed [S3]. Switch to it if the TaCZ port fails the experiment, the
license check or the owner's playtest.

**Not verified for any candidate:** rarity tiers or affixes, an ammo economy, and server performance. The
sources checked did not document them. That is missing information, not evidence the features are absent
(R3, R5, R6). Plan for rarity and affixes to be our own layer unless research shows otherwise, which makes
open, hookable code the deciding factor.

**Checks to run first.** None of these blocked approving the prototype direction, and all three run in a dev
workspace with `pack/` untouched:
1. **E1 load test ([#10]):** boot the GameTest server with Ghost Core and the port on NeoForge 21.1.256, and
   pass the existing 8 Ghost Core tests. This turns "listed" into "tested".
2. **R9 and R10 license and export check ([#9]):** read the [source report](tacz-license-terms.md), including
   the two separate owner decisions recorded on 2026-10-09. Unanswered scope/permission/dependency questions
   remain listed. No default-file copying/reuse is approved; the TaCZ jar's engine dependency may remain.
   Route verification and original-pack proof remain publication gates; the separate E1 networking
   integration failure still blocks [#12] and [#16]. The decisions do not unconditionally unblock either.
3. **E4 downed-fire test (with R2, [#10]):** find out whether a downed player can still fire. A failure does
   not rule out the direction; it starts Ghost Core follow-up [#11].

**Asked of the owner, and the answers (2026-10-09):**
1. Approve the TaCZ port as the direction to prototype, or pick another row. **Approved: the TaCZ port, with
   Superb Warfare as the fallback.**
2. Accept relying on an unofficial, Modrinth-hosted port for the prototype. This is not a decision about
   where the pack will be published. **Accepted for the prototype only.**
3. Playtest gun feel in a throwaway Prism instance (checklist below). No source measured feel, so this
   is the owner's call. **Not done yet: [#14].**
4. Approve the integration experiment as the next issue. It runs in a child workspace and does not touch
   `pack/`. **Created as [#10]; not started.**

---

## Shortlist (NeoForge 1.21.1 builds listed on Modrinth; none tested on 21.1.256)

| Mod | Version (date) | Code / asset license | Required deps | Source code | Ecosystem (listed for 1.21.1 NeoForge) | Fit notes |
|---|---|---|---|---|---|---|
| **TaCZ NeoForge port** (unofficial) | 1.1.8-hotfix-r7 (2026-10-01) [S5][S24] | GPL-3.0-only Modrinth code metadata; imported asset statement and default-pack metadata CC BY-NC-ND 4.0; scope/owner acceptance pending ([report](tacz-license-terms.md)) | None mandatory; Cloth Config for data JSON [S6] | Yes, MUKSC/TACZ-1.21.1 [S7] | TACZ Turrets (MIT), TACZ Durability (GPL-3.0, jamming), Tactical Breaching (GPL-3.0), Elite X Quality Guns (ARR), Fallout gunpack port (ARR), ViewModel Tuner (ARR) [S20] | Gun packs as `.zip`, attachment UI, workbench crafting [S6]. Separate CurseForge port verified [S24]; byte identity and exact export contents unchecked. See [R9/R10](tacz-license-terms.md) for routes and limits. 1.20.1 worlds are incompatible (irrelevant for a new pack). |
| **Superb Warfare** | 0.8.9.2 (2026-09-30) [S1] | GPL-3.0-only code; asset metadata ARR [S2] | Kotlin for Forge, GeckoLib, Curios, Cloth Config, Patchouli, geckoanimfix, plus one unresolved; required vs optional unconfirmed [S22] | Repo exists [S3]; README still describes Forge 1.20.1, 1.21.1 branch unconfirmed | SBW Mini Auto Turret, Block Expansion [S20] | Perks; block destruction on by default (`explosion_destroy` config); not Arclight-compatible [S2]. On CurseForge too [S4]. |
| **Vic's Point Blank** | 2.2.0 (2026-09-11) [S10] | ARR [S11] | None listed [S11] | No public repo | Cyberpunk 2077 Guns (ARR; a 1.20.1 build listed for 1.21.1) [S21] | About 40 guns, fire modes, reload/inspect animations, accuracy changes while moving and aiming [S11]. CurseForge shows only a Fabric 1.21.1 file [S12]. |
| **BlockFront** | 0.9.0.41b (2026-10-08) [S13] | ARR [S13] | None listed | No public repo | Not checked | Guns, explosives, vehicles. Feature detail and CurseForge not checked. |
| **Fel's Machine Guns II** | 0.2.9 (2026-08-10) [S18] | ARR [S18] | GeckoLib (MIT), Iguana Lib (ARR) [S22] | No | Needs addons for content [S18][S19] | Heavy machine guns. A base mod, not a full arsenal. |
| **Scorched Guns Neoforged** (unofficial port) | 1.5 (2026-06-22) [S14] | GPL-3.0-or-later code; upstream asset license unresolved [S14][S15] | Conflict: Modrinth lists none, README lists Framework, Curios, GeckoLib [S15] | Yes [S15] | Not checked | Upstream described as having "custom ammunition systems." Oldest latest release on the list. |
| **Just Enough Guns New** (unofficial fork) | 1.8.2 (2026-10-07) [S16] | Metadata GPL-2.0-only vs README GPL-3.0; assets ARR by MigaMi "used with explicit authorization"; some Superb Warfare-derived assets CC BY-NC-SA 3.0 [S16][S17] | GeckoLib [S16] | Yes [S17] | Not checked | License provenance is the messiest on the list. |

**Excluded:** official TaCZ (Forge 1.20.1 only [S8]); Orbital Railgun (a strike weapon, not a handheld gun; files unverified [S23]); Laser Toys (listed for NeoForge 26.1.2, not 1.21.1); several names seen only on mirror sites; Guns++, Modern Guns and Gamingbarn's (datapacks, not mods).

### Looter-shooter fit

| | TaCZ port | Superb Warfare | Vic's Point Blank |
|---|---|---|---|
| Gun feel | Unverified, needs owner playtest | Unverified, needs owner playtest | Unverified; page lists fire modes, animations and an accuracy model [S11] |
| Attachments | Yes, with UI [S6] | No verified information | No verified information |
| Rarity / affixes | No verified information. Elite X Quality Guns addon exists, features unverified [S20] | Perks [S2]; closest thing to affixes found | No verified information |
| Ammo economy | No verified information (not on the Modrinth page [S6]) | No verified information (scout: "No ammo or progression documented" [S2]) | No verified information |
| Server performance | No verified data; no reports found | No verified data; no reports found | No verified data; no reports found |
| Sci-fi fit | No sci-fi guns found. Custom gun packs are the route to an original Company arsenal [S6][S20], subject to R9 | No sci-fi guns found; asset metadata ARR [S2], reuse permission unverified | The only sci-fi-adjacent content found is an IP-branded Cyberpunk 2077 pack [S21], which conflicts with the original-content rule |
| Hookability for our rarity, ammo and downed layers | Best: GPL source [S7] and data-driven stats [S6] | Medium: GPL, but the 1.21.1 source is unconfirmed [S3] | Poor: closed source |
| Hosting (verified) | Modrinth [S5]; separate CurseForge port [S24]. Export behavior/limits in [R10](tacz-license-terms.md) | Modrinth and CurseForge [S2][S4] | NeoForge on Modrinth; CurseForge page showed only Fabric [S12] |

**Why not a combination?** Two gun mods would mean two ammo systems, two HUDs, two sets of keybinds and two
balance passes. One platform plus addons from its own ecosystem (for example TACZ Turrets later) keeps the
looter layer simple.

---

## Ghost Core integration (read from local code)

Paths are under `mods/ghostcore/src/main/java/com/iampaycheck/ghostcore/`. NeoForge references are to
`neoforge-21.1.256-sources.jar`.

| Ghost Core behaviour | What the code does | What a gun mod changes | Status |
|---|---|---|---|
| Resurrection trigger | `LivingDeathEvent` at LOW priority cancels death, spends a charge, sets downed (`ghost/Resurrection.java:46-78`). `ServerPlayer.die` posts that event (`ServerPlayer.java:687`), and `LivingEntity.hurt` calls `die` at health 0 (`LivingEntity.java:1260-1266`). | Any gun kill that goes through `hurt()` downs the player instead of killing them. Kills via `kill()` (`GENERIC_KILL`) skip the Ghost by design (`Resurrection.java:51`). A mod that sets health to 0 directly would bypass the Ghost. | Mechanism verified; each mod's damage path unverified (experiment E2) |
| Downed invulnerability | `LivingIncomingDamageEvent` is cancelled while downed (`Resurrection.java:150-155`). NeoForge fires it inside `hurt()` before any damage applies (`LivingEntity.java:1153`). | Bullets that call `hurt()` do nothing to a downed player. | Mechanism verified; per-mod check is E3 |
| Downed action lock | Cancels only vanilla interaction events: attack, right-click item/block, left-click block, entity interact (`Resurrection.java:162-189`). The check is server-side only (`:144-146`). | Gun mods that fire through their own network packets would **not** be stopped, so a downed player could keep shooting. Client-side recoil and sound would also still play. | **Possible gap, not confirmed.** The fire path is unverified for every candidate (E4, [#10]) |
| Mob targeting while downed | `LivingChangeTargetEvent` is cancelled (`Resurrection.java:157-160`). | Covers mobs that use vanilla targeting. Turrets or gun-wielding mobs with their own targeting may ignore it. | Unverified per addon |
| Revive tuning | Revive restores 50% health by default and gives Resistance II for 3 s (`Resurrection.java:106-110`, config `reviveHealthFraction`). | Gun damage per second sets how useful that window is. This needs a balance pass. | Design follow-up |
| Revive hook for the pack | `GhostReviveEvent` carries the `DamageSource` and is cancellable (`api/GhostReviveEvent.java:13-24`). | The pack can make specific gun damage types bypass the Ghost (for example a Company "termination round") without changing Ghost Core. | Verified API; gun damage type IDs unverified |
| Relocation | Only environmental deaths teleport you to safe ground (`Resurrection.java:81-91`). | Gun deaths revive in place. Expected and fine. | Verified |
| Transmat | Cancelled by any damage (`LivingDamageEvent.Post`, `ghost/Transmat.java:110-116`) or by moving more than 0.75 blocks (`:63`). | Gun hits and strong knockback cancel a channel. Recoil, which only rotates the camera, does not. A player can fire while channeling; whether that is allowed is a design call. | Verified mechanism |
| Scan: hostiles | Marks `LivingEntity` that `instanceof Enemy` (`ghost/Scanner.java:55-57`). | Gun-mod turrets, vehicles or armed mobs show red only if they implement `Enemy`. Bullet entities are not `LivingEntity`, so they never show. | Unverified per addon |
| Scan: caches | Marks any `RandomizableContainer` with a loot table (`Scanner.java:70`). | Guns or ammo placed in loot tables show up as gold caches with no Ghost Core change. A gun mod's own crates only show if they use that interface. | Verified for vanilla containers |
| Scan rendering | Draws at `AFTER_LEVEL` and resets the model-view matrix itself (`client/ScanHighlights.java:63, 97`). | Should survive scope FOV zoom. Scope overlays or custom first-person passes may hide the lines. | Owner playtest |
| HUD and keys | Panel is top-left via `registerAboveAll` (`client/GhostHud.java:25`). Keys G/H/J/K (`client/GhostKeys.java:17-20`). | Gun ammo HUDs and default keys may overlap. Gun-mod defaults are unknown. | Owner playtest; key defaults are research item R7 |
| Ghost drone in the line of fire | `GhostEntity` keeps vanilla `isPickable() == false` (`Entity.java:1667`) and ignores damage (`ghost/GhostEntity.java:164-171`). Vanilla projectiles skip non-pickable entities (`Projectile.java:257-263`). | A gun mod with its own raycast that does not filter on `isPickable` could stop bullets on your own Ghost over your right shoulder. | Unverified (E6 / playtest) |
| Ghost Cache | Stores any `ItemStack` via `ItemStack.OPTIONAL_CODEC` (`ghost/GhostData.java:33`). | Guns with attachment or ammo data components should round-trip. | Expected; E5 confirms |
| GameTests | Mock players have no real connection, so Ghost Core sends packets only through `GhostNetwork.send` (`network/GhostNetwork.java:22-23`). Mock players are in creative (`AGENTS.md`). | A gun mod may send packets to mock players unguarded and throw. Creative may skip ammo use. | Experiment design constraint |

The client already receives `downedUntil` (`network/GhostSyncPayload.java:27`), so if E4 confirms the gap, a
fix ([#11]) can also suppress fire input on the client while downed.

---

## Verified, uncertain, and for the owner

**Verified (by source or local code):** that Modrinth lists a NeoForge 1.21.1 build, with the version and date
shown, for all seven rows (a listing, not a load test); the license metadata as the scout recorded it; that
the TaCZ port lists no mandatory dependencies and loads gun packs as `.zip`; that the official TaCZ
CurseForge page showed no NeoForge file; Superb Warfare's dependency list and block-destruction default;
and the "What the code does" column of the Ghost Core table.

**Uncertain:**
- *Version:* Modrinth metadata establishes listed availability, not tested compatibility. The scout did not
  read each jar's minimum NeoForge version, and no candidate has been loaded. Compatibility with 21.1.256
  is unproven until E1.
- *Maintenance:* the TaCZ port is unofficial (MUKSC/TACZ-1.21.1 [S7]) and upstream has no NeoForge build to
  fall back on [S8]. GPL-3.0 means we could fork it if it stalls, but that would be real work.
- *License, distribution and export:* [R9/R10 research](tacz-license-terms.md) records GPLv3 code, broad
  imported CC BY-NC-ND 4.0 asset statements and matching default-pack metadata, with exact file scope
  unresolved. No explicit author modpack statement was located; Modrinth supplies separate hosted-file
  guidance. Format capabilities do not settle permissions. The [recorded owner decisions](tacz-license-terms.md)
  accept a limited project direction with publication gates; this report gives no legal conclusion or
  authors' permission. Superb Warfare's ARR asset metadata, JEG New's GPL-2.0 vs GPL-3.0
  conflict and Scorched Guns' asset license remain unresolved.
- *Hosting:* the separate CurseForge TaCZ port project and NeoForge 1.21.1 r7 file are now verified [S24].
  A client manifest route exists in the documentation; byte identity, exact exporter metadata/contents
  and submission acceptance are unchecked. See [format-specific findings](tacz-license-terms.md).
- *IP:* the only sci-fi-adjacent content found is a Cyberpunk 2077 gun pack [S21], and the TaCZ ecosystem
  includes a Fallout gunpack port [S20]. Both are third-party IP, which the publishable-pack rule should treat
  the same way as Bungie's.
- *Performance:* no verified data. The scout's searches found no measured reports, which says nothing either
  way about actual performance.
- *Ammo, rarity and fire path:* no verified information for any candidate (see the research request below).

**Needs the owner's playtest:** gun feel (recoil, aim-down-sights, reload, hit feedback, sound), scan
highlights while scoped, HUD overlap, key conflicts, the Ghost drone blocking your own shots, and FPS.

---

## Proposed integration experiment (not run; tracked in [#10])

This is the cheapest test that would show the TaCZ port and Ghost Core coexist. Run it in a child workspace
under [#10], with the gun jar as a dev-only runtime dependency behind an opt-in Gradle property, so default
CI and `pack/` stay unchanged. The first step is to read the port's GPL source [S7] for its shoot entrypoint
and damage type IDs. Then add GameTests that load only when the mod is present:

| ID | Test | Pass means |
|---|---|---|
| E1 | `runGameTestServer` boots with Ghost Core + TaCZ port on NeoForge 21.1.256, and the existing 8 Ghost Core tests pass | No load conflict, no client classes on the server |
| E2 | A bound mock player takes lethal damage from the gun mod's own bullet damage source | Alive, downed, one charge spent |
| E3 | Further gun damage while downed | Health unchanged |
| E4 | A downed player triggers the gun mod's server-side fire path | No shot fired and no ammo spent. Unknown until run; a failure starts follow-up [#11] |
| E5 | A loaded gun with an attachment round-trips through `GhostData.CODEC`, as in `ghostDataSurvivesSaveAndLoad` | Stack matches |
| E6 | A bound mock player fires with their own Ghost between the muzzle and a target | The target is hit |

Constraints: switch mock players to survival for any ammo assertion. If the gun mod throws when sending
packets to a mock player, drive E2 and E3 through `player.hurt(<gun damage source>, …)` and record the
networking problem as a finding. If Superb Warfare is chosen instead, the same table applies with its
entrypoints.

The experiment cannot prove feel, rendering or real-load performance. Those come from the owner playtest
([#14]) and the profiling follow-up ([#15]).

---

## Research request (one sequential `mod-scout` pass, Haiku, high)

These would firm up the recommendation before or during the experiment:

- **R1.** TaCZ port: can its bundled default gun pack be disabled or replaced so that only our packs load?
  Give the config key or mechanism, with a source link.
- **R2.** TaCZ port: what server-side events does it expose for shoot, hit and kill on NeoForge 1.21.1?
  Give class names and say whether each is cancellable. Does firing travel over a custom client-to-server packet?
  What are its damage type IDs?
- **R3.** TaCZ port and Superb Warfare: do guns consume ammo items in survival? Is ammo craftable, and is
  there a config for it?
- **R4.** TaCZ port: release count and open issues over the last six months, and any crash reports on
  NeoForge 21.1.2xx.
- **R5.** Elite X Quality Guns: what does it add (quality or rarity tiers?), and does it have a NeoForge
  1.21.1 build?
- **R6.** Any measured server-performance reports (spark profiles, TPS) for the TaCZ port or Superb Warfare.
- **R7.** TaCZ port: default keybinds, to compare with Ghost Core's G/H/J/K.
- **R8.** Superb Warfare: is the 1.21.1 source published, and which dependencies are required versus optional?
- **R9.** Can a custom TaCZ gun pack be built without any of the CC BY-NC-ND default-pack files (shared
  animations, shaders, display files)?
- **R10.** TaCZ port: which files the CC BY-NC-ND 4.0 terms cover, any author statement on modpack inclusion,
  and whether a Modrinth pack, a CurseForge pack export or a server pack would reference or bundle the jar.
  Is there a separate CurseForge project for the port? Report what the sources say; do not conclude legality.

**R9/R10 findings (2026-10-09):** [the sourced report](tacz-license-terms.md) answers these to the extent
sources support, pins code/resource paths, corrects CurseForge hosting and distinguishes references from
copied files. A fully independent custom pack, exact asset scope and explicit author inclusion permission
remain unanswered. The owner's two separate 2026-10-09 answers and their limits are now recorded in the
linked report and on [#9]; they do not resolve those unanswered questions or the E1 networking blocker.

**Where these are tracked:** R9 and R10 in [#9]. R2 in [#10] (fire path and damage types) and [#13]. R3 and
R5 in [#13]. R1 in [#12]. [#15] measures performance directly instead of relying on R6. R4, R7 and R8 have no
issue; [#14] checks key conflicts in play.

---

## Follow-up issues (created 2026-10-09)

The prerequisite and the seven integration follow-ups (placeholders F1–F7 in the proposal) are open on
GitHub. Each issue carries its own goal, acceptance criteria and gates.

| Issue | Was | Title | Labels | Gated by |
|---|---|---|---|---|
| [#9] | R9, R10 | TaCZ port: verify license, asset and export terms | `tier:standard`, `area:pack`, `needs:owner` | None |
| [#10] | F1 | Gun compat experiment: TaCZ port × Ghost Core (E1–E6) | `tier:standard`, `area:ghostcore` | None |
| [#11] | F2 | Ghost Core: block gun fire while downed (only if E4 fails) | `tier:hard`, `area:ghostcore` | A failing E4 in [#10] |
| [#12] | F3 | Pack: add the TaCZ port with packwiz | `tier:standard`, `area:pack` | [#9], a passing E1 in [#10], and the [#4] baseline |
| [#13] | F4 | Design: gun rarity, affixes and ammo economy | `tier:hard`, `area:pack` | None |
| [#14] | F5 | Owner playtest: gun feel and Ghost Core overlap | `tier:standard`, `needs:owner`, `area:pack` | None |
| [#15] | F6 | Server performance smoke test with the TaCZ port | `tier:standard`, `needs:owner`, `area:pack` | None |
| [#16] | F7 | First Company gun pack: 2–3 original sci-fi guns | `tier:standard`, `needs:owner`, `area:art`, `area:pack` | [#9] and a passing E1 in [#10] |

**Owner playtest checklist ([#14]).** Use a throwaway Prism instance (1.21.1, NeoForge 21.1.256) with the Ghost
Core CI jar plus the candidate and its dependencies. Spend 20–30 minutes on the candidate and check:
recoil, aim-down-sights, reload and hit feedback; firing while downed; transmat while under fire; reviving
mid-firefight; scan pulse while scoped; the HUD panel against the ammo HUD; G/H/J/K against the gun keys;
whether your Ghost blocks your shots; FPS.

---

## Sources

Checked by `mod-scout` on 2026-10-09 (raw notes: `.gradle/issue3/mod-scout-report.txt`, ignored by git).

- [S1] Modrinth API, Superb Warfare versions: <https://api.modrinth.com/v2/project/Cd3DYqzn/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D>
- [S2] Modrinth, Superb Warfare: <https://modrinth.com/mod/superb-warfare> (project API: <https://api.modrinth.com/v2/project/Cd3DYqzn>)
- [S3] GitHub, Superb Warfare: <https://github.com/Mercurows/SuperbWarfare>
- [S4] CurseForge, Superb Warfare: <https://www.curseforge.com/minecraft/mc-mods/superb-warfare>
- [S5] Modrinth API, TaCZ port versions: <https://api.modrinth.com/v2/project/tacz-1.21.1/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D>
  (exact approved r7: <https://api.modrinth.com/v2/version/62q6mB5Q>)
- [S6] Modrinth, TaCZ port: <https://modrinth.com/mod/tacz-1.21.1> (project API: <https://api.modrinth.com/v2/project/tacz-1.21.1>)
- [S7] GitHub, TaCZ NeoForge port: <https://github.com/MUKSC/TACZ-1.21.1>
- [S8] GitHub, official TaCZ (Forge 1.20.1): <https://github.com/MCModderAnchor/TACZ>
- [S9] CurseForge, Timeless and Classics Zero: <https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero>
- [S10] Modrinth API, Vic's Point Blank versions: <https://api.modrinth.com/v2/project/vics-point-blank/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D>
- [S11] Modrinth, Vic's Point Blank: <https://modrinth.com/mod/vics-point-blank> (project API: <https://api.modrinth.com/v2/project/vics-point-blank>)
- [S12] CurseForge, Vic's Point Blank: <https://www.curseforge.com/minecraft/mc-mods/vics-point-blank>
- [S13] Modrinth API, BlockFront: <https://api.modrinth.com/v2/project/blockfront> (versions: <https://api.modrinth.com/v2/project/blockfront/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D>)
- [S14] Modrinth API, Scorched Guns Neoforged: <https://api.modrinth.com/v2/project/scorched-guns-neoforged> (versions: <https://api.modrinth.com/v2/project/scorched-guns-neoforged/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D>)
- [S15] GitHub, Scorched Guns Neoforged: <https://github.com/sadeast69/ScorchedGunsNeoforge>
- [S16] Modrinth API, Just Enough Guns New: <https://api.modrinth.com/v2/project/just-enough-guns-neoforge> (versions: <https://api.modrinth.com/v2/project/just-enough-guns-neoforge/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D>)
- [S17] GitHub, Just Enough Guns New: <https://github.com/maoruiQa/just-enough-guns-neoforge>
- [S18] Modrinth API, Fel's Machine Guns II: <https://api.modrinth.com/v2/project/fels-machine-guns-ii> (versions: <https://api.modrinth.com/v2/project/fels-machine-guns-ii/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D>)
- [S19] CurseForge, Fel's Machine Guns II: <https://www.curseforge.com/minecraft/mc-mods/fels-machine-guns-ii>
- [S20] Modrinth search, guns on 1.21.1 NeoForge (addon names, licenses and dates): <https://api.modrinth.com/v2/search?query=gun&facets=%5B%5B%22versions:1.21.1%22%5D,%5B%22categories:neoforge%22%5D%5D&limit=50&index=downloads>
- [S21] Modrinth API, Cyberpunk 2077 Guns for Vic's: <https://api.modrinth.com/v2/project/cyberpunk-2077-guns-for-vics-point-blank/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D>
- [S22] Modrinth API dependency projects: [GeckoLib](https://api.modrinth.com/v2/project/8BmcQJ2H), [Curios](https://api.modrinth.com/v2/project/vvuO3ImH), [Kotlin for Forge](https://api.modrinth.com/v2/project/ordsPcFz), [Cloth Config](https://api.modrinth.com/v2/project/9s6osm5g), [Patchouli](https://api.modrinth.com/v2/project/nU0bVIaL), [geckoanimfix](https://api.modrinth.com/v2/project/TbriQCWD), [Iguana Lib](https://api.modrinth.com/v2/project/5axv9QEo)
- [S23] CurseForge search result, Orbital Railgun (unverified): <https://www.curseforge.com/minecraft/mc-mods/orbital-railgun-neoforge-1-21-1>
- [S24] Separate CurseForge TaCZ port by MUKSC (checked 2026-10-09): <https://www.curseforge.com/minecraft/mc-mods/tacz-1-21-1>;
  r7 / NeoForge 1.21.1 file: <https://www.curseforge.com/minecraft/mc-mods/tacz-1-21-1/files/9027337>.

[#4]: https://github.com/iampaycheck/lazarus-clause/issues/4
[#9]: https://github.com/iampaycheck/lazarus-clause/issues/9
[#10]: https://github.com/iampaycheck/lazarus-clause/issues/10
[#11]: https://github.com/iampaycheck/lazarus-clause/issues/11
[#12]: https://github.com/iampaycheck/lazarus-clause/issues/12
[#13]: https://github.com/iampaycheck/lazarus-clause/issues/13
[#14]: https://github.com/iampaycheck/lazarus-clause/issues/14
[#15]: https://github.com/iampaycheck/lazarus-clause/issues/15
[#16]: https://github.com/iampaycheck/lazarus-clause/issues/16
