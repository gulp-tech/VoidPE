# VoidPE — сервер Minecraft Bedrock на ядре Lumi

Репозиторий-заготовка для сервера **Minecraft Bedrock Edition** на ядре [Lumi](https://github.com/KoshakMineDEV/Lumi).
Здесь лежит: стартовые скрипты, конфиг, `.gitignore` и инструкции — чтобы не собирать всё с нуля.

> ⚠️ Lumi — это **Bedrock (PE)**, а не Java. Плагины от Paper/Spigot сюда **не подходят**.
> Lumi — хард-форк Nukkit-MOT, поэтому подходят плагины Nukkit-MOT (и большинство старых Nukkit-плагинов).

---

## 0. Что такое Lumi (коротко)

| | |
|---|---|
| Платформа | Minecraft Bedrock Edition |
| Версии | 1.20.0 – 1.26.50 (мультиверсия, настраивается `settings.yml`) |
| Язык плагинов | Java 21+ |
| База | Nukkit-MOT (LGPL-3.0) |
| Фишки | кастомные блоки/предметы/зачарования, анти-икс-рей, миксины через [Ignite](https://github.com/KoshakMineDEV/ignite-lumi), мобы через MobPluginLumi |
| Сайт / загрузка | https://dl.lumi.su/ |
| Релизы на GitHub | https://github.com/KoshakMineDEV/Lumi/releases |
| Discord (поддержка) | https://discord.gg/HkTnrkUySJ |
| Шаблон плагина | https://github.com/LuminiaDev/ExamplePlugin |
| Документация | https://github.com/LuminiaDev/Lumi-Docs |

---

## 1. Что нужно поставить на машину

1. **Java 21+** (рекомендуется Azul Zulu 21). Проверка: `java -version` → должно быть `21` или выше.
   - Ubuntu/Debian: `sudo apt install openjdk-21-jre-headless`
   - Windows: Zulu 21 MSI с https://www.azul.com/downloads/
2. **2 ГБ RAM минимум** под сервер (спокойно играть вдвоём-впятером — 2–4 ГБ).
3. **Открытый UDP-порт 19132** (по умолчанию Lumi слушает именно UDP, не TCP!).

## 2. Установка сервера (5 минут)

> Нужны готовые команды для copy-paste (Windows / Linux, порт, tmux, обновление, бэкапы)?
> → **[`docs/commands.md`](docs/commands.md)**

```bash
cd server
./download.sh      # скачает последний Lumi-*.jar с GitHub Releases
./start.sh         # первый запуск: сгенерирует settings.yml, worlds/, logs/
```

Windows: `cd server`, затем `download.cmd` (или положи `Lumi-x.y.z.jar` руками) и `start.cmd`.

Чтобы зайти с телефона/консоли/PC-версии Bedrock, в игре: **Play → Servers → Add Server**,
адрес — `IP_машины:19132` (для локальной сети `127.0.0.1:19132` при игре на том же ПК).

> Первый запуск на публичном IP: оставь `xbox-auth: true` — иначе у игроков не будет
> нормальных Xbox-ников и скинов, а сервер будет «пиратским».

## 3. Что настроить в первую очередь

Файл `server/settings.yml` (создаётся при первом запуске; в репозитории лежит заготовка с базовыми ключами —
недостающее Lumi допишет сам, лишние ключи вырежет).

Ключевые места:

```yaml
general:
  motd: "VoidPE"
  server-port: 19132
  max-players: 50
  multiversion:
    min-protocol: 0      # 0 = от самой старой поддерживаемой
    max-protocol: -1     # -1 = до последней

network:
  xbox-auth: true
  encryption: false      # true, если НЕ используешь прокси

world:
  default-world-name: world
  default-world-seed: "" # свой сид
  view-distance: 8
  difficulty: NORMAL
  allow-pvp: true

player:
  default-gamemode: 0    # 0 = выживание, 1 = креатив
  whitelist: false

performance:
  async-workers: "auto"
  auto-tick-rate: true
  thread-watchdog: true

features:
  enable-experiment-mode: true   # нужно для кастомных предметов
  vanilla-portals: true
```

Полный список настроек — в исходниках пакета `cn.nukkit.settings`
(https://github.com/KoshakMineDEV/Lumi/tree/stable/src/main/java/cn/nukkit/settings) —
там каждый ключ с комментарием.

## 4. Плагины

- Кидаешь `.jar` в `server/plugins/`, перезапускаешь сервер.
- **Обязательно к установке:** [MobPluginLumi](https://github.com/LuminiaDev/MobPluginLumi/releases) —
  мобы (в самом Lumi поддержка мобов ещё WIP, у мобов ядра нет ИИ).
  Если мобы стоят на месте — см. [`docs/mobs.md`](docs/mobs.md).
- Полезные библиотеки от той же команды: `FakeInventories` (фейковые GUI-инвентари),
  `Placeholders`, `Basics`, `DynamicEconomy`.
- Готовые плагины под Lumi ищи на форуме talk.24serv.pro (раздел «Плагины», в названии темы метка `[Lumi]`).
- Плагины под Paper/Spigot **не работают**. Плагины под Nukkit-MOT — работают (проверяй версию API).

Команды в консоли сервера: `op <ник>`, `stop`, `plugins`, `version`, `whitelist on` и т.д.

## 5. Куда дальше (по возрастанию усилий)

1. **Проверить, что всё живо** → `server/logs/`, `./start.sh`, порт `19132/udp`.
2. **Настроить геймплей** → `settings.yml` + `server/plugins/*/config.yml`.
3. **Свой плагин (главное для уникальности сервера)** → см. [`docs/plugin-dev.md`](docs/plugin-dev.md):
   Gradle + `com.koshakmine:Lumi`, `plugin.yml`, команды, слушатели событий.
4. **Прокси/сеть (Waterdog PE)** — если планируешь несколько серверов за одним IP
   (тогда `xbox-auth: false` + `use-waterdog: true` на бэкенде, авторизация — на прокси).
5. **Бэкапы и деплой** — `server/worlds/` в git **не** кладём (см. `.gitignore`), лучше rsync/архив по cron.

## 6. Структура репозитория

```
.
├── README.md              # этот файл
├── docs/
│   └── plugin-dev.md      # как писать свой плагин под Lumi
└── server/                # рабочий каталог сервера (сам jar и миры не в git)
    ├── download.sh        # скачать/обновить Lumi
    ├── download.cmd
    ├── start.sh           # запуск (RAM настраивается через переменную RAM)
    ├── start.cmd
    └── settings.yml       # заготовка конфига
```

Запуск с другим объёмом памяти: `RAM=6G ./start.sh` (Windows: правишь `-Xmx` в `start.cmd`).

---

## Траблшутинг-чеклист

| Симптом | Причина/что делать |
|---|---|
| «Server is offline» в списке | не открыт **UDP** 19132 (фаервол/роутер/хостинг) |
| Игрок не может зайти, в логе `Outdated client/server` | ограничь `multiversion.min-protocol` под свой клиент |
| `UnsupportedClassVersionError` | Java старее 21 |
| Плагин не грузится | собран под Paper/Spigot, либо `api` в `plugin.yml` несовместим с версией Lumi |
| Лаги при 20+ игроках | `view-distance: 6`, `chunk.sending-per-tick`, память ≥4 ГБ, NVMe-диск под миры |
