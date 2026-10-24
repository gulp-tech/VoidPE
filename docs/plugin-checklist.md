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

## 3. PlaceholderChat — ✅ настроен (ник над головой)

`plugins/PlaceholderChat/config.yml` (см. `pnx/PlaceholderChat/config.yml`):

```yaml
nametag: "§8[§6VoidPE§8] §f%player_displayname%"
chatmessage: "§f%player_displayname%§8 » §7%message%"
```

Факты из исходников:
- конфиг читается **один раз** в конструкторе листенера → только рестарт, `/reload` не помогает;
- плейсхолдеры `%luckperms_prefix%` **не работают**: `PlaceholderAPI` регистрирует только
  `player_*`, `server_*`, `time`, `level_tps`, а LuckPerms-PNX не имеет placeholder-хука;
- `%money%` появляется только если PlaceholderAPI включён **раньше** LlamaEconomy (в логе так и есть);
- неизвестный плейсхолдер остаётся в тексте буквально.

Жирный `§l` **удалён** — на VoidPE жирного текста в чате нет.
После установки VoidChat (п. 3b) эта строка `chatmessage` в игре уже не видна:
VoidChat перебивает формат на `EventPriority.HIGHEST`, а PlaceholderChat ставит свой на `HIGH`.

Приёмка: над головой `[VoidPE] Ник` (без жирного).

## 3b. VoidChat — локальный (100 блоков) и глобальный (`!`) чат

Готового плагина под это в Plugin Hub PNX **нет** (76 плагинов: чат — только PlaceholderChat
и WelcomeMessage). Ядро PNX тоже не умеет радиус: в `Player.java` событие чата рассылается
всем подписчикам `BROADCAST_CHANNEL_USERS`, ключа радиуса в `ServerSettings`/`pnx.yml` нет.
Поэтому — свой микроплагин, компилируется на твоей машине тем же JDK 25 (20 секунд).

Исходники в репозитории: `pnx/VoidChat/VoidChat.java`, `pnx/VoidChat/plugin.yml`,
`pnx/VoidChat/config.yml`.

Как работает:
- сообщение **без** `!` → локальный чат: видят только игроки в радиусе 100 блоков (тот же мир) + консоль;
- сообщение с `!` → глобальный чат: видят все, `!` отрезается;
- формат `{%0}` = ник, `{%1}` = текст — эти подстановки делает само ядро
  (`Player.java`: `lang.tr(chatEvent.getFormat(), {displayName, message})`);
- если в радиусе никого нет — игроку приходит подсказка про `!`.

Приёмка:
1. `/plugins` → **VoidChat зелёный**;
2. в чате `привет` → `Ник » привет` (без `[G]`);
3. в чате `!привет` → `[G] Ник » привет` — второй игрок/консоль видит, `!` не виден;
4. отойти на 150 блоков → локальное сообщение не доходит, глобальное с `!` доходит;
5. в чате нет жирного текста.

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
