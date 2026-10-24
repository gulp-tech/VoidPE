# Лайт-анархия (FunTime / HolyWorld style) на PowerNukkitX

Отличие от «хаоса»: **гриферство и PvP разрешены, но есть регионы (приваты), античит
и запрет крашей/эксплойтов**. Т.е. мир можно взрывать, но:
- свою базу можно защитить регионом;
- читы, краш-эксплойты и лаг-машины = бан;
- спавн — безопасная зона.

Это ровно модель FunTime/HolyWorld: лайт-анархия с экономикой, регионами и модерацией.

---

## 1. Чем отличается от «чистой анархии»

| Параметр | Хаос | **Лайт (наш вариант)** |
|---|---|---|
| `spawnProtection` | 0 | **32** (безопасный спавн) |
| Регионы | нет | **SRegionProtector** (приват за деньги) |
| Античит | «на глаз» | **Astra + PNX checkMovement** |
| Взрывы | везде | **везде, кроме регионов** |
| Читы/краши | не контролируются | **бан (правила + античит)** |
| Телепорты Essentials | запрещены | `/spawn`, `/back` — можно; `/home`, `/tpa` — нет |

---

## 2. Конфиг PNX (скрипт)

Сервер **остановить** (`stop`), затем в папке `PNX`:

```powershell
cd "$([Environment]::GetFolderPath('Desktop'))\PNX"
powershell -ExecutionPolicy Bypass -File .\lite-anarchy-config.ps1
```

Файл `pnx/lite-anarchy-config.ps1` — из репозитория (положить в папку PNX).
Он меняет только существующие ключи и делает бэкап `pnx.yml`.

Ключевые отличия от «хаос»-скрипта:

| Ключ | Значение | Зачем |
|---|---|---|
| `spawnProtection` | `32` | спавн нельзя гриферить (точка сбора новичков) |
| `pvp` | `true` | PvP везде |
| `difficulty` | `2` | Normal (Hard слишком жёстко для лайта) |
| `enableCommandBlocks` | `false` | командные блоки — источник крашей/эксплойтов |
| `checkMovement` | `true` | базовый античит (флай/спид) |
| `overrideServerAuthBlockBreaking` | `true` | **обязательно для Astra** |
| `packetLimit` | `4000` | ограничение пакетов на IP (антипакетный флуд) |
| `globalPacketLimit` | `100000` | глобальный лимит |
| `entitySpawnCap` | `512` | потолок сущностей (анти-лаг-машины) |
| `viewDistance` | `6` | стабильный TPS |
| `enableExplosionBlockDamage` | `true` | взрывы ломают мир **вне регионов** |

---

## 3. Регионы: SRegionProtector

### 3.1 Установка

```powershell
cd "$([Environment]::GetFolderPath('Desktop'))\PNX"
Invoke-WebRequest "https://github.com/PowerNukkitX-Bundle/SRegionProtector/releases/latest/download/SRegionProtector.jar" -OutFile "plugins\SRegionProtector.jar"
```

Версия на момент проверки: `v30.2`, ~24 МБ. Требует Java 21+ (у тебя 25 — ок),
LlamaEconomy (уже стоит) — для платных регионов.

Запусти сервер один раз, чтобы появилась папка `plugins/SRegionProtector/`, затем останови.

### 3.2 Важно: значение флага = «защита включена»

Проверено по исходникам (`RegionEventsHandler#handleEvent`): если флаг `false` — регион
ничего не ограничивает. Поэтому `place: true` = **нельзя ставить блоки чужим**.

### 3.3 Рекомендуемые флаги (`plugins/SRegionProtector/region-settings.yml`)

```yaml
default-flags:
  place: true               # чужие не строят
  break: true               # чужие не ломают
  interact: true            # рычаги/кнопки/сундуки-интерфейсы
  use: true                 # использование предметов по блокам
  chest-access: true        # нельзя открывать чужие сундуки
  explode: true             # нельзя взрывать регион (ТНТ/крист/гаст)
  explode-block-break: true # взрыв не разрушает блоки региона
  fire: true                # нельзя поджигать
  lighter: true             # зажигалка не работает в регионе
  liquid-flow: true         # вода/лава не затекает
  block-fall: true          # гравий/песок не проваливается
  crops-destroy: true       # нельзя топтать посевы
  smart-doors: true         # двери чужим не открываются
  hopper: true              # воронки не тянут из региона
  minecart-destroy: true    # нельзя ломать вагонетки
  frame-item-drop: true     # нельзя снимать предметы с рамок
  redstone: false           # редстоун внутри работает (иначе сломаются фермы хозяина)
  pvp: false                # PvP РАЗРЕШЁН даже в регионе (как HolyWorld)
  mob-spawn: false          # мобы спавнятся (иначе пустой мир)
  mob-damage: false
  entry: false              # вход свободный — базу видно, но ломать нельзя
  teleport: true            # нельзя телепортироваться внутрь чужого региона
  move: false
  health-regen: false
  heal: false
  invincible: false
  potion-launch: true
  ender-pearl: true         # нельзя закинуть эндер-перл в регион
  lightning-strike: true
  nether-portal: true
  bucket-empty: true
  bucket-fill: true
  sleep: true
```

> Хочешь «регион = полная неприкосновенность, включая PvP» → поставь `pvp: true`.
> Хочешь как на FunTime (в приват не залезть, но убить могут) → оставь `pvp: false`.

### 3.4 Платные регионы (экономика работает как сток денег)

`plugins/SRegionProtector/config.yml`:

```yaml
provider: yaml
language: rus              # есть русская локализация
gui-type: form             # form = удобные формы Bedrock
region-creation-price: true
price-per-block: 2         # 1 блок региона = 2 монеты LlamaEconomy
default-max-region-size: 10000     # площадь в блоках
default-max-region-amount: 2       # сколько регионов игроку
priority-system: false
active-flags:
  place: true
  break: true
  interact: true
  use: true
  pvp: true
  explode: true
  lighter: true
  invincible: true
  potion-launch: true
  teleport: true
  crops-destroy: true
  explode-block-break: true
  liquid-flow: true
  lightning-strike: true
  fire: true
  chest-access: true
  sleep: true
  smart-doors: true
  minefarm: true
  fall-damage: true
  nether-portal: true
  frame-item-drop: true
  bucket-empty: true
  bucket-fill: true
  block-fall: true
  minecart-destroy: true
  hopper: true
  entry: true
```

Чем больше `price-per-block` и меньше `default-max-region-size` — тем больше
грифа в мире и тем ценнее приват. Для старта: `price-per-block: 2`, размер `10000`.

### 3.5 Как игрок делает приват

```
/rg wand                 → деревянный топор-выделялка
(два клика по углам)
/rg size                 → проверить размер и цену
/rg create mybase        → создать
/rg gui mybase           → управление (добавить друзей, флаги)
/rg addmember mybase Ник → друг может строить
/rg list owner           → свои регионы
/rg remove mybase        → удалить
```

---

## 4. Античит

### 4.1 Astra (базовый)

```
https://github.com/PowerNukkitX-Bundle/Astra/releases/latest/download/Astra-0.0.3-DEV.jar
```

⚠️ Обязательно `misc-settings.overrideServerAuthBlockBreaking: true` в `pnx.yml`
(иначе Astra не работает) — это и ставит скрипт.

Astra — DEV-версия, не панацея: ловит грубые флай/спид/килл-аура, но не заменяет модерацию.

### 4.2 Встроенное в PNX

- `player-settings.checkMovement: true` — серверная проверка движения;
- `network-settings.raknet.packetLimit` / `globalPacketLimit` — антифлуд;
- `network-settings.rate-limit` / `botnet` — не отключать (бот-атаки на публичной анархии начнутся сразу);
- `gameplay-settings.enableCommandBlocks: false` — командные блоки вне креатива не нужны.

### 4.3 Что «крашит» сервер и как это закрыто

| Вектор | Защита |
|---|---|
| пакетный флуд, эксплойт с пакетами | `packetLimit`, `maxQueuedBytes` (дефолт) |
| тысячи сущностей (лаг-машина) | `entitySpawnCap: 512`, `gamerule` + правила |
| командные блоки / структуры | `enableCommandBlocks: false` |
| огромные книги/таблички, NBT-бомбы | правила + `debug-settings` лимиты, модерация |
| дюп через лаг | бэкапы + откат, правила |

Полностью от крашей защищает только: **актуальная версия ядра + бэкапы + модерация**. Не отключай
обновления PNX: эксплойты закрывают в патчах.

---

## 4b. Брендинг и локализация LlamaEconomy (VoidPE)

Готовые файлы в репозитории:

| Файл | Куда |
|---|---|
| `pnx/LlamaEconomy/messages.yml` | `plugins/LlamaEconomy/messages.yml` |
| `pnx/LlamaEconomy/config.yml` | `plugins/LlamaEconomy/config.yml` |

Что внутри: префикс `§8» §6§lVoidPE §8| §e`, все сообщения на русском,
стартовый баланс `1000`, валюта `$`, алиасы `/bal`, `/baltop`, `/pay`.

```powershell
cd "$([Environment]::GetFolderPath('Desktop'))\PNX"
Copy-Item .\LlamaEconomy\messages.yml .\plugins\LlamaEconomy\messages.yml -Force
Copy-Item .\LlamaEconomy\config.yml   .\plugins\LlamaEconomy\config.yml   -Force
```

- `messages.yml` применяется **без рестарта**: `/lecoreload`;
- `config.yml` (валюта, стартовый баланс, алиасы) — **только рестартом**;
- плейсхолдеры `[0]`, `[1]`… в сообщениях не трогать — это подстановки плагина;
- символ валюты задаётся в `config.yml` → `MonetaryUnit`, в тексте он появляется как `[0]`/`[1]`.

## 5. Правила (готовый текст)

Лежит в [`docs/rules.md`](rules.md) — вставляй в Discord/на сайт/в `/rules`.
Коротко: гриферство и PvP разрешены, читы/краши/дюп/лаг-машины — бан.

---

## 6. Порядок запуска (чек-лист)

1. `stop` → `lite-anarchy-config.ps1` → `start.bat`.
2. Консоль: `difficulty normal`, `gamerule keepInventory false`, `gamerule mobGriefing true`,
   `gamerule tntExplodes true`, `gamerule showcoordinates true`.
3. `setworldspawn` в точке спавна (в игре), `spawnProtection: 32` уже защищает её.
4. Поставить `SRegionProtector` + `Astra`, перезапустить, проверить `plugins`.
5. Настроить флаги регионов (п.3.3) и цену (п.3.4), перезапустить.
6. Тест с другом: он не может ломать/ставить в твоём регионе, но может тебя убить;
   вне региона — может всё.
7. Только теперь открывать внешний адрес.

## 7. Экономика лайт-анархии

- Продажа ресурсов — основной доход (`/sell`), покупка — расходники (`/shop`,
  конфиг `pnx/shop-config.yml`).
- Регионы — главный сток денег (`price-per-block`).
- Не поднимай цены скупки массовых ресурсов (тростник, пшеница, железо), иначе инфляция
  за неделю. Регулируй `sell` в `plugins/ShopUI/config.yml`.
