#!/usr/bin/env bash
# Скачивает последний релиз Lumi с GitHub в текущую папку.
# Нужен curl. Ничего не перезаписывает без --force.
set -euo pipefail
cd "$(dirname "$0")"

FORCE=0
[ "${1:-}" = "--force" ] && FORCE=1

API="https://api.github.com/repos/KoshakMineDEV/Lumi/releases/latest"

echo "→ Запрашиваю информацию о последнем релизе Lumi..."
JSON="$(curl -fsSL "$API")"

URL="$(printf '%s' "$JSON" | grep -o '"browser_download_url": *"[^"]*\.jar"' | head -n1 | sed 's/.*"\(https[^"]*\)"/\1/')"
TAG="$(printf '%s' "$JSON" | grep -o '"tag_name": *"[^"]*"' | head -n1 | sed 's/.*"\([^"]*\)"$/\1/')"

if [ -z "${URL}" ]; then
  echo "✗ Не удалось найти .jar в последнем релизе." >&2
  echo "  Скачай вручную: https://dl.lumi.su/ или https://github.com/KoshakMineDEV/Lumi/releases" >&2
  exit 1
fi

NAME="$(basename "$URL")"

if [ -f "$NAME" ] && [ "$FORCE" -eq 0 ]; then
  echo "✓ $NAME уже есть (перезаписать: ./download.sh --force)"
  exit 0
fi

echo "→ Скачиваю $NAME ($TAG)..."
curl -fL --progress-bar -o "$NAME" "$URL"

# Удаляем старые версии ядра, чтобы start.sh не путался
for old in Lumi-*.jar; do
  [ -e "$old" ] || continue
  [ "$old" = "$NAME" ] && continue
  echo "→ Удаляю старую версию: $old"
  rm -f "$old"
done

echo "✓ Готово: $NAME"
echo "  Запуск: ./start.sh"
