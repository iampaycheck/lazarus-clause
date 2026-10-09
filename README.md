# Lazarus Clause

A dystopian sci-fi looter-shooter modpack for Minecraft **1.21.1** (NeoForge).

> *§9.4 — The Lazarus Clause. Death does not constitute termination of employment.*
>
> Every operator signs it. Every operator is issued a Ghost to enforce it: a Company-owned AI drone
> wired into your neural lace that drags you back from the dead, again and again, because your body
> is an asset and the Company does not write off assets. Each resurrection is logged against your contract.

## Pillars

- **Loot or die in debt:** sealed caches, salvage and rarity drive progression.
- **Gunplay:** the shooter half (gun mod still to be chosen).
- **The Company:** contracts, debt and surveillance tech. The world runs on paperwork.
- **Death has a price:** your Ghost revives you, but only so many times, and some places are beyond its reach.

## Repo layout

```
pack/            the modpack, managed with packwiz (mod list, configs, versions)
mods/ghostcore/  Ghost Core: the foundational custom mod (your Ghost companion)
.github/         CI builds every custom mod and runs its GameTests on each push
```

Future custom mods go next to `ghostcore` under `mods/`.

## Ghost Core

The Ghost floats at your shoulder. It **resurrects** you on lethal damage (limited charges), **scans** for unlooted caches, ore and hostiles through walls, **lights** the dark, **transmats** you to a beacon, and carries a small **cache**. Details, controls and config are in [mods/ghostcore/README.md](mods/ghostcore/README.md).

## Playtesting

1. In Prism Launcher, create an instance with Minecraft 1.21.1 and NeoForge 21.1.256 or newer.
2. Download `ghostcore` from the latest [Build workflow run](../../actions/workflows/build.yml) (under Artifacts) or build it locally, then add the jar under Edit → Mods.

## Developing

```bash
cd mods/ghostcore
./gradlew runClient            # dev client
./gradlew runGameTestServer    # headless tests
./gradlew build                # jar in build/libs/
```

Requires a JDK that can run Gradle 9 (17+). Gradle downloads Java 21 for compiling. On Windows, keep the checkout in a short path.

### Managing the pack

Install [packwiz](https://packwiz.infra.link/), then run from `pack/`:

```bash
packwiz modrinth add <mod>     # or: packwiz curseforge add <mod>
packwiz refresh
```

Ghost Core gets added to the pack once CI publishes release builds.

## License

All rights reserved for now.
