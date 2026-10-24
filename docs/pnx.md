# PowerNukkitX — установка и настройка (Windows)

PNX — второй вариант ядра для VoidPE. Отличия от Lumi, которые важны именно тебе:

| | Lumi | **PowerNukkitX** |
|---|---|---|
| ИИ мобов | **нет** в ядре (нужен MobPlugin) | **есть из коробки** (`org.powernukkitx.entity.ai`: behaviors, sensors, executors, A* route finder) |
| Конфиг | `settings.yml` | **`pnx.yml`** |
| Плагины | Nukkit-MOT (`cn.nukkit.plugin`) | PNX API (`org.powernukkitx.plugin`) |
| Версия Minecraft | мультиверсия 1.20.0 – 1.26.50 | последняя (3.0.5 → **1.26.50**, protocol 2193) |
| Java | 21+ | **21+** |
| Первый запуск | без мастера | **мастер настройки** в консоли (лицензия, язык, MOTD, порт) |

Документация: https://powernukkitx.org/docs · Релизы: https://github.com/PowerNukkitX/PowerNukkitX/releases · Плагины: https://powernukkitx.org/plugins · Discord: https://discord.gg/apwd7uauZg

---

## 1. Папка сервера

Держи PNX **отдельно** от Lumi (не мешай миры и плагины двух ядер):

```powershell
$desk = [Environment]::GetFolderPath('Desktop')
New-Item -ItemType Directory -Path "$desk\PNX" -Force | Out-Null
Set-Location "$desk\PNX"
pwd
```

## 2. Скачать ядро

```powershell
$r = Invoke-RestMethod -Uri 'https://api.github.com/repos/PowerNukkitX/PowerNukkitX/releases/latest' -Headers @{'User-Agent'='VoidPE'}
$a = $r.assets | Where-Object { $_.name -eq 'powernukkitx.jar' } | Select-Object -First 1
Write-Host "Версия: $($r.tag_name)"; Invoke-WebRequest $a.browser_download_url -OutFile 'powernukkitx.jar'
Get-Item powernukkitx.jar | Select-Object Name, @{n='MB';e={[math]::Round($_.Length/1MB,1)}}
```

Ожидается ~**86 МБ** (`3.0.5` = 86 100 503 байт).

Или через curl (флаг `--ssl-no-revoke` — та же история с проверкой отзыва сертификата, что была у Lumi):

```powershell
curl.exe --ssl-no-revoke -L -o powernukkitx.jar "https://github.com/PowerNukkitX/PowerNukkitX/releases/download/3.0.5/powernukkitx.jar"
```

## 3. `start.bat`

⚠️ PNX **нельзя запускать просто `java -jar`** — ему нужны `--add-opens`, иначе падает загрузка
реестров/плагинов. JVM-флаги должны идти **до** `-jar` (в официальной доке пример с перепутанным
порядком — `java -jar --add-opens ...` — так не сработает).

В папке сервера выполни:

```powershell
$bat = @(
    '@echo off'
    'chcp 65001 >nul'
    'cd /d "%~dp0"'
    'java -Xms2G -Xmx2G -Dfile.encoding=UTF-8 -XX:+UseG1GC ^'
    ' --add-opens java.base/java.lang=ALL-UNNAMED ^'
    ' --add-opens java.base/java.io=ALL-UNNAMED ^'
    ' --add-opens java.base/java.net=ALL-UNNAMED ^'
    ' --enable-native-access=ALL-UNNAMED ^'
    ' -jar powernukkitx.jar %*'
    'pause'
)
[IO.File]::WriteAllLines((Join-Path $PWD 'start.bat'), $bat)
Get-Content .\start.bat | ForEach-Object { "[" + $_ + "]" }
```

Память: `-Xms2G -Xmx2G` → для 5–10 игроков хватает 2 ГБ; на 20+ ставь `4G`.

## 4. Первый запуск (мастер настройки)

Двойной клик по `start.bat` (или `.\start.bat`). В консоли мастер спросит:

1. **Лицензию** → введи `y` и Enter;
2. **Язык** → `rus` (есть русская локализация в ядре);
3. **MOTD / название сервера** → например `VoidPE`;
4. **Порт** → Enter (по умолчанию `19132`).

После этого создаются `pnx.yml`, `worlds/`, `players/`, `plugins/`, `logs/` и сервер пишет,
что слушает порт. Остановка — команда `stop`.

Можно и без мастера, всё флагами:

```powershell
.\start.bat --accept-license --language rus --server-name "VoidPE" --port 19132
```

(или с `--skip-setup`, если хочешь дефолты без вопросов)

## 5. Подключение из игры

| Откуда | Адрес |
|---|---|
| С этого же ПК | `127.0.0.1:19132` |
| Телефон/другой ПК в той же Wi-Fi | `192.168.0.227:19132` |

Порт — **UDP 19132**. Клиент должен быть версии, которую поддерживает ядро
(3.0.5 → Bedrock **1.26.50**); мультиверсии, как в Lumi, тут нет.

Проверить, что порт слушается:

```powershell
Get-NetUDPEndpoint -LocalPort 19132 | Select-Object LocalAddress, LocalPort, OwningProcess
```

## 6. Настройка `pnx.yml`

> ⚠️ Останавливай сервер (`stop`) перед правкой: при выключении PNX перезаписывает
> `pnx.yml` из памяти и затрёт ручные изменения.

Основное (имена ключей — как в `pnx.yml`, регистр важен):

```yaml
baseSettings:
  ip: 0.0.0.0
  port: 19132
  maxPlayers: 20
  defaultLevelName: world
  motd: VoidPE
  subMotd: powernukkitx.org
  xboxAuth: true          # true = лицензионные аккаунты; false = оффлайн (пиратка)
  allowList: false
  autosaveDelay: 6000

playerSettings:
  savePlayerData: true
  forceSkinTrusted: false
  checkMovement: true
  spawnRadius: 16

gameplaySettings:
  gamemode: 0             # 0 выживание, 1 креатив
  difficulty: 2           # 0 мирный, 1 лёгкий, 2 нормальный, 3 сложный
  pvp: true
  allowFlight: false
  viewDistance: 20        # у PNX по умолчанию 20 чанков — для слабого ПК снизь до 8–12
  allowNether: true
  allowTheEnd: true
  spawnProtection: 16

performanceSettings:
  asyncWorkers: 4
  registryCacheEnabled: true
```

Полный справочник: https://powernukkitx.org/docs/configuration

## 7. Плагины

```powershell
New-Item -ItemType Directory -Path plugins -Force | Out-Null
```

- Кидаешь `.jar` в `plugins\`, перезапускаешь сервер (`stop` → снова `start.bat`).
- Каталог готовых: https://powernukkitx.org/plugins
- Плагины Lumi/Nukkit-MOT (`cn.nukkit.*`) **не подойдут** — у PNX API `org.powernukkitx.*`.
- MobPlugin из Lumi **не нужен и не совместим** — мобы с ИИ уже в ядре.

В консоли сервера: `plugins`, `version`, `op ТвойНик`, `gamemode 1 ТвойНик`, `stop`.

## 8. Свой плагин (если понадобится)

Шаблон: https://github.com/PowerNukkitX/PluginTemplate

`build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.powernukkitx.org/releases")
}
dependencies {
    compileOnly("org.powernukkitx:server:3.0.0-SNAPSHOT")   // API 3.x
}
```

`src/main/resources/plugin.yml`:

```yaml
name: VoidCore
main: dev.voidpe.voidcore.VoidCore
version: "1.0.0"
api: "2.0.0"
load: POSTWORLD
```

Сам класс — `extends org.powernukkitx.plugin.PluginBase` (пакеты `org.powernukkitx.*`,
не `cn.nukkit.*`).

## 9. Обновление и бэкапы

Обновление (сервер предварительно остановить):

```powershell
$r = Invoke-RestMethod -Uri 'https://api.github.com/repos/PowerNukkitX/PowerNukkitX/releases/latest' -Headers @{'User-Agent'='VoidPE'}
$r.assets | Where-Object { $_.name -eq 'powernukkitx.jar' } | ForEach-Object { Invoke-WebRequest $_.browser_download_url -OutFile 'powernukkitx.jar' }
```

Бэкап:

```powershell
Compress-Archive -Path worlds, players, plugins, pnx.yml -DestinationPath "backup-$(Get-Date -Format yyyy-MM-dd).zip" -Force
```

## 10. Переезд с Lumi

1. Остановить Lumi (`stop`), папку Lumi можно оставить как есть — PNX ставится в отдельную папку.
2. **Не копируй** `plugins\` из Lumi (другой API) и `MobPlugin`.
3. Миры (`worlds\`) технически в том же LevelDB-формате, но сейвы содержат NBT сущностей
   ядра/плагина — переносить рискованно. Для чистого старта оставь PNX свой `world`.
4. Настрой `pnx.yml` под себя (см. п.6), проверь `xboxAuth`.
5. Зайди в игру и проверь мобов: `summon zombie` в консоли PNX **работает** (в отличие от Lumi)
   или яйцом призыва в креативе.

## 11. Типичные ошибки

| Симптом | Причина / решение |
|---|---|
| `UnsupportedClassVersionError` | Java < 21 |
| `IllegalAccessError` / `InaccessibleObjectException` при старте | нет `--add-opens` — используй `start.bat` из п.3 |
| `BindException: Address already in use` | порт 19132 занят (запущен Lumi!) — закрой его или поменяй `port` в `pnx.yml` |
| `java: command not found` | Java не в PATH, переустанови Temurin 21 и перезапусти консоль |
| Клиент не подключается по версии | клиент ≠ 1.26.50; PNX поддерживает только актуальную версию |
| Настройки в `pnx.yml` не применились | правил при запущенном сервере — он перезаписал файл |
| Мобы стоят | у PNX ИИ есть; проверь `difficulty` (не 0) и `checkMovement`, а также что не стоит сторонний плагин, глушащий спавн |
