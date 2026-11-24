# 🛒 SmartRent — Microservicio de Carrito (`ms-smartrent-cart`)

Microservicio encargado de mantener y sincronizar el carrito de compras (`Cart` / `CartItem`) de cada usuario autenticado en PostgreSQL, separando ítems por modalidad (`express` vs `quote`) y publicando eventos de inicio de checkout en **RabbitMQ**.

---

## 🛠️ Stack Tecnológico

- **Java**: `17` | **Spring Boot**: `3.3.2`
- **Persistencia**: Spring Data JPA + PostgreSQL (`carts`, `cart_items`)
- **Mensajería**: Spring AMQP / RabbitMQ (`Jackson2JsonMessageConverter` + reintentos exponenciales)
- **Seguridad**: OAuth2 Resource Server JWT (Entra External ID CIAM, `@EnableMethodSecurity`, titular derivado del JWT)
- **Pruebas**: JUnit 5, Mockito, JaCoCo `0.8.14`

---

## 📡 Endpoints REST (`/api/cart`)

Definidos en [`CartController.java`](./src/main/java/cl/duoc/mssmartrentcart/controller/CartController.java) — protegidos a nivel de clase para `ROLE_ARRENDATARIO` (`Arrendatario`, `Cliente`) y `ROLE_ADMIN` (`Administrador`). El `userId` se extrae siempre desde `@AuthenticationPrincipal Jwt` (`oid` → `sub`):

| Método | Ruta | Descripción |
| :---: | :--- | :--- |
| `GET` | `/api/cart` | Obtiene (o inicializa) el carrito persistido del usuario autenticado. |
| `POST` | `/api/cart/sync` | Sincroniza la lista de `CartItem` del cliente con PostgreSQL de forma transaccional. |
| `POST` | `/api/cart/checkout/{type}` | Filtra ítems por tipo (`express` o `quote`), los remueve y persiste en PostgreSQL **antes** de emitir `CheckoutEvent` a RabbitMQ. |

---

## 📨 Mensajería RabbitMQ

- **Exchange**: `smartrent.exchange` (`DirectExchange`, durable).
- **Routing key**: `smartrent.routing.checkout`.
- **Payload**: `CheckoutEvent` (`userId`, `checkoutType`, `items`).
- **Consistencia**: la limpieza del carrito se ejecuta bajo `@Transactional` y se guarda en base de datos antes de invocar `rabbitTemplate.convertAndSend(...)`.

---

## ⚙️ Variables de Entorno

| Variable / Propiedad | Valor por Defecto | Descripción |
| :--- | :--- | :--- |
| `server.port` | `8085` | Puerto HTTP del servicio |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/smartrent_audit` | Base de datos PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | `smartrent` / `password` | Credenciales DB |
| `SPRING_RABBITMQ_HOST` | `localhost` | Host de RabbitMQ (`172.31.29.239` en AWS) |

---

## 💻 Ejecución Local, Docker y Pruebas

```bash
./mvnw spring-boot:run
./mvnw test -B
docker build -t smartrent-cart .
```
