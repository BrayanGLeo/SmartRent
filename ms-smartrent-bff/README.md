# ⚙️ SmartRent — Backend for Frontend (`ms-smartrent-bff`)

Microservicio **BFF (Backend for Frontend)** y puerta de enlace interna de la plataforma **SmartRent**. Actúa como **OAuth2 Resource Server** frente a **Microsoft Entra External ID (CIAM)**, aplica políticas perimetrales de seguridad **RBAC** y **CORS**, enruta tráfico hacia los microservicios internos mediante **Spring Cloud Gateway MVC**, consulta usuarios federados en **Microsoft Graph** y expone las APIs administrativas de **RabbitMQ** y **Apache Kafka HA**.

- **URL Pública HTTPS (vía AWS HTTP API Gateway):** `https://hnws3rgb6i.execute-api.us-east-1.amazonaws.com/v2/api`
- **Prueba rápida pública:** `GET https://hnws3rgb6i.execute-api.us-east-1.amazonaws.com/v2/api/catalog/services`

---

## 🛠️ Stack Tecnológico

- **Java**: `17`
- **Spring Boot**: `3.3.2`
- **Spring Cloud Gateway MVC**: enrutamiento HTTP declarativo y limpieza de cabeceras CORS duplicadas (`GatewayConfig`)
- **Spring Security OAuth2 Resource Server**: validación criptográfica JWT (`NimbusJwtDecoder`, `iss` + `aud` + roles)
- **Spring Kafka / Kafka AdminClient**: administración directa del clúster Kafka HA de 3 brokers (`KafkaAdminService`)
- **Microsoft Graph REST API**: consulta de usuarios del tenant con caché de token OAuth2 *Client Credentials* (`AzureGraphService`)
- **Pruebas**: JUnit 5, Mockito, Spring Security Test, JaCoCo `0.8.14`

---

## 🔀 Enrutamiento Gateway MVC (`GatewayConfig`)

Todas las rutas soportan los prefijos `/api/**`, `/v1/api/**` y `/v2/api/**` (aplicando `stripPrefix(1)` en versiones `v1`/`v2` y eliminando `Access-Control-Allow-Origin` aguas abajo para evitar cabeceras duplicadas):

| Prefijo Externo | Servicio Destino Interno | Puerto |
| :--- | :--- | :---: |
| `/api/catalog/**` (y `/v1`, `/v2`) | `http://smartrent-catalog:8081` | `8081` |
| `/api/rentals/**` (y `/v1`, `/v2`) | `http://smartrent-rentals:8082` | `8082` |
| `/api/reports/**` (y `/v1`, `/v2`) | `http://smartrent-report:8083` | `8083` |
| `/api/audit/**` (y `/v1`, `/v2`) | `http://smartrent-audit:8084` | `8084` |
| `/api/cart/**` (y `/v1`, `/v2`) | `http://smartrent-cart:8085` | `8085` |
| `/api/checkout/**` (y `/v1`, `/v2`) | `http://smartrent-checkout:8086` | `8086` |

---

## 📡 Endpoints Propios del BFF

### 1. Directorio de Usuarios (`AdminUserController` — Solo `ROLE_ADMIN` / `Administrador`)
| Método | Rutas | Descripción |
| :---: | :--- | :--- |
| `GET` | `/api/users`, `/v1/api/users`, `/v2/api/users`, `/users` | Obtiene el listado de usuarios del tenant CIAM desde Microsoft Graph API (`AzureGraphService`). |

### 2. Administración de Apache Kafka (`KafkaAdminController` — Solo `ROLE_ADMIN` / `Administrador`)
Todas las rutas están disponibles con prefijo `/api`, `/v1/api` y `/v2/api`:

| Método | Ruta | Descripción |
| :---: | :--- | :--- |
| `GET` | `/api/kafka/cluster` | ID del clúster, nodo controller y lista de brokers activos (`kafka-1..3`). |
| `GET` | `/api/kafka/topics?includeInternal=false` | Lista tópicos, particiones, factor de replicación, particiones sub-replicadas y configs clave. |
| `GET` | `/api/kafka/topics/{name}` | Detalle por partición (líder, réplicas, ISR, `earliestOffset`, `latestOffset`, `messageCount`) y configuraciones completas. |
| `POST` | `/api/kafka/topics` | Crea un tópico validando nombre (`^[a-zA-Z0-9._-]+$`, prohíbe prefijo `__`). |
| `PUT` | `/api/kafka/topics/{name}/config` | Incrementa particiones (impide reducción) y altera configuraciones dinámicas (`incrementalAlterConfigs`). |
| `DELETE` | `/api/kafka/topics/{name}` | Elimina un tópico no interno. |
| `GET` | `/api/kafka/consumer-groups` | Lista consumer groups activos y su estado. |
| `GET` | `/api/kafka/consumer-groups/{groupId}` | Detalle del grupo: coordinador, miembros, particiones asignadas, offsets y **`totalLag`**. |
| `DELETE` | `/api/kafka/consumer-groups/{groupId}` | Elimina un consumer group inactivo. |

### 3. Administración de Topología RabbitMQ (`RabbitTopologyController` — Solo `ROLE_ADMIN` / `Administrador`)
Rutas disponibles bajo `/api`, `/v1/api` y `/v2/api`:
- `GET / POST /api/queues`, `DELETE /api/queues/{name}`
- `GET / POST /api/exchanges`, `DELETE /api/exchanges/{name}`
- `GET / POST / DELETE /api/bindings`

### 4. Controlador Mock Legado Aislado (`RentalsController`)
Aislado bajo `/api/mock-legacy/**` (`/rentals`, `/rentals/{id}/status`, `/reportes/kpis`) para no colisionar con las rutas productivas del Gateway MVC.

---

## 🛡️ Seguridad (`SecurityConfig`)

- **Validación JWT**: `NimbusJwtDecoder` verifica firma JWKS, `issuer` CIAM y `aud` exacto (`749dc676-d557-460a-b138-dac9a6744b6e` o `api://749dc676-d557-460a-b138-dac9a6744b6e`).
- **Reglas de acceso**:
  - `GET /api/catalog/services/all/**` y mutaciones `/api/catalog/**`: `ROLE_ADMIN`, `ROLE_JEFEBODEGA`.
  - `GET /api/catalog/**`: abierto a anónimos y `Arrendatario`; **bloqueado explícitamente** a roles internos (`ADMIN`, `JEFEBODEGA`, `AUDITOR`).
  - `/api/users/**`, `/api/admin/**`, `/api/queues/**`, `/api/exchanges/**`, `/api/bindings/**`, `/api/kafka/**`, `/api/reports/**`: solo `ROLE_ADMIN`.
  - `/api/audit/**`: `ROLE_AUDITOR`, `ROLE_ADMIN`.
  - Resto de rutas: requieren autenticación JWT válida.
- **Normalización de roles (`mapRole`)**: convierte variantes de Entra ID (`Admin`, `Administrador`, `ROLE_ADMIN`, `Auditor`, etc.) a autoridades consistentes.

---

## ⚙️ Variables de Entorno y Configuración

Archivo [`src/main/resources/application.properties`](./src/main/resources/application.properties) y metadatos [`additional-spring-configuration-metadata.json`](./src/main/resources/META-INF/additional-spring-configuration-metadata.json):

| Variable / Propiedad | Valor por Defecto | Descripción |
| :--- | :--- | :--- |
| `server.port` | `8080` | Puerto HTTP del BFF |
| `AZURE_GRAPH_CLIENT_SECRET` | *(vacío)* | Client secret para consultar usuarios en Microsoft Graph |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092,localhost:9093,localhost:9094` | Brokers Kafka del clúster HA |
| `smartrent.kafka.admin.request-timeout-ms` | `5000` | Timeout de solicitud del `AdminClient` Kafka |
| `smartrent.kafka.admin.default-api-timeout-ms` | `8000` | Timeout global API del `AdminClient` Kafka |

---

## 💻 Ejecución Local, Docker y Pruebas

```bash
# Compilar y ejecutar localmente
./mvnw spring-boot:run

# Ejecutar suite de pruebas unitarias + JaCoCo
./mvnw test -B

# Construir imagen Docker (multi-stage, usuario no-root spring:spring)
docker build -t smartrent-bff .
```
