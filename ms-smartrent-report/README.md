# 📊 SmartRent — Microservicio de Reportes y KPIs (`ms-smartrent-report`)

Microservicio consumidor de eventos de **Apache Kafka** que calcula y expone en tiempo real indicadores operacionales (KPIs diarios, ranking de maquinarias más solicitadas, distribución por estado y actividad del catálogo).

---

## 🛠️ Stack Tecnológico

- **Java**: `17` | **Spring Boot**: `3.3.2`
- **Mensajería**: Spring Kafka (consumidores concurrentes + `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`)
- **Seguridad**: OAuth2 Resource Server JWT (Entra External ID CIAM, `iss` + `aud` + `@EnableMethodSecurity`)
- **Estado**: estructuras concurrentes en memoria (`AtomicInteger`, `ConcurrentHashMap`, `ConcurrentLinkedDeque`)
- **Pruebas**: JUnit 5, Mockito, JaCoCo `0.8.14`

---

## 📡 Endpoints REST (`/api/reports`)

Definidos en [`ReportController.java`](./src/main/java/com/smartrent/report/controller/ReportController.java) — protegidos con `@PreAuthorize` para `ROLE_ADMIN` (`Administrador`), `ROLE_JEFEBODEGA` (`JefeBodega`) y `ROLE_AUDITOR` (`Auditor`):

| Método | Ruta | Descripción |
| :---: | :--- | :--- |
| `GET` | `/api/reports/kpis` | Devuelve `dailyRentals`, serie `rentalsPerDay`, `labels`, `rentalsByStatus` y `catalogSummary`. |
| `GET` | `/api/reports/top-services` | Ranking Top 5 de maquinarias con mayor demanda (`List<Map.Entry<String, Integer>>`). |
| `GET` | `/api/reports/catalog-activity` | Resumen de actividad del catálogo: `totalEvents`, `eventsByType`, `availableMachinesCount`, `trackedMachines` y `recentEvents` (últimos 20). |

---

## ⚡ Consumo Apache Kafka y Resiliencia

Configurado en [`KafkaConfig.java`](./src/main/java/com/smartrent/report/config/KafkaConfig.java):

- **Listeners**:
  - [`ReportKafkaListener.java`](./src/main/java/com/smartrent/report/listener/ReportKafkaListener.java): consume `rentals.events` (`report-group`) y monitorea `rentals.events-dlt` (`report-group-dlt`).
  - [`CatalogReportKafkaListener.java`](./src/main/java/com/smartrent/report/listener/CatalogReportKafkaListener.java): consume `catalog.events` (`report-group`) y monitorea `catalog.events-dlt` (`report-group-dlt`).
- **Parámetros de resiliencia**:
  - `concurrency = 3` (1 consumidor por partición), `enable.auto.commit = false`, `AckMode.RECORD`, `auto-offset-reset = earliest`, `max-poll-records = 50`.
  - `ErrorHandlingDeserializer` envolviendo `StringDeserializer` / `JsonDeserializer` (`USE_TYPE_INFO_HEADERS = false`).
  - `DefaultErrorHandler` con `ExponentialBackOffWithMaxRetries(2)` (`1s`, multiplicador `2.0`, máx `5s`) y `DeadLetterPublishingRecoverer` hacia `<topic>-dlt` preservando partición.
  - Excepciones no reintentables: `IllegalArgumentException`, `DeserializationException`.

---

## ⚙️ Variables de Entorno

Archivo [`application.yml`](./src/main/resources/application.yml) y [`additional-spring-configuration-metadata.json`](./src/main/resources/META-INF/additional-spring-configuration-metadata.json):

| Variable / Propiedad | Valor por Defecto | Descripción |
| :--- | :--- | :--- |
| `server.port` | `8083` | Puerto HTTP del servicio |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092,localhost:9093,localhost:9094` | Brokers Kafka HA |
| `smartrent.kafka.consumer.group-id` | `report-group` | Consumer group principal |
| `smartrent.kafka.consumer.dlt-group-id` | `report-group-dlt` | Consumer group para DLTs |
| `smartrent.kafka.consumer.concurrency` | `3` | Hilos concurrentes por listener |

---

## 💻 Ejecución Local, Docker y Pruebas

```bash
./mvnw spring-boot:run
./mvnw test -B
docker build -t smartrent-report .
```
