# Lazarus Clause

Dystopian sci-fi looter-shooter modpack. Minecraft 1.21.1, NeoForge 21.1.256. Public repo: iampaycheck/lazarus-clause.

## Layout
- `pack/`: packwiz pack (mod list, configs). Change it with packwiz; never hand-edit hashes.
- `mods/ghostcore/`: Ghost Core, the foundational mod (Gradle + ModDevGradle, Java 21). Read its README before changing it.
- `.github/workflows/build.yml`: CI builds each mod and runs its GameTests. Green CI is the merge gate.
- `docs/workflow.md`: how work is run (models, effort, sessions, Orca). Follow it.

## Commands (run from mods/ghostcore)
- `./gradlew build`: jar in build/libs
- `./gradlew runGameTestServer`: headless GameTests; must pass before a PR
- `./gradlew runClient`: dev client. Visual and feel checks are the owner's playtest, not an agent's job.

## Rules
- One GitHub issue per session. Branch `<issue>-<slug>`. PR says `Closes #N`, what changed, how it was verified and what's left. Durable notes go in the issue or PR, not in chat.
- Name model AND effort on every subagent spawn. Fan-out of more than one agent needs the owner's sign-off on count × model first.
- The main session never bulk-reads logs, web pages or long docs. Hand them to `log-triage` or `mod-scout` (Haiku).
- Every gameplay feature in a custom mod ships with a GameTest in `gametest/`.
- 1.21.1 / NeoForge 21.1 APIs only. When unsure of a signature, check `mods/ghostcore/build/moddev/artifacts/neoforge-*-sources.jar`; don't guess from newer versions.
- Player-facing text goes through lang keys. Voice: corporate-dystopian ("the Company"). Destiny-inspired, but no Bungie names, terms or assets.
- Client-only classes live in `client/` packages and are referenced from common code only inside lambdas.
- Server→client payloads go through `GhostNetwork.send`, which skips fake and mock players.

## Gotchas
- Windows MAX_PATH: NeoForge's setup fails when a working dir passes 260 chars. Keep checkouts and worktrees in short paths and set `git config core.longpaths true`.
- GameTest mock players aren't ticked (use `helper.onEachTick(player::doTick)`), are in creative, and drop about 1 block in their first ticks.
- `RenderLevelStageEvent` (1.21.1): set the model-view matrix yourself before drawing (see `ScanHighlights`).
- `pack/**` is LF-only, because packwiz hashes break on CRLF.

## Brain
Decisions and lessons that outlive this repo go to centrifuge under `projects/lazarus-clause/`, through its brain-writer. Never edit the brain directly.
