# 🐳 SmartRent — Infraestructura Docker Compose y Scripts Kafka HA (`infra/`)

Contiene las definiciones de despliegue **Docker Compose** desacopladas por instancia **AWS EC2** (o entorno local) junto con los scripts CLI replicables para inicializar y monitorear el clúster **Apache Kafka HA 3×3**.

---

## 📁 Contenido del Directorio y Accesos AWS

| Archivo | Rol / Instancia Objetivo | IP Privada VPC | URL Pública / IP Elástica | Puertos Expuestos |
| :--- | :--- | :---: | :--- | :--- |
| [`compose.apps.yml`](./compose.apps.yml) | **EC2-Apps** (`i-0f64ef75cc7bcb761`) — 8 microservicios + PostgreSQL 15 | `172.31.76.200` | `https://hnws3rgb6i.execute-api.us-east-1.amazonaws.com/v2/api` | BFF `8080`, PG `127.0.0.1:5432` |
| [`compose.mq.yml`](./compose.mq.yml) | **EC2-MQ** (`i-0965eaa82b5d4db5f`) — RabbitMQ 3 Management | `172.31.29.239` | `http://100.50.92.218:15672` (`guest`/`guest`) | AMQP `5672`, UI `15672` |
| [`compose.kafka.yml`](./compose.kafka.yml) | **EC2-Kafka** (`i-0c90ee30cdfc784a7`) — 3 ZK + 3 Brokers + `kafka-init` + UI | `172.31.30.107` | `http://3.231.182.87:8090` | ZK `2181..2183`, Kafka `9092..9094`, UI `8090` |
| [`compose.frontend.yml`](./compose.frontend.yml) | **EC2-Frontend** (`i-0e0c3536edfd0b413`) — SPA Angular 22 en Nginx | `172.31.27.98` | `https://tv9aux1za4.execute-api.us-east-1.amazonaws.com` (EIP `34.225.219.131`) | HTTP `80` |
| [`app-scripts/wait-and-start.sh`](./app-scripts/wait-and-start.sh) | Entrypoint con delay y espera TCP inteligente para PostgreSQL, RabbitMQ y los 3 brokers Kafka en cada reinicio de `EC2-Apps` | — | — | — |
| [`kafka-scripts/init-topics.sh`](./kafka-scripts/init-topics.sh) | Inicialización idempotente de tópicos oficiales y DLTs (`kafka-init`) | — | — | — |
| [`kafka-scripts/manage-kafka.sh`](./kafka-scripts/manage-kafka.sh) | CLI replicable para administrar tópicos, consumer groups, lag y pruebas produce/consume | — | — | — |

---

## ⚙️ Parámetros y Características Clave

- **Autoarranque tras reinicio EC2**: todos los servicios persistentes usan `restart: unless-stopped`.
- **Healthchecks y orden de inicio**:
  - `postgres` (`pg_isready`) bloquea el arranque de los microservicios JPA hasta `service_healthy`.
  - `zookeeper-1..3` (`cub zk-ready`) bloquean a `kafka-1..3`, y estos (`kafka-broker-api-versions`) bloquean a `kafka-init` y `kafka-ui`.
  - `rabbitmq` valida salud con `rabbitmq-diagnostics -q ping`.
- **Variables sobreescribibles**: `KAFKA_HOST`, `RABBITMQ_HOST`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `KAFKA_BROKER_HEAP` (`256M` por defecto), `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS`.
- **Seguridad**: PostgreSQL enlazado únicamente a `127.0.0.1:5432`; creación automática de tópicos deshabilitada en Kafka (`KAFKA_AUTO_CREATE_TOPICS_ENABLE="false"`).

---

## 🚀 Comandos Principales

Ejecutar desde la raíz del repositorio (`SmartRent/`):

```bash
# Levantar cada capa
docker compose -f infra/compose.mq.yml up -d
docker compose -f infra/compose.kafka.yml up -d
docker compose -f infra/compose.apps.yml up -d --build
docker compose -f infra/compose.frontend.yml up -d --build

# Re-ejecutar inicialización de tópicos Kafka (idempotente)
docker compose -f infra/compose.kafka.yml run --rm kafka-init
```

### Operación CLI del Clúster Kafka (`manage-kafka.sh`)
```bash
chmod +x infra/kafka-scripts/init-topics.sh infra/kafka-scripts/manage-kafka.sh

# Vista consolidada: tópicos, líderes, ISR, consumer groups, offsets y lag
./infra/kafka-scripts/manage-kafka.sh monitor

# Inspección puntual
./infra/kafka-scripts/manage-kafka.sh cluster
./infra/kafka-scripts/manage-kafka.sh describe-topic rentals.events
./infra/kafka-scripts/manage-kafka.sh describe-topic catalog.events
./infra/kafka-scripts/manage-kafka.sh describe-group audit-group
./infra/kafka-scripts/manage-kafka.sh describe-group report-group

# Gestión de tópicos y pruebas end-to-end
./infra/kafka-scripts/manage-kafka.sh create-topic demo.events 3 3 604800000 delete
./infra/kafka-scripts/manage-kafka.sh alter-config demo.events retention.ms=86400000
./infra/kafka-scripts/manage-kafka.sh alter-partitions demo.events 6
./infra/kafka-scripts/manage-kafka.sh produce-test rentals.events 99 '{"eventType":"RENTAL_CREATED","rentalId":99,"machineId":1,"machineName":"Excavadora","status":"SOLICITADO"}'
./infra/kafka-scripts/manage-kafka.sh consume-test rentals.events 5
./infra/kafka-scripts/manage-kafka.sh delete-topic demo.events
```

Para la guía completa de administración en **AWS Learner Lab**, consulta [`COMANDOS_AWS.md`](../COMANDOS_AWS.md).
