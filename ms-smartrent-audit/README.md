# 🔍 SmartRent — Microservicio de Auditoría (`ms-smartrent-audit`)

Microservicio de trazabilidad inmutable que consume eventos de dominio desde **Apache Kafka** (`rentals.events`, `catalog.events` y sus Dead Letter Topics asociados), los persiste en PostgreSQL (`AUDIT_TIMELINE`) y expone la línea de tiempo de auditoría para roles autorizados.

---

## 🛠️ Stack Tecnológico

- **Java**: `17` | **Spring Boot**: `3.3.2`
- **Persistencia**: Spring Data JPA + PostgreSQL (tabla `AUDIT_TIMELINE` con columna `event_payload TEXT`)
- **Mensajería**: Spring Kafka (consumidores concurrentes + `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`)
- **Seguridad**: OAuth2 Resource Server JWT (Entra External ID CIAM, `iss` + `aud` + `@EnableMethodSecurity`)
- **Pruebas**: JUnit 5, Mockito, JaCoCo `0.8.14`

---

## 📡 Endpoints REST (`/api/audit`)

Definidos en [`AuditController.java`](./src/main/java/com/smartrent/audit/controller/AuditController.java):

| Método | Rutas | Autorización | Descripción |
| :---: | :--- | :--- | :--- |
| `GET` | `/api/audit`, `/api/audit/events` | `ROLE_AUDITOR` (`Auditor`), `ROLE_ADMIN` (`Administrador`) | Devuelve todos los registros de `AuditTimeline` ordenados por `timestamp DESC`. |

---

## ⚡ Consumo Apache Kafka y Persistencia

Configurado en [`KafkaConfig.java`](./src/main/java/com/smartrent/audit/config/KafkaConfig.java) y procesado en [`AuditService.java`](./src/main/java/com/smartrent/audit/service/AuditService.java):

- **Listeners**:
  - [`AuditKafkaListener.java`](./src/main/java/com/smartrent/audit/listener/AuditKafkaListener.java): consume `rentals.events` (`audit-group`) y registra mensajes fallidos desde `rentals.events-dlt` (`audit-group-dlt`, tipo `RENTAL_EVENT_DLT`).
  - [`CatalogAuditKafkaListener.java`](./src/main/java/com/smartrent/audit/listener/CatalogAuditKafkaListener.java): consume `catalog.events` (`audit-group`) y registra mensajes fallidos desde `catalog.events-dlt` (`audit-group-dlt`, tipo `CATALOG_EVENT_DLT`).
- **Resiliencia**:
  - `concurrency = 3`, `enable.auto.commit = false`, `AckMode.RECORD`, `ErrorHandlingDeserializer`.
  - `DefaultErrorHandler` con `ExponentialBackOffWithMaxRetries(2)` (`1s`, `x2`, máx `5s`) + `DeadLetterPublishingRecoverer` hacia `<topic>-dlt`.
- **Nota de migración PostgreSQL**: si la tabla `audit_timeline` fue creada en una versión previa con `VARCHAR(255)`, ampliar la columna antes de iniciar:
  ```sql
  ALTER TABLE audit_timeline ALTER COLUMN event_payload TYPE TEXT;
  ```

---

## ⚙️ Variables de Entorno

Archivo [`application.yml`](./src/main/resources/application.yml) y [`additional-spring-configuration-metadata.json`](./src/main/resources/META-INF/additional-spring-configuration-metadata.json):

| Variable / Propiedad | Valor por Defecto | Descripción |
| :--- | :--- | :--- |
| `server.port` | `8084` | Puerto HTTP del servicio |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/smartrent_audit` | Base de datos PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | `smartrent` / `password` | Credenciales DB |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092,localhost:9093,localhost:9094` | Brokers Kafka HA |
| `smartrent.kafka.consumer.group-id` | `audit-group` | Consumer group principal |
| `smartrent.kafka.consumer.dlt-group-id` | `audit-group-dlt` | Consumer group para DLTs |

---

## 💻 Ejecución Local, Docker y Pruebas

```bash
./mvnw spring-boot:run
./mvnw test -B
docker build -t smartrent-audit .
```
