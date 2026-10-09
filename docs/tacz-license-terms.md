# TaCZ port: license, assets and export terms (R9 / R10)

**Checked: 2026-10-09. Two owner decisions recorded on 2026-10-09 for [#9](https://github.com/iampaycheck/lazarus-clause/issues/9); scope and remaining gates below.**
This report documents source statements and format behavior. It makes no legal conclusion, grants no
distribution approval, and does not establish that an independent custom gun pack loads successfully.
The [shortlist](gun-mod-shortlist.md) approved prototyping only. Pack inclusion ([#12](https://github.com/iampaycheck/lazarus-clause/issues/12))
and original gun packs ([#16](https://github.com/iampaycheck/lazarus-clause/issues/16)) remain subject to the
recorded limits, later verification and the separate E1 networking integration blocker.

## Confirmed findings and their limits

- Code and assets have separate statements: GPLv3 code; CC BY-NC-ND 4.0 assets in the imported original
  README and the Modrinth description; the bundled default pack has a pack-level `license` field
  declaring `CC BY-NC-ND 4.0`.
  These statements do not supply a file-by-file scope map. [Pinned README][readme], [LICENSE][license],
  [default-pack metadata][metadata], [Modrinth project API][mr-project].
- A **separate CurseForge port project exists**, owned by MUKSC and linked to the same source repository.
  It lists a NeoForge 1.21.1 r7 release. The original Forge project's file list cannot answer whether the
  port is hosted. Cross-host byte identity has not been checked. [Port project][cf-project], [r7 file][cf-file].
- No explicit author statement about modpack inclusion was located in the bounded sources below.
  Modrinth supplies a separate platform statement for specific hosted files; it is not a file-by-file
  asset-scope statement or permission to copy assets into an original gun pack. [Modrinth guidance][mr-permission].
- Custom packs can reference their own resources or resources in another pack. The checked loader does
  not demand copying a default display file, but defaults can introduce TaCZ resource dependencies.
  A pack containing no copied default files and a pack using no default resources are different targets.
  [Gun-pack guide][wiki], [display loader][display-loader].

## Evidence boundary and exact target

The approved prototype remains **1.1.8-hotfix-r7**, Modrinth version **`62q6mB5Q`**, project **`OypNE65K`**
(`tacz-1.21.1`). Its [version API][mr-version] lists NeoForge, Minecraft 1.21.1, publication
`2026-10-01T12:35:58.974869Z`, and the primary file:

- Filename: `tacz-neoforge-1.21.1-1.1.8-hotfix-r7.jar`
- Size: `57247579` bytes
- SHA-1 reported by Modrinth: `c6ef4976de69a46c4e267bd7f91c730d2dc43a4e`
- Download: [Modrinth primary jar][mr-jar]

Port source analysis is pinned to **`c471821c771a62cc8850694bbea579c2ba0b9e3a`** in
`MUKSC/TACZ-1.21.1`. Every port source link below uses that commit. The sequential mod-scout report was
consumed, then its primary statements were rechecked; bounded Java reads were located using the retained
issue #10 source and compared with the pinned raw files after normalizing line endings. This is not a
complete source-to-binary provenance check. No jar was loaded, no custom pack was built, and no export
was generated for this report. Existing integration evidence belongs to #10 / #19 and does not prove R9.
Live project pages and format documentation were checked on the date above; they are not immutable pins.

## R10: code license, asset scope and inclusion statements

| Question | What the primary source establishes | What remains unanswered |
|---|---|---|
| Code license | Root [`LICENSE`][license] contains GNU GPL version 3 text. The imported “Original README” labels code GNU GPL 3.0; [Modrinth project metadata][mr-project] identifies `GPL-3.0-only`. | These code statements do not resolve the separate asset scope. |
| Broad asset statement | The [pinned README][readme], under “Original README”, labels assets CC BY-NC-ND 4.0. The [Modrinth description][mr-project], under its imported original project description, says all assets have that license. | Neither gives a path inventory separating inherited assets, port additions, jar-level resources, scripts and default-pack resources. Do not silently narrow the statement to only gun art or only the extracted pack. |
| Default-pack declaration | [`src/main/resources/assets/tacz/custom/tacz_default_gun/assets/tacz/gunpack_info.json`][metadata] declares `CC BY-NC-ND 4.0`, authors `TACZ Dev Team`, version `1.1.8`, date `2024-06-01`. | This is a pack-level metadata declaration, not a per-file license/provenance list or an exclusion for assets elsewhere in the jar. Its date/version are metadata values, not the port release date/version. |
| Explicit author permission for modpacks | No such statement located in the pinned README, root license, default metadata, Modrinth project description or CurseForge port description. Those pages link downloads, gun packs and source. [README][readme], [Modrinth][mr-project], [CurseForge][cf-project]. | No located statement is not a prohibition, permission grant, or proof that no statement exists elsewhere. An attributable statement specifying version, files and distribution routes would resolve this evidentiary gap. |
| Modrinth platform guidance | [Obtaining modpack permissions][mr-permission], Step 1, says a specific file hosted on Modrinth may be used in Modrinth modpacks, explaining the upload grant to Modrinth. The exact r7 jar is listed in [version `62q6mB5Q`][mr-version]. | This is platform guidance for that hosted file. It does not answer permission for extracted/modified default assets, independently distributed server ZIPs or new gun-pack content. |
| CurseForge hosting identity | [Project `1353462`][cf-project], slug `tacz-1-21-1`, is the unofficial port by MUKSC; its Source link targets `MUKSC/TACZ-1.21.1`. The [pinned port README][readme] and [Modrinth description][mr-project] link that project. [File `9027337`][cf-file] lists r7, the same filename, NeoForge / 1.21.1, uploaded 2026-10-01 by MUKSC. | Matching release metadata is not matching bytes. No CurseForge-versus-Modrinth digest comparison was performed; use the Modrinth ID/hash as the exact target until that is checked. Hosting is not a substitute for asset-scope evidence. |

The referenced asset license's primary [Creative Commons deed][cc-deed] links its [legal code][cc-code].
This report records the label and scope uncertainty; it does not apply those terms to Lazarus Clause or
decide whether an intended use is an adaptation, commercial use, or permitted distribution.

## R9: custom pack files versus runtime dependencies

**Answer supported by sources:** an original pack can declare its own namespace and resource identifiers;
no checked gun-display validation requires copying the default display JSON. **Still unanswered:** a
complete, functioning pack without any default-pack resources has not been demonstrated, nor has a
runtime independent of all TaCZ jar assets. The [official guide][wiki] describes cross-pack references
and says not to reuse existing namespaces such as `tacz` for one's own pack, with exceptions for refit
packs. That documents a mechanism, not asset reuse permission. Its source is also
[pinned at `a44ab82757c0599e3c89bdd8b2fea1c6e04e0ae8`][wiki-pin],
path `docs/zh/gunpack/02_first_pack.md`; it is general upstream guidance, not a port load result.

### What the bounded source reads establish

Java paths below are relative to `src/main/java/com/tacz/guns/` at the port commit above.

| Resource or mechanism | Requirement / fallback seen in source | Copying versus referencing |
|---|---|---|
| Pack metadata | [Guide][wiki] requires root `gunpack.meta.json`; `dependencies` is optional. [`resource/GunPackLoader.java`][pack-loader] reads that name and checks dependencies when present. | Create original metadata. The dependency field is a mod-version constraint, not an asset-copy instruction. |
| Gun model and texture | [`client/resource/GunDisplayInstance.java`][display-loader], `checkTextureAndModel`, requires `model`, a resolved model and model data, plus a `texture` identifier. | The check does not restrict identifiers to `tacz`; it does not require a particular default model/display file. |
| Optional display resources | [`client/resource/pojo/display/gun/GunDisplay.java`][display-pojo] makes animation, LOD, HUD, slot, muzzle flash and sound fields nullable. | Nullable fields alone do not establish functional independence; consult loader fallbacks. |
| Animation | [Display loader][display-loader], `checkAnimation`, accepts omission; supplied IDs must resolve. `default_animation` or `use_default_animation` adds fallback animations. | Own IDs can be supplied. Default fallback use references runtime resources; it does not copy them into the custom ZIP. |
| State machine | [Display loader][display-loader] falls back to `tacz:default_state_machine` if omitted and throws if the script is absent. | Own script ID can be supplied. Omission introduces a default-pack dependency. |
| Third-person / sounds | [Display loader][display-loader] defaults third-person player animation to `tacz:rifle_default.player_animation`; a nonempty sound map receives missing TaCZ sound entries. | Explicit original replacements need an audit; these defaults are dependencies without copied files. |
| Engine animation and model resources | [`client/event/ReloadResourceEvent.java`][reload] calls [`InternalAssetLoader.onResourceReload`][internal-loader]. It loads jar-level pistol/rifle animations and bedrock models (smith table, target, target minecart, bullet, statue) through Minecraft's resource manager regardless of a gun's fallback choice. | Omitting per-gun fallback does not stop engine loading. Replacing resources at those IDs would be a separate, untested resource override; no independence claim follows. |
| Shaders | [Checked display schema][display-pojo] has no per-gun shader field. | No mandatory shader-copy requirement was established. A working override of engine shaders/resources, or removing their dependence, remains unverified. |

### Concrete resource locations

These paths distinguish resources outside the default pack from files contained in it. Presence and
references do not resolve which license covers an individual file.

| Location at the pinned port commit | Evidence / use |
|---|---|
| Jar-level `src/main/resources/assets/tacz/animations/pistol_default.animation.json` and `rifle_default.animation.json` | [Pistol file][pistol], [rifle file][rifle]; [internal loader][internal-loader] loads both via Minecraft's resource manager. These are outside `custom/tacz_default_gun/`. |
| Default pack `src/main/resources/assets/tacz/custom/tacz_default_gun/assets/tacz/display/guns/glock_17_display.json` | [Display JSON][glock] references `tacz:glock_17`, pistol fallback animation, `tacz:pistol_default.player_animation`, `tacz:glock_17_state_machine` and `tacz:flash/common_muzzle_flash`. This is an example, not a mandatory template. |
| Same default-pack root, `player_animator/pistol_default.player_animation.json`, `textures/flash/common_muzzle_flash.png`, `scripts/glock_17_state_machine.lua`, `scripts/default_state_machine.lua` | [Player animation][player-animation], [flash][flash], [gun script][gun-script], [fallback script][default-script]. References resolve to default-pack content without requiring copies in a new pack. |

[`GunMod.registerDefaultExtraGunPack`][gunmod] registers the jar's default-pack directory through
[`ResourceManager.registerExportResource`][resource-manager]. [GunPackLoader][pack-loader] copies registered
directories to the game `tacz/` directory on the first discovery in each game start unless
[`gunpack.DefaultPackDebug`][preload] is true (default false). The [bundled README][pack-readme] describes
preventing overwrite and backups. This is an extraction/overwrite control, not demonstrated complete
disabling of default content: existing files can still be discovered, and jar-level assets remain.

Technical custom-pack routes described by the evidence (the owner-approved direction is below):

- **Original files with explicit cross-pack dependencies:** author original display/data/model/texture/
  animation/script files and list every TaCZ ID used at runtime. This avoids assuming that a ZIP with no
  copied files is independent. Scope/permission questions remain as documented above.
- **Attempt no default-pack resource dependency:** supply original IDs and scripts, audit indirect imports,
  sounds, third-person resources and engine reloads, and verify a throwaway instance with extracted
  defaults absent and extraction prevented. A client resource-load check is needed; a headless server
  pass alone cannot prove animation/render behavior. No such implementation or test is part of this report.

## R10: distribution routes and concrete contents

These are documented format capabilities and platform statements, **not clearance for this jar or its
assets**. A downloaded jar still contains its bundled assets. A reference-only manifest is not a licensing
exemption. No current Lazarus Clause export has been inspected because `pack/` is unchanged.

| Concrete route | References and downloads | Bundled files / route-specific limits |
|---|---|---|
| Hosted packwiz files + packwiz-installer | [`.pw.toml` specification][pw-mod] describes external file URLs and hashes; installer downloads them. | [Index specification][pw-index] also tracks ordinary pack files. Hosting a jar or extracted assets as ordinary files serves their bytes; metadata does not make those reference-only. Exact r7 can be pinned by its Modrinth ID/hash; this report adds nothing to the pack. |
| `.mrpack` / `packwiz modrinth export` | [Modrinth specification][mr-format]: `modrinth.index.json` has `files[].path`, SHA-1/SHA-512 hashes, HTTPS `downloads`, and side `env`. `cdn.modrinth.com` is an allowed download domain. | `overrides`, `client-overrides`, `server-overrides` carry actual files copied into the instance. [packwiz export docs][pw-mr] warn that missing Modrinth metadata causes jars to be put in the ZIP. Keep exact Modrinth metadata to pursue a download-reference export; verify actual contents later. Hosted-file guidance is [separate][mr-permission]. |
| CurseForge **client profile export** / `packwiz curseforge export` | [CurseForge submission docs][cf-export] specify `manifest.json` references for CurseForge-hosted mods. The port has project/file IDs `1353462` / `9027337`. | `overrides` contains actual configs/scripts/resources; approved non-CurseForge mods can be bundled in `overrides/mods`. [packwiz docs][pw-cf] say missing CurseForge metadata bundles jars. A Modrinth-only entry does not become a CurseForge reference merely because a matching project exists. Byte identity and metadata mapping remain to check. |
| **packwiz server bootstrap**, a download route | [Installer server instructions][pw-installer] use `packwiz-installer-bootstrap.jar -g -s server` and a hosted `pack.toml`; download mods marked `both` or `server`. | This documents installation, not a universal server ZIP. Local installation creates actual files. Publishing an installed directory is a separate bundling choice. |
| **Server installation from `.mrpack`** | [Format][mr-format] supplies server `env` and downloadable entries; [Modrinth guide][mr-general] names server installation tools. | `server-overrides` applies after ordinary overrides. This is a concrete format route, not proof of a particular server export's contents or permissions. |
| **CurseForge manual server ZIP**, per official tutorial | [Server Packs Tutorial][cf-server] (2025-05-22) instructs copying the installed `mods`, `config` and relevant `kubejs` directories into a new ZIP, then uploading an additional file. | Following that recipe with the port installed in `mods/` bundles the jar bytes. That inference is specific to this recipe; client manifest behavior does not describe this archive. Treatment/acceptance of this jar and additional TaCZ directories in a submitted server ZIP remains unanswered. |

CurseForge's [submission guidance][cf-export] requires hosted mods to be referenced in the client manifest
and documents review conditions for non-CurseForge overrides. This identifies a potential client route;
it does not settle this project's mixed code/asset scope or prove submission acceptance. Its text also
still describes Forge/Fabric submissions only, so it is not evidence of an accepted NeoForge pack export.
The [API schema][cf-api] defines nullable `allowModDistribution`; **this project's actual value was not
retrieved**. Schema examples are not project permission, and no third-party download entitlement is inferred.

There is no universal “server pack” archive behavior established by these sources. For another exporter,
its exact tool/version, configuration, official documentation and an inspected output would be needed.
The [packwiz CurseForge guide][pw-cf] documents `--side` filtering; that selects mods for the same export
route and does not turn a manifest ZIP into the manual server ZIP described above.

## Unanswered questions and evidence needed

| Unanswered | Evidence that would resolve or narrow it |
|---|---|
| Exact scope of CC BY-NC-ND across jar assets, default-pack files, scripts, port additions and third-party material | A primary, attributable file/path scope and provenance statement for the pinned release. Do not replace the broad statements with an invented narrow scope. |
| Explicit author modpack statement beyond platform guidance | A located author statement naming intended distribution routes and included files; none is supplied here. No authors were contacted. |
| Fully independent, functional original pack | Original pack inventory and dependency audit, followed by separate port-specific client/resource and server loading evidence with defaults absent. Source reads are not that test. |
| CurseForge r7 byte identity and exporter metadata mapping | Download/digest comparison with Modrinth r7, then inspect generated manifest/overrides with exact project/file IDs. The filename/date match alone is insufficient. |
| Route-specific publication acceptance / distribution settings | Actual project setting evidence and the chosen export's contents; server ZIP acceptance cannot be inferred from client profile rules. |

## Owner decisions recorded for #9 — 2026-10-09

The owner supplied **two separate answers** on 2026-10-09, recorded here and in the
[issue #9 owner-decision record](https://github.com/iampaycheck/lazarus-clause/issues/9#issuecomment-6077245313):

1. **Pack inclusion / distribution:** "Accept the documented terms/unknowns for packwiz or Modrinth download references; defer bundled server ZIPs"
   This accepts the documented terms and unresolved questions as the project direction for the exact
   r7 target above, limited to packwiz or Modrinth download references. Verify the eventual manifest,
   metadata and overrides actually preserve that route before publishing. Bundled server ZIPs are
   deferred; CurseForge export acceptance, cross-host byte identity, exporter metadata mapping and
   the project's actual `allowModDistribution` value remain unresolved and are not approved by this answer.
2. **Original Company gun packs:** "Approve original files only; prove no default-pack resource dependency before publishing"
   Only original files are approved as the project direction. No copying or reusing default-pack files
   is approved. Before publishing, prove no direct or indirect default-pack resource dependencies with
   an inventory/dependency audit and separate client/resource and server loading evidence with extracted
   defaults absent and extraction prevented. A ZIP with no copied files alone is insufficient. The TaCZ
   jar's engine dependency is distinct and may remain; independence from all jar-level resources is not
   required by this answer and has not been demonstrated.

These are owner acceptance/direction, **not legal verdicts or authors' permission**. Exact asset-license
scope and explicit author modpack statements remain unanswered. This records the #9 owner decision
criterion; it does not unconditionally unblock #12 or #16. Their separate E1 networking integration
failure remains a blocker, alongside the route verification and original-pack proof required above.
PR #20 can proceed to owner review after the documentation closeout and green final CI; the owner merges.

## Primary sources

All links were checked on **2026-10-09**. Port links pin the full commit specified above; the wiki has its
own pin. Platform documentation is live and should be rechecked for the eventual export.

[license]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/LICENSE
[readme]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/readme.md
[metadata]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/resources/assets/tacz/custom/tacz_default_gun/assets/tacz/gunpack_info.json
[mr-project]: https://api.modrinth.com/v2/project/OypNE65K
[mr-version]: https://api.modrinth.com/v2/version/62q6mB5Q
[mr-jar]: https://cdn.modrinth.com/data/OypNE65K/versions/62q6mB5Q/tacz-neoforge-1.21.1-1.1.8-hotfix-r7.jar
[cf-project]: https://www.curseforge.com/minecraft/mc-mods/tacz-1-21-1
[cf-file]: https://www.curseforge.com/minecraft/mc-mods/tacz-1-21-1/files/9027337
[mr-permission]: https://support.modrinth.com/en/articles/8797527-obtaining-modpack-permissions
[cc-deed]: https://creativecommons.org/licenses/by-nc-nd/4.0/
[cc-code]: https://creativecommons.org/licenses/by-nc-nd/4.0/legalcode.en
[wiki]: https://tacwiki.mcma.club/zh/gunpack/02_first_pack.html
[wiki-pin]: https://github.com/MCModderAnchor/tacwiki/blob/a44ab82757c0599e3c89bdd8b2fea1c6e04e0ae8/docs/zh/gunpack/02_first_pack.md
[pack-loader]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/resource/GunPackLoader.java
[display-loader]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/client/resource/GunDisplayInstance.java
[display-pojo]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/client/resource/pojo/display/gun/GunDisplay.java
[reload]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/client/event/ReloadResourceEvent.java
[internal-loader]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/client/resource/InternalAssetLoader.java
[pistol]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/resources/assets/tacz/animations/pistol_default.animation.json
[rifle]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/resources/assets/tacz/animations/rifle_default.animation.json
[glock]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/resources/assets/tacz/custom/tacz_default_gun/assets/tacz/display/guns/glock_17_display.json
[player-animation]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/resources/assets/tacz/custom/tacz_default_gun/assets/tacz/player_animator/pistol_default.player_animation.json
[flash]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/resources/assets/tacz/custom/tacz_default_gun/assets/tacz/textures/flash/common_muzzle_flash.png
[gun-script]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/resources/assets/tacz/custom/tacz_default_gun/assets/tacz/scripts/glock_17_state_machine.lua
[default-script]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/resources/assets/tacz/custom/tacz_default_gun/assets/tacz/scripts/default_state_machine.lua
[gunmod]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/GunMod.java
[resource-manager]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/api/resource/ResourceManager.java
[preload]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/java/com/tacz/guns/config/PreLoadConfig.java
[pack-readme]: https://github.com/MUKSC/TACZ-1.21.1/blob/c471821c771a62cc8850694bbea579c2ba0b9e3a/src/main/resources/assets/tacz/custom/tacz_default_gun/README.txt
[pw-mod]: https://packwiz.infra.link/reference/pack-format/mod-toml/
[pw-index]: https://packwiz.infra.link/reference/pack-format/index-toml/
[pw-installer]: https://packwiz.infra.link/tutorials/installing/packwiz-installer/
[pw-mr]: https://packwiz.infra.link/tutorials/hosting/modrinth/
[pw-cf]: https://packwiz.infra.link/tutorials/hosting/curseforge/
[mr-format]: https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack
[mr-general]: https://support.modrinth.com/en/articles/8802250-modpacks-on-modrinth
[cf-export]: https://support.curseforge.com/support/solutions/articles/9000197908-exporting-a-modpack-for-curseforge-project-submission
[cf-server]: https://blog.curseforge.com/server-packs-tutorial/
[cf-api]: https://docs.curseforge.com/rest-api/#mod
