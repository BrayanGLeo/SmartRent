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

# Levantar Kafka y Zookeeper
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

2. **Cuidado con las IPs dinámicas:**  
   En AWS Learner Lab, la **IP Pública** de la instancia cambia cada vez que se detiene e inicia.  
   * Si tus instancias están en la misma VPC/Subred, utiliza la **IP Privada (Private IPv4)** en tus variables de entorno (`.env` para `RABBITMQ_HOST` y `KAFKA_HOST`). Las IPs privadas **no cambian** al apagar y encender la máquina.


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
