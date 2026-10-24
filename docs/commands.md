# VoidPE — команды по шагам (copy-paste)

Ниже — ровно то, что вставлять в терминал. Выбери свою ОС.
Ничего дополнительно искать не нужно: все ссылки уже подставлены.

**Важно:** Lumi — это **Bedrock (PE)** и порт **UDP 19132**. `eula.txt` (как у Paper) тут не нужен.

---

## A. Windows 10/11 (PowerShell)

### A0. Проверить Java (нужна 21+)

```powershell
java -version
```

Если пишет «не найден» или версия ниже 21 — установить:

```powershell
winget install --id Azul.Zulu.21.JDK -e
```

или скачать MSI вручную: https://cdn.azul.com/zulu/bin/zulu21.44.17-ca-jdk21.0.8-win_x64.msi
После установки **закрой и снова открой PowerShell**, снова `java -version`.

### A1. Папка сервера и скачивание ядра

```powershell
cd $HOME
mkdir VoidPE-server
cd VoidPE-server
curl.exe -L -o Lumi-1.6.9.jar "https://github.com/KoshakMineDEV/Lumi/releases/download/v1.6.9-19bfccbff1b6c1ae793857536050e658c198cd4e/Lumi-1.6.9.jar"
dir
```

(должен появиться `Lumi-1.6.9.jar`, ~30–60 МБ)

### A2. Первый запуск

```powershell
java -Xms2G -Xmx2G -jar Lumi-1.6.9.jar
```

Ждём строку вида `Server started` / `Done`. Появились `settings.yml`, `worlds/`, `plugins/`, `logs/`.
Остановить: `Ctrl+C` в окне, или команда `stop` + Enter.

### A3. Настройки (минимум)

```powershell
notepad settings.yml
```

Поменять:
- `motd:` — название сервера
- `max-players:` — сколько игроков
- `world.default-gamemode:` — `0` выживание / `1` креатив
- `xbox-auth: true` — оставить `true` для публичного сервера
- `general.server-authoritative-movement: "server-auth"` — оставить

Сохранить → снова запустить сервер.

### A4. Мобы (обязательный плагин)

```powershell
mkdir plugins -ErrorAction SilentlyContinue
curl.exe -L -o plugins/MobPlugin-1.0.0-SNAPSHOT.jar "https://github.com/LuminiaDev/MobPluginLumi/releases/download/1.0.0/MobPlugin-1.0.0-SNAPSHOT.jar"
```

### A5. Подключиться из игры

Узнать IP компьютера в локальной сети:

```powershell
ipconfig
```

Берём `IPv4-адрес` (например `192.168.1.50`).
В Minecraft Bedrock: **Play → Servers → Add Server → Address: `192.168.1.50:19132`**.

Чтобы заходили из интернета: проброс **UDP 19132** на роутере + открыть порт в фаерволе:

```powershell
New-NetFirewallRule -DisplayName "Lumi 19132 UDP" -Direction Inbound -Protocol UDP -LocalPort 19132 -Action Allow
```

### A6. Автозапуск (чтобы окно не закрывалось)

Создай `start.bat` в папке сервера:

```bat
@echo off
cd /d "%~dp0"
java -Xms2G -Xmx2G -XX:+UseG1GC -jar Lumi-1.6.9.jar
pause
```

Запуск — двойным кликом по `start.bat`.

---

## B. Linux (Ubuntu 22.04+/Debian 13, VPS)

### B0. Java

```bash
sudo apt update && sudo apt install -y openjdk-21-jre-headless curl unzip
java -version    # должно быть 21+
```

Если в репозитории дистрибутива нет 21 (например Debian 12):

```bash
curl -fsSL "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jre/hotspot/normal/eclipse" -o /tmp/jre21.tar.gz
sudo mkdir -p /opt/jre21 && sudo tar -xzf /tmp/jre21.tar.gz -C /opt/jre21 --strip-components=1
export PATH=/opt/jre21/bin:$PATH
java -version
```

### B1. Папка и скачивание ядра

```bash
mkdir -p ~/VoidPE-server && cd ~/VoidPE-server
curl -fL -o Lumi-1.6.9.jar "https://github.com/KoshakMineDEV/Lumi/releases/download/v1.6.9-19bfccbff1b6c1ae793857536050e658c198cd4e/Lumi-1.6.9.jar"
ls -lh
```

### B2. Первый запуск

```bash
java -Xms2G -Xmx2G -jar Lumi-1.6.9.jar
```

(остановить — команда `stop`)

Для слабого VPS: `-Xms1G -Xmx1G`. Для 20+ игроков: `-Xms4G -Xmx4G`.

### B3. Настройки

```bash
nano settings.yml      # Ctrl+O, Enter, Ctrl+X — сохранить и выйти
```

### B4. Мобы

```bash
mkdir -p plugins
curl -fL -o plugins/MobPlugin-1.0.0-SNAPSHOT.jar "https://github.com/LuminiaDev/MobPluginLumi/releases/download/1.0.0/MobPlugin-1.0.0-SNAPSHOT.jar"
```

### B5. Открыть порт (если не открыт)

```bash
sudo ufw allow 19132/udp
sudo ufw status
```

Проверить, что сервер реально слушает:

```bash
ss -lunp | grep 19132
```

### B6. Держать сервер 24/7 (tmux)

```bash
sudo apt install -y tmux
tmux new -s lumi
cd ~/VoidPE-server
java -Xms4G -Xmx4G -jar Lumi-1.6.9.jar
# выйти из tmux не убивая сервер: Ctrl+B, затем D
# вернуться: tmux attach -t lumi
```

Остановить корректно — в консоли сервера команда `stop` (не `kill -9`, иначе можно побить миры).

### B7. Автозапуск через systemd (по желанию)

```bash
sudo tee /etc/systemd/system/lumi.service > /dev/null <<'EOF'
[Unit]
Description=VoidPE (Lumi Bedrock server)
After=network-online.target

[Service]
Type=simple
User=YOUR_USER
WorkingDirectory=/home/YOUR_USER/VoidPE-server
ExecStart=/usr/bin/java -Xms4G -Xmx4G -XX:+UseG1GC -jar /home/YOUR_USER/VoidPE-server/Lumi-1.6.9.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo sed -i "s/YOUR_USER/$USER/g" /etc/systemd/system/lumi.service
sudo systemctl daemon-reload
sudo systemctl enable --now lumi
sudo systemctl status lumi
# логи: journalctl -u lumi -f
```

---

## C. Первые команды в консоли сервера

```
op ТвойНик          # выдать себе права оператора
gamemode 0 ТвойНик  # выживание
time set day
plugins             # список плагинов
stop                # корректная остановка
```

---

## D. Обновление ядра (когда выйдет новая версия)

```bash
# Linux
cd ~/VoidPE-server
URL=$(curl -fsSL https://api.github.com/repos/KoshakMineDEV/Lumi/releases/latest \
  | grep -o '"browser_download_url": *"[^"]*\.jar"' | head -n1 | sed 's/.*"\(https[^"]*\)"/\1/')
echo "$URL"
# сначала остановить сервер командой stop, затем:
curl -fL -O "$URL"
```

```powershell
# Windows (PowerShell)
cd $HOME\VoidPE-server
$r = Invoke-RestMethod -Uri 'https://api.github.com/repos/KoshakMineDEV/Lumi/releases/latest' -Headers @{'User-Agent'='VoidPE'}
$a = $r.assets | Where-Object { $_.name -like '*.jar' } | Select-Object -First 1
Write-Host $a.name
Invoke-WebRequest $a.browser_download_url -OutFile $a.name
```

**Перед обновлением сделай бэкап:**

```bash
# Linux
tar -czf backup-$(date +%F).tar.gz world* players plugins settings.yml
```
```powershell
# Windows
Compress-Archive -Path world*,players,plugins,settings.yml -DestinationPath "backup-$(Get-Date -Format yyyy-MM-dd).zip"
```

---

## E. Если что-то не работает

| Что видишь | Что делать |
|---|---|
| `java: command not found` / «не является внутренней командой» | Java не установлена или не в PATH → шаг A0 / B0 |
| `UnsupportedClassVersionError` | Java старее 21 |
| `Error: Unable to access jarfile Lumi-1.6.9.jar` | не в той папке → `ls`/`dir`, скачать заново |
| Порт занят / сервер не стартует | другой сервер уже слушает 19132 → `ss -lunp \| grep 19132` (Linux) / `netstat -ano \| findstr 19132` (Win) |
| В игре «Unable to connect» на своём ПК | в адрес сервера пиши `127.0.0.1:19132` |
| Друзья снаружи не заходят | порт **UDP** 19132 не проброшен/не открыт; проверь `ufw` и роутер |
| `Outdated client` | клиент старее минимальной версии → в `settings.yml` `multiversion.min-protocol: 0` |
| Мир «пропал» после `kill -9` | не убивай процесс жёстко, только `stop`; откатись из бэкапа |
