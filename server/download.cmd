@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo [i] Скачиваю последний релиз Lumi...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "$r = Invoke-RestMethod -Uri 'https://api.github.com/repos/KoshakMineDEV/Lumi/releases/latest' -Headers @{ 'User-Agent'='VoidPE' };" ^
  "$a = $r.assets | Where-Object { $_.name -like '*.jar' } | Select-Object -First 1;" ^
  "if (-not $a) { Write-Host '[x] jar не найден в релизе. Скачай вручную: https://dl.lumi.su/'; exit 1 };" ^
  "Write-Host ('[i] ' + $r.tag_name + ' -> ' + $a.name);" ^
  "Invoke-WebRequest -Uri $a.browser_download_url -OutFile $a.name;" ^
  "Write-Host '[i] Готово. Запуск: start.cmd'"

pause
