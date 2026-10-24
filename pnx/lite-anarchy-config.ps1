# lite-anarchy-config.ps1 — настройка pnx.yml под ЛАЙТ-анархию (FunTime/HolyWorld style)
# Гриф и PvP разрешены, но: безопасный спавн, регионы, античит, запрет крашей.
#
# Запуск из папки сервера (PNX остановить командой stop!):
#   powershell -ExecutionPolicy Bypass -File .\lite-anarchy-config.ps1
#
# Параметр -WhatIfOnly — показать, что изменилось бы, без записи.

param([switch]$WhatIfOnly)

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

$configPath = Join-Path $PWD 'pnx.yml'
if (-not (Test-Path $configPath)) {
    Write-Host "[x] pnx.yml не найден в $PWD" -ForegroundColor Red
    exit 1
}

# --- 1. Сервер должен быть остановлен --------------------------------------
$running = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match 'powernukkitx\.jar' }
if ($running) {
    Write-Host "[!] Сервер PNX запущен — сначала 'stop' в его консоли." -ForegroundColor Yellow
    Write-Host "    Иначе PNX перезапишет pnx.yml при выключении." -ForegroundColor Yellow
    exit 2
}

# --- 2. Значения под лайт-анархию ------------------------------------------
$targets = [ordered]@{
    # спавн и общий доступ
    'motd'                              = @('§6§lLITE ANARCHY §8| §fvoidpe',  'название сервера')
    'maxPlayers'                        = @('100',                            'лимит игроков')
    'xboxAuth'                          = @('true',                           'true = только Xbox-аккаунты')
    'safeSpawn'                         = @('true',                           'не спавнить в лаве/камне')
    'autosaveDelay'                     = @('6000',                           'автосейв 5 мин')

    # игроки
    'savePlayerData'                    = @('true',                           'сохранять инвентарь')
    'checkMovement'                     = @('true',                           'античит движения (PNX)')
    'forceSkinTrusted'                  = @('true',                           'сторонние скины')
    'spawnRadius'                       = @('16',                             'разброс спавна')

    # геймплей лайт-анархии
    'spawnProtection'                   = @('32',                             'безопасная зона спавна (32 блока)')
    'pvp'                               = @('true',                           'PvP везде')
    'difficulty'                        = @('2',                              '2 = Normal (Hard слишком жёстко)')
    'gamemode'                          = @('0',                              '0 = выживание')
    'forceGamemode'                     = @('false',                          'OP может переключиться')
    'enableExplosionBlockDamage'        = @('true',                           'взрывы ломают мир ВНЕ регионов')
    'enableRedstone'                    = @('true',                           'редстоун включён')
    'tickRedstone'                      = @('true',                           'редстоун тикает')
    'enableCommandBlocks'               = @('false',                          'командные блоки выключены (краши)')
    'enableHunger'                      = @('true',                           'голод включён')
    'enableEntitySpawning'              = @('true',                           'мобы спавнятся')
    'enableMobAI'                       = @('true',                           'ИИ мобов включён')
    'viewDistance'                      = @('6',                              'дальность прорисовки')
    'announceAchievements'              = @('false',                          'не спамить достижениями')

    # утечки/античит/антикраш
    'overrideServerAuthBlockBreaking'   = @('true',                           'ОБЯЗАТЕЛЬНО для Astra (античит)')
    'packetLimit'                       = @('4000',                           'пакетов на IP (антифлуд)')
    'globalPacketLimit'                 = @('100000',                         'глобальный лимит пакетов')
    'entitySpawnCap'                    = @('512',                            'потолок сущностей на мир')
    'chunkUnloadDelay'                  = @('15000',                          'выгрузка чанков, мс')
    'tickRadius'                        = @('4',                              'радиус тика чанков')

    # сеть/мониторинг
    'enableQuery'                       = @('true',                           'query для мониторингов')
    'queryPlugins'                      = @('false',                          'не палить список плагинов')
}

# --- 3. Бэкап ---------------------------------------------------------------
$backup = "$configPath.bak-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
Copy-Item $configPath $backup
Write-Host "[i] Бэкап: $(Split-Path $backup -Leaf)" -ForegroundColor DarkGray

# --- 4. Правка --------------------------------------------------------------
$lines = Get-Content $configPath
$changed = @()
$missing = @()

foreach ($key in $targets.Keys) {
    $value, $why = $targets[$key]
    $pattern = "^(\s*" + [regex]::Escape($key) + "\s*:\s*)(\S.*)$"
    $found = $false

    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match $pattern) {
            $old = $matches[2]
            $newLine = $matches[1] + $value
            if ($lines[$i] -ne $newLine) {
                $lines[$i] = $newLine
                $changed += [pscustomobject]@{ Key = $key; Old = $old.Trim(); New = $value; Why = $why }
            }
            $found = $true
            break
        }
    }

    if (-not $found) { $missing += $key }
}

# --- 5. Запись и отчёт ------------------------------------------------------
if (-not $WhatIfOnly -and $changed.Count -gt 0) {
    [IO.File]::WriteAllLines($configPath, $lines)   # UTF-8 без BOM
}

Write-Host ""
if ($changed.Count -gt 0) {
    Write-Host "Изменено ($($changed.Count)):" -ForegroundColor Green
    $changed | Format-Table Key, Old, New, Why -AutoSize | Out-String -Width 220 | Write-Host
} else {
    Write-Host "Нечего менять: ключи уже в нужном состоянии." -ForegroundColor DarkGray
}

if ($missing.Count -gt 0) {
    Write-Host ""
    Write-Host "НЕ найдены в pnx.yml (проверь вручную):" -ForegroundColor Yellow
    $missing | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
    Write-Host "  Поиск похожих:  Select-String -Path .\pnx.yml -Pattern 'spawn|pvp|packet|commandBlocks'" -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "Дальше:" -ForegroundColor Cyan
Write-Host "  1) .\start.bat" -ForegroundColor Cyan
Write-Host "  2) в консоли сервера:" -ForegroundColor Cyan
Write-Host "     difficulty normal" -ForegroundColor Cyan
Write-Host "     gamerule keepInventory false" -ForegroundColor Cyan
Write-Host "     gamerule mobGriefing true" -ForegroundColor Cyan
Write-Host "     gamerule tntExplodes true" -ForegroundColor Cyan
Write-Host "     gamerule showcoordinates true" -ForegroundColor Cyan
Write-Host "  3) регионы/античит: docs/lite-anarchy.md, правила: docs/rules.md" -ForegroundColor DarkGray
if ($WhatIfOnly) { Write-Host "[i] -WhatIfOnly: файл НЕ изменён." -ForegroundColor Cyan }
