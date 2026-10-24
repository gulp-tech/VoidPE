# Свой плагин под Lumi (Java 21)

Плагин под Lumi = обычный Nukkit-плагин: класс, наследующий `cn.nukkit.plugin.PluginBase`,
плюс `plugin.yml`. Собирается Gradle-ом, кидается `.jar` в `server/plugins/`.

## 1. Куда положить код

Плагин держим отдельной папкой в этом же репозитории:

```
.
├── server/
└── plugins-src/
    └── VoidCore/            # имя твоего плагина
        ├── build.gradle.kts
        ├── settings.gradle.kts
        └── src/main/
            ├── java/dev/voidpe/voidcore/...
            └── resources/plugin.yml
```

Готовый референс со всеми фишками (кастомные блоки, предметы, зачарования, команды, эффекты):
https://github.com/LuminiaDev/ExamplePlugin

## 2. `settings.gradle.kts`

```kotlin
rootProject.name = "VoidCore"
```

## 3. `build.gradle.kts`

```kotlin
plugins {
    java
}

group = "dev.voidpe"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    // releases-репозиторий Lumi (там же лежат её зависимости)
    maven("https://repo.luminiadev.com/releases")
    // если нужна SNAPSHOT-версия ядра:
    // maven("https://repo.lumi.su/snapshots")
}

dependencies {
    // ВАЖНО: именно compileOnly — ядро уже есть в рантайме сервера
    compileOnly("com.koshakmine:Lumi:1.6.9")
    // compileOnly("com.koshakmine:Lumi:1.6.9-SNAPSHOT")  // для snapshots-репозитория
    compileOnly("org.jetbrains:annotations:26.0.2")
}

tasks.jar {
    archiveBaseName.set("VoidCore")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
```

Версию ядра бери равной той, что стоит у тебя в `server/` (`Lumi-*.jar`), иначе возможны
`NoSuchMethodError` в рантайме.

## 4. `src/main/resources/plugin.yml`

```yaml
name: "VoidCore"
version: "1.0.0"
description: "Ядро проекта VoidPE"
api: ["1.0.0"]          # версия API Nukkit, которую использует плагин
main: "dev.voidpe.voidcore.VoidCore"
```

`api` в большинстве случаев `1.0.0` — именно это значение понимает Nukkit-MOT/Lumi.

## 5. Минимальный плагин

```java
package dev.voidpe.voidcore;

import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;

public class VoidCore extends PluginBase {

    @Override
    public void onLoad() {
        getLogger().info("VoidCore загружается");
    }

    @Override
    public void onEnable() {
        // приветствие на входе — событие без рефлексии (быстрее)
        getServer().getEventBus().subscribe(PlayerJoinEvent.class, event ->
                event.getPlayer().sendMessage(TextFormat.GREEN + "Добро пожаловать на VoidPE!"));

        // своя команда
        getServer().getCommandMap().register("voidcore", new VoidCommand(this));

        getLogger().info("VoidCore включён");
    }

    @Override
    public void onDisable() {
        getLogger().info("VoidCore выключен");
    }
}
```

Команда (простой вариант):

```java
package dev.voidpe.voidcore;

import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.utils.TextFormat;

public class VoidCommand extends PluginCommand<VoidCore> {

    public VoidCommand(VoidCore owner) {
        super("voidcore", owner);
        setDescription("Главная команда VoidPE");
        setPermission("voidcore.use");                   // по умолчанию — только для OP
        setAliases(new String[]{"vc"});

        getCommandParameters().clear();
        addCommandParameters("default", new CommandParameter[]{
                CommandParameter.newEnum("action", new String[]{"info", "reload"}),
                CommandParameter.newType("player", true, CommandParamType.TARGET) // true = необязательный
        });
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!testPermission(sender)) return false;

        String action = args.length > 0 ? args[0].toLowerCase() : "info";
        switch (action) {
            case "info" -> sender.sendMessage(TextFormat.AQUA + "VoidPE Core v1.0.0 | Lumi");
            case "reload" -> sender.sendMessage(TextFormat.YELLOW + "Перезагрузка...");
            default -> sender.sendMessage(TextFormat.RED + "Использование: /vc <info|reload>");
        }
        return true;
    }
}
```

## 6. Сборка и установка

```bash
cd plugins-src/VoidCore
./gradlew build          # Windows: gradlew.bat build
cp build/libs/VoidCore-1.0.0.jar ../../server/plugins/
cd ../../server && ./start.sh
```

Проверка в логе при старте: `Enabling VoidCore v1.0.0` без ошибок.

Быстрый цикл разработки (без рестарта) — плагин-менеджер в консоли сервера:
```
plugins
reload VoidCore      # если поддерживается вашей сборкой; иначе просто рестарт
```

## 7. Что дальше по API

| Хочу | Куда смотреть |
|---|---|
| Слушатели событий | `cn.nukkit.event.*`, `Server#getEventBus()`, `PluginManager#subscribeEvent` |
| Команды с аргументами | `PluginCommand`, `CommandParameter`, `enableParamTree()` |
| Кастомные блоки/предметы | `Registries.BLOCK.registerCustom(...)`, `Registries.ITEM.registerCustom(...)` — только при `features.enable-experiment-mode: true` |
| Кастомные зачарования | `Registries.ENCHANTMENT.registerCustom(...)` |
| GUI-инвентари | библиотека `FakeInventories` |
| Плейсхолдеры в чате | библиотека `Placeholders` |
| Мобы | `MobPluginLumi` (внешний плагин) |
| Миры/измерения | `Server#loadLevel`, настройки `world.*`, `multi-nether-worlds` |
| Миксины (правка ванильного кода) | https://github.com/KoshakMineDEV/ignite-lumi |

## 8. Грабли

- **Не** тяни `implementation("com.koshakmine:Lumi:...")` — получишь `ClassNotFoundException`
  и конфликт классов. Только `compileOnly`.
- Не шарь `.jar` с вложенными зависимостями ядра (`nukkit` уже в сервере).
- Версия Java = 21. Плагин, собранный на 25, не запустится.
- Все команды/листенеры регистрируй в `onEnable()`, а не в конструкторе.
- Для тяжёлых задач (БД, HTTP, файлы) — `getServer().getScheduler().scheduleAsyncTask(...)`,
  не блокируй основной тик.
- Плагины Paper/Spigot (`org.bukkit.*`) тут не работают вообще — другой API.
