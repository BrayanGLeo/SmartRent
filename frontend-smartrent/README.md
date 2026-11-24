# 🌐 SmartRent — Frontend Application (Angular 22 SPA)

Aplicación cliente *Single Page Application* (SPA) de **SmartRent**, diseñada con componentes *standalone*, Angular Signals, autenticación **Microsoft Entra External ID (MSAL)**, interceptores HTTP centralizados y paneles diferenciados por rol RBAC (incluyendo consolas en vivo para **RabbitMQ** y **Apache Kafka HA**).

---

## 🛠️ Stack Tecnológico

| Tecnología | Versión | Rol |
| :--- | :---: | :--- |
| **Angular** (`@angular/*`) | `22.2.4` (CLI `22.2.6`) | Framework SPA (*standalone components*, *lazy loading*, Signals) |
| **TypeScript** | `~6.0.2` | Tipado estático |
| **MSAL Angular / Browser** | `6.2.0` / `5.21.0` | SSO OIDC/PKCE contra Microsoft Entra External ID (CIAM) |
| **RxJS** | `~7.8.0` | Flujos reactivos HTTP y eventos MSAL |
| **Chart.js / ng2-charts** | `4.5.1` / `10.0.0` | Gráficos de KPIs y reportería |
| **Vitest + JSDOM + Coverage V8** | `4.0.8` / `28.0.0` / `4.1.12` | Pruebas unitarias y cobertura |
| **Nginx** | `alpine` / Ubuntu | Servidor de estáticos SPA con cabeceras de seguridad y Gzip |

---

## 🗺️ Rutas, Módulos y Control de Acceso (RBAC)

Definidas en [`src/app/app.routes.ts`](./src/app/app.routes.ts):

| Ruta | Componente | Guards | Acceso Permitido |
| :--- | :--- | :--- | :--- |
| `/` | `Home` | Público | Todos |
| `/about` | `About` *(lazy)* | Público | Todos |
| `/catalog` | `Catalog` *(lazy)* | `catalogGuard` | Visitantes anónimos y `Arrendatario` *(roles internos redirigidos a sus paneles)* |
| `/rentals` | `Rentals` *(lazy)* | `MsalGuard`, `roleGuard` | `Arrendatario`, `JefeBodega` |
| `/cart` | `Cart` *(lazy)* | `MsalGuard`, `roleGuard` | `Arrendatario` |
| `/checkout` | `CheckoutComponent` *(lazy)* | `MsalGuard`, `roleGuard` | `Arrendatario` |
| `/reports` | `Reports` *(lazy)* | `MsalGuard`, `roleGuard` | `Administrador` |
| `/audit` | `Audit` *(lazy)* | `MsalGuard`, `roleGuard` | `Auditor` |
| `/admin` | `AdminPanel` *(lazy)* | `MsalGuard`, `roleGuard` | `Administrador` |

### Pestañas del Panel de Administración (`/admin`)
1. **Usuarios y Tenants (`users`)**: sincronización del directorio federado desde Entra ID (`/users` vía BFF + Microsoft Graph).
2. **Inventario y Stock (`inventory`)**: CRUD completo de maquinarias conectado a `/catalog/services/all` y `/catalog/services/{id}`.
3. **Validación de Bodega (`validation`)**: checklist de inspección, horómetros y cambio de estados de arriendos.
4. **KPIs y Métricas (`kpis`)**: resumen de ocupación, arriendos diarios y top maquinarias (`/reports/kpis`, `/reports/top-services`).
5. **Topología RabbitMQ (`rabbitmq`)**: gestión de exchanges, queues y bindings vía `/queues`, `/exchanges` y `/bindings`.
6. **Clúster Apache Kafka (`kafka`)**: monitoreo de los 3 brokers y controller, creación/edición/eliminación/inspección de tópicos (particiones, líderes, ISR, offsets, políticas de retención) y supervisión de consumer groups con cálculo de **lag** (`/kafka/**`).

---

## 🔐 Autenticación, Interceptores y Entorno

Configurados en [`src/app/app.config.ts`](./src/app/app.config.ts) y [`src/environments/environment.ts`](./src/environments/environment.ts):

- **Cadena HTTP unificada (`HttpClient`)**:
  1. `errorInterceptor`: manejo centralizado de errores HTTP y modal de acceso denegado (`AccessDeniedService`).
  2. `loadingInterceptor`: estado global de carga (`LoadingService`).
  3. `MsalInterceptor` (`strictMatching: true`): adjunta automáticamente `Authorization: Bearer <JWT>` con el scope `api://749dc676-d557-460a-b138-dac9a6744b6e/Arriendos.Leer`.
- **Mapa de recursos protegidos (`protectedResourceMap`)**:
  - Protegidos en todos los métodos: `/rentals*`, `/reports*`, `/audit*`, `/users*`, `/cart*`, `/checkout*`, `/queues*`, `/exchanges*`, `/bindings*`, `/kafka*`, `/catalog/services/all*`.
  - `/catalog*`: `GET` sin token para catálogo público; `POST`, `PUT`, `DELETE`, `PATCH` con `Bearer JWT`.
- **Identidad CIAM y URLs de Despliegue**:
  - **URL Pública HTTPS (Frontend):** `https://tv9aux1za4.execute-api.us-east-1.amazonaws.com` (AWS HTTP API Gateway `$default` → `EC2-Frontend` IP Elástica `34.225.219.131:80`)
  - **API Gateway Backend (`apiBaseUrl`):** `https://hnws3rgb6i.execute-api.us-east-1.amazonaws.com/v2/api`
  - **Authority CIAM:** `https://753ae9df-d5ec-4c6b-8227-b334fc775087.ciamlogin.com/753ae9df-d5ec-4c6b-8227-b334fc775087/v2.0`
  - **Client ID Frontend:** `f5890cb5-fba6-429e-b886-323d05ea886c`
- **Resolución de rol (`AuthService`)**: prioriza claims explícitos del token (`roles`, `role`, `extension_Role`, `groups`) antes de cualquier heurística de respaldo.

---

## 📁 Estructura de Carpetas

```text
frontend-smartrent/
├── public/images/             # Imágenes locales del catálogo de maquinarias
├── src/
│   ├── environments/          # Configuración Azure CIAM y URL del API Gateway
│   └── app/
│       ├── core/
│       │   ├── components/    # navbar, access-denied-modal
│       │   ├── guards/        # admin.guard, catalog.guard, role.guard
│       │   ├── interceptors/  # error.interceptor, loading.interceptor
│       │   ├── models/        # audit, cart, catalog, checkout, rental, report
│       │   └── services/      # ApiService, AuthService, CartService, LoadingService, AccessDeniedService
│       ├── dashboard/         # Vista dashboard base
│       ├── features/          # about, admin, audit, cart, catalog, checkout, rentals, reports
│       └── home/              # Landing principal
├── Dockerfile                 # Build multi-stage Node 22 + Nginx endurecido
├── nginx.conf                 # Reglas SPA (try_files), Gzip y cabeceras de seguridad
└── deploy-ec2.sh              # Script idempotente de despliegue en Ubuntu EC2 (swap 2GB + Node 22 + Nginx)
```

---

## 💻 Desarrollo Local y Pruebas

### Requisitos Previos
- **Node.js**: v22 LTS (mínimo v18+)
- **NPM**: v11+

### Instalación y Ejecución
```bash
npm ci
npm start        # Servidor de desarrollo en http://localhost:4200/
```

### Pruebas Unitarias y Cobertura (Vitest)
```bash
# Ejecutar todas las suites una vez
npx ng test --watch=false

# Ejecutar con reporte de cobertura V8 (excluye **/*.html según angular.json)
npx ng test --watch=false --coverage
```

### Build de Producción
```bash
npm run build
# Salida en: dist/frontend-smartrent/browser/
```

---

## 🚀 Despliegue en AWS EC2

### Opción A: Script automatizado en Ubuntu EC2 (Recomendado)
Crea automáticamente 2 GB de swap (idempotente), instala Node 22 y Nginx, compila Angular con `--max-old-space-size=2048` y publica en `/var/www/html`:
```bash
cd ~/SmartRent/frontend-smartrent
sed -i 's/\r$//' deploy-ec2.sh
chmod +x deploy-ec2.sh
./deploy-ec2.sh
```

### Opción B: Docker Compose
```bash
cd ~/SmartRent
docker compose -f infra/compose.frontend.yml up -d --build
```

La configuración de [`nginx.conf`](./nginx.conf) incluye `server_tokens off`, `X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, compresión Gzip, `no-cache` para `/index.html` y caché de 7 días para estáticos.
