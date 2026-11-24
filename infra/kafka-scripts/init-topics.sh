#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP="${BOOTSTRAP_SERVERS:-kafka-1:29092,kafka-2:29092,kafka-3:29092}"

echo "[kafka-init] Esperando disponibilidad del cluster Kafka (${BOOTSTRAP})..."
for i in $(seq 1 30); do
  if kafka-broker-api-versions --bootstrap-server "$BOOTSTRAP" >/dev/null 2>&1; then
    echo "[kafka-init] Cluster Kafka disponible."
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "[kafka-init] ERROR: No se pudo conectar a Kafka tras 30 intentos." >&2
    exit 1
  fi
  sleep 2
done

create_or_update_topic() {
  local topic="$1"
  local partitions="$2"
  local replicas="$3"
  local configs_csv="$4"

  echo "[kafka-init] Creando tópico '${topic}' (P=${partitions}, RF=${replicas}) si no existe..."
  kafka-topics --bootstrap-server "$BOOTSTRAP" \
    --create --if-not-exists \
    --topic "$topic" \
    --partitions "$partitions" \
    --replication-factor "$replicas"

  echo "[kafka-init] Aplicando políticas '${configs_csv}' en tópico '${topic}'..."
  kafka-configs --bootstrap-server "$BOOTSTRAP" \
    --entity-type topics \
    --entity-name "$topic" \
    --alter --add-config "$configs_csv"
}

# Tópico 1: Flujo transaccional e inmutable de arriendos (retención 7 días, delete, límite 1GB)
create_or_update_topic \
  "rentals.events" 3 3 \
  "min.insync.replicas=2,cleanup.policy=delete,retention.ms=604800000,retention.bytes=1073741824"

# Tópico 2: Flujo de estado del catálogo de maquinarias (compact + delete, retención 14 días)
create_or_update_topic \
  "catalog.events" 3 3 \
  "min.insync.replicas=2,cleanup.policy=[compact,delete],retention.ms=1209600000,segment.ms=86400000,delete.retention.ms=86400000"

# Dead Letter Topics asociados (retención extendida 30 días para auditoría de fallos)
create_or_update_topic \
  "rentals.events-dlt" 3 3 \
  "min.insync.replicas=2,cleanup.policy=delete,retention.ms=2592000000"

create_or_update_topic \
  "catalog.events-dlt" 3 3 \
  "min.insync.replicas=2,cleanup.policy=delete,retention.ms=2592000000"

echo "[kafka-init] Descripción actual de tópicos (líderes, réplicas e ISR):"
kafka-topics --bootstrap-server "$BOOTSTRAP" --describe \
  --topic "rentals.events" \
  --topic "catalog.events" \
  --topic "rentals.events-dlt" \
  --topic "catalog.events-dlt"

echo "[kafka-init] Inicialización completada exitosamente."
