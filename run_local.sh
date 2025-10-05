#!/usr/bin/env bash
set -Eeuo pipefail

# ===== Settings =====
MODEL="${MODEL:-llama3.2}"
OLLAMA_URL="${OLLAMA_URL:-http://localhost:11434}"
BACKEND_DIR="${BACKEND_DIR:-aura-backend}"
FRONTEND_DIR="${FRONTEND_DIR:-aura-frontend}"
FRONTEND_PORT="${FRONTEND_PORT:-4200}"
LOG_DIR="${LOG_DIR:-run-logs}"

mkdir -p "$LOG_DIR"

log()  { printf "\033[1;36m[SETUP]\033[0m %s\n" "$*"; }
fail() { printf "\033[1;31m[ERROR]\033[0m %s\n" "$*" >&2; exit 1; }
need_cmd() { command -v "$1" >/dev/null 2>&1 || fail "Missing command: $1"; }

wait_http() {
  # $1=url, $2=name, $3=timeout_s
  local url="$1" name="$2" timeout="${3:-60}"
  local i=0
  until curl -fsS "$url" -o /dev/null; do
    ((i++))
    if (( i >= timeout )); then return 1; fi
    sleep 1
  done
  log "$name is ready"
}

# ===== Preflight =====
need_cmd curl
need_cmd ollama
need_cmd mvn
need_cmd npm

# ===== 1) Ensure Ollama server is running =====
log "Checking Ollama server at $OLLAMA_URL ..."
if curl -fsS "$OLLAMA_URL/api/tags" -o /dev/null; then
  log "Ollama server already running."
else
  log "Starting Ollama server ..."
  ( nohup ollama serve > "$LOG_DIR/ollama.log" 2>&1 & echo $! > "$LOG_DIR/ollama.pid" ) || true
  sleep 2
  if ! wait_http "$OLLAMA_URL/api/tags" "Ollama" 90; then
    fail "Ollama did not become ready. Check $LOG_DIR/ollama.log"
  fi
fi

# ===== 2) Ensure model exists, then warm-load it =====
log "Ensuring model '$MODEL' exists ..."
if curl -fsS "$OLLAMA_URL/api/tags" | grep -q "\"name\":\"${MODEL}"; then
  log "Model '$MODEL' already present."
else
  log "Pulling model: $MODEL (one-time download) ..."
  ollama pull "$MODEL" 2>&1 | tee "$LOG_DIR/ollama-pull.log"
  log "Model '$MODEL' pulled."
fi

log "Warming model in memory ..."
curl -fsS -X POST "$OLLAMA_URL/api/generate" \
  -H 'Content-Type: application/json' \
  -d "$(printf '{"model":"%s","prompt":"ping","stream":false}' "$MODEL")" \
  -o /dev/null || true
log "Model warmed."

# ===== 3) Backend (mvn clean spring-boot:run) =====
[ -d "$BACKEND_DIR" ] || fail "Backend directory not found: $BACKEND_DIR"
log "Starting backend (Spring Boot) ..."
(
  cd "$BACKEND_DIR"
  nohup mvn -q -DskipTests clean spring-boot:run > "../$LOG_DIR/backend.log" 2>&1 &
  echo $! > "../$LOG_DIR/backend.pid"
)
# Optional readiness wait (non-blocking servers remain up after script exits)
for i in {1..120}; do
  if curl -sS -o /dev/null "http://localhost:8080"; then
    log "Backend is up on http://localhost:8080"
    break
  fi
  sleep 1
done

# ===== 4) Frontend (npm start on 4200) =====
[ -d "$FRONTEND_DIR" ] || fail "Frontend directory not found: $FRONTEND_DIR"
log "Starting frontend (Angular dev server, port ${FRONTEND_PORT}) ..."
(
  cd "$FRONTEND_DIR"
  if [ ! -d node_modules ]; then
    log "Installing frontend dependencies (npm ci) ..."
    npm ci
  fi
  nohup npm start -- --port "$FRONTEND_PORT" > "../$LOG_DIR/frontend.log" 2>&1 &
  echo $! > "../$LOG_DIR/frontend.pid"
)
# Optional readiness wait
for i in {1..120}; do
  if curl -fsS "http://localhost:${FRONTEND_PORT}" -o /dev/null; then
    log "Frontend is up on http://localhost:${FRONTEND_PORT}"
    break
  fi
  sleep 1
done

# ===== Summary =====
echo
log "All set!"
echo "  • Ollama  : $OLLAMA_URL            (logs: $LOG_DIR/ollama.log)"
echo "  • Backend : http://localhost:8080  (logs: $LOG_DIR/backend.log)"
echo "  • Frontend: http://localhost:${FRONTEND_PORT} (logs: $LOG_DIR/frontend.log)"
echo
echo "Tail logs with:"
echo "  tail -f $LOG_DIR/ollama.log $LOG_DIR/backend.log $LOG_DIR/frontend.log"

