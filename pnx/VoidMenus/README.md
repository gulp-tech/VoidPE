# VoidMenus — меню как DeluxeMenus, но на родных формах Bedrock

Готовый jar: `jars/VoidMenus.jar` (9 КБ, `api: 3.0.0`, class 65 = Java 21+).

## Зачем свой плагин

В Plugin Hub PNX аналога DeluxeMenus нет: `InvMenuPNX` — библиотека для разработчиков
(сундук-инвентари из кода), `ShopUI` — только магазин. На Bedrock нативные меню — это
**формы** (`org.powernukkitx.form.window.SimpleForm`), их и используем: большие кнопки,
картинки, перенос строки `\n`.

## Команды и права

- `/menu` — главное меню (`main`);
- `/menu <id>` — конкретное меню;
- `/menu reload` — перечитать `menus.yml` (право `voidmenus.admin`);
- `voidmenus.use` — доступ к `/menu` (по умолчанию всем), `voidmenus.admin` — только опам.

## Формат `plugins/VoidMenus/menus.yml`

```yaml
menus:
  <id>:
    title: "заголовок"
    content: "текст\nвторая строка"
    permission: "необязательно"
    buttons:
      - text: "текст кнопки\nвторая строка"
        image: "textures/ui/infobulb"        # или https://... (Type.URL)
        permission: "необязательно"
        actions:
          - "player: spawn"                   # команда от игрока
          - "console: say привет"             # команда от консоли
          - "menu: info"                      # открыть другое меню
          - "message: §7текст"                # сообщение игроку
          - "close"                           # закрыть форму
```

Метки: `{player}` `{online}` `{max}` `{tps}` `{ping}` `{motd}` `{world}`.
Если установлен PlaceholderAPI — работают и его плейсхолдеры (`%money%` и т.д.),
подключаются по рефлексии, без него плагин не падает.

## Тесты

Логика прогнана на заглушках API (ECJ + Temurin 25): загрузка меню из конфига,
рендер меток, картинки PATH/URL, все пять типов действий, права на меню и на кнопку,
`/menu`, `/menu <id>`, консоль — 20/20 PASS.

## Грабли API PNX 3.0.5 (проверено на живом сервере)

1. `Server.getCommandMap()` возвращает **`SimpleCommandMap`**, а не `CommandMap`
   (интерфейс). Если скомпилировать под `CommandMap`, сервер падает с
   `NoSuchMethodError: 'org.powernukkitx.command.CommandMap org.powernukkitx.Server.getCommandMap()'`
   и плагин выключается. Компилировать строго против `SimpleCommandMap`.
2. `Server.dispatchCommand(...)` в 3.0.5 **нет**. Команды из кнопок запускаются через
   `getServer().getCommandMap().executeCommand(sender, commandLine)`.
3. `Server.getOnlinePlayers()` возвращает `Map<UUID, Player>` — для онлайна `size()`.
4. Два jar одного плагина в `plugins/` → `Невозможно загрузить "...": плагин уже существует`,
   и грузится не тот, что ожидаешь. Один плагин — один jar.
5. Браузер сохраняет повторную загрузку как `VoidMenus (1).jar` / `(2)` — копировать
   лучше так: `Copy-Item -LiteralPath "$env:USERPROFILE\Downloads\VoidMenus.jar" -Destination "plugins\VoidMenus.jar" -Force`.
