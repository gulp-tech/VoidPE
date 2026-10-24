# VoidMenus 2.0.0 — меню как на Java-серверах (сундук с предметами)

Плагин рисует **настоящее меню-сундук** (54 слота): предметы, названия, описание,
клик по предмету выполняет команды. Формат файлов совместим с DeluxeMenus,
поэтому меню можно копировать почти как есть.

## Куда что класть

```
plugins/VoidMenus.jar
plugins/VoidMenus/menus/menu.yml      <- меню (один файл = одно меню)
```

При первом запуске `menus/menu.yml` создаётся сам из jar — сервер сразу рабочий.

## Команды

| команда | что делает |
|---|---|
| `/menu` | открыть главное меню (`open_command: menu`) |
| `/menu <id>` | открыть меню по имени файла или по `open_command` |
| `/menu reload` | перечитать папку `menus` (`voidmenus.admin` или оп) |
| `/menu list` | список меню |

## Формат файла

```yaml
menu_title: '• Меню сервера'     # заголовок окна
open_command: menu              # этой командой тоже открывается
size: 54                        # 9/18/27/36/45/54 (27 и 54 — обычный и двойной сундук)

items:
  Информация:
    slots: [13]                 # можно 13, [13, 14] или "0-8"
    material: BOOK              # как в Bukkit: BOOK, NETHERITE_SWORD, head-{player}
    amount: 1                   # необязательно
    display_name: '&aИнфо'
    lore:
      - ''
      - '&7Онлайн: &a{online}'
    click_commands:
      - '[player] spawn'        # от имени игрока
      - '[console] say привет'  # от консоли
      - '[menu] info'           # открыть другое меню
      - '[message] &7текст'     # сообщение игроку
      - '[close]'               # закрыть меню
      # [sound] ... и прочее из DeluxeMenus просто игнорируются
```

Метки: `{player}` `%player_name%` `{online}` `{max}` `{tps}` `{ping}` `{motd}` `{world}`,
а если установлен PlaceholderAPI — работают и его плейсхолдеры (по рефлексии, без зависимости).

Цвета: `&7`, `&a`, `&l`; hex `&#00F15E` и java-hex `&x&1&C&A&7&6&9` переводятся
в **ближайший из 16 цветов Bedrock** (в Bedrock hex в названиях предметов не работает).

## Старый формат (формы с кнопками)

Если в файле нет `items`, но есть `buttons`, плагин откроет форму Bedrock —
как в 1.x. Файл `plugins/VoidMenus/menus.yml` (старый) читается, только если
папка `menus` пустая.

## Тесты

Логика прогнана на заглушках API PNX 3.0.5 (ECJ + Temurin 25): загрузка меню,
разбор `[player]`/`[console]`/`[close]`/`menu:`, слоты (`13`, `[13]`, `0-3`, `1, 5`),
hex/&-цвета, открытие сундука, отмена клика (предмет нельзя забрать),
`[close]`, `[menu]`, `[message]`, права, устойчивость к исключениям, формы, `/menu*` — **46/46 PASS**.

## Грабли API PNX 3.0.5 (проверено на живом сервере)

1. `Server.getCommandMap()` возвращает **`SimpleCommandMap`**, а не `CommandMap`.
2. `Server.dispatchCommand(...)` в 3.0.5 **нет** — только
   `getServer().getCommandMap().executeCommand(sender, "команда")`.
3. `Server.getOnlinePlayers()` возвращает `Map<UUID, Player>` — для онлайна `size()`.
4. `Player.getPing()` возвращает **`long`**, а не `int`. Ошибка в дескрипторе метода
   = `NoSuchMethodError` внутри тика игрока = сервер выключается целиком.
5. Кастомный сундук в 3.0.5 — это `org.powernukkitx.inventory.fake.FakeInventory`
   (`FakeInventoryType.CHEST` = 27, `DOUBLE_CHEST` = 54). Открытие `player.addWindow(inv)`,
   клики ловятся через `setItemHandler(slot, ItemHandler)`, а чтобы предмет нельзя было
   забрать — `event.setCancelled(true)`.
6. Предмет: `Item.get("minecraft:diamond_sword", meta, count)` — id в нижнем регистре
   с `minecraft:`; `setCustomName`, `setLore(String...)`.
7. Два jar одного плагина в `plugins/` → грузится не тот, что ожидаешь. Один плагин — один jar.
