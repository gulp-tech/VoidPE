#!/usr/bin/env bash
# Запуск сервера Lumi. Объём памяти: RAM=6G ./start.sh
set -euo pipefail
cd "$(dirname "$0")"

RAM="${RAM:-4G}"

JAR="$(ls -1 Lumi-*.jar 2>/dev/null | sort -V | tail -n1 || true)"
if [ -z "${JAR}" ]; then
  echo "✗ Не найден Lumi-*.jar. Сначала выполни: ./download.sh" >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "✗ Java не найдена. Нужна Java 21+ (Azul Zulu 21)." >&2
  exit 1
fi

echo "→ Ядро: $JAR | RAM: $RAM"

exec java \
  -Xms"$RAM" -Xmx"$RAM" \
  -XX:+UseG1GC \
  -XX:+ParallelRefProcEnabled \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+DisableExplicitGC \
  -XX:G1NewSizePercent=30 \
  -XX:G1MaxNewSizePercent=40 \
  -XX:G1HeapRegionSize=8M \
  -XX:G1ReservePercent=20 \
  -XX:G1HeapWastePercent=5 \
  -XX:G1MixedGCCountTarget=4 \
  -XX:InitiatingHeapOccupancyPercent=15 \
  -XX:G1MixedGCLiveThresholdPercent=90 \
  -XX:SurvivorRatio=32 \
  -XX:+PerfDisableSharedMem \
  -XX:MaxTenuringThreshold=1 \
  -Dfile.encoding=UTF-8 \
  -jar "$JAR" "$@"
