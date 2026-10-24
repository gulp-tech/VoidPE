# VoidPE — настройка плагинов по одному (с приёмкой)

Правило: **один плагин → проверка → следующий**. Не двигаемся дальше, пока текущий не зелёный.

Сервер: PowerNukkitX 3.0.5, Java 25, папка `Desktop\PNX`, порт 19132/udp.

## 0. База (уже сделано)

- [x] Ядро PNX 3.0.5, `pnx.yml` (лайт-анархия)
- [x] Java 25 в `start.bat` (иначе ShopUI падает с class 69)
- [x] `--add-opens` в `start.bat`
- [ ] Русский язык ядра: `pnx.yml` -> `language: rus` (в ядре есть папка `language/rus`),
      иначе все системные сообщения (вход, выход, ошибки команд) остаются английскими

## 0.1. СБРОС: оставляем только LuckPerms + EssentialsPNX

Всё остальное (Astra, AnyVersion, PlaceholderAPI, Scoreboard, LlamaEconomy, ShopUI,
SRegionProtector, VoidChat) уезжает в `plugins_off\` — не удаляется, чтобы вернуть одной строкой.
Папки с настройками переезжают туда же.

```powershell
cd C:\Users\Рустам\Desktop\PNX
New-Item -ItemType Directory -Force plugins_off | Out-Null
Get-ChildItem plugins -Filter *.jar | Where-Object { $_.BaseName -notmatch '^(LuckPerms|EssentialsPNX)' } |
    Move-Item -Destination plugins_off -Force
Get-ChildItem plugins -Directory | Where-Object { $_.Name -notmatch '^(LuckPerms|EssentialsPNX|EssentialsNK)' } |
    Move-Item -Destination plugins_off -Force
Get-ChildItem plugins | Select-Object Name
```

Вернуть плагин потом: `Move-Item plugins_off\VoidChat.jar plugins\`.

## 0.2. EssentialsPNX — базовые команды

Факты из исходников (`EssentialsNK.java`, `EssentialsAPI.java`, `CommandManager.java`):

- **язык берётся из языка ядра**: `Language.load(getServer().getLanguage().getLang())` →
  в jar лежат `lang/eng.ini`, `lang/rus.ini`, … Поэтому `language: rus` в `pnx.yml`
  автоматически даёт русские сообщения Essentials — отдельный перевод не нужен;
- **`/setspawn` пишет точку спавна прямо в уровень** (`level.setSpawnLocation(round())`
  + `setDefaultLevel`) → сохраняется в мире, после рестарта на месте;
- `/spawn` телепортит на спавн **дефолтного уровня**, гоняет через `api.onTP`;
- кулдауны — это права, а не конфиг: `essentialsnk.cooldown.<сек>` (общий КД команд),
  `essentialsnk.tp.cooldown.<сек>` (задержка перед телепортом, `TeleportationTask`),
  `essentialsnk.homes.<n>` (лимит домов). Нет права — нет кулдауна/домов;
- `message`, `reply`, `ignore` **не описаны** в `plugin.yml` → их надо выдать руками
  через LuckPerms, иначе у обычных игроков `/msg` не работает;
- файлы данных: `home.yml`, `warp.yml`, `mute.yml`, `ignore.yml` в папке плагина.

Набор для лайт-анархии (без домов и tpa — их роль играют регионы):

```powershell
lp group default permission set essentialsnk.spawn true
lp group default permission set essentialsnk.back true
lp group default permission set essentialsnk.message true
lp group default permission set essentialsnk.reply true
lp group default permission set essentialsnk.ignore true
lp group default permission set essentialsnk.ping true
lp group default permission set essentialsnk.getpos true
lp group default permission set essentialsnk.compass true
lp group default permission set essentialsnk.depth true
lp group default permission set essentialsnk.itemdb true
lp group default permission set essentialsnk.tp.cooldown.5 true

lp group default permission set essentialsnk.home false
lp group default permission set essentialsnk.sethome false
lp group default permission set essentialsnk.delhome false
lp group default permission set essentialsnk.tpa false
lp group default permission set essentialsnk.tpaccept false
lp group default permission set essentialsnk.tpdeny false
lp group default permission set essentialsnk.tpahere false
lp group default permission set essentialsnk.warp false
```

Приёмка:
1. `/plugins` → ровно **LuckPerms + EssentialsPNX**, оба зелёные;
2. `/setspawn` (оп) → «точка спавна установлена» **по-русски**, после рестарта `/spawn` ведёт туда же;
3. `/home` и `/tpa MUSD38` → «нет прав» по-русски;
4. `/msg MUSD38 привет` и `/r ответ` → работают;
5. `/ping`, `/getpos`, `/compass` → работают;
6. `/spawn` с задержкой 5 секунд и сообщением о задержке.

## 0.3. VoidMenus — меню сервера (аналог DeluxeMenus)

Готовый jar: `jars/VoidMenus.jar`. Ставится сразу после Essentials, потому что кнопки меню
вызывают его команды (`spawn`), а позже — магазин и регионы.

- `/menu`, `/menu <id>`, `/menu reload` (`voidmenus.admin`);
- меню описываются в `plugins/VoidMenus/menus.yml` (родные формы Bedrock: кнопки, картинки, `\n`);
- действия кнопок: `player:` / `console:` / `menu:` / `message:` / `close`;
- метки `{player} {online} {max} {tps} {ping} {motd} {world}` + PlaceholderAPI (если стоит);
- права: `voidmenus.use` (всем), `voidmenus.admin` (оп).

Приёмка:
1. `/menu` → форма «Меню» с кнопками Спавн / Инфо / Закрыть;
2. «Спавн» → закрывает меню и телепортит на спавн (`/spawn` от игрока);
3. «Инфо» → второе меню, «Назад» возвращает в главное;
4. `§7` в `content` показывает онлайн и TPS, `{ping}` — твой пинг;
5. правка `menus.yml` + `/menu reload` → изменения видны без рестарта;
6. `/menu nope` → «Меню nope не найдено» по-русски.

## 1. Astra (античит) — ✅ настроен

Правки: `plugins/Astra/config.json`
- `enable_chat_profanity_filtering: false` — иначе `Failed to find wordlist` и плагин выключается
  (в релизном jar нет `profanity_filter.wlist`, а фильтр всё равно заглушка `return message; // TODO`)
- `allowed_chars` + кириллица — иначе любой русский чат отбивается

Приёмка:
1. `plugins` → **Astra зелёная** (в PNX красный = `isEnabled()==false`);
2. в игре написать `привет, сервер работает` — сообщение проходит;
3. 12 сообщений подряд → `§cYou're sending messages too fast`;
4. в логе нет `An error occurred while enabling the plugin Astra`.

Кикает по умолчанию: `invalid_creative_destroy_action`, `inappropriate_block_interaction_range`,
`invalid_attack_actor`, `invalid_permission_request`, `invalid_chat`, `too_much_chat_attempts`.
Reach/CPS — только предупреждение с правом `astra.violations`.

## 2. PlaceholderAPI

Проверить: `/papi` (или `/placeholderapi`) — список плейсхолдеров; `%money%` от LlamaEconomy
должен быть зарегистрирован.

Приёмка: `%money%` подставляется (видно после настройки чата/скорборда).

## 3. PlaceholderChat — ❌ УДАЛЁН (заменён на VoidChat)

Формат чата от PlaceholderChat больше не используется: его листенер ставил формат на `HIGH`,
а ник над головой обновлялся **каждый тик** отдельной таской. Всё это теперь делает VoidChat
без единой повторяющейся таски. Jар и папка плагина удалены:
`plugins/PlaceholderChat.jar` + `plugins/PlaceholderChat/`.

`PlaceholderAPI` остаётся — он нужен Scoreboard (`%money%`, `%server_online%` и т.д.).

## 3b. VoidChat 1.1.0 — чат (локальный 100 блоков / глобальный `!`), ник, КД, плейсхолдеры

Готового плагина под радиус чата в Plugin Hub PNX нет (76 плагинов: чат — только PlaceholderChat
и WelcomeMessage; ChatRadius/ChatControl — это Bukkit/Spigot, API 3.0.0 их не грузит).
Поэтому свой микроплагин. **Собран заранее**, компилировать на сервере ничего не нужно:

- готовый jar: `jars/VoidChat.jar` (4,2 КБ, `api: 3.0.0`, class file 65 = Java 21+);
- исходники: `pnx/VoidChat/VoidChat.java`, `plugin.yml`, `config.yml`.

Как работает:
- без `!` → локальный чат: только игроки в радиусе `radius` (100) в том же мире + консоль;
- с `!` → глобальный чат, `!` отрезается;
- КД между сообщениями (`cooldown-seconds`, по умолчанию 2 с), опам не действует;
- ник над головой `[VoidPE] Ник` — на входе/респавне (+1 дубль через 20 тиков), без тасок;
- подсказки «тебя никто не слышит» нет (по ней можно было палить игроков рядом);
- все тексты — в `config.yml`, на русском.

### Плейсхолдеры (PlaceholderAPI)

Подключаются **по рефлексии** (`Class.forName("org.powernukkitx.placeholderapi.PlaceholderAPI")`),
поэтому без PlaceholderAPI плагин всё равно грузится (в логе `placeholders=false`).
Разворачиваются в `format-local`, `format-global`, `nametag`, `cooldown-message`.
Текст игрока **не** разворачивается — иначе игроки писали бы `%money%` и получали значение.

Порядок: сначала PlaceholderAPI по шаблону, потом `{player}`/`{message}`.
Если наоборот — игрок подставил бы себе плейсхолдеры в текст.

### Ловушка ядра: `{%0}` не работает

`Player.java`: `broadcastMessage(lang.tr(chatEvent.getFormat(), new String[]{displayName, message}), recipients)`.
`BaseLang.tr` сначала гонит строку через `parseLanguageText`, а там `Pattern.compile("%[A-Za-z0-9_.-]+")`:
любой `%<слово>` заменяется лукапом по ключу. `{%0}` → `%0` → ключ `0` не найден → в чат уходит `{0}`.
Поэтому свои метки VoidChat — `{player}` / `{message}` / `{seconds}`.

### Тесты перед сборкой

ECJ 3.39 + Temurin 25 в песочнице, прогон на заглушках API: радиус и отсечение дальнего игрока,
глобальный чат со снятием `!`, КД, обход КД опом, ник, пустое сообщение, `!` без текста,
плейсхолдеры в чате и нике, текст игрока не разворачивается — 17/17 PASS.

Приёмка:
1. `/plugins` → **VoidChat зелёный**, PlaceholderChat отсутствует;
2. в логе `VoidChat enabled. ... placeholders=true`;
3. `привет` → `Ник » привет` (никаких `{}`);
4. `!привет` → `[G] Ник » привет`, `!` не видна;
5. два сообщения подряд → `Не так быстро: подожди ещё N сек.`;
6. над головой `[VoidPE] Ник`, жирного нет;
7. поставить в `format-local` `%player_ping%` → в чате число пинга;
8. отойти на 150 блоков: без `!` не доходит, с `!` доходит.

## 4. Scoreboard

Панель: баланс, онлайн, TPS, координаты (optional).
Приёмка: панель видна, баланс обновляется после `/pay`.

## 5. EssentialsPNX (лайт-анархия)

Запретить телепорты-«домой», оставить `/spawn`, `/back`:
```
lp group default permission set essentialsnk.home false
lp group default permission set essentialsnk.sethome false
lp group default permission set essentialsnk.tpa false
lp group default permission set essentialsnk.tpaccept false
lp group default permission set essentialsnk.warp false
lp group default permission set essentialsnk.back true
lp group default permission set essentialsnk.spawn true
```
Приёмка: `/sethome` → нет прав; `/spawn` → работает.

## 6. LlamaEconomy

`messages.yml` — префикс VoidPE + русский (см. `pnx/LlamaEconomy/messages.yml`),
`config.yml` — `MonetaryUnit`, `DefaultMoney`.
Приёмка: `/money` → `§8» §6§lVoidPE §8| §eВаш баланс: $1000`, `/lecoreload` работает.

## 7. ShopUI

`plugins/ShopUI/config.yml` — магазин из `pnx/shop-config.yml` (продажа ресурсов, покупка расходников).
Приёмка: `/shop` открывает категории, покупка списывает деньги, `/sell all` начисляет.

## 8. SRegionProtector

`region-settings.yml` → `default-flags` (place/break/chest/explode true, `pvp: false`),
`config.yml` → `region-creation-price: true`, `price-per-block: 2`, `language: rus`.
Приёмка: `/rg wand` → два угла → `/rg create test`; друг не ломает блоки внутри, но может убить.

## Полезное

- Лог: `logs/latest.log`; ошибки плагинов — `Select-String -Path .\logs\latest.log -Pattern 'ERROR|Exception'`.
- Перезапуск после конфигов плагинов: `stop` → `.\start.bat`.
- `messages.yml` LlamaEconomy перечитывается `/lecoreload` без рестарта.
