@echo off
chcp 65001 >nul
title VoidPE - PowerNukkitX
cd /d "%~dp0"

set RAM=2G
if not exist powernukkitx.jar (
  echo [x] powernukkitx.jar не найден рядом со скриптом.
  echo     Скачай: https://github.com/PowerNukkitX/PowerNukkitX/releases
  pause
  exit /b 1
)

java -Xms%RAM% -Xmx%RAM% -Dfile.encoding=UTF-8 -XX:+UseG1GC ^
 --add-opens java.base/java.lang=ALL-UNNAMED ^
 --add-opens java.base/java.io=ALL-UNNAMED ^
 --add-opens java.base/java.net=ALL-UNNAMED ^
 --enable-native-access=ALL-UNNAMED ^
 -jar powernukkitx.jar %*

echo.
echo [i] Сервер остановлен.
pause
