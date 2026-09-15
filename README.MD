# SmartRent - Sistema de Arriendo de Maquinaria

Repositorio principal para el sistema **SmartRent**, desarrollado como parte de la Evaluación 1 de Desarrollo en Cloud Native. 

El proyecto consta de una arquitectura moderna basada en la nube, separando el Frontend (Angular SPA) del Backend (Spring Boot BFF) e integrando seguridad mediante **Microsoft Entra ID (Azure AD)**.

## Arquitectura

- **Frontend (`frontend-smartrent`):** Aplicación Angular que utiliza MSAL para la autenticación delegada contra Azure AD. 
- **Backend BFF (`ms-smartrent-bff`):** Microservicio en Spring Boot 3 + Java 21 que actúa como *Resource Server* validando tokens JWT.

---

## 🛠 Backend (ms-smartrent-bff)

El Backend for Frontend (BFF) está construido con **Spring Boot** y **Spring Security**. Su responsabilidad principal es interceptar las peticiones del frontend, verificar la validez criptográfica del Access Token de Azure AD y aplicar reglas de autorización basadas en **Scopes** y **Roles**.

### Características Técnicas

*   **Java 21** y **Spring Boot 3.4+**
*   **OAuth2 Resource Server:** Validación de JWT asimétrica mediante JWKS (JSON Web Key Set) obtenidos directamente del Tenant de Azure AD.
*   **Conversión de Claims:** Se implementó un `JwtAuthenticationConverter` personalizado para extraer el claim `roles` de Azure y convertirlo en `GrantedAuthority` reconocibles por Spring (`ROLE_Admin`, `ROLE_JefeBodega`, etc.).
*   **Defensa en Profundidad:** Rutas protegidas a nivel de método con `@PreAuthorize`.

### Variables de Entorno y Configuración (`application.properties`)

El backend requiere conocer a quién debe confiarle la emisión de tokens. Esto se configura con los IDs generados en Azure:

*   **Tenant ID:** `753ae9df-d5ec-4c6b-8227-b334fc775087`
*   **Client ID (API):** `749dc676-d557-460a-b138-dac9a6744b6e`

### Endpoints Base y Reglas de Acceso (Matriz)

*   `GET /api/rentals`: Requiere el permiso (scope) delegado `Arriendos.Leer`.
*   `PUT /api/rentals/{id}/status`: Requiere el permiso (scope) `Arriendos.Escribir` **Y** tener asignado el rol de `Admin` o `JefeBodega`.
*   `GET /api/reportes/kpis`: Exclusivo para el rol `Admin`.

### Ejecución Local

Para ejecutar el backend en entorno de desarrollo:
```bash
cd ms-smartrent-bff
./mvnw spring-boot:run
```
El servidor se iniciará en `http://localhost:8080`. Se espera recibir un token portador (Bearer) en la cabecera `Authorization` para acceder a las rutas de `/api/*`.
