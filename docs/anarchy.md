# Анархия на PowerNukkitX (по мотивам NeverTime)

Тема: **анархия Bedrock** — без приватов, без телепортов, с гриферством и PvP,
плюс «обвес» как у NeverTime (экономика, магазины, кланы, кейсы, киты).

> ⚠️ Сначала останови сервер командой `stop`. PNX при выключении перезаписывает
> `pnx.yml` из памяти — правки на работающем сервере потеряются.

---

## 1. Что вообще значит «анархия»

| Разрешено игрокам | Технически всё равно ограничиваем |
|---|---|
| гриферство, поджоги, ТНТ, ловушки | античит/проверка движения (`checkMovement`) |
| PvP без согласия, спавн-килл | анти-бот и rate-limit на подключения |
| кража из сундуков, обман | лимиты сущностей и чанков (иначе лаги положат сервер) |
| токсичный чат (по желанию) | бэкапы и защита от DDoS |

У NeverTime, несмотря на «анархию», **читы и лаг-машины банятся** — иначе сервер умирает
за неделю. Не путай анархию с «без античита».

Никаких приватов: **не ставь** `SRegionProtector`, `FuturePlots`, `EssentialsPNX` с `/home`,
`/tpa`, `/sethome` — это убивает смысл режима.

---

## 2. Конфиг анархии (одна команда)

⚠️ PowerShell стартует в `C:\WINDOWS\system32` — оттуда скрипт не найдётся
(`Аргумент ".\anarchy-config.ps1" для параметра -File не существует`).
Сначала перейди в папку сервера:

```powershell
cd "$([Environment]::GetFolderPath('Desktop'))\PNX"
pwd
```

### Вариант А — если файл `anarchy-config.ps1` лежит в папке PNX

```powershell
powershell -ExecutionPolicy Bypass -File .\anarchy-config.ps1
```

### Вариант Б — без файла, вставить целиком в PowerShell (из папки PNX)

```powershell
$targets = [ordered]@{
  'motd'='ANARCHY'; 'maxPlayers'='100'; 'xboxAuth'='true'; 'autosaveDelay'='6000'
  'savePlayerData'='true'; 'checkMovement'='true'; 'forceSkinTrusted'='true'; 'spawnRadius'='32'
  'spawnProtection'='0'; 'pvp'='true'; 'difficulty'='3'; 'gamemode'='0'; 'forceGamemode'='false'
  'enableExplosionBlockDamage'='true'; 'enableEntitySpawning'='true'; 'viewDistance'='6'
  'entitySpawnCap'='512'; 'chunkUnloadDelay'='15000'; 'tickRadius'='4'
  'enableQuery'='true'; 'queryPlugins'='false'; 'announceAchievements'='false'
}
if (Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'powernukkitx\.jar' }) {
  'СЕРВЕР ЗАПУЩЕН — сначала stop в его консоли!'
} else {
  Copy-Item pnx.yml "pnx.yml.bak-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
  $lines = Get-Content pnx.yml; $changed=@(); $missing=@()
  foreach ($k in $targets.Keys) {
    $re = '^(\s*' + [regex]::Escape($k) + '\s*:\s*)(\S.*)$'; $hit=$false
    for ($i=0; $i -lt $lines.Count; $i++) {
      if ($lines[$i] -match $re) { $lines[$i] = $matches[1] + $targets[$k]; $changed += "$k -> $($targets[$k])"; $hit=$true; break }
    }
    if (-not $hit) { $missing += $k }
  }
  [IO.File]::WriteAllLines((Join-Path $PWD 'pnx.yml'), $lines)
  'ИЗМЕНЕНО:'; $changed; 'НЕ НАЙДЕНО (проверь вручную):'; $missing
}
```

`[IO.File]::WriteAllLines` пишет UTF-8 **без BOM** — важно, `Set-Content -Encoding UTF8`
в PowerShell 5.1 добавляет BOM и YAML-парсер PNX может ругнуться.

Скрипт лежит в репозитории как `pnx/anarchy-config.ps1` — скопируй его в папку PNX рядом
с `pnx.yml`. Что он делает:

1. требует, чтобы сервер был остановлен `stop`;
2. делает бэкап `pnx.yml` → `pnx.yml.bak-<дата>`;
3. меняет только те ключи, которые **уже есть** в файле (значения и структуру не ломает);
4. печатает отчёт: что изменено, а какие ключи не найдены.

Ключевые значения, которые он ставит:

| Ключ | Значение | Зачем |
|---|---|---|
| `spawnProtection` | `0` | нет защиты спавна — можно гриферить спавн |
| `pvp` | `true` | PvP без согласия |
| `difficulty` | `3` | Hard: мобы злее, голод, урон |
| `gamemode` | `0` | все заходят в выживание |
| `forceGamemode` | `false` | оператор может переключиться в креатив |
| `enableExplosionBlockDamage` | `true` | ТНТ/криперы реально ломают блоки |
| `viewDistance` | `6` | база для 20+ игроков (поднимай по TPS) |
| `checkMovement` | `true` | дешёвый античит от флая/спида |
| `forceSkinTrusted` | `true` | кастомные скины |
| `maxPlayers` | `100` | как ты задал в мастере |
| `xboxAuth` | `true` | оставь `true`; `false` = оффлайн/пиратка (см. п.7) |
| `entitySpawnCap` | `512` | потолок сущностей на мир |
| `tickRadius` | `4` | радиус симуляции чанков |
| `enableQuery` | `true` | query для мониторингов (в PNX 3.0.5 ключ называется `enableQuery`, не `query`) |

После этого запусти сервер и добей правила командами (консоль):

```
difficulty hard
gamerule keepInventory false
gamerule showcoordinates true
gamerule doDaylightCycle true
gamerule tntExplodes true
gamerule mobGriefing true
gamerule doFireTick true
gamerule showdeathmessages true
gamerule naturalRegeneration true
gamerule sendCommandFeedback false
```

(`sendCommandFeedback false` — чтобы игроки не видели чужие команды; для спавн-килла это важно.)

---

## 3. Спавн: делаем «злой» спавн

1. В игре встань туда, где будет спавн, и выполни `setworldspawn`.
2. `spawnProtection: 0` уже стоит → ломать спавн можно.
3. `spawnRadius: 32` (в `player-settings`) — новичков раскидывает шире, меньше «мясорубки» в одной точке.
4. Кит новичка выдавать **не** на спавне (см. экономику ниже), иначе всё растащат за минуту.

---

## 4. Плагины: собираем «обвес» NeverTime

Каталог: https://powernukkitx.org/plugins. Ядро режима — минимум:

| Плагин | Зачем | Прямая ссылка |
|---|---|---|
| **AnyVersion** | мультиверсия: клиенты 1.21.x–1.26.x заходят на одно ядро (у PNX по умолчанию только последняя версия) | `https://github.com/PowerNukkitX-Bundle/AnyVersion/releases/latest` |
| **LlamaEconomy** | деньги, `/pay`, `/money`, `/topmoney` | `.../LlamaEconomy/releases/latest` → `LlamaEconomy.jar` |
| **ShopUI** | магазин на GUI (нужна экономика) | `.../ShopUI/releases/latest` → `ShopUI.jar` |
| **PlaceholderAPI** | плейсхолдеры для чата/скорборда | `.../PlaceholderAPI/releases/latest` |
| **Scoreboard** | боковая панель: баланс, онлайн | `.../Scoreboard/releases/latest` |
| **PlaceholderChat** | формат чата через плейсхолдеры | `.../PlaceholderChat/releases/latest` |
| **LuckPerms** | права/группы (когда захочется донат-групп) | `.../LuckPerms/releases/latest` |
| **RCON** | удалённое управление консолью | `.../RCON/releases/latest` |
| **Holograms** | таблички-голограммы над спавном/магазином | `.../Holograms/releases/latest` |

Установка (PowerShell, в папке сервера):

```powershell
New-Item -ItemType Directory -Path plugins -Force | Out-Null

$mods = @{
  'AnyVersion.jar'      = 'https://github.com/PowerNukkitX-Bundle/AnyVersion/releases/latest/download/AnyVersion.jar'
  'LlamaEconomy.jar'    = 'https://github.com/PowerNukkitX-Bundle/LlamaEconomy/releases/latest/download/LlamaEconomy.jar'
  'ShopUI.jar'          = 'https://github.com/PowerNukkitX-Bundle/ShopUI/releases/latest/download/ShopUI.jar'
  'PlaceholderAPI.jar'  = 'https://github.com/PowerNukkitX-Bundle/PlaceholderAPI/releases/latest/download/PlaceholderAPI.jar'
  'Scoreboard.jar'      = 'https://github.com/PowerNukkitX-Bundle/Scoreboard/releases/latest/download/Scoreboard.jar'
  'PlaceholderChat.jar' = 'https://github.com/PowerNukkitX-Bundle/PlaceholderChat/releases/latest/download/PlaceholderChat.jar'
}

foreach ($m in $mods.GetEnumerator()) {
  try {
    Invoke-WebRequest -Uri $m.Value -OutFile (Join-Path 'plugins' $m.Key) -ErrorAction Stop
    Write-Host "OK  $($m.Key)"
  } catch {
    Write-Host "FAIL $($m.Key) — качай вручную со страницы плагина"
  }
}
Get-ChildItem plugins | Select-Object Name, @{n='KB';e={[math]::Round($_.Length/1KB)}}
```

> Ссылки `releases/latest/download/<file>` работают, если имя файла в последнем релизе
> именно такое. Если 404 — открой страницу плагина на powernukkitx.org/plugins и скачай jar руками.

Чего **не** ставить на анархию: SRegionProtector, FuturePlots, EssentialsPNX (телепорты/дома),
любые «анти-гриф» и «приват» плагины.

---

## 4a. EssentialsPNX на анархии: отключить телепорты

`EssentialsPNX` по умолчанию **разрешает всем** (`true`) команды, которые убивают анархию:
`/home`, `/sethome`, `/tpa`, `/tpaccept`, `/tpdeny`, `/warp`, `/spawn`, `/back`, `/tpaall`.

Так как LuckPerms у тебя уже стоит, проще всего запретить эти права группе `default`:

```
lp creategroup anarchy
lp group default parent set anarchy
lp group default permission set essentialsnk.home false
lp group default permission set essentialsnk.sethome false
lp group default permission set essentialsnk.delhome false
lp group default permission set essentialsnk.tpa false
lp group default permission set essentialsnk.tpaccept false
lp group default permission set essentialsnk.tpdeny false
lp group default permission set essentialsnk.warp false
lp group default permission set essentialsnk.spawn false
lp group default permission set essentialsnk.back false
lp group default permission set essentialsnk.tpaall false
```

Оставляем полезное и безобидное: `/ping`, `/compass`, `/depth`, `/getpos`, `/itemdb`, `/ignore`.

> Если LuckPerms ещё не инициализирован (нет прав `lp`), удали `EssentialsPNX.jar` —
> для анархии он не обязателен, а телепорты ломают режим.

## 5. Производительность: главное узкое место

Анархия = гигантские постройки, тысячи сущностей, ТНТ-машины. Домашний ПК не тянет 100 игроков.
Реалистично:

| Игроков | RAM | viewDistance | Ядро хостинга |
|---|---|---|---|
| до 10 | 2–4 ГБ | 8 | домашний ПК |
| 10–30 | 6–8 ГБ | 6 | VPS 4 vCPU |
| 50+ | 12–16 ГБ | 5–6 | выделенный сервер |

Настройки в `pnx.yml`:

- `viewDistance: 6` (не 20 — у PNX дефолт 20, на анархии это убивает TPS);
- `entitySpawnCap: 512` (меньше — стабильнее);
- `tickRadius: 4`, `chunksPerTicks: -1` (авто);
- `asyncWorkers: "auto"`;
- `autosaveDelay: 6000` (5 мин) — чаще сохранять большие миры дорого;
- `chunkUnloadDelay: 15000`.

Диагностика лагов: `status`, `debug`, или поставь Spark (`installSpark: true` в `misc-settings`)
и снимай профиль.

Анти-бот/рейт-лимиты (раздел `network-settings`) — оставь дефолты, но проверь:

- `enableQuery: true`, а `queryPlugins: false` (чтобы не палить список плагинов);
- `rate-limit` / `botnet` — не отключай: на публичной анархии бот-атаки начнутся в первую неделю.

---

## 6. Бэкапы и вайпы

Анархия — это когда твой мир регулярно пытаются сломать (в том числе крашем сервера):

```powershell
# остановить сервер (stop), затем:
Compress-Archive -Path worlds, players, plugins, pnx.yml -DestinationPath "backup-$(Get-Date -Format yyyy-MM-dd-HHmm).zip" -Force
```

- Делай бэкап **перед каждым обновлением ядра/плагинов** и раз в сутки по расписанию.
- Миры на анархии быстро растут (гигабайты) — держи их вне OneDrive/облачных синков.
- Вайп — инструмент баланса: NeverTime не вайпал мир ~3 года, но у них серверный хостинг
  и команда. Для старта лучше объявить дату вайпа заранее и жить циклами.

---

## 7. Лицензия / Xbox Auth

- `xboxAuth: true` — заходят только с Xbox-аккаунтом (ники и скины нормальные, меньше ботов).
- `xboxAuth: false` — оффлайн-режим (пиратка). Тогда любой может зайти под чужим ником,
  а UUID игроков плавают → испорченные инвентари и «пропавшие» базы. Для анархии с
  незнакомыми игроками это плохой выбор, если только ты не ставишь отдельный
  auth-плагин (на PNX готового нет — обычно так делают через прокси).

NeverTime официально поддерживает и лицензию, и пиратку — но у них для этого
отдельная инфраструктура и модерация.

---

## 8. Реальность про домашний ПК

Ты уже видишь внешний адрес `185.59.244.125` — это, скорее всего, адрес роутера/провайдера.
Чтобы друзья зашли из интернета, нужен проброс **UDP 19132** на `192.168.0.227`.

Но для «сервера как NeverTime» (сотни игроков, аптайм 100%) домашний ПК не годится:

- нет статического IP и нормального аптайма (свет/интернет);
- канал 10–50 Мбит не тянет 50+ игроков;
- DDoS кладёт домашний роутер мгновенно.

Практичный путь: сначала тестируешь режим локально с 5–10 друзьями, потом переносишь
папку сервера на VPS (Linux: те же файлы + `systemd`-юнит из `docs/commands.md`,
только запуск `java ... -jar powernukkitx.jar`).

---

## 9. План на первые дни

1. `anarchy-config.ps1` → `start.bat` → проверить, что `spawnProtection` реально 0
   (подойди к спавну и сломай блок не-опом).
2. Поставить `AnyVersion` + `LlamaEconomy` + `PlaceholderAPI` + `Scoreboard`.
3. `setworldspawn` в выбранной точке, прогнать `gamerule ...` из п.2.
4. Позвать 2–3 друзей, проверить: PvP, ТНТ ломает ландшафт, мобы водятся, лагов нет.
5. Только после этого открывать адрес наружу и думать про кланы/кейсы/киты
   (это отдельные плагины — на PNX часть придётся писать самому или искать на форумах).

## Чего в PNX «из коробки» нет (и что делать)

| Фича NeverTime | На PNX |
|---|---|
| кланы | готового нет → свой плагин (API `org.powernukkitx.*`) |
| кейсы/рулетка | готового нет → свой плагин + `InvMenuPNX` |
| аукцион | готового нет → свой плагин на базе экономики |
| кит старт | `EssentialsPNX` (осторожно: там же телепорты — отключи их в конфиге) |
| кастомные чары | поддерживаются ядром (custom enchants) или аддонами через `AddonBridge` |
| вампиризм/батлпасс | только свой плагин |

Это нормальный путь: анархия-проект = ядро + набор собственных плагинов.
