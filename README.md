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
docs/workflow.md how work gets done: models, effort, one issue per session, Orca setup
.claude/agents/  project subagents (Haiku scouts and log triage, Sonnet builders and reviewers)
```

Future custom mods go next to `ghostcore` under `mods/`.

## Ghost Core

The Ghost floats at your shoulder. It **resurrects** you on lethal damage (limited charges), **scans** for unlooted caches, ore and hostiles through walls, **lights** the dark, **transmats** you to a beacon, and carries a small **cache**. Details, controls and config are in [mods/ghostcore/README.md](mods/ghostcore/README.md).

## Playtesting

Import the baseline `.mrpack` into a **new** Prism instance: Minecraft **1.21.1**, NeoForge **21.1.256**, Java **21**. See [baseline setup, export and owner checklist](docs/pack-baseline.md) for the exact artifact path, pinned mod versions and log locations. Client launch acceptance is **PENDING owner playtest**.

Ghost Core is a separate, optional integration step: download `ghostcore` from the latest [Build workflow run](../../actions/workflows/build.yml) (under Artifacts) or build it locally, then add the jar under Edit → Mods. The baseline export does not include Ghost Core; a hosted release is needed before packwiz can distribute it.

## Developing

```bash
cd mods/ghostcore
./gradlew runClient            # dev client
./gradlew runGameTestServer    # headless tests
./gradlew build                # jar in build/libs/
```

Requires a JDK that can run Gradle 9 (17+). Gradle downloads Java 21 for compiling. On Windows, keep the checkout in a short path.

### Managing the pack

From the repository root in PowerShell, install the pinned packwiz build locally (Windows x64):

```powershell
$ErrorActionPreference = 'Stop'
.\scripts\install-packwiz.ps1
New-Item -ItemType Directory -Force .gradle/issue4/output | Out-Null
Push-Location pack
try {
    & ../.gradle/issue4/tools/bin/packwiz.exe --cache ../.gradle/issue4/packwiz-cache refresh
    if ($LASTEXITCODE -ne 0) { throw 'Refresh failed' }
    & ../.gradle/issue4/tools/bin/packwiz.exe --cache ../.gradle/issue4/packwiz-cache modrinth export --output ../.gradle/issue4/output/lazarus-clause-0.1.0.mrpack
    if ($LASTEXITCODE -ne 0) { throw 'Export failed' }
} finally { Pop-Location }
python scripts/verify-pack.py .gradle/issue4/output/lazarus-clause-0.1.0.mrpack --download --report .gradle/issue4/output/verification.json
if ($LASTEXITCODE -ne 0) { throw 'Verification failed' }
```

The installer pins Go and packwiz, verifies the Go archive checksum, and restores its process environment. All tools, caches and exports stay under ignored `.gradle/issue4/`; no persistent PATH changes. Verification requires Python 3.11+. [Full instructions and sources](docs/pack-baseline.md) include uninstalling and rebuilding the exact selection. Change `pack/` with packwiz; keep its files LF-only and never edit hashes manually.

## License

All rights reserved for now.
