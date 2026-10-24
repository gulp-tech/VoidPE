# anarchy-config.ps1 — правит pnx.yml под анархию (PowerNukkitX 3.x)
# Запуск из папки сервера:  powershell -ExecutionPolicy Bypass -File .\anarchy-config.ps1
#
# Что делает:
#   1) следит, чтобы сервер был остановлен (иначе PNX перезапишет pnx.yml из памяти);
#   2) делает бэкап pnx.yml.bak-<дата>;
#   3) меняет ТОЛЬКО существующие ключи (структуру файла не ломает);
#   4) печатает отчёт: изменено / не найдено.
#
# Параметр -WhatIfOnly — показать, что было бы изменено, без записи.

param([switch]$WhatIfOnly)

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

$configPath = Join-Path $PWD 'pnx.yml'
if (-not (Test-Path $configPath)) {
    Write-Host "[x] pnx.yml не найден в $PWD" -ForegroundColor Red
    Write-Host "    Запусти сервер хотя бы раз (мастер создаст pnx.yml), затем повтори."
    exit 1
}

# --- 1. Проверка, что сервер не запущен -------------------------------------
$running = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match 'powernukkitx\.jar' }

if ($running) {
    Write-Host "[!] Похоже, сервер PNX запущен (java.exe с powernukkitx.jar)." -ForegroundColor Yellow
    Write-Host "    Останови его командой 'stop' в консоли сервера и запусти скрипт снова." -ForegroundColor Yellow
    Write-Host "    Иначе PNX перезапишет pnx.yml при выключении." -ForegroundColor Yellow
    exit 2
}

# --- 2. Целевые значения ----------------------------------------------------
# Ключ -> @(значение, пояснение)
$targets = [ordered]@{
    # База
    'motd'                            = @('§c§lANARCHY §8| §7voidpe',          'название в списке серверов')
    'subMotd'                         = @('powernukkitx.org',                  'вторая строка MOTD')
    'maxPlayers'                      = @('100',                               'лимит игроков')
    'xboxAuth'                        = @('true',                              'true = только Xbox-аккаунты')
    'safeSpawn'                       = @('true',                              'не спавнить в лаве/камне')
    'autosaveDelay'                   = @('6000',                              'автосейв каждые 5 мин')

    # Игроки
    'savePlayerData'                  = @('true',                              'сохранять инвентарь игрока')
    'checkMovement'                   = @('true',                              'базовый античит движения')
    'forceSkinTrusted'                = @('true',                              'разрешить сторонние скины')
    'spawnRadius'                     = @('32',                                'разброс спавна новичков')

    # Геймплей анархии
    'spawnProtection'                 = @('0',                                 'НЕТ защиты спавна')
    'pvp'                             = @('true',                              'PvP без согласия')
    'difficulty'                      = @('3',                                 '3 = Hard')
    'gamemode'                        = @('0',                                 '0 = выживание')
    'forceGamemode'                   = @('false',                             'OP может в креатив')
    'enableExplosionBlockDamage'      = @('true',                              'ТНТ/криперы ломают блоки')
    'enableRedstone'                  = @('true',                              'редстоун работает')
    'tickRedstone'                    = @('true',                              'редстоун тикает')
    'enableCommandBlocks'             = @('false',                             'командные блоки выкл (безопасность)')
    'enableHunger'                    = @('true',                              'голод включён')
    'enableEntitySpawning'            = @('true',                              'мобы спавнятся')
    'enableMobAI'                     = @('true',                              'ИИ мобов включён')
    'viewDistance'                    = @('6',                                 'радиус видимости (6 для 20+ игроков)')
    'announceAchievements'            = @('false',                             'не спамить достижениями')

    # Уровень/чанки/перф
    'entitySpawnCap'                  = @('512',                               'потолок сущностей на мир')
    'chunkUnloadDelay'                = @('15000',                             'выгрузка чанков, мс')
    'tickRadius'                      = @('4',                                 'радиус тика чанков')
    'asyncWorkers'                    = @('"auto"',                            'async-потоки')

    # Сеть
    'enableQuery'                     = @('true',                              'query для мониторингов (в 3.0.5 ключ называется enableQuery)')
    'queryPlugins'                    = @('false',                             'не палить список плагинов')
}

# --- 3. Бэкап ---------------------------------------------------------------
$backup = "$configPath.bak-$(Get-Date -Format 'yyyy-MM-dd-HHmmss')"
Copy-Item $configPath $backup
Write-Host "[i] Бэкап: $(Split-Path $backup -Leaf)" -ForegroundColor DarkGray

# --- 4. Правка --------------------------------------------------------------
$lines = Get-Content $configPath
$changed = @()
$missing = @()

foreach ($key in $targets.Keys) {
    $value, $why = $targets[$key]
    $pattern = "^(\s*" + [regex]::Escape($key) + "\s*:\s*)(.*)$"
    $found = $false

    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match $pattern) {
            $old = $matches[2]
            # не трогаем вложенные секции (значение-объект начинается с пустоты/комментария)
            if ($old -match '^\s*(#.*)?$') { continue }
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
    Set-Content -Path $configPath -Value $lines -Encoding UTF8
}

Write-Host ""
if ($changed.Count -gt 0) {
    Write-Host "Изменено ($($changed.Count)):" -ForegroundColor Green
    $changed | Format-Table Key, Old, New, Why -AutoSize | Out-String -Width 200 | Write-Host
} else {
    Write-Host "Нечего менять: все найденные ключи уже стоят как нужно." -ForegroundColor DarkGray
}

if ($missing.Count -gt 0) {
    Write-Host ""
    Write-Host "НЕ найдены в pnx.yml (проверь вручную, регистр важен):" -ForegroundColor Yellow
    $missing | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
    Write-Host "  Подсказка: ищи похожие имена через  Select-String -Path .\pnx.yml -Pattern 'spawn|pvp|difficulty'"
}

if ($WhatIfOnly) {
    Write-Host ""
    Write-Host "[i] Режим -WhatIfOnly: файл НЕ изменён." -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "[i] Готово. Запускай сервер: .\start.bat" -ForegroundColor Cyan
    Write-Host "    Если что-то не так — верни файл из бэкапа: Copy-Item '$backup' .\pnx.yml -Force" -ForegroundColor DarkGray
}
