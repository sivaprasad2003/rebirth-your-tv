# Smart TV Auto-Detector: queries the connected TV model over ADB and matches
# it against tested profiles in examples/.
#
# Usage:
#     powershell -ExecutionPolicy Bypass -File tools\quick_debloat.ps1 -TvHost 192.168.1.50
param(
    [string]$TvHost = $env:TV_HOST
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoDir = Split-Path -Parent $ScriptDir

if ($TvHost) {
    $env:TV_HOST = $TvHost
}

Write-Host "Probing connected Smart TV..." -ForegroundColor Cyan
python "$ScriptDir\tv.py" --probe

$Brand = (python "$ScriptDir\tv.py" "getprop ro.product.brand" 2>$null).Trim().ToLower()
$Model = (python "$ScriptDir\tv.py" "getprop ro.product.model" 2>$null).Trim().ToLower()
$Device = (python "$ScriptDir\tv.py" "getprop ro.product.device" 2>$null).Trim().ToLower()

Write-Host "Detected: Brand='$Brand', Model='$Model', Device='$Device'" -ForegroundColor Green

$Match = $null
$Examples = Get-ChildItem -Path "$RepoDir\examples" -Directory

foreach ($dir in $Examples) {
    $debloat = Join-Path $dir.FullName "debloat.txt"
    if (-not (Test-Path $debloat)) { continue }

    $base = $dir.Name.ToLower()
    $modelSlug = $Model.Replace(" ", "-")

    if ($Model -and $base.Contains($modelSlug)) {
        $Match = $debloat
        break
    }
    if ($Device -and $base.Contains($Device)) {
        $Match = $debloat
        break
    }

    $firstLine = (Get-Content -Path $debloat -TotalCount 1).ToLower()
    if ($Model -and $firstLine.Contains($Model)) {
        $Match = $debloat
        break
    }
}

if ($Match) {
    Write-Host "Found matching debloat profile: $Match" -ForegroundColor Cyan
    Write-Host "Applying profile..." -ForegroundColor Green
    & powershell -ExecutionPolicy Bypass -File "$ScriptDir\disable.ps1" -TvHost $TvHost $Match
} else {
    Write-Host "No exact match found in examples/ for Model: '$Model'." -ForegroundColor Yellow
    Write-Host "Available profiles for brand '$Brand':" -ForegroundColor Cyan
    Get-ChildItem -Path "$RepoDir\examples" -Directory | Where-Object { $_.Name.ToLower().Contains($Brand) } | ForEach-Object { $_.Name }
    Write-Host "To apply manually: powershell -ExecutionPolicy Bypass -File tools\disable.ps1 examples\<model>\debloat.txt"
}

