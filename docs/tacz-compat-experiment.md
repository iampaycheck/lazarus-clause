# TaCZ × Ghost Core compatibility experiment (#10)

This is a development experiment for Minecraft 1.21.1 / NeoForge 21.1.256,
based on merged main `37f8465`. It does not add TaCZ to the pack or to the published
Ghost Core jar. Since #11, the published jar does contain an optional downed-fire
guard (see [below](#11-downed-fire-guard)). It looks TaCZ up by name, so it carries
no TaCZ classes or dependency. The E1 integration gate is still **closed**:
dedicated-server loading works, but the port throws when existing GameTests admit
unnegotiated mock players.

## Reproduce

From `mods/ghostcore` with Java 21:

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat -PtaczExperiment=true build
.\gradlew.bat -PtaczExperiment=true runGameTestServer
```

Without the property, only the existing eight tests run. With it, the unpublished
`taczExperiment` source set loads a separate development mod and pins the port
through `localRuntime`; seven additional tests register only when `tacz` is present.
The experiment classes, metadata, mixin and TaCZ dependency are absent from the
published jar and normal dependency graph. The opt-in test run intentionally
returns a failing exit code for observed failures; no tests are optional or skipped.
Check the actual test count/completion summary, not just Gradle's exit status.

## Version and authoritative source evidence

- Approved artifact: unofficial `tacz-1.21.1`, **1.1.8-hotfix-r7**,
  [Modrinth version `62q6mB5Q`](https://modrinth.com/mod/tacz-1.21.1/version/62q6mB5Q),
  project `OypNE65K`, no declared Modrinth dependencies.
- Binary SHA-512:
  `7700ed99a080729c3aa21f9619e018fbe9e2303cfbcaa463175ee5170553a000b692bc4cfa2e8ce0778747eed34dcc0197fb5e4344bb258cff1ba90adc842642`.
- Published source archive SHA-512:
  `595de9b41482988223961f45f1f4f74b7b5f4154eb3a87548b5769464564d07ebacb265eca2bb5ef7741819799141c63683c16c732dee381d45e0960b2a938a6`.
- Authoritative port commit:
  [`MUKSC/TACZ-1.21.1@c471821c771a62cc8850694bbea579c2ba0b9e3a`](https://github.com/MUKSC/TACZ-1.21.1/commit/c471821c771a62cc8850694bbea579c2ba0b9e3a).
  Its `gradle.properties` names r7. After normalizing CRLF to LF, seven relevant
  files exactly match the published source archive; individual hashes and match
  results are retained in `.gradle/issue10/source-evidence.json`.

All links below are pinned to that commit; no newer Forge/NeoForge API was assumed.

| Question | Source finding |
|---|---|
| Custom C→S firing packet | [`ClientMessagePlayerShoot`](https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/network/message/ClientMessagePlayerShoot.java) carries timestamp and charge progress under `tacz:client_player_shoot`; its handler enqueues `IGunOperator.fromLivingEntity(player).shoot(player::getXRot, player::getYRot, timestamp, chargeProgress)`. |
| Server entrypoint | [`LivingEntityMixin`](https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/mixin/common/LivingEntityMixin.java) delegates to [`LivingEntityShoot.shoot`](https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/entity/shooter/LivingEntityShoot.java). Normal draw, timestamp/network, cooldown, sprint, reload, bolt and ammo checks remain enabled in E4/E6. |
| Shot and fire hooks | `GunShootEvent` and `GunFireEvent` implement `ICancellableEvent`. The former is posted before gun logic, after closed-bolt chamber preparation; the latter is posted for actual firing cycles in [`ModernKineticGunScriptAPI.shootOnce`](https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/item/ModernKineticGunScriptAPI.java). #11's guard cancels both. |
| Client fire input | [`ShootKey.autoShoot`](https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/client/input/ShootKey.java) (`ClientTickEvent.Post`) reads `SHOOT_KEY.isDown() \|\| controllerShootDown`, then calls `chargeShoot` and `shoot`. It is the only caller of [`LocalPlayerShoot`](https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/client/gameplay/LocalPlayerShoot.java)'s fire methods. `preCheck` (dry-fire click, auto-bolt) runs before the client `GunShootEvent`; that event precedes the state lock, fire packet, animation and shot sound. Recoil (`CameraSetupEvent`) and muzzle flash listen to the client `GunFireEvent`. |
| Hit and kill hooks | [`EntityHurtByGunEvent`](https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/api/event/common/EntityHurtByGunEvent.java) implements `ICancellableEvent`; `Pre` is posted before damage and `Post` after damage, so canceling Post cannot undo applied damage. [`EntityKillByGunEvent`](https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/api/event/common/EntityKillByGunEvent.java) is not cancellable. |
| Damage IDs | [`ModDamageTypes`](https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/init/ModDamageTypes.java): `tacz:bullet`, `tacz:bullet_ignore_armor`, `tacz:bullet_void`, `tacz:bullet_void_ignore_armor`; tag `tacz:bullets`. `Sources.bullet` chooses the first two, `Sources.bulletVoid` the latter two. |
| Sound path | `shootOnce` consumes ammo, creates real `EntityKineticBullet` entities, then calls [`SoundManager.sendSoundToNearby`](https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/sound/SoundManager.java), which sends `ServerMessageSound` to nearby tracking players other than the shooter. Headless observation proves method invocation, not delivery or audible playback. |
| Mock login blocker | [`CommonAssetsManager.OnDatapackSync`](https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/resource/CommonAssetsManager.java) unconditionally sends `tacz:server_sync_gun_pack`. Level admission also unconditionally sends `tacz:server_sync_base_timestamp` from `SyncBaseTimestamp.onPlayerJoinWorld`. NeoForge rejects these on an unnegotiated mock connection. |

Version-specific signatures were read from the local
`mods/ghostcore/build/moddev/artifacts/neoforge-21.1.256-sources.jar`:
`RegisterGameTestsEvent.register(Class<?>)` on the mod bus,
`EntityJoinLevelEvent`, `EntityTickEvent.Pre`, `ServerPlayer` construction,
`ServerLevel.addNewPlayer`, `ServerGamePacketListenerImpl` construction,
`ServerCommonPacketListenerImpl.send(Packet<?>, PacketSendListener)`,
`Entity.setOldPosAndRot`, and the GameTest helper. Selected files are retained
under `.gradle/issue10/neo-source`.

## Results

| ID | Outcome | Evidence / limit |
|---|---|---|
| E1 | **FAIL** | Port and Ghost Core load on a dedicated GameTest server, with no fatal client-class load error. The unadapted mock-login E1 test and five of the existing eight tests fail with `Payload tacz:server_sync_gun_pack may not be sent to the client!`; the aggregate gate requires all eight to pass. |
| E2 | **PASS, fixture qualified** | After vanilla's 60-tick spawn immunity expires, lethal `tacz:bullet` via the port's `Sources.bullet` leaves the bound survival mock alive/downed and spends exactly one charge. Direct `hurt` and a mock packet sink do not verify bullet networking. |
| E3 | **PASS, fixture qualified** | Clear vanilla hurt cooldown, apply additional `tacz:bullet_ignore_armor` while downed; health and charge count stay unchanged. Same networking limitation as E2. |
| E4 | **PASS since #11** (was FAIL in #10) | #10, with no guard: the real four-argument entrypoint returned `SUCCESS` while downed, spawned one `EntityKineticBullet`, took ammo **11 → 10** and dispatched **`shoot_3p`**. #11, `e4DownedPlayerCannotFireUntilRevived`, while downed: the entrypoint returns `FORGE_EVENT_CANCEL`, and a wire-decoded `ClientMessagePlayerShoot` through the port's real handler is also refused. Both give **0 bullets, 11 → 11, no sound**, and a server `GunFireEvent` is cancelled. The decoded Ghost sync shows the player downed, then not downed after the actual revive. After the revive, real fire returns `SUCCESS`: **1 bullet, 11 → 10, `shoot_3p`**. No client or audio verification. |
| E5 | **PASS** | Real bundled `tacz:ak47`, ten magazine rounds, a chambered round, semi fire mode and installed `tacz:sight_552` round-trip through `GhostData.CODEC`; `ItemStack.matches` succeeds. Fixture preconditions verify actual ammo and scope installation. |
| E6 | **PASS, fixture qualified** | Real server fire path returns `SUCCESS`, spawns one bullet, and its actual collision-tick segment intersects the owner's Ghost and the target. The port's block ray trace has no obstruction before the target; target health **10 → 1**. Ghost is not pickable. Mock packet delivery is not verified. |
| E7 | **PASS** (#11) | Closed-bolt AK47 with an empty chamber and ten magazine rounds, fired while downed: `FORGE_EVENT_CANCEL`, 0 bullets, no sound, total **10 → 10**. The port chambers one round before posting `GunShootEvent` (magazine 9, chamber loaded), so a round moves but nothing is spent. After revive: `SUCCESS`, 1 bullet, **10 → 9**, `shoot_3p`. |

The original tests that fail only with the port are `bindingSpawnsAGhost`,
`lethalDamageDownsThenRevives`, `noChargesMeansRealDeath`, `transmatLandsOnBeacon`,
and `ghostLightsTheDark`. `scanFindsCachesOreAndHostiles`,
`orphanedLightCleansItselfUp`, and `ghostDataSurvivesSaveAndLoad` still pass.

#11 local verification (2026-10-09, logs in ignored `.gradle/issue11`):

| Command / check | Result |
|---|---|
| `gradlew.bat build runGameTestServer` | Exit 0, **8/8** (`default-final.log`). Six default runs on this branch: 5 × 8/8, and once `ghostLightsTheDark` timed out (`default.log`). Light code is untouched and the guard registers nothing without TaCZ. It hasn't reproduced since, and it never failed in #10's logs or CI. |
| `gradlew.bat -PtaczExperiment=true build runGameTestServer` | Exit 1 by design. **15** required tests: **9 pass / 6 fail**. The failures are E1 and the same five original login tests as #10. E4 and E7 pass (`opt-in-final.log`). |
| `gradlew.bat runServer`, stopped over local RCON | `Done`, saved, exit 0. No TaCZ, no guard line, no class errors (`server-default.log`). |
| `gradlew.bat -PtaczExperiment=true runServer` | `Done`, saved, exit 0. Logs `TaCZ found: downed operators can't fire (server guard)`; no client guard and no client-class errors (`server-tacz.log`). |
| `dependencies` (runtime and compile classpaths) | No TaCZ or Modrinth entry (`default-*-dependencies.log`). |
| Published jar | `compat/TaczCompat`, `client/TaczClientGuard`; no `com/tacz` classes or class references (names only), no experiment, mixin or TaCZ metadata. |

#10 final local verification (2026-10-09):

| Command / check | Result |
|---|---|
| `gradlew.bat build runGameTestServer` | Exit 0, **8/8** required tests pass (`default-final.log`). |
| `gradlew.bat -PtaczExperiment=true build runGameTestServer` | Experiment compiles/builds; server completes **14** required tests, **7 pass / 7 fail**, exit 1 (`opt-in-final.log`). Failures: E1, E4 and the five original player/login tests. |
| `gradlew.bat dependencies --configuration runtimeClasspath` | Exit 0; no TaCZ or experiment dependency (`default-runtime-dependencies.log`). |
| Published jar contents | No `com/tacz`, experiment classes, experiment metadata or sound mixin. |
| `git diff -- pack .github/workflows/build.yml` | Empty. Default CI retains its ordinary build/eight-test commands. |

## Harness boundaries

E1 and all eight original tests retain their real, unadapted helper/login path.
E2/E3/E4/E6/E7 construct survival `ServerPlayer`s because the legacy helper hardcodes
`isCreative() == true` even after `setGameMode(SURVIVAL)`. These players are explicitly
ticked, admitted to the level without a full login, and use a connection whose
`send(Packet<?>, PacketSendListener)` is a headless packet sink. This permits
server-logic measurements despite the documented sync errors; it is **not** a
networking compatibility fix. Default Ghost Core behavior is unchanged.

The only experiment mixin observes `SoundManager.sendSoundToNearby` at method entry;
it neither cancels nor changes the port. E4/E7 count actual bullet admission and
check total magazine-plus-chamber ammo. E4/E6/E7 use the exact packet handler's
server entrypoint with normal validation, real gun data/scripts and real projectiles.
E4 also feeds a wire-encoded `ClientMessagePlayerShoot` through the port's own
`handle`, with a context that runs enqueued work immediately (the test already runs
on the server thread). For E4 only, the packet sink claims Ghost Core's channel, so
`GhostNetwork.send` delivers Ghost syncs to it; TaCZ channels stay unnegotiated.

E6 aims at a stationary target from an unobstructed standing firing position.
The target has AI and gravity disabled, keeping the firing lane above arena blocks.
An `EntityTickEvent.Pre` listener places the **actual owned Ghost** across the
generated bullet's first segment immediately before collision. It verifies both
Ghost and target bounding-box intersections and finally asserts actual target
health loss. It never alters bullet velocity, collision filtering, target damage,
downed state, or firing cancellation. A geometric miss is reported as blocked,
rather than attributed to the Ghost. Listeners are removed after measurement.

## Evidence and remaining work

Ignored local evidence lives under `.gradle/issue10`: pinned binary/source archives,
`versions.json`, `source-evidence.json`, selected NeoForge sources, baseline/default
and opt-in logs. `opt-in-first.log` retains the original raw login failures;
`opt-in-final.log` is the final instrumented experiment. Intermediate logs retain
fixture debugging (wrong initial scope ID, mixin package isolation, spawn immunity,
and the embedded initial muzzle position). They are not successful integration runs.

The opt-in failures are experimental findings, while normal build/eight-test CI
remains the merge check. #12/#16 cannot treat E1 as passed; #9 remains a separate
gate. #11 added the downed firing fix (below). Real client negotiation, sound playback,
gun feel, rendering, performance, assets/licensing and pack integration remain
outside this experiment. No client was launched and `pack/` is untouched.

## #11 downed-fire guard

Production Ghost Core blocks TaCZ fire while its synced `downedUntil` is set. It does
this only when `tacz` is loaded. Main code never imports `com.tacz`: port classes and
members are looked up by name, so the default build never resolves TaCZ, and the mod
metadata declares no dependency.

- **Server, authoritative** (`compat/TaczCompat`, registered from the mod constructor):
  HIGHEST-priority listeners cancel `GunShootEvent` and `GunFireEvent` when the shooter
  is a downed `ServerPlayer`. A cancelled `GunShootEvent` returns `FORGE_EVENT_CANCEL`
  before `ServerMessageGunShoot`, the shoot timestamps (so no cooldown carries over)
  and any gun logic. The per-cycle `GunFireEvent` cancel also stops a burst that is
  still running when the player goes down. This covers every `ClientMessagePlayerShoot`,
  including one from a modified client. Closed-bolt chambering happens before the event
  and is ammo-neutral (E7).
- **Client** (`client/TaczClientGuard`, registered from `GhostCoreClient`, so never on a
  dedicated server). While `GhostSyncPayload.isDowned()` is true (the same predicate as
  the HUD's downed overlay), a `ClientTickEvent.Pre` listener runs before TaCZ reads
  input in `Post`. It sets `ShootKey.SHOOT_KEY` up, clears `controllerShootDown` through
  `ShootKey.shootControllerTick(false)`, and empties the local `chargeProgress`/`isCharging`.
  With the key up and no charge, `chargeShoot` returns false for auto, hold and delay
  charge, so `shoot()` and `preCheck` never run. The result is no dry-fire click, no
  input bolt, no fire packet, no animation, no shot sound and no recoil. The client
  `GunShootEvent`/`GunFireEvent` are also cancelled for the local player, as a backstop.
- **Revive.** Suppression lifts when the `downedUntil = 0` sync arrives; logout resets
  it. A trigger held through the downed state must be pressed again, because vanilla only
  sets mouse keys on a press. Queued charge is already gone, so nothing fires by itself
  on standing up.

What is proven headlessly:
- E4/E7 cover the server behaviour.
- E4 shows the decoded Ghost sync going downed, then not downed, around a real revive.
- Dedicated `runServer` starts and stops cleanly with and without TaCZ. With TaCZ it logs
  `TaCZ found: downed operators can't fire (server guard)` and never the client guard.
- `taczExperiment/.../client/GuardContract` compiles against the pinned port. It
  references every member the guards look up, with its exact public type.

What is not proven, and is left to the owner's real-client playtest:
- the client gate running in a live client;
- no recoil, sound or animation while downed;
- the fresh press after revive.

Known limits:
- If Controllable is installed and dispatches its end-of-tick callback inside
  `ClientTickEvent.Post` ahead of TaCZ's `autoShoot`, that tick's controller press could
  reach `preCheck` before the backstop. Controllable is not in the pack and was not tested.
- In the latency window between the server downing a player and the sync arriving, a
  local shot can still animate and sound. The server spends and spawns nothing.
- A manual-action *charge* gun with an empty chamber auto-bolts every tick whatever the
  input. That is TaCZ behaviour; no such gun is bundled.
