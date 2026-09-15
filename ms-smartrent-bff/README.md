# Backend SmartRent (Spring Boot BFF)

Este directorio contiene el backend de la aplicación, implementado con **Spring Boot 3** y **Java 21**. Su objetivo es exponer una API REST segura mediante el patrón Backend For Frontend (BFF).

## Arquitectura y Seguridad

Este componente está configurado como un **OAuth2 Resource Server**. En lugar de emitir sus propios tokens de autenticación, el BFF delega la validación de tokens a **Microsoft Entra ID (Azure AD)**.

### Características Clave
- **Validación JWT Asimétrica:** Valida automáticamente el token (Bearer) enviado por el Frontend utilizando las claves públicas (JWKS) del Tenant de Azure AD.
- **Mapeo de Roles:** Utiliza un `JwtAuthenticationConverter` para transformar el *claim* `roles` del token emitido por Azure en autoridades de Spring Security (ej. `ROLE_Admin`).
- **Control de Acceso basado en Roles (RBAC):** Protege los *endpoints* a nivel de método con anotaciones como `@PreAuthorize("hasRole('Admin')")`.
- **CORS Configurado:** Permite orígenes específicos (`http://localhost:4200` y S3 para producción) garantizando que solo el frontend autorizado pueda interactuar con el backend.

## Ejecución Local

Para ejecutar este microservicio en entorno de desarrollo, asegúrate de tener configuradas las credenciales de Azure AD en el archivo `application.properties`:

- **Tenant ID:** `753ae9df-d5ec-4c6b-8227-b334fc775087`
- **Client ID (API):** `749dc676-d557-460a-b138-dac9a6744b6e`

### Comandos de Ejecución

Utiliza el Maven Wrapper incluido en la raíz del proyecto para compilar y ejecutar:

```bash
# Limpiar y compilar el proyecto
./mvnw clean compile

# Ejecutar el servidor de Spring Boot (Puerto 8080)
./mvnw spring-boot:run
```
