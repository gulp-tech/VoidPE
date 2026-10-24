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

## 3. PlaceholderChat

Формат чата с брендом VoidPE и префиксом из LuckPerms.
Приёмка: в чате `§8» §6§lVoidPE §8| §fНик §8» §7текст`.

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
