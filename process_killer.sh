#!/usr/bin/env bash
set -Eeuo pipefail

LOG_DIR="${LOG_DIR:-run-logs}"

log()  { printf "\033[1;33m[KILL]\033[0m %s\n" "$*"; }

kill_tree() {
  # Kill a PID and its children (best-effort).
  local pid="$1"
  [ -z "${pid:-}" ] && return 0
  if kill -0 "$pid" 2>/dev/null; then
    log "Sending TERM to $pid ..."
    pkill -TERM -P "$pid" 2>/dev/null || true
    kill  -TERM "$pid" 2>/dev/null || true
    for i in {1..10}; do
      kill -0 "$pid" 2>/dev/null || break
      sleep 0.2
    done
    if kill -0 "$pid" 2>/dev/null; then
      log "Force killing $pid ..."
      pkill -KILL -P "$pid" 2>/dev/null || true
      kill  -KILL "$pid" 2>/dev/null || true
    fi
  fi
}

kill_pid_file() {
  # Read a PID from file and kill the process tree.
  local file="$1"
  [ -f "$file" ] || return 0
  local pid
  pid="$(cat "$file" 2>/dev/null || true)"
  [[ "$pid" =~ ^[0-9]+$ ]] || { rm -f "$file"; return 0; }
  log "Killing PID from file: $file -> $pid"
  kill_tree "$pid"
  rm -f "$file"
}

kill_by_port() {
  # Kill listeners bound to a TCP port (Linux).
  local port="$1"
  log "Scanning listeners on port $port ..."
  if command -v lsof >/dev/null 2>&1; then
    local pids
    pids="$(lsof -t -iTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)"
    for pid in $pids; do kill_tree "$pid"; done
  elif command -v ss >/dev/null 2>&1; then
    ss -ltnp 2>/dev/null \
      | awk -v p=":$port" '$4 ~ p {print $7}' \
      | sed 's/.*pid=\([0-9]\+\).*/\1/' \
      | sort -u \
      | while read -r pid; do kill_tree "$pid"; done
  else
    log "Neither lsof nor ss available; skipping port scan for $port."
  fi
}

kill_by_pattern() {
  # Kill by command-line pattern.
  local pattern="$1"
  log "Killing by pattern: $pattern"
  if command -v pgrep >/dev/null 2>&1; then
    pgrep -f "$pattern" 2>/dev/null | sort -u | while read -r pid; do
      kill_tree "$pid"
    done
  else
    ps aux | grep -E "$pattern" | grep -v grep | awk '{print $2}' | while read -r pid; do
      kill_tree "$pid"
    done
  fi
}

kill_pid_file "$LOG_DIR/backend.pid"
kill_pid_file "$LOG_DIR/frontend.pid"
kill_pid_file "$LOG_DIR/ollama.pid"

kill_by_port 8080   
kill_by_port 4200   
kill_by_port 11434 

kill_by_pattern "ollama serve"
kill_by_pattern "mvn .*spring-boot:run"
kill_by_pattern "ng serve"
kill_by_pattern "node .*ng serve"

log "Done."

