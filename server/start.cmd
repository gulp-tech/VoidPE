@echo off
chcp 65001 >nul
title VoidPE - Lumi server
cd /d "%~dp0"

set RAM=4G
set JAR=
for /f "delims=" %%f in ('dir /b /o-n Lumi-*.jar 2^>nul') do (
  if not defined JAR set JAR=%%f
)

if not defined JAR (
  echo [x] Не найден Lumi-*.jar. Положи jar рядом со скриптом или запусти download.cmd
  pause
  exit /b 1
)

echo [i] Ядро: %JAR% ^| RAM: %RAM%

java -Xms%RAM% -Xmx%RAM% ^
 -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 ^
 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC ^
 -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M ^
 -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 ^
 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 ^
 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 ^
 -Dfile.encoding=UTF-8 -jar %JAR% %*

echo.
echo [i] Сервер остановлен.
pause
