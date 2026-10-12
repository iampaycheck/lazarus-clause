# Ghost Core

The foundational mod of [Lazarus Clause](../../README.md), a dystopian sci-fi looter-shooter modpack. NeoForge **1.21.1**.

> Every operator is issued a **Ghost**: a Company-owned AI drone wired into your neural lace.
> It keeps you alive, but not out of kindness. Your contract says your body is Company property,
> and every resurrection is logged against what you owe.

Inspired by Destiny's Ghost, with project-specific lore and shell design.

## Features

| | What it does |
|---|---|
| **Companion** | A drone floats over your right shoulder and follows you smoothly. When it works, its shell spreads open and orbits its glowing lens. Other players can see it. |
| **Resurrection** | Lethal damage leaves you *downed* instead of dead. For a few seconds you're rooted and invulnerable while the Ghost restores you. Each revive spends a charge (3 max, one regenerates every 5 min). With no charges left, death is real. Void, lava and suffocation deaths pull you back to the last safe ground you stood on. If the optional TaCZ gun port is installed, a downed operator can't fire either. |
| **Scan** (`U`) | A sonar ring sweeps outward and x-rays, through walls, **unlooted caches** (gold; containers that still have a loot table), **ore** (cyan, `c:ores` tag) and **hostiles** (red). |
| **Light** (`K`) | In the dark, the Ghost carries an invisible light block with it. These light blocks delete themselves once no Ghost claims them, so crashes and chunk unloads never leave stray lights behind. |
| **Transmat** (`Y`) | Right-click a **Transmat Beacon** to link it. Press `Y` and hold still for 3 s to teleport there, including across dimensions. Moving or taking damage cancels it. 5 min cooldown. |
| **Ghost Cache** (`J`) | 9 slots of storage the Ghost keeps for you. It follows you everywhere and survives death. |
| **HUD** | A top-left panel shows charges, scan and transmat cooldowns, and a distance/bearing arrow to your beacon. Full-screen overlays appear while you're downed or channeling a transmat. |

New players get a **Ghost Shell** on first join and use it to bind (configurable).

The keys above are defaults; remap them in Minecraft's **Options → Controls → Key Binds**.
Scan uses `U` and transmat uses `Y` to avoid the TaCZ r7 defaults (`G` for fire-mode
selection, `H` for gun inspection). Updates preserve saved bindings: existing players
with scan on `G` or transmat on `H` must manually remap those Ghost Core actions to
`U` and `Y` in Controls. Ghost Cache (`J`) and Light (`K`) are unchanged.

## Config

`config/ghostcore-client.toml`: `hudEnabled`.
`serverconfig/ghostcore-server.toml` (per world, synced to clients):
- `starterMode`: `ITEM`, `BIND` or `NONE`
- resurrection: charges, regen time, revive delay, revive health
- scan: radius, ore radius, cooldown, highlight time
- transmat: cooldown, channel time, whether cross-dimension is allowed
- light: on/off

## Commands (op level 2)

`/ghost bind|unbind|recharge [targets]`: for testing, quests and pack scripting.

## Modpack hooks

`com.iampaycheck.ghostcore.api.GhostReviveEvent` fires on the NeoForge bus before a resurrection spends a charge. Cancel it to make an area lethal (raids, dark zones, a hardcore dimension):

```java
NeoForge.EVENT_BUS.addListener((GhostReviveEvent e) -> {
    if (e.getEntity().level().dimension() == Level.NETHER) e.setCanceled(true);
});
```

KubeJS can hook the same event through `NativeEvents`. All player-facing text is in `assets/ghostcore/lang/en_us.json`, so the pack can rewrite the lore through a resource pack.

## Development

### License

Original Ghost Core code and documentation associated with it use the
[MIT License](../../LICENSE). Original models, textures, audio and other artistic
assets have all rights reserved separately for now. The
[scope and asset notice](../../LICENSE-SCOPE.md) explains the boundary, including
original model geometry embedded in source versus MIT rendering code.

The jar contains `LICENSE` and `LICENSE-SCOPE.md` at its root; its mod metadata
states this split. Third-party code, dependencies and assets retain their own
terms and notices, including Minecraft, NeoForge and optional TaCZ content.
The project's own license policy is settled (#7). Release integration remains
separate under #5, including third-party distribution checks and the
[TaCZ license and asset review](../../docs/tacz-license-terms.md) before any public release.

### Build and tests

```bash
./gradlew build                # jar in build/libs/
./gradlew runClient            # dev client
./gradlew runGameTestServer    # headless GameTests (8 tests: bind, revive, real death, scan, transmat, light, cleanup, save/load)
```

Windows note: keep the project in a short path, such as `C:\dev\ghostcore`. NeoForge's setup step launches processes whose working directory sits deep inside `build/`, and Windows refuses working directories longer than 260 characters.

The optional [TaCZ compatibility experiment](../../docs/tacz-compat-experiment.md)
adds seven dev-only GameTests with `-PtaczExperiment=true`. It has known failing
compatibility checks; normal builds and the published jar exclude the experiment.

TaCZ is never required. When it is loaded, `compat/TaczCompat` (server) and
`client/TaczClientGuard` stop a downed player from firing. Both look the port up by
name, so main code must not import `com.tacz`; the experiment's `GuardContract`
compiles those names against the pinned port.

### Layout

```
ghost/      server logic: GhostManager (lifecycle + HUD sync), Resurrection, Scanner, Transmat, GhostLight, GhostPocket, GhostEntity
client/     keys, HUD, scan x-ray renderer, Ghost model/renderer
network/    payloads (key actions C→S, HUD sync + scan results S→C)
api/        events for other mods / scripts
compat/     optional integrations, loaded only when the other mod is (TaCZ downed-fire guard)
gametest/   GameTests (arena: data/ghostcore/structure/platform.nbt)
```

Design notes:
- Ghost state lives in a player **data attachment** (`GhostData`) that persists through death and logout. All timers are absolute game times, so the HUD counts down on the client with no per-tick packets. The server syncs a snapshot only when it changes.
- The Ghost **entity** is never saved. The server respawns it from the attachment, and clients simulate its follow motion locally, so it stays smooth without heavy position syncing.

### Placeholder art

Textures are programmer art and the model is a simple cube rig (`client/render/GhostModel.java`). Replace them with a Blockbench model using *Modded Entity → Mojang mappings* export and matching textures. Keep `ghost_glow.png` as the emissive (eyes) layer.

## Roadmap ideas

Shell cosmetics and rarities · Ghost perks (scan radius, extra charges) · dark-zone regions that block revives · Lootr compatibility (per-player "looted" state in scans) · voice/subtitle barks · a JEI page.
