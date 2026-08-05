#!/bin/bash
# Limpia el proyecto: artefactos de compilacion, archivos basura/copias y logs/temporales.
# Uso:
#   ./sbin/clean.sh        -> modo simulacion, solo muestra que se eliminaria
#   ./sbin/clean.sh -f     -> elimina de verdad

cd "$(dirname "$0")/.." || exit 1

FORCE=false
if [[ "$1" == "-f" || "$1" == "--force" ]]; then
  FORCE=true
fi

TARGETS=()

# Artefactos de compilacion (residuo de sbin/compile.sh si falla a mitad)
[ -d bin ] && TARGETS+=("bin")
while IFS= read -r f; do TARGETS+=("$f"); done < <(find . -path ./.git -prune -o -type f -name "*.class" -print)

# Copias, backups y basura de SO
while IFS= read -r f; do TARGETS+=("$f"); done < <(find . -path ./.git -prune -o -type f \( -iname "* copy.*" -o -iname "* copy" -o -iname "*.bak" -o -iname "*~" -o -iname ".DS_Store" -o -iname "Thumbs.db" \) -print)

# Logs y temporales
while IFS= read -r f; do TARGETS+=("$f"); done < <(find . -path ./.git -prune -o -type f \( -iname "*.log" -o -iname "*.tmp" \) -print)

if [ ${#TARGETS[@]} -eq 0 ]; then
  echo "Nada que limpiar."
  exit 0
fi

echo "Se encontraron ${#TARGETS[@]} elemento(s):"
printf ' - %s\n' "${TARGETS[@]}"

if $FORCE; then
  for t in "${TARGETS[@]}"; do
    rm -rf -- "$t"
  done
  echo "Limpieza completada."
else
  echo
  echo "Modo simulacion (dry-run). Ejecuta con -f para eliminar de verdad."
fi
