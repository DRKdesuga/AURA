#!/usr/bin/env bash
set -Eeuo pipefail

LOG_DIR="${LOG_DIR:-run-logs}"

kill_pid_file() {
  local file="$1"
  [ -f "$file" ] || return 0

  local pid
  pid="$(cat "$file" 2>/dev/null || true)"
  [[ "$pid" =~ ^[0-9]+$ ]] || { rm -f "$file"; return 0; }

  if kill -0 "$pid" 2>/dev/null; then
    echo "Killing PID $pid (from $file) ..."
    kill "$pid" 2>/dev/null || true
    # wait up to ~3s for graceful shutdown
    for i in {1..10}; do
      kill -0 "$pid" 2>/dev/null || break
      sleep 0.3
    done
    if kill -0 "$pid" 2>/dev/null; then
      echo "Force killing PID $pid ..."
      kill -9 "$pid" 2>/dev/null || true
    fi
  fi

  rm -f "$file"
}

kill_pid_file "$LOG_DIR/backend.pid"
kill_pid_file "$LOG_DIR/frontend.pid"
kill_pid_file "$LOG_DIR/ollama.pid"

echo "Done."

