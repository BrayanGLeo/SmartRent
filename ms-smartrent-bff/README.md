# SmartRent - Backend for Frontend (BFF)

Este es el microservicio **BFF (Backend for Frontend)** de la plataforma SmartRent. Su propósito principal es servir como intermediario entre la aplicación frontend (Angular) y los servicios internos o recursos externos, proporcionando una capa de seguridad y orquestación de datos.

## 🛠️ Tecnologías y Frameworks

El proyecto está desarrollado utilizando el ecosistema de Java y Spring:

- **Java**: Versión 17/18 (Configurado con compatibilidad para Release 17).
- **Spring Boot**: 3.3.4 (Framework principal para la creación del microservicio).
- **Spring Security**: Para el manejo de seguridad, autenticación y autorización.
- **Spring Cloud Azure**: Integración nativa con los servicios de Microsoft Azure.
- **Azure Active Directory (AAD)**: Autenticación OAuth2 delegada y Resource Server (validación de tokens JWT).
- **Maven**: Herramienta de gestión de dependencias y construcción del proyecto.

## 🚀 Requisitos Previos

- Java Development Kit (JDK) 17 o superior.
- Maven 3.8+ (Aunque el proyecto cuenta con el Maven Wrapper `mvnw`).

## ⚙️ Configuración y Ejecución

Para iniciar el servidor de desarrollo, ejecuta el siguiente comando en la raíz del backend:

```bash
./mvnw spring-boot:run
```

El servidor arrancará por defecto en el puerto `8080`.

### Seguridad (CORS y JWT)

La aplicación implementa políticas estrictas de CORS para permitir peticiones del frontend en modo desarrollo y producción (`http://localhost:4200`, `http://localhost:5173`, y entornos S3). 
Asimismo, expone un Resource Server que valida tokens de acceso provenientes de Azure AD mediante un `JwtAuthenticationConverter` personalizado para inyectar roles al contexto de seguridad de Spring.
