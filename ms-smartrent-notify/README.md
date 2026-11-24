# 📩 SmartRent — Microservicio de Notificaciones (`ms-smartrent-notify`)

Worker asíncrono basado en **RabbitMQ** que declara la topología central de notificaciones y procesa comandos de envío de correos electrónicos y generación de tickets de alistamiento (*picking*) en bodega, incorporando reintentos exponenciales y **Dead Letter Queue (DLQ)**.

---

## 🛠️ Stack Tecnológico

- **Java**: `17` | **Spring Boot**: `3.3.2`
- **Mensajería**: Spring AMQP / RabbitMQ (reintentos `max-attempts: 3`, backoff `1s` → `x2.0` → máx `10s`, `missing-queues-fatal: false`)
- **Pruebas**: JUnit 5, Mockito, JaCoCo `0.8.14`

---

## 🐇 Topología RabbitMQ Declarada (`RabbitMQConfig`)

Definida en [`RabbitMQConfig.java`](./src/main/java/com/smartrent/notify/config/RabbitMQConfig.java):

### Colas (Durables)
| Cola | Dead Letter Queue | Propósito |
| :--- | :--- | :--- |
| `q.cmd.email` | `q.cmd.email.dlq` (`x-dead-letter-exchange: ""`) | Notificaciones de correo por aprobación, devolución, cancelación, checkout y órdenes creadas. |
| `q.cmd.prep` | — | Generación de ticket de alistamiento (*picking*) en bodega. |
| `q.cmd.email.dlq` | — | Contingencia para mensajes fallidos tras agotar reintentos en `q.cmd.email`. |

### Exchanges y Bindings (`DirectExchange`, durable)
| Exchange | Routing Key | Cola Destino | Productor Principal |
| :--- | :--- | :--- | :--- |
| `x.notify` | `email` | `q.cmd.email` | Notificaciones genéricas |
| `x.notify` | `prep` | `q.cmd.prep` | Comandos de alistamiento |
| `cmd.direct` | `email.send` | `q.cmd.email` | `ms-smartrent-rentals` |
| `smartrent.exchange` | `order.created` | `q.cmd.email` | `ms-smartrent-checkout` |
| `smartrent.exchange` | `smartrent.routing.checkout` | `q.cmd.email` | `ms-smartrent-cart` |

### Listeners ([`NotificationListener.java`](./src/main/java/com/smartrent/notify/listener/NotificationListener.java))
- `processEmail(String message)` escucha `q.cmd.email`.
- `processPrep(String message)` escucha `q.cmd.prep`.

---

## ⚙️ Variables de Entorno

Archivo [`application.yml`](./src/main/resources/application.yml):

| Variable / Propiedad | Valor por Defecto | Descripción |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8087` | Puerto interno (evita colisión con `cart :8085`) |
| `SPRING_RABBITMQ_HOST` | `localhost` | Host del broker RabbitMQ (`172.31.29.239` en AWS) |
| `spring.rabbitmq.port` | `5672` | Puerto AMQP |

---

## 💻 Ejecución Local, Docker y Pruebas

```bash
./mvnw spring-boot:run
./mvnw test -B
docker build -t smartrent-notify .
```
