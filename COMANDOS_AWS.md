# 🛠️ Guía Rápida de Comandos para AWS EC2 y Docker (SmartRent)

Esta guía contiene los comandos esenciales para administrar, monitorear y solucionar problemas en las instancias EC2 de **AWS Learner Lab**.

---

## 📊 1. Diagnóstico de Recursos del Sistema

Antes de levantar o compilar servicios, siempre es recomendable revisar memoria y espacio.

### Verificar RAM y Disco en un solo comando:
```bash
free -h && echo "--- Espacio en Disco ---" && df -h /
```

### Comandos individuales:
* **Memoria RAM disponible:**
  ```bash
  free -h
  ```
  *(Revisar la columna `available`, que indica la memoria real que el sistema puede asignar).*

* **Espacio en disco restante:**
  ```bash
  df -h /
  ```
  *(Revisar la columna `Avail` y que `Use%` no llegue al 100%).*

* **Monitoreo en tiempo real (CPU / RAM):**
  ```bash
  htop
  ```
  *(o `top` si `htop` no está instalado. Salir con `q`).*

---

## 🧹 2. Solución a "No space left on device" (Liberar Espacio)

Cuando las instancias de AWS tienen discos de 8 a 20 GB, Docker puede llenarlos rápidamente con capas de compilación y dependencias de Maven.

Ejecuta estos comandos en orden para liberar varios gigabytes sin perder datos de la base de datos:

```bash
# 1. Eliminar caché de compilación de Docker (suele liberar de 4 a 10 GB)
docker builder prune -a -f

# 2. Eliminar imágenes huérfanas y contenedores detenidos
docker system prune -f

# 3. Limpiar caché de paquetes de Ubuntu/Linux
sudo apt-get clean

# 4. Truncar logs acumulados del sistema operativo a máximo 50MB
sudo journalctl --vacuum-size=50M

# 5. Comprobar cuánto espacio se recuperó
df -h /
```

---

## 🐳 3. Servicio y Daemon de Docker

Asegúrate de que Docker esté corriendo y configurado para iniciar automáticamente cuando se encienda la instancia EC2:

```bash
# Ver estado del servicio
sudo systemctl status docker

# Iniciar Docker si está detenido
sudo systemctl start docker

# Habilitar Docker para que inicie automáticamente al bootear la máquina
sudo systemctl enable docker
```

---

## 🚀 4. Despliegue con Docker Compose

> ⚠️ **Atención a la ruta:** Si estás en `~/SmartRent`, incluye la carpeta `infra/`. Si ya estás dentro de `~/SmartRent/infra`, no dupliques la ruta.

### Desde la raíz del repositorio (`~/SmartRent`):
```bash
# Levantar aplicaciones (Microservicios + PostgreSQL)
docker compose -f infra/compose.apps.yml up -d

# Levantar Cluster Kafka HA (3 Zookeeper + 3 Brokers + Kafka Init + Kafka UI)
docker compose -f infra/compose.kafka.yml up -d

# Levantar RabbitMQ
docker compose -f infra/compose.mq.yml up -d
```

### Si realizaste cambios en el código (Recompilar y levantar):
```bash
# Recompilar TODO el compose:
docker compose -f infra/compose.apps.yml up -d --build

# O recompilar los servicios modificados (BFF + microservicios):
docker compose -f infra/compose.apps.yml up -d --build smartrent-bff smartrent-report smartrent-audit smartrent-cart smartrent-checkout
```


### Monitoreo de contenedores:
```bash
# Listar contenedores corriendo
docker ps

# Ver logs de todos los servicios en vivo
docker compose -f infra/compose.apps.yml logs -f

# Ver logs de un contenedor específico (ejemplo: BFF o Catalog)
docker logs smartrent-bff --tail 100 -f
docker logs smartrent-catalog --tail 100 -f

# Detener todos los contenedores de un compose
docker compose -f infra/compose.apps.yml down
```

---

## 🐘 5. Base de Datos PostgreSQL (`smartrent-postgres`)

### Consultar registros (Maquinarias y Categorías):
```bash
# Conteo rápido de ambas tablas
docker exec -it smartrent-postgres psql -U smartrent -d smartrent_audit -c "SELECT 'machines' AS tabla, count(*) FROM machines UNION ALL SELECT 'categories', count(*) FROM categories;"

# Ver las maquinarias registradas
docker exec -it smartrent-postgres psql -U smartrent -d smartrent_audit -c "SELECT id, name, serial_number, daily_price, is_available FROM machines;"
```

### Poblar la base de datos (Seed Data inicial):
Si la base de datos está vacía, inserta los datos de prueba desde la raíz del proyecto:
```bash
docker exec -i smartrent-postgres psql -U smartrent -d smartrent_audit < ~/SmartRent/aws_seed_data.sql
```

### Entrar a la consola interactiva de Postgres (`psql`):
```bash
docker exec -it smartrent-postgres psql -U smartrent -d smartrent_audit
```
* **Comandos útiles dentro de `psql`:**
  * `\dt` : Lista todas las tablas.
  * `\d machines` : Muestra las columnas y estructura de la tabla `machines`.
  * `SELECT * FROM machines;` : Muestra todos los registros.
  * `\q` : Salir de la consola interactiva.

---

## 💡 6. Buenas Prácticas en AWS Learner Lab

1. **Autoarranque de contenedores:**  
   Todos los archivos compose (`compose.apps.yml`, `compose.kafka.yml`, `compose.mq.yml`) tienen configurado `restart: unless-stopped`.  
   * **Tip:** Al terminar tu sesión de estudio, **no** ejecutes `docker compose down`. Deja los contenedores corriendo y simplemente apaga la instancia en la consola de AWS. Al volver a encenderla en el siguiente laboratorio, Docker levantará todo automáticamente.

2. **Cuidado con las IPs dinámicas (IPs Privadas Fijas de la VPC):**  
   En AWS Learner Lab, la **IP Pública** de la instancia cambia cada vez que se detiene e inicia, pero las **IPs Privadas (Private IPv4)** dentro de la misma VPC **no cambian**. Los archivos `infra/compose.apps.yml` e `infra/compose.kafka.yml` ya vienen preconfigurados por defecto con estas IPs privadas para autoarranque sin intervención manual:

   | Instancia EC2 | Instance ID | Rol / Compose | IP Privada Fija (VPC) | IP Elástica / URL Pública |
   | :--- | :--- | :--- | :---: | :--- |
   | **EC2-Apps** | `i-0f64ef75cc7bcb761` | Microservicios + BFF + PostgreSQL (`infra/compose.apps.yml`) | `172.31.76.200` | Backend API: `https://hnws3rgb6i.execute-api.us-east-1.amazonaws.com/v2/api` |
   | **EC2-MQ** | `i-0965eaa82b5d4db5f` | RabbitMQ (`infra/compose.mq.yml`) | `172.31.29.239` | `http://100.50.92.218:15672` (EIP `100.50.92.218`, `guest`/`guest`) |
   | **EC2-Kafka** | `i-0c90ee30cdfc784a7` | Apache Kafka HA 3×3 + UI (`infra/compose.kafka.yml`) | `172.31.30.107` | `http://3.231.182.87:8090` (EIP `3.231.182.87`) |
   | **EC2-Frontend** | `i-0e0c3536edfd0b413` | Angular 22 SPA en Nginx (`deploy-ec2.sh` / `compose.frontend.yml`) | `172.31.27.98` | `https://tv9aux1za4.execute-api.us-east-1.amazonaws.com` (EIP `34.225.219.131`) |

   *(Nota: Si alguna vez recreas una instancia y cambia su IP privada, puedes sobreescribirla con `RABBITMQ_HOST` y `KAFKA_HOST` o actualizar los valores por defecto en `infra/`).*


---

## 🌐 7. Despliegue del Frontend (`frontend-smartrent`) en EC2

Puedes desplegar el Frontend en una instancia EC2 de dos formas (ambas ya están automatizadas en el repositorio):

### Opción A: Instalación directa en Ubuntu con un solo comando (Recomendado)
```bash
cd ~/SmartRent/frontend-smartrent
sed -i 's/\r$//' deploy-ec2.sh
chmod +x deploy-ec2.sh
./deploy-ec2.sh
```
*(Este script crea automáticamente 2 GB de Swap, instala Node.js 22 y Nginx, compila Angular en modo producción y publica el sitio en el puerto 80).*

### Opción B: Con Docker Compose
```bash
cd ~/SmartRent
docker compose -f infra/compose.frontend.yml up -d --build
```

---

## ⚡ 8. Operación y Monitoreo del Clúster Apache Kafka (HA 3 Brokers)

### Puertos requeridos en el Security Group de `EC2-Kafka` (`172.31.30.107`):
- **TCP 9092, 9093, 9094**: Origen `EC2-Apps` (`172.31.76.200/32` o Security Group de Apps) para conexión a los 3 brokers (`kafka-1`, `kafka-2`, `kafka-3`).
- **TCP 8090**: Origen administrador/docente para acceder a **Kafka UI** (`http://<IP_PUBLICA_KAFKA>:8090`).

### Administración replicable por CLI (`infra/kafka-scripts/`):
```bash
cd ~/SmartRent
chmod +x infra/kafka-scripts/init-topics.sh infra/kafka-scripts/manage-kafka.sh

# Verificar o recrear los 2 tópicos oficiales (3 particiones, 3 réplicas, min.insync.replicas=2) y DLTs
docker compose -f infra/compose.kafka.yml run --rm kafka-init

# Monitoreo consolidado (brokers, tópicos, líderes, ISR, consumer groups, offsets y lag)
./infra/kafka-scripts/manage-kafka.sh monitor

# Inspeccionar un tópico o un consumer group específico
./infra/kafka-scripts/manage-kafka.sh describe-topic rentals.events
./infra/kafka-scripts/manage-kafka.sh describe-topic catalog.events
./infra/kafka-scripts/manage-kafka.sh describe-group audit-group
./infra/kafka-scripts/manage-kafka.sh describe-group report-group

# Crear, alterar configuración, ampliar particiones o eliminar tópicos
./infra/kafka-scripts/manage-kafka.sh create-topic demo.events 3 3 604800000 delete
./infra/kafka-scripts/manage-kafka.sh alter-config demo.events retention.ms=86400000
./infra/kafka-scripts/manage-kafka.sh alter-partitions demo.events 6
./infra/kafka-scripts/manage-kafka.sh delete-topic demo.events

# Prueba en vivo de producción y consumo
./infra/kafka-scripts/manage-kafka.sh produce-test rentals.events 99 '{"eventType":"RENTAL_CREATED","rentalId":99,"machineId":1,"machineName":"Excavadora","status":"SOLICITADO"}'
./infra/kafka-scripts/manage-kafka.sh consume-test rentals.events 5
```

### API REST de Administración Kafka en BFF (Rol `Administrador`):
- `GET /v2/api/kafka/cluster` — Estado del clúster, controller y los 3 brokers.
- `GET /v2/api/kafka/topics` — Listado de tópicos, particiones, réplicas, ISR y políticas.
- `GET /v2/api/kafka/topics/{name}` — Detalle por partición (líder, ISR, earliest/latest offsets).
- `POST /v2/api/kafka/topics` — Creación declarativa de tópicos.
- `PUT /v2/api/kafka/topics/{name}/config` — Actualización de retención, `cleanup.policy` y particiones.
- `DELETE /v2/api/kafka/topics/{name}` — Eliminación de tópicos no internos.
- `GET /v2/api/kafka/consumer-groups` y `GET /v2/api/kafka/consumer-groups/{groupId}` — Supervisión de miembros, offsets y **lag**.

### Nota PostgreSQL (`ms-smartrent-audit`):
Si la tabla `audit_timeline` ya existía en PostgreSQL de despliegues previos con `event_payload` como `VARCHAR(255)`, amplía el tipo a `TEXT` antes o durante el despliegue:
```bash
docker exec -i smartrent-postgres psql -U smartrent -d smartrent_audit -c "ALTER TABLE audit_timeline ALTER COLUMN event_payload TYPE TEXT;"
```
