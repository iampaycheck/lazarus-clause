# Windows x64, checkout-local installation; no persistent PATH or registry changes.
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$tools = Join-Path $repo '.gradle/issue4/tools'
$goVersion = 'go1.27.2'
$goHash = '1314008898bd40df77af4b014f777f08873dbdfbcd3d92308728ee03304fe04f'
$packwizCommit = 'ef87d964f8cbd52b3b13ea42453ef322290e2b9e'
New-Item -ItemType Directory -Force $tools | Out-Null
$archive = Join-Path $tools "$goVersion.windows-amd64.zip"
if (!(Test-Path $archive)) {
    Invoke-WebRequest "https://go.dev/dl/$goVersion.windows-amd64.zip" -OutFile $archive
}
if ((Get-FileHash $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $goHash) {
    throw 'Go archive SHA256 does not match the pinned official download.'
}
$go = Join-Path $tools 'go/bin/go.exe'
if (!(Test-Path $go)) { Expand-Archive $archive -DestinationPath $tools }
$names = @('GOBIN', 'GOPATH', 'GOCACHE', 'GOTOOLCHAIN')
$saved = @{}
foreach ($name in $names) { $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    $env:GOBIN = Join-Path $tools 'bin'
    $env:GOPATH = Join-Path $tools 'gopath'
    $env:GOCACHE = Join-Path $tools 'gocache'
    $env:GOTOOLCHAIN = 'local'
    & $go install "github.com/packwiz/packwiz@$packwizCommit"
    if ($LASTEXITCODE -ne 0) { throw "packwiz compilation failed ($LASTEXITCODE)." }
    & $go version -m (Join-Path $env:GOBIN 'packwiz.exe')
    if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect installed packwiz.' }
} finally {
    foreach ($name in $names) {
        if ($null -eq $saved[$name]) {
            Remove-Item "Env:$name" -ErrorAction SilentlyContinue
        } else {
            [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process')
        }
    }
}
Write-Output "Installed: $tools/bin/packwiz.exe"
