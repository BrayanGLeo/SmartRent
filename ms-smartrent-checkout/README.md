# 💳 SmartRent — Microservicio de Checkout (`ms-smartrent-checkout`)

Microservicio responsable de procesar órdenes de arriendo o cotización (`Order` / `OrderItem`), persistirlas en PostgreSQL de forma transaccional y notificar el evento `order.created` en **RabbitMQ** para disparar la creación automática de arriendos (`ms-smartrent-rentals`) y el envío de correos (`ms-smartrent-notify`).

---

## 🛠️ Stack Tecnológico

- **Java**: `17` | **Spring Boot**: `3.3.2`
- **Persistencia**: Spring Data JPA + PostgreSQL (`orders`, `order_items`)
- **Mensajería**: Spring AMQP / RabbitMQ (`Jackson2JsonMessageConverter` + reintentos exponenciales)
- **Seguridad**: OAuth2 Resource Server JWT (Entra External ID CIAM, `@EnableMethodSecurity`, titular derivado del JWT)
- **Pruebas**: JUnit 5, Mockito, JaCoCo `0.8.14`

---

## 📡 Endpoints REST (`/api/checkout`)

Definido en [`CheckoutController.java`](./src/main/java/cl/duoc/mssmartrentcheckout/controller/CheckoutController.java):

| Método | Ruta | Autorización | Descripción |
| :---: | :--- | :--- | :--- |
| `POST` | `/api/checkout/process` | `ROLE_ARRENDATARIO` (`Arrendatario`, `Cliente`), `ROLE_ADMIN` (`Administrador`) | Valida que la orden contenga al menos 1 ítem, extrae `userId` desde `oid`/`sub` del JWT, persiste la orden y publica `OrderCreatedEvent`. Devuelve `orderId`, `status` y `message` (`400` si la solicitud es inválida). |

### Estructura del Payload (`CheckoutRequest`)
Incluye `type` (`EXPRESS` / `QUOTE`), `customerType` (`COMPANY` / `PERSON`), `rut`, `name`, `phone`, `deliveryType` (`DISPATCH` / `PICKUP`), `deliveryAddress`, `paymentMethod` e `items` (`machineId`, `machineName`, `dailyPrice`).

---

## 📨 Mensajería RabbitMQ

Configurado en [`RabbitMQConfig.java`](./src/main/java/cl/duoc/mssmartrentcheckout/config/RabbitMQConfig.java):

- **Exchange**: `smartrent.exchange` (`DirectExchange`, durable).
- **Routing key**: `order.created`.
- **Consumidores aguas abajo**:
  - `q.rentals.order.created` → `ms-smartrent-rentals` (`OrderCreatedListener`) crea los arriendos `SOLICITADO`.
  - `q.cmd.email` → `ms-smartrent-notify` (`NotificationListener`) despacha correo de confirmación.

---

## ⚙️ Variables de Entorno

| Variable / Propiedad | Valor por Defecto | Descripción |
| :--- | :--- | :--- |
| `server.port` | `8086` | Puerto HTTP del servicio |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/smartrent_audit` | Base de datos PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | `smartrent` / `password` | Credenciales DB |
| `SPRING_RABBITMQ_HOST` | `localhost` | Host de RabbitMQ (`172.31.29.239` en AWS) |

---

## 💻 Ejecución Local, Docker y Pruebas

```bash
./mvnw spring-boot:run
./mvnw test -B
docker build -t smartrent-checkout .
```
