# Workflow

The goal is high quality without burning usage. Expensive models handle judgment and hard code. Cheap models handle reading, lookups and routine edits. GameTests and CI handle verification at no token cost.

## Who does what

| Work | Session model | Effort | Helpers |
|---|---|---|---|
| New custom-mod system, hard bug (rendering, networking, worldgen, perf), architecture | Opus 5.5 | xhigh | `mod-scout`, `log-triage` |
| Routine mod change that follows an existing pattern or a written plan (item, block, recipe, config, lang) | Sonnet 5.5 | high, or xhigh if tricky | `mod-implementer`, `mod-reviewer` |
| Pack work: choosing and adding mods, configs, KubeJS, quests, loot, datapacks | Sonnet 5.5 | high | `mod-scout`, `pack-builder` |
| Crash reports and logs | (whoever is driving) | n/a | `log-triage` |
| Lookups: does mod X exist for 1.21.1 NeoForge, compatibility, licenses | (whoever is driving) | n/a | `mod-scout` |
| Big design calls (gun mod, progression, economy) | Opus 5.5, or Fable 5.1 as a judgment seat | xhigh | `mod-scout` |
| Lore, quest text, item descriptions | Sonnet 5.5 | medium to high | n/a |

Rules of thumb:
- Start sessions on Sonnet. Switch to Opus only for issues labelled `tier:opus`.
- xhigh is the default ceiling. Use `max` only when the owner says so for that task.
- Name model and effort on every spawn. Spawning more than one agent at once needs the owner's sign-off on count × model first.
- Opus doesn't read: logs, web pages and long files go to a Haiku helper, which returns a brief.

## Session protocol

1. **Issue first.** Every task is a GitHub issue with a goal, acceptance criteria and a `tier:opus` or `tier:sonnet` label.
2. **One fresh session per issue.** Pick the model from the label and open with "Work on #N". `CLAUDE.md` and the issue carry the context, so don't paste old chat.
3. **Plan first for mod work.** Run `/plan` and record the plan as an issue comment. If the plan is routine, end the Opus session and let a Sonnet session (or `mod-implementer`) carry it out. Reuse a plan that's already good enough; don't re-plan.
4. **Branch and PR.** Use `<N>-<slug>`. The PR says `Closes #N`, what changed, how it was verified and what's left. CI must be green. The PR description is the handoff.
5. **Close out.** A new gotcha becomes one line in `CLAUDE.md`. A decision goes to centrifuge. Then end the session; the next task gets a new one. Same-PR fixes may stay in the same session.

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

## Moving into Orca

Move once two or more independent issues are ready at the same time. Until then, one session at a time is cheaper.

1. Run `git config --global core.longpaths true` (centrifuge machine bootstrap).
2. Run `orca repo add --path C:\Users\teddy\Documents\GitHub\lazarus-clause`.
3. Commit a root `orca.yaml` whose `scripts.setup` warms Gradle (`cd mods/ghostcore` then `.\gradlew.bat compileJava`). Never start a server there, and don't use bare `.sh` hooks on Windows.
4. Put worktrees on a **short** root such as `C:\w\lc\<issue>`, because NeoForge breaks in deep paths. Create them with `git worktree add -b <branch> C:\w\lc\<issue> origin/main`, then register them with Orca.
5. Give each child workspace one issue and one PR. Follow centrifuge's child-workspace doctrine for seat limits and sign-off. The Minecraft/NeoForm cache in `~/.gradle` is shared, so a new worktree's first build skips the ~3-minute Minecraft setup.
