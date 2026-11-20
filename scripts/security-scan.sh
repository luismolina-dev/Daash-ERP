#!/usr/bin/env bash
# Escaneo de seguridad reproducible del MVP Daash ERP & DTE.
# Uso: ./scripts/security-scan.sh  (desde cualquier directorio)
# Salidas: evidence/*.log y evidence/summary.txt
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EVIDENCE_DIR="$ROOT_DIR/evidence"

mkdir -p "$EVIDENCE_DIR"

if [[ ! -f "$EVIDENCE_DIR/.gitignore" ]]; then
  cat > "$EVIDENCE_DIR/.gitignore" <<'EOF'
# Salidas generadas por scripts/security-scan.sh: no versionar reportes pesados ni binarios.
*.log
*.json
*.sarif
*.html
*.txt
*.db
*.sqlite
*.pdf
reports/
zap/
!.gitignore
EOF
fi

log() {
  printf '[security-scan] %s\n' "$*"
}

record() {
  printf '%s\n' "$*" >> "$EVIDENCE_DIR/summary.txt"
}

# Ejecuta un paso, refleja la salida en pantalla y la conserva en evidence/.
run_step() {
  local name="$1"
  shift
  local logfile="$EVIDENCE_DIR/${name}.log"

  {
    echo "=== $name ==="
    echo "Comando: $*"
    echo "Inicio UTC: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
    echo
  } > "$logfile"

  log "Ejecutando $name ..."
  if ! (cd "$ROOT_DIR" && "$@") 2>&1 | tee -a "$logfile"; then
    {
      echo
      echo "RESULTADO: FALLÓ - $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
    } >> "$logfile"
    record "$name=FAILED"
    log "ERROR: $name falló; revisar $logfile"
    return 1
  fi

  {
    echo
    echo "RESULTADO: OK - $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  } >> "$logfile"
  record "$name=OK"
  log "OK: $name"
}

step_backend_verify() {
  cd "$ROOT_DIR/backend"
  ./mvnw verify
}

{
  echo "security-scan - resumen de ejecución"
  echo "Fecha UTC: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  echo "Repositorio: $ROOT_DIR"
  echo
} > "$EVIDENCE_DIR/summary.txt"

log "Evidencia en $EVIDENCE_DIR"

# 1) Trivy: vulnerabilidades, secretos y configuraciones inseguras.
if command -v trivy >/dev/null 2>&1; then
  run_step trivy-fs trivy fs --scanners vuln,secret,misconfig .
else
  record "trivy-fs=SKIPPED"
  log "Trivy no disponible; instalar desde https://trivy.dev/docs/latest/getting-started/installation/ (omitiendo, no falla)."
fi

# 2) Backend: Maven Wrapper vive en backend/, no en la raíz.
if [[ -x "$ROOT_DIR/backend/mvnw" ]]; then
  run_step backend-verify step_backend_verify
else
  record "backend-verify=SKIPPED"
  log "backend/mvnw no encontrado o no ejecutable; omitiendo verify del backend."
fi

# 3) Frontend: build y lint.
run_step frontend-build npm run build
run_step frontend-lint npm run lint

# 4) Semgrep: opcional; su ausencia no invalida el resto del escaneo.
if command -v semgrep >/dev/null 2>&1; then
  run_step semgrep semgrep scan --config "$ROOT_DIR/semgrep.yml" --exclude node_modules --exclude target --exclude dist --exclude evidence --metrics=off --timeout 120 .
else
  cat > "$EVIDENCE_DIR/semgrep-skipped.log" <<'EOF'
Semgrep no estaba disponible en este entorno.
Instalación sugerida (sin instalar globalmente desde este script):
  python3 -m pip install semgrep
  # o: pipx install semgrep
Luego reejecutar: ./scripts/security-scan.sh
EOF
  record "semgrep=SKIPPED"
  log "Semgrep no disponible: python3 -m pip install semgrep (omitiendo, no falla)."
fi

log "Resumen: $EVIDENCE_DIR/summary.txt"
cat "$EVIDENCE_DIR/summary.txt"
