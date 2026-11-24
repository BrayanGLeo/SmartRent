#!/bin/sh
set -eu

# Script de arranque resiliente para microservicios SmartRent en AWS EC2.
# Se ejecuta en cada inicio o reinicio del contenedor (incluido el autoarranque
# tras detener/iniciar las instancias en AWS Academy Learner Lab).
# Espera a que PostgreSQL, RabbitMQ y/o los 3 brokers Kafka acepten conexiones TCP
# antes de lanzar Spring Boot, evitando fallos de arranque simultáneo multi-EC2.

SERVICE_NAME="${SPRING_APPLICATION_NAME:-$(hostname)}"
INITIAL_DELAY="${WAIT_INITIAL_DELAY:-15}"
POST_READY_DELAY="${WAIT_POST_READY_DELAY:-0}"
MAX_WAIT="${WAIT_MAX_SECONDS:-180}"
INTERVAL="${WAIT_INTERVAL_SECONDS:-3}"
TARGETS="${WAIT_FOR_TARGETS:-}"

if [ "$INITIAL_DELAY" -gt 0 ] 2>/dev/null; then
  echo "[wait-and-start][$SERVICE_NAME] Delay inicial de ${INITIAL_DELAY}s antes de verificar dependencias..."
  sleep "$INITIAL_DELAY"
fi

if [ -n "$TARGETS" ]; then
  START_TS="$(date +%s)"
  for target in $(printf '%s' "$TARGETS" | tr ',' ' '); do
    host="${target%:*}"
    port="${target##*:}"
    if [ -z "$host" ] || [ -z "$port" ] || [ "$host" = "$port" ]; then
      continue
    fi

    echo "[wait-and-start][$SERVICE_NAME] Esperando disponibilidad TCP de ${host}:${port} (máx ${MAX_WAIT}s)..."
    while ! nc -z -w 3 "$host" "$port" >/dev/null 2>&1; do
      NOW_TS="$(date +%s)"
      ELAPSED=$((NOW_TS - START_TS))
      if [ "$ELAPSED" -ge "$MAX_WAIT" ]; then
        echo "[wait-and-start][$SERVICE_NAME] ADVERTENCIA: Tiempo máximo (${MAX_WAIT}s) agotado esperando ${host}:${port}. Continuando arranque..."
        break
      fi
      sleep "$INTERVAL"
    done

    if nc -z -w 3 "$host" "$port" >/dev/null 2>&1; then
      echo "[wait-and-start][$SERVICE_NAME] Dependencia lista: ${host}:${port}"
    fi
  done
fi

if [ "$POST_READY_DELAY" -gt 0 ] 2>/dev/null; then
  echo "[wait-and-start][$SERVICE_NAME] Esperando ${POST_READY_DELAY}s de estabilización del clúster antes de iniciar Spring Boot..."
  sleep "$POST_READY_DELAY"
fi

echo "[wait-and-start][$SERVICE_NAME] Iniciando aplicación Java..."
exec java -jar /app/app.jar "$@"
