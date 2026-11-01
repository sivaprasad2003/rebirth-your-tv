# System-wide AdGuard Private DNS Switcher for Android TV / Google TV.
#
# Usage:
#     powershell -ExecutionPolicy Bypass -File tools\adblock.ps1 -Action on -TvHost 192.168.1.50
#     powershell -ExecutionPolicy Bypass -File tools\adblock.ps1 -Action off -TvHost 192.168.1.50
#     powershell -ExecutionPolicy Bypass -File tools\adblock.ps1 -Action status -TvHost 192.168.1.50
param(
    [ValidateSet("on", "off", "status", "enable", "disable")]
    [string]$Action = "status",
    [string]$DnsHost = "dns.adguard-dns.com",
    [string]$TvHost = $env:TV_HOST
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

if ($TvHost) {
    $env:TV_HOST = $TvHost
}

switch ($Action.ToLower()) {
    { $_ -in "on", "enable" } {
        Write-Host "Enabling Private DNS ($DnsHost)..." -ForegroundColor Cyan
        python "$ScriptDir\tv.py" "settings put global private_dns_mode hostname"
        python "$ScriptDir\tv.py" "settings put global private_dns_specifier $DnsHost"
        Write-Host "Private DNS enabled: $DnsHost" -ForegroundColor Green
    }
    { $_ -in "off", "disable" } {
        Write-Host "Disabling Private DNS..." -ForegroundColor Cyan
        python "$ScriptDir\tv.py" "settings put global private_dns_mode off"
        try { python "$ScriptDir\tv.py" "settings delete global private_dns_specifier" } catch {}
        Write-Host "Private DNS disabled." -ForegroundColor Yellow
    }
    "status" {
        Write-Host "Checking Private DNS status..." -ForegroundColor Cyan
        $mode = python "$ScriptDir\tv.py" "settings get global private_dns_mode" 2>$null
        $spec = python "$ScriptDir\tv.py" "settings get global private_dns_specifier" 2>$null
        Write-Host "Mode:      $mode"
        Write-Host "Specifier: $spec"
    }
}

