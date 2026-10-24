# Почему мобы «без ИИ» на Lumi — разбор и проверки

## Коротко: в Lumi две разные «мобы»

| | Кто | Есть ли ИИ |
|---|---|---|
| `cn.nukkit.entity.mob.EntityZombie` и т.п. | **ядро Lumi** (WIP internal mob support) | **Нет.** Это только модель + хитбокс + дроп. Интерфейс `cn.nukkit.entity.mob.EntityMob` — пустой маркер (`interface EntityMob {}`), никакого AI/поиска пути в ядре нет |
| `nukkitcoders.mobplugin.entities.monster.walking.Zombie` и т.п. | **плагин MobPlugin** | **Да.** AI в `WalkingMonster/WalkingAnimal.onUpdate()` → `updateMove()` → `WalkingEntity` (route finder, target, panic) |

В README Lumi это и написано: *«Mobs support (with MobPluginLumi), with WIP internal support for them»*.
Т.е. «мобы стоят и ничего не делают» — это не баг сервера, а ожидаемое поведение мобов **ядра**.
Живые мобы = только мобы **MobPlugin**.

Проверить, что MobPlugin зарегистрировал свои классы поверх ядра: в логе при старте

```
Loading MobPlugin v1.0.0-SNAPSHOT
Enabling MobPlugin v1.0.0-SNAPSHOT
```

Регистрация в `MobPlugin#onEnable` идёт **после** `Registries.ENTITY.init()` ядра,
поэтому `Registries.ENTITY` для `"Zombie"`, `"Cow"`, `"32"` и т.д. указывает на класс плагина.
Значит спавн-яйца и `/summon` должны создавать уже «умных» мобов.

## Проверка 1. AI вообще жив? (`/summon`)

В консоли сервера (или в игре с OP), стоя на месте:

```
summon minecraft:zombie ~ ~ ~
```

- **Зомби пошёл к тебе** → AI работает, а «без ИИ» были мобы ядра (спавненные до установки
  MobPlugin, из блока-спавнера или из старой сессии) либо мобы далеко от игрока (см. проверку 3).
- **Зомби стоит** → AI падает с ошибкой, см. проверку 2.

## Проверка 2. Ошибки в логе (главное)

MobPlugin 1.0.0 собран против **Lumi 1.5.0-SNAPSHOT**, у тебя ядро **1.6.9**.
Если в API что-то поменялось, в момент первого «шага» моба вылетает
`NoSuchMethodError` / `AbstractMethodError` — и моб навсегда замирает.

PowerShell (в папке сервера):

```powershell
Select-String -Path .\logs\latest.log -Pattern "Exception|NoSuchMethod|AbstractMethod|MobPlugin|nukkitcoders" |
    Select-Object -Last 40 | ForEach-Object { $_.Line }
```

Или в окне консоли просто прокрути вверх после того, как моб должен был двинуться.

Если такие ошибки есть — версия MobPlugin не подходит к 1.6.9:
нужен билд под 1.6.x (спроси в Discord Lumi / собери сам, см. ниже «Собрать MobPlugin под 1.6.9»).

## Проверка 3. Дистанция тика (Lumi-специфика)

Мобы в Lumi тикают только в «тикающих» чанках, а их радиус задаётся настройками:

```yaml
world:
  view-distance: 8
  chunk:
    ticking-radius: 3      # ← радиус симуляции в чанках (3 = ~48 блоков)
    ticking-per-tick: 40   # сколько чанков обрабатывать за тик
```

В коде: `Level#chunkTickRadius = min(view-distance, max(1, ticking-radius))`,
и `tickChunks()` раздаёт `scheduleUpdate()` сущностям только в этих чанках.

Плюс сам MobPlugin не двигает моба, если рядом нет игрока ближе 80 блоков
(`BaseEntity#isInTickingRange()`, `distanceSquared < 6400`).

Итого: моб дальше ~48 блоков от игрока или в незагруженном чанке — стоит. Это нормально
и в ванилле тоже. Если хочется «живой» мир дальше — поднять:

```yaml
world:
  view-distance: 10
  chunk:
    ticking-radius: 6      # ~96 блоков
    ticking-per-tick: 80
```

(выше = больше нагрузки на CPU, поднимай постепенно и смотри TPS)

## Проверка 4. Сложность мира

`WalkingMonster#onUpdate()`:

```java
if (this.server.getDifficulty().getId() < 1) {
    this.close();   // PEACEFUL -> монстры удаляются
    return false;
}
```

В `settings.yml` должно быть `world.difficulty: NORMAL` (или HARD). Проверить/поправить в игре:
`difficulty normal`.

## Проверка 5. Конфиг MobPlugin

Файл `plugins/MobPlugin/config.yml` (создаётся при первом запуске плагина):

```yaml
autospawn-ticks: 200          # каждые ~5 c (монстры и животные по очереди)
despawn-ticks: 12000
spawn-mobs: true              # монстры
spawn-animals: true           # животные
worlds-spawning-disabled: ""  # список миров через запятую, где спавн выключен
```

Проверь, что `spawn-mobs`/`spawn-animals: true` и твой мир (`world`, `nether`, `the_end`)
не в списке отключённых.

Ещё: животные спавнятся на траве при свете, монстры — в темноте и **дальше 24 блоков** от игрока.
Если стоишь на открытом освещённом месте — монстры рядом просто не появятся, это норма.

## Проверка 6. Сколько мобов вообще

```
plugins          # MobPlugin в списке Enabled
list             # игроки онлайн (спавн идёт только вокруг игроков)
```

## Собрать MobPlugin под 1.6.9 (если версия несовместима)

```bash
git clone https://github.com/LuminiaDev/MobPluginLumi.git
cd MobPluginLumi
# в build.gradle.kts: compileOnly("com.koshakmine:Lumi:1.6.9")
./gradlew build
# build/libs/MobPlugin-1.0.0-SNAPSHOT.jar -> в папку plugins сервера
```

Понадобится доступ к `https://repo.luminiadev.com/releases` (или `.../snapshots`).
Готовый jar кладёшь в `plugins/`, старый удаляешь, сервер перезапускаешь.

---

## Итог: что делать по шагам

1. `/summon minecraft:zombie` рядом с собой → пошёл или стоит?
2. Если стоит — ищи в `logs/latest.log` `NoSuchMethodError`/`AbstractMethodError` от `nukkitcoders`.
3. Есть ошибки → нужен MobPlugin под Lumi 1.6.x (Discord Lumi / сборка из исходников).
4. Нет ошибок → подними `chunk.ticking-radius` до 6 и проверь `difficulty: NORMAL`.
5. Мобы ядра (спавненные до MobPlugin) двигаться не будут никогда — это WIP в самом Lumi.
