# 🏗️ SmartRent — Microservicio de Catálogo (`ms-smartrent-catalog`)

Microservicio responsable de la gestión del inventario de maquinarias (`Machine`) y categorías (`Category`) en PostgreSQL, control de disponibilidad y publicación de eventos de dominio hacia **Apache Kafka (`catalog.events`)**.

---

## 🛠️ Stack Tecnológico

- **Java**: `17` | **Spring Boot**: `3.3.2`
- **Persistencia**: Spring Data JPA + PostgreSQL (`machines`, `categories`)
- **Seguridad**: OAuth2 Resource Server JWT (Entra External ID CIAM, `iss` + `aud` + `@EnableMethodSecurity`)
- **Mensajería**: Spring Kafka (productor idempotente `acks=all`, compresión `gzip`, `enable.idempotence=true`)
- **Pruebas**: JUnit 5, Mockito, JaCoCo `0.8.14`

---

## 📡 Endpoints REST (`/api/catalog/services`)

Definidos en [`CatalogController.java`](./src/main/java/cl/duoc/mssmartrentcatalog/controllers/CatalogController.java):

| Método | Ruta | Autorización | Descripción |
| :---: | :--- | :--- | :--- |
| `GET` | `/api/catalog/services` | Visitante anónimo o `Arrendatario` *(bloqueado a roles internos)* | Lista maquinarias disponibles (`isAvailable = true`). |
| `GET` | `/api/catalog/services/all` | `ROLE_ADMIN`, `ROLE_JEFEBODEGA` | Lista el catálogo completo (disponibles, arrendadas y en mantención) para paneles internos. |
| `GET` | `/api/catalog/services/{id}` | Autenticado / interno | Obtiene detalle de una maquinaria por ID (`404` si no existe). |
| `POST` | `/api/catalog/services` | `ROLE_ADMIN`, `ROLE_JEFEBODEGA` | Crea una nueva maquinaria (o actualiza si coincide `serialNumber`, resolviendo/creando categoría) y publica evento Kafka. |
| `PUT` | `/api/catalog/services/{id}` | `ROLE_ADMIN`, `ROLE_JEFEBODEGA` | Actualiza datos, tarifa, categoría o disponibilidad de una maquinaria. |
| `PUT` | `/api/catalog/services/{id}/availability` | `ROLE_ADMIN`, `ROLE_JEFEBODEGA` | Cambia `isAvailable` (consumido también vía OpenFeign por `ms-smartrent-rentals`). |
| `DELETE` | `/api/catalog/services/{id}` | `ROLE_ADMIN`, `ROLE_JEFEBODEGA` | Elimina la maquinaria (`204 No Content`) y publica evento `MACHINE_DELETED`. |

---

## ⚡ Eventos Apache Kafka (`catalog.events`)

Configurado en [`KafkaProducerConfig.java`](./src/main/java/cl/duoc/mssmartrentcatalog/config/KafkaProducerConfig.java) y emitido por [`CatalogEventPublisher.java`](./src/main/java/cl/duoc/mssmartrentcatalog/events/CatalogEventPublisher.java):

- **Tópico**: `catalog.events` (`3` particiones, `RF=3`, `min.insync.replicas=2`, `cleanup.policy=compact,delete`, retención `14 días`).
- **DLT asociado**: `catalog.events-dlt` (retención `30 días`).
- **Clave de partición**: `String.valueOf(machineId)` (garantiza orden por equipo y compactación por clave).
- **Tipos de evento (`CatalogEventDto`)**:
  - `MACHINE_CREATED`
  - `MACHINE_UPDATED`
  - `MACHINE_AVAILABILITY_CHANGED`
  - `MACHINE_DELETED`

---

## ⚙️ Variables de Entorno y Propiedades

Archivo [`application.properties`](./src/main/resources/application.properties) y [`additional-spring-configuration-metadata.json`](./src/main/resources/META-INF/additional-spring-configuration-metadata.json):

| Variable / Propiedad | Valor por Defecto | Descripción |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8081` | Puerto HTTP del servicio |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/smartrent_audit` | Conexión PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | `smartrent` / `password` | Credenciales DB |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092,localhost:9093,localhost:9094` | Brokers Kafka HA |
| `smartrent.kafka.topics.catalog` | `catalog.events` | Nombre del tópico de catálogo |
| `smartrent.kafka.topics.catalog-cleanup-policy` | `compact,delete` | Política de limpieza del tópico |
| `smartrent.kafka.topics.catalog-retention-ms` | `1209600000` (14 días) | Retención del tópico |

---

## 💻 Ejecución Local, Docker y Pruebas

```bash
# Ejecutar localmente
./mvnw spring-boot:run

# Ejecutar pruebas unitarias + reporte JaCoCo
./mvnw test -B

# Construir imagen Docker
docker build -t smartrent-catalog .
```
