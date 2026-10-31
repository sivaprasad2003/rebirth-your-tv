# Windows version of backup.sh: full system state backup of the TV before debloating.
# Saves package states (installed, enabled, disabled), system properties, and Android settings
# into backup/<model>_<timestamp>/
#
# Usage: tools\backup.ps1 [-TvHost 192.168.1.50]
param(
    [string]$TvHost = $env:TV_HOST
)
$ErrorActionPreference = 'Stop'
$OutputEncoding = New-Object System.Text.UTF8Encoding $false
$repo = Split-Path -Parent $PSScriptRoot
$py = Join-Path $repo 'tools\venv\Scripts\python.exe'
if (-not (Test-Path $py)) { throw 'run: python -m venv tools\venv; tools\venv\Scripts\python.exe -m pip install -r tools\requirements.txt' }
if (-not $TvHost) { throw 'pass -TvHost <tv ip> or set $env:TV_HOST' }
$env:TV_HOST = $TvHost

Write-Host "Connecting to TV at $TvHost..." -ForegroundColor Cyan

# Gather device info
$model = (& $py -I (Join-Path $repo 'tools\tv.py') 'getprop ro.product.model' 2>$null) -join ''
$device = (& $py -I (Join-Path $repo 'tools\tv.py') 'getprop ro.product.device' 2>$null) -join ''
$version = (& $py -I (Join-Path $repo 'tools\tv.py') 'getprop ro.build.version.release' 2>$null) -join ''
$sdk = (& $py -I (Join-Path $repo 'tools\tv.py') 'getprop ro.build.version.sdk' 2>$null) -join ''
$model = if ($model) { $model.Trim() } else { "UnknownTV" }
$device = if ($device) { $device.Trim() } else { "unknown" }
$version = if ($version) { $version.Trim() } else { "unknown" }
$sdk = if ($sdk) { $sdk.Trim() } else { "unknown" }

$cleanModel = ($model -replace '[^\w\-]', '_').Trim('_')
$timestamp = (Get-Date).ToString("yyyyMMdd_HHmmss")
$backupDir = Join-Path $repo "backup\${cleanModel}_${timestamp}"
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

Write-Host "Creating backup in: $backupDir" -ForegroundColor Cyan

# 1. Packages All
$allPkgs = & $py -I (Join-Path $repo 'tools\tv.py') 'pm list packages -u'
$allPkgs | Out-File -Encoding utf8 (Join-Path $backupDir 'packages_all.txt')

# 2. Packages Disabled
$disabledPkgs = & $py -I (Join-Path $repo 'tools\tv.py') 'pm list packages -d'
$disabledPkgs | Out-File -Encoding utf8 (Join-Path $backupDir 'packages_disabled.txt')

# 3. Packages Enabled
$enabledPkgs = & $py -I (Join-Path $repo 'tools\tv.py') 'pm list packages -e'
$enabledPkgs | Out-File -Encoding utf8 (Join-Path $backupDir 'packages_enabled.txt')

# 4. Current Home Activity
$currentHome = & $py -I (Join-Path $repo 'tools\tv.py') 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME'
$currentHome | Out-File -Encoding utf8 (Join-Path $backupDir 'current_home.txt')

# 5. Device Properties (getprop)
$props = & $py -I (Join-Path $repo 'tools\tv.py') 'getprop'
$props | Out-File -Encoding utf8 (Join-Path $backupDir 'properties.txt')

# 6. Settings (global, secure, system)
$settingsGlobal = & $py -I (Join-Path $repo 'tools\tv.py') 'settings list global' 2>$null
$settingsGlobal | Out-File -Encoding utf8 (Join-Path $backupDir 'settings_global.txt')

$settingsSecure = & $py -I (Join-Path $repo 'tools\tv.py') 'settings list secure' 2>$null
$settingsSecure | Out-File -Encoding utf8 (Join-Path $backupDir 'settings_secure.txt')

$settingsSystem = & $py -I (Join-Path $repo 'tools\tv.py') 'settings list system' 2>$null
$settingsSystem | Out-File -Encoding utf8 (Join-Path $backupDir 'settings_system.txt')

$totalCount = ($allPkgs | Measure-Object).Count
$disabledCount = ($disabledPkgs | Measure-Object).Count

# Save metadata JSON
$meta = [PSCustomObject]@{
    model = $model
    device = $device
    android_version = $version
    sdk = $sdk
    timestamp = $timestamp
    total_packages = $totalCount
    disabled_packages = $disabledCount
    current_home = ($currentHome | Select-Object -Last 1)
}
$meta | ConvertTo-Json | Out-File -Encoding utf8 (Join-Path $backupDir 'backup_metadata.json')

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host " RebirthYourTV Backup Engine: Completed Successfully!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host " TV Model:       $model ($device)"
Write-Host " Android OS:     $version (SDK $sdk)"
Write-Host " Total Packages: $totalCount"
Write-Host " Disabled Apps:  $disabledCount"
Write-Host " Backup Folder:  $backupDir"
Write-Host "============================================================" -ForegroundColor Green
Write-Host " To restore packages anytime, run: tools\restore.ps1 <debloat.txt>" -ForegroundColor Yellow
Write-Host ""

