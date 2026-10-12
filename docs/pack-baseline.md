# Pack baseline 0.1.0

Minecraft **1.21.1**, NeoForge **21.1.256**, Java **21**. Five stable releases, pinned by Modrinth version ID through packwiz. No additional required dependencies or config/menu helpers were reported by their exact-version APIs, so no helpers or custom configs are included. No content mods, TaCZ, shader packs or additional optimization mods are part of this baseline.

## Selection and declared licenses

Verified **2026-10-11** against the official Modrinth project/version APIs. Version links identify the exact release; project API links record the declared license and side support. These are upstream declarations, not legal conclusions.

| Mod | Exact release / version ID | Modrinth project ID / source | Pack side | Declared license | Selection rationale |
|---|---|---|---|---|---|
| Sodium | [0.8.13, mc1.21.1-0.8.13-neoforge / uMOpc5uV](https://modrinth.com/mod/sodium/version/uMOpc5uV) | [AANobbMI](https://api.modrinth.com/v2/project/AANobbMI) | client | LicenseRef-Polyform-Shield-1.0.0 | Client renderer; server unsupported. |
| Lithium | [0.15.4, mc1.21.1-0.15.4-neoforge / DDUrRVCA](https://modrinth.com/mod/lithium/version/DDUrRVCA) | [gvQqBUqZ](https://api.modrinth.com/v2/project/gvQqBUqZ) | both | LGPL-3.0-only | Game-logic optimization on either side. |
| FerriteCore | [7.0.3-neoforge / x7kQWVju](https://modrinth.com/mod/ferrite-core/version/x7kQWVju) | [uXXizFIs](https://api.modrinth.com/v2/project/uXXizFIs) | both | MIT | Memory optimization; upstream allows either side independently. |
| JEI | [19.51.0.418 / ufHUqt9b](https://modrinth.com/mod/jei/version/ufHUqt9b) | [u6dRKJwZ](https://api.modrinth.com/v2/project/u6dRKJwZ) | both | MIT | One recipe viewer; this stable release needs no extra config dependency. |
| Jade | [15.10.6+neoforge / eYz2YBGT](https://modrinth.com/mod/jade/version/eYz2YBGT) | [nvQzSEkH](https://api.modrinth.com/v2/project/nvQzSEkH) | both | CC-BY-NC-SA-4.0 | Requested block/entity information HUD; JEI integration is optional. |

The independent check fetches `https://api.modrinth.com/v2/version/<version-ID>` and compares the primary filename, project ID, stable release type, loader, game version, dependencies, download URL, size, SHA1 and SHA512 with the export. It also hashes each downloaded JAR. No third-party JAR is checked into Git or bundled in the `.mrpack`.

The installed server JAR metadata was inspected too: FerriteCore requires NeoForge 21.1.218+, JEI 21.1.238+, and Jade 21.0.143+, all satisfied by 21.1.256. JEI's raw Minecraft range is `[1.21, 1.21.1)`, despite its published 1.21.1 release: `VersionSupportMatrix` in the exact [FancyModLoader 4.0.45 sources](https://maven.neoforged.net/releases/net/neoforged/fancymodloader/loader/4.0.45/loader-4.0.45-sources.jar) permits a 1.21 compatibility match when running 1.21.1. The dedicated server reached `Done` with this unmodified JAR; client behavior still needs the owner check below.

## Reproducible local tooling and export

Run `scripts/install-packwiz.ps1` from the README in PowerShell on Windows x64. It uses official [Go downloads](https://go.dev/dl/) and the [packwiz source-install procedure](https://packwiz.infra.link/installation/), with these fixed inputs:

- Go archive: `go1.27.2.windows-amd64.zip`; SHA256 `1314008898bd40df77af4b014f777f08873dbdfbcd3d92308728ee03304fe04f`.
- packwiz commit: [`ef87d964f8cbd52b3b13ea42453ef322290e2b9e`](https://github.com/packwiz/packwiz/tree/ef87d964f8cbd52b3b13ea42453ef322290e2b9e), Go module `v0.0.0-20260906154125-ef87d964f8cb`.
- Executable: `.gradle/issue4/tools/bin/packwiz.exe`; Go dependencies/build caches remain in `.gradle/issue4/tools/`.
- Export: `.gradle/issue4/output/lazarus-clause-0.1.0.mrpack`; verification report: `.gradle/issue4/output/verification.json`.

The installer changes only its temporary process environment, restores it in `finally`, and does not install a global tool or edit PATH. To uninstall, first check the resolved path of `.gradle/issue4/tools`, then delete that checkout-local directory; it is disposable. Packwiz's separate `.gradle/issue4/packwiz-cache` and `.gradle/issue4/output` can also be deleted and regenerated. On another platform, use an appropriate official Go archive and `go install github.com/packwiz/packwiz@ef87d964f8cbd52b3b13ea42453ef322290e2b9e` with a local `GOBIN`.

The committed `pack/` already contains the selection. To reconstruct those entries, run the following from `pack/` after installing the tool:

```powershell
$ErrorActionPreference = 'Stop'
$pw = '../.gradle/issue4/tools/bin/packwiz.exe'
$cache = '../.gradle/issue4/packwiz-cache'
$selection = @(
    @('AANobbMI', 'uMOpc5uV', 'sodium'),
    @('gvQqBUqZ', 'DDUrRVCA', 'lithium'),
    @('uXXizFIs', 'x7kQWVju', 'ferrite-core'),
    @('u6dRKJwZ', 'ufHUqt9b', 'jei'),
    @('nvQzSEkH', 'eYz2YBGT', 'jade')
)
foreach ($mod in $selection) {
    & $pw --cache $cache -y modrinth add --project-id $mod[0] --version-id $mod[1]
    if ($LASTEXITCODE -ne 0) { throw "Add failed: $($mod[2])" }
    & $pw --cache $cache -y pin $mod[2]
    if ($LASTEXITCODE -ne 0) { throw "Pin failed: $($mod[2])" }
}
& $pw --cache $cache refresh
if ($LASTEXITCODE -ne 0) { throw 'Refresh failed' }
```

Use the README export commands next. `pin` prevents automatic version updates; it does not prevent an explicit future replacement. Review and re-verify deliberate version changes. All pack hashes are generated by packwiz.

## Prism import and owner acceptance

[Prism's official import documentation](https://prismlauncher.org/wiki/getting-started/download-modpacks/) explicitly accepts a local `.mrpack` through **Add Instance → Import**. The packwiz export is a snapshot; importing it does not establish automatic packwiz updates.

1. Generate the artifact using the README commands, or use the prepared file at `C:\w\lc\4\.gradle\issue4\output\lazarus-clause-0.1.0.mrpack` on this issue checkout.
2. In Prism, **Add Instance → Import → Browse** and select that file. Give it a new name such as `Lazarus Clause baseline 0.1.0`; keep existing instances untouched.
3. Confirm **Edit → Version** shows Minecraft `1.21.1` and NeoForge `21.1.256`. Confirm **Edit → Mods** contains exactly the five releases above.
4. Select **Java 21** in this instance's **Edit → Settings → Java**. [Prism's Java guide](https://prismlauncher.org/wiki/getting-started/installing-java/) covers automatic Java downloads (Prism 9+) and instance-specific selection. Use a moderate allocation such as 4 GiB initially.

Short owner checklist — **all client checks remain PENDING** until performed:

- [ ] Import the new instance and launch to the title screen without a crash.
- [ ] Create a disposable world, confirm JEI recipes and Jade tooltips, then exit normally.
- [ ] Provide that instance's `latest.log` for `log-triage`; clean client acceptance requires its review.

Use Prism's instance **Folder / Minecraft Folder** control to find `<instance>/minecraft/logs/latest.log` (some instances use `.minecraft/logs/latest.log`). A typical Windows default is `%APPDATA%\PrismLauncher\instances\<instance-name>\minecraft\logs\latest.log`; portable Prism uses `<Prism-directory>\instances\<instance-name>\minecraft\logs\latest.log`. Crash reports, if any, are under the same Minecraft folder's `crash-reports/`. The instance's folder is authoritative.

Ghost Core is intentionally absent from this export because no hosted custom release is wired into packwiz. After the baseline check, test it separately by adding the local/CI `mods/ghostcore/build/libs/ghostcore-*.jar` through Prism **Edit → Mods**; record that as a separate integration run.

## Side and archive semantics

[Modrinth's format specification](https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack) defines `modrinth.index.json`, exact runtime dependencies, HTTPS download references, SHA1/SHA512 and side environments. The [pinned packwiz exporter](https://github.com/packwiz/packwiz/blob/ef87d964f8cbd52b3b13ea42453ef322290e2b9e/modrinth/export.go) maps `side = "client"` to client `required` / server `unsupported`; `side = "both"` maps to both `required` for the selected non-optional files. Upstream project support being optional means the mod can function without installation on that side; the pack deliberately installs its both-side entries on both sides.

The inspected export contains only `modrinth.index.json` and an empty `overrides/` directory. Its five download references use `https://cdn.modrinth.com`; no JARs, custom configs, worlds, bootstrap executable or Ghost Core are inside. Keep [export domain restrictions](https://packwiz.infra.link/reference/commands/packwiz/modrinth/export/) enabled (the default). `scripts/verify-pack.py` checks the exact pinned set, pack/index/metadata hashes, LF line endings, manifest/download agreement, sides, archive contents and, with `--download`, independent API/download hashes.

The Build workflow runs the same pinned packwiz refresh/export and independent checks, rejects a refresh that changes the committed index, and uploads the `.mrpack` plus verification JSON as the `pack-baseline` artifact. The separate `ghostcore` job builds the custom mod and runs its default GameTests.

## Isolated dedicated-server check

This uses a fresh ignored `.gradle/issue4/server/` directory, never an owner's Prism instance. Use Java 21 explicitly. The [official packwiz server procedure](https://packwiz.infra.link/tutorials/installing/packwiz-installer/#using-a-modpack-with-a-server) selects only `both`/`server` entries:

```powershell
# Terminal 1, from pack/; stop with Ctrl+C after the installation.
../.gradle/issue4/tools/bin/packwiz.exe --cache ../.gradle/issue4/packwiz-cache serve --port 18084
```

Download [`packwiz-installer-bootstrap.jar` v0.0.3](https://github.com/packwiz/packwiz-installer-bootstrap/releases/tag/v0.0.3) into the fresh server directory. Observed SHA256: `a8fbb24dc604278e97f4688e82d3d91a318b98efc08d5dbfcbcbcab6443d116c`. Run there with your explicit Java 21 executable:

```powershell
& $java21 -jar packwiz-installer-bootstrap.jar -g -s server http://127.0.0.1:18084/pack.toml
```

Confirm `server/mods/` contains Lithium, FerriteCore, JEI and Jade, and **no Sodium**. Install [NeoForge's official server](https://docs.neoforged.net/user/docs/server/) using the exact [21.1.256 installer](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.256/neoforge-21.1.256-installer.jar) and `& $java21 -jar neoforge-21.1.256-installer.jar --installServer`. Follow its EULA step for your server. For a smoke check, set `server-ip=127.0.0.1` and a free local port in `server.properties`, then start from that directory:

```powershell
& $java21 -Xms1G -Xmx2G '@libraries/net/neoforged/neoforge/21.1.256/win_args.txt' nogui
# Wait for Done, then type stop for a clean shutdown.
```

The local server check proves dedicated-server startup only; it does not establish client rendering, mod UI behavior or a clean client log. Server `latest.log` is `.gradle/issue4/server/logs/latest.log`. GhostCore's separate default GameTest log is `mods/ghostcore/run/logs/latest.log`; captured build/test console is `.gradle/issue4/ghostcore-build-gametest.log`. Coordinator log triage and owner client launch remain separate acceptance steps.
