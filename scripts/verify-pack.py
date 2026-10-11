"""Read-only integrity checks for the issue #4 baseline (Python 3.11+)."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import tomllib
from urllib.parse import urlparse
from urllib.request import Request, urlopen
from zipfile import ZipFile


def require(condition, message):
    if not condition:
        raise ValueError(message)


def digest(data, algorithm):
    return hashlib.new(algorithm, data).hexdigest()


def fetch(url):
    with urlopen(Request(url, headers={"User-Agent": "lazarus-clause/issue4"}), timeout=120) as response:
        return response.read()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path)
    parser.add_argument("--download", action="store_true", help="Verify live API metadata and every remote JAR")
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    pack_dir = root / "pack"
    pack = tomllib.loads((pack_dir / "pack.toml").read_text())
    require(pack["versions"] == {"minecraft": "1.21.1", "neoforge": "21.1.256"}, "Unexpected pack runtime versions")
    index_bytes = (pack_dir / pack["index"]["file"]).read_bytes()
    require(digest(index_bytes, pack["index"]["hash-format"]) == pack["index"]["hash"], "Pack index hash mismatch")
    index = tomllib.loads(index_bytes.decode())
    expected = {"uMOpc5uV", "DDUrRVCA", "x7kQWVju", "ufHUqt9b", "eYz2YBGT"}
    mods = {}
    for entry in index["files"]:
        data = (pack_dir / entry["file"]).read_bytes()
        require(b"\r" not in data, f"Non-LF pack file: {entry['file']}")
        require(digest(data, entry.get("hash-format", index["hash-format"])) == entry["hash"], f"Index entry hash mismatch: {entry['file']}")
        require(entry.get("metafile") is True, "Baseline unexpectedly contains an override")
        mod = tomllib.loads(data.decode())
        require(mod.get("pin") is True, f"Unpinned mod: {mod['name']}")
        mods["mods/" + mod["filename"]] = mod
    for path in pack_dir.rglob("*"):
        if path.is_file():
            require(b"\r" not in path.read_bytes(), f"Non-LF pack file: {path}")
    require({m["update"]["modrinth"]["version"] for m in mods.values()} == expected, "Unexpected baseline versions")
    with ZipFile(args.archive) as archive:
        require(set(archive.namelist()) == {"modrinth.index.json", "overrides/"}, "Unexpected bundled files or overrides")
        manifest = json.loads(archive.read("modrinth.index.json"))
    require(manifest["formatVersion"] == 1 and manifest["game"] == "minecraft", "Invalid mrpack format")
    require(manifest["dependencies"] == {"minecraft": "1.21.1", "neoforge": "21.1.256"}, "Unexpected runtime versions")
    require(manifest["versionId"] == pack["version"] and manifest["name"] == pack["name"], "Pack identity mismatch")
    require(len(manifest["files"]) == len(mods) == 5, "Expected exactly five mods")
    require({f["path"] for f in manifest["files"]} == set(mods), "Export file set mismatch")
    evidence = []
    for file in manifest["files"]:
        path = PurePosixPath(file["path"])
        require(not path.is_absolute() and ".." not in path.parts, "Unsafe manifest path")
        mod = mods[file["path"]]
        version_id = mod["update"]["modrinth"]["version"]
        side = "client" if version_id == "uMOpc5uV" else "both"
        require(mod["side"] == side, f"Wrong pack side: {mod['name']}")
        env = {"client": "required", "server": "unsupported" if side == "client" else "required"}
        require(file["env"] == env, f"Wrong export environment: {mod['name']}")
        require(file["downloads"] == [mod["download"]["url"]], "Export download mismatch")
        require(all(urlparse(url).scheme == "https" and urlparse(url).hostname == "cdn.modrinth.com" for url in file["downloads"]), "Unexpected download host")
        require(set(file["hashes"]) == {"sha1", "sha512"}, "Missing manifest hashes")
        require(file["hashes"][mod["download"]["hash-format"]] == mod["download"]["hash"], "Pack/export hash mismatch")
        if args.download:
            version = json.loads(fetch(f"https://api.modrinth.com/v2/version/{version_id}"))
            require("neoforge" in version["loaders"] and "1.21.1" in version["game_versions"], "Incompatible API metadata")
            require(version["version_type"] == "release", "Expected a stable release")
            require(version["project_id"] == mod["update"]["modrinth"]["mod-id"], "Modrinth project mismatch")
            require(not any(d["dependency_type"] == "required" for d in version["dependencies"]), "Unexpected required dependency")
            api_file = next(f for f in version["files"] if f["primary"])
            require(api_file["filename"] == mod["filename"], "Primary API filename mismatch")
            require(api_file["url"] == file["downloads"][0] and api_file["hashes"] == file["hashes"] and api_file["size"] == file["fileSize"], "API/export mismatch")
            jar = fetch(api_file["url"])
            require(len(jar) == file["fileSize"], "Download size mismatch")
            for algorithm, value in file["hashes"].items():
                require(digest(jar, algorithm) == value, f"Download {algorithm} mismatch")
        evidence.append({"path": file["path"], "version_id": version_id, "env": file["env"], "hashes": file["hashes"], "downloads": file["downloads"]})
    report = {"artifact": str(args.archive.resolve()), "size": args.archive.stat().st_size,
              "sha256": digest(args.archive.read_bytes(), "sha256"), "live_downloads_verified": args.download,
              "dependencies": manifest["dependencies"], "archive_entries": ["modrinth.index.json", "overrides/"], "files": evidence}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2) + "\n")
    print(f"PASS: five pinned mods; LF/index/export hashes; sides; no bundled JARs; live downloads={args.download}")
    print(f"Artifact: {report['artifact']} ({report['size']} bytes; SHA256 {report['sha256']})")


if __name__ == "__main__":
    main()
