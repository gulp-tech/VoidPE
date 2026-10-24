# Скачивает последний релиз PowerNukkitX в текущую папку.
# Запуск:  powershell -ExecutionPolicy Bypass -File .\download.ps1
$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

Write-Host '-> Запрашиваю последний релиз PowerNukkitX...'
$r = Invoke-RestMethod -Uri 'https://api.github.com/repos/PowerNukkitX/PowerNukkitX/releases/latest' `
    -Headers @{ 'User-Agent' = 'VoidPE' }

$a = $r.assets | Where-Object { $_.name -eq 'powernukkitx.jar' } | Select-Object -First 1
if (-not $a) {
    Write-Host '[x] В релизе нет powernukkitx.jar. Скачай вручную:'
    Write-Host '    https://github.com/PowerNukkitX/PowerNukkitX/releases'
    exit 1
}

Write-Host "-> Версия $($r.tag_name), скачиваю $($a.name)..."
Invoke-WebRequest -Uri $a.browser_download_url -OutFile 'powernukkitx.jar'

$mb = [math]::Round((Get-Item 'powernukkitx.jar').Length / 1MB, 1)
Write-Host "OK: powernukkitx.jar ($mb MB). Запуск: start.bat"
