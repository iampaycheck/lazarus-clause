# Workflow

The goal is high quality without burning usage. Expensive models handle judgment and hard code. Cheap models handle reading, lookups and routine edits. GameTests and CI handle verification at no token cost.

## Model routing

Issues carry a difficulty label, not a model. This table maps labels to models, so a model change is one edit here and no relabelling.

| Work | Model and execution |
|---|---|
| `tier:hard`: new custom-mod systems, hard bugs (rendering, networking, worldgen, perf), big design calls | Opus 5.5, xhigh |
| `tier:standard`: routine mod changes, pack work, configs, KubeJS, docs, lore | **GPT 6.1 Sol** (Codex), high, in Orca |
| Read-and-report helpers: `mod-scout` (Haiku, high), `log-triage` (Haiku, xhigh) | Sol calls them through `claude -p` (below) |
| File-changing helpers: `pack-builder` (Sonnet, high), `mod-implementer` (Sonnet, xhigh) | Orca child workspaces, one worktree each |
| Review before merge | The reviewer must be a different model from the author: Sol reviews Claude-written code (medium), and `mod-reviewer` (Sonnet, xhigh) reviews Sol-written code |

Codex has the most usage headroom, so Sol does the bulk of the work. Claude is kept for `tier:hard` work and the cheap Haiku/Sonnet helpers.

Rules of thumb:
- xhigh is the default ceiling. Use `max` only when the owner says so for that task.
- Name model and effort on every spawn or helper call. Running more than one at once needs the owner's sign-off on count × model first.
- The main session doesn't read logs, web pages or long files. A helper reads them and returns a brief.

### Calling Claude helpers from Codex (Sol)

```bash
claude -p --agent mod-scout  --model haiku  --effort high  "Is <mod> on NeoForge 1.21.1? Latest version, slugs, license."
claude -p --agent log-triage --model haiku  --effort xhigh "Triage mods/ghostcore/run/logs/latest.log"
claude -p --agent mod-reviewer --model sonnet --effort xhigh "Review this branch against origin/main"
```

Agent definitions live in `.claude/agents/`. `--agent` loads the definition's prompt and tools; `--model` and `--effort` restate the choice explicitly.

Run helpers from the issue worktree so they find `.claude/agents/` and `CLAUDE.md`. If a helper reports an expired OAuth session, run `claude auth login` and retry; Orca's Codex login does not authenticate the standalone Claude CLI.

## Session protocol

1. **Issue first.** Every task is a GitHub issue with a goal, acceptance criteria, a tier label and an area label. `needs:owner` marks work only a human can do (playtests, decisions, art).
2. **One fresh session per issue.** Pick the model from the tier label and open with "Work on #N". `AGENTS.md` and the issue carry the context, so don't paste old chat.
3. **Plan first for mod work.** Record the plan as an issue comment. If the plan is routine, a cheaper session or `mod-implementer` carries it out. Reuse a plan that's already good enough; don't re-plan.
4. **Branch and PR.** Use `<N>-<slug>`. The PR says `Closes #N`, what changed, how it was verified and what's left. CI must be green. The PR description is the handoff.
5. **Close out.** A new gotcha becomes one line in `AGENTS.md`. A decision goes to centrifuge. Then end the session; the next task gets a new one. Same-PR fixes may stay in the same session.

## Verification without burning tokens

- **Logic:** GameTests, run headless by CI on every push. Write the test with the feature.
- **Visuals and feel:** the owner playtests the CI artifact (Actions → run → `ghostcore`) in Prism and files issues, attaching `latest.log` or the crash report.
- **Agents don't launch the game client to look.** It's slow, costly and needs screen access.

## Subagents (`.claude/agents/`)

| Agent | Model | Effort | Job |
|---|---|---|---|
| `mod-scout` | Haiku | high | Mod availability, versions, slugs, licenses and compatibility for 1.21.1 NeoForge. Returns a table with sources. |
| `log-triage` | Haiku | xhigh | Crash reports, `latest.log`, CI logs. Returns root cause, evidence lines and a likely fix. |
| `pack-builder` | Sonnet | high | Applies pack changes with packwiz: mods, configs, KubeJS, datapacks. |
| `mod-implementer` | Sonnet | xhigh | Implements a written plan in `mods/`, including GameTests. Stops if the plan needs new architecture. |
| `mod-reviewer` | Sonnet | xhigh | Reviews a branch diff in `mods/` and runs the tests. Reports findings and never edits. |

The effort in each agent's file is its standing setting. Override it on a spawn when the task warrants it, and say so.

## Orca workspace setup

The repo is registered in Orca at `C:\Users\teddy\Documents\GitHub\lazarus-clause`. Keep issue worktrees under `C:\w\lc`; Orca's default workspace path is too deep for NeoForge. This is a machine setting and checkout convention, not a path stored in `orca.yaml`.

1. Run `git config --global core.longpaths true` (centrifuge machine bootstrap).
2. Run `orca repo add --path C:\Users\teddy\Documents\GitHub\lazarus-clause`.
3. The root `orca.yaml` supplies `scripts.setup`: `cd /d mods\ghostcore`, then `.\gradlew.bat compileJava`. Orca runs Windows hooks through `cmd.exe` and stops on a failed command. This warms Gradle without starting a server. Never use bare `.sh` hooks on Windows. Java 21 is the mod's toolchain; Gradle can provision it through the configured Foojay resolver.
4. Create short issue worktrees from the registered repo, then expose them in Orca:

   ```powershell
   git fetch origin
   git worktree add -b <issue>-<slug> C:\w\lc\<issue> origin/main
   orca repo set --repo path:C:\Users\teddy\Documents\GitHub\lazarus-clause --external-worktree-visibility show --json
   orca worktree show --worktree path:C:\w\lc\<issue> --json
   orca worktree set --worktree path:C:\w\lc\<issue> --issue <issue> --json
   ```

   Orca discovers these Git worktrees; `repo add` is only needed once for the main checkout. For manually created worktrees, run the setup command from the worktree root: `cmd /d /c "cd /d mods\ghostcore && .\gradlew.bat compileJava"`. Then run `.\gradlew.bat build` and `.\gradlew.bat runGameTestServer` from `mods\ghostcore` before opening a PR. Do not use an Orca-created checkout until its configured workspace root is short enough.

5. Give each child workspace one issue and one PR. Follow centrifuge's child-workspace doctrine for seat limits and sign-off. The Minecraft/NeoForm cache in `~/.gradle` is shared, so a new worktree's first build skips the ~3-minute Minecraft setup.
6. Codex reads `AGENTS.md`, and Claude Code reads it through `CLAUDE.md`. Keep shared rules in `AGENTS.md` only.
