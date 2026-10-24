# VoidPE — настройка плагинов по одному (с приёмкой)

Правило: **один плагин → проверка → следующий**. Не двигаемся дальше, пока текущий не зелёный.

Сервер: PowerNukkitX 3.0.5, Java 25, папка `Desktop\PNX`, порт 19132/udp.

## 0. База (уже сделано)

- [x] Ядро PNX 3.0.5, `pnx.yml` (лайт-анархия)
- [x] Java 25 в `start.bat` (иначе ShopUI падает с class 69)
- [x] `--add-opens` в `start.bat`

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

## 3b. VoidChat — чат (локальный 100 блоков / глобальный `!`) + ник + КД

Готового плагина под радиус чата в Plugin Hub PNX нет (76 плагинов: чат — только PlaceholderChat
и WelcomeMessage), в ядре тоже нет настройки. Поэтому свой микроплагин, компилируется JDK 25.
Исходники: `pnx/VoidChat/VoidChat.java`, `plugin.yml`, `config.yml`.

Как работает:
- без `!` → локальный чат: только игроки в радиусе `radius` (100) в том же мире + консоль;
- с `!` → глобальный чат, `!` отрезается;
- КД между сообщениями (`cooldown-seconds`, по умолчанию 2 с), опам не действует;
- ник над головой `[VoidPE] Ник` — ставится на входе/респавне (+1 дубль через 20 тиков),
  без повторяющихся тасок;
- подсказки «тебя никто не слышит» нет (она позволяла вычислять, есть ли рядом игроки).

### Ловушка ядра: `{%0}` не работает

`Player.java`:
`broadcastMessage(lang.tr(chatEvent.getFormat(), new String[]{displayName, message}), recipients)`.
`BaseLang.tr` сначала прогоняет строку через `parseLanguageText`, а там
`Pattern.compile("%[A-Za-z0-9_.-]+")` — любой `%<слово>` заменяется на лукап по ключу.
`{%0}` → `%0` → ключ `0` не найден → в чат уходит `{0}`. Поэтому плейсхолдеры VoidChat —
`{player}` / `{message}` / `{seconds}`, а строка собирается целиком ещё в событии.

Приёмка:
1. `/plugins` → **VoidChat зелёный**, PlaceholderChat отсутствует;
2. `привет` → `Ник » привет` (никаких `{}`);
3. `!привет` → `[G] Ник » привет`, `!` не видна;
4. два сообщения подряд → `Не так быстро: подожди ещё N сек.`;
5. над головой `[VoidPE] Ник`, жирного текста нет;
6. отойти на 150 блоков: без `!` не доходит, с `!` доходит.

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
