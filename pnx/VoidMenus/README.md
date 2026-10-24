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
