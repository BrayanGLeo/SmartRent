#!/usr/bin/env bash
set -euo pipefail

# Script replicable de administración y monitoreo Kafka CLI para SmartRent.
# Puede ejecutarse dentro del contenedor kafka-1 o desde el host EC2 mediante:
#   ./infra/kafka-scripts/manage-kafka.sh monitor

CONTAINER_NAME="${KAFKA_CONTAINER:-smartrent-kafka-1}"

run_kafka_cmd() {
  local tool="$1"
  shift
  if command -v "$tool" >/dev/null 2>&1; then
    "$tool" "$@"
  else
    docker exec -i "$CONTAINER_NAME" "$tool" "$@"
  fi
}

if command -v kafka-topics >/dev/null 2>&1; then
  BOOTSTRAP="${BOOTSTRAP_SERVERS:-kafka-1:29092,kafka-2:29092,kafka-3:29092}"
else
  BOOTSTRAP="${BOOTSTRAP_SERVERS:-kafka-1:29092,kafka-2:29092,kafka-3:29092}"
fi

usage() {
  cat <<'EOF'
Uso: manage-kafka.sh <comando> [argumentos]

Comandos disponibles:
  cluster                                           Lista brokers e ID del cluster
  list-topics                                       Lista todos los tópicos
  describe-topic <topic>                            Muestra particiones, réplicas, ISR y configs
  create-topic <name> [P] [RF] [ret_ms] [policy]    Crea un tópico con defaults 3P/3RF/7d/delete
  alter-config <topic> <key=val[,key2=val2]>        Actualiza configuraciones dinámicas del tópico
  alter-partitions <topic> <count>                  Incrementa particiones del tópico
  delete-topic <topic>                              Elimina un tópico
  list-groups                                       Lista consumer groups activos
  describe-group <group>                            Muestra miembros, offsets y lag del grupo
  monitor                                           Vista consolidada: brokers, tópicos, ISR y lag
  produce-test <topic> [key] [json_payload]         Publica un mensaje de prueba al tópico
  consume-test <topic> [max_messages]               Lee mensajes desde el inicio (from-beginning)
EOF
}

cmd="${1:-help}"
shift || true

case "$cmd" in
  cluster)
    echo "=== Brokers & Metadata del Cluster ==="
    run_kafka_cmd kafka-metadata-quorum --bootstrap-server "$BOOTSTRAP" describe --status 2>/dev/null || true
    run_kafka_cmd kafka-broker-api-versions --bootstrap-server "$BOOTSTRAP" | grep -E "id:|rack:" || \
      run_kafka_cmd kafka-broker-api-versions --bootstrap-server "$BOOTSTRAP" | head -n 10
    ;;
  list-topics)
    run_kafka_cmd kafka-topics --bootstrap-server "$BOOTSTRAP" --list
    ;;
  describe-topic)
    topic="${1:?Debe especificar el nombre del tópico}"
    run_kafka_cmd kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic "$topic"
    echo "--- Configuraciones dinámicas ---"
    run_kafka_cmd kafka-configs --bootstrap-server "$BOOTSTRAP" --entity-type topics --entity-name "$topic" --describe
    ;;
  create-topic)
    topic="${1:?Debe especificar el nombre del tópico}"
    partitions="${2:-3}"
    replicas="${3:-3}"
    retention_ms="${4:-604800000}"
    cleanup_policy="${5:-delete}"
    run_kafka_cmd kafka-topics --bootstrap-server "$BOOTSTRAP" \
      --create --if-not-exists \
      --topic "$topic" \
      --partitions "$partitions" \
      --replication-factor "$replicas" \
      --config "min.insync.replicas=2" \
      --config "retention.ms=${retention_ms}" \
      --config "cleanup.policy=${cleanup_policy}"
    ;;
  alter-config)
    topic="${1:?Debe especificar el nombre del tópico}"
    cfg="${2:?Debe especificar key=value}"
    run_kafka_cmd kafka-configs --bootstrap-server "$BOOTSTRAP" \
      --entity-type topics \
      --entity-name "$topic" \
      --alter --add-config "$cfg"
    ;;
  alter-partitions)
    topic="${1:?Debe especificar el nombre del tópico}"
    count="${2:?Debe especificar el número total de particiones}"
    run_kafka_cmd kafka-topics --bootstrap-server "$BOOTSTRAP" \
      --alter --topic "$topic" --partitions "$count"
    ;;
  delete-topic)
    topic="${1:?Debe especificar el nombre del tópico}"
    run_kafka_cmd kafka-topics --bootstrap-server "$BOOTSTRAP" --delete --topic "$topic"
    ;;
  list-groups)
    run_kafka_cmd kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --list
    ;;
  describe-group)
    group="${1:?Debe especificar el consumer group}"
    run_kafka_cmd kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --describe --group "$group"
    ;;
  monitor)
    echo "=== [1/3] Tópicos, Líderes, Réplicas e ISR ==="
    run_kafka_cmd kafka-topics --bootstrap-server "$BOOTSTRAP" --describe
    echo ""
    echo "=== [2/3] Consumer Groups Activos ==="
    groups="$(run_kafka_cmd kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --list || true)"
    echo "$groups"
    echo ""
    echo "=== [3/3] Offsets y Lag por Consumer Group ==="
    for grp in $groups; do
      echo "--- Grupo: $grp ---"
      run_kafka_cmd kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --describe --group "$grp" || true
    done
    ;;
  produce-test)
    topic="${1:?Debe especificar el tópico}"
    key="${2:-1}"
    payload="${3:-{\"eventType\":\"CLI_TEST\",\"rentalId\":1,\"machineId\":1,\"machineName\":\"Excavadora\",\"status\":\"SOLICITADO\"}}"
    printf '%s:%s\n' "$key" "$payload" | run_kafka_cmd kafka-console-producer \
      --bootstrap-server "$BOOTSTRAP" \
      --topic "$topic" \
      --property "parse.key=true" \
      --property "key.separator=:"
    echo "Mensaje enviado a '${topic}' con key='${key}'."
    ;;
  consume-test)
    topic="${1:?Debe especificar el tópico}"
    max_msgs="${2:-10}"
    run_kafka_cmd kafka-console-consumer \
      --bootstrap-server "$BOOTSTRAP" \
      --topic "$topic" \
      --from-beginning \
      --max-messages "$max_msgs" \
      --property print.partition=true \
      --property print.offset=true \
      --property print.key=true \
      --timeout-ms 5000
    ;;
  *)
    usage
    exit 1
    ;;
esac
