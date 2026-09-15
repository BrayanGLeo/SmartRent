# SmartRent - Sistema de Arriendo de Maquinaria

Repositorio principal para el sistema **SmartRent**, desarrollado como parte de la Evaluación 1 de Desarrollo en Cloud Native. 

El proyecto consta de una arquitectura moderna basada en la nube, separando el Frontend del Backend e integrando seguridad empresarial mediante **Microsoft Entra ID (Azure AD)**.

## Estructura del Proyecto

El repositorio está dividido en dos grandes componentes, cada uno con su propio entorno de desarrollo y dependencias:

### 1. [Frontend (Angular SPA)](./frontend-smartrent/)
Aplicación cliente desarrollada en Angular. 
- Utiliza **MSAL (Microsoft Authentication Library)** para manejar el inicio de sesión contra Azure AD.
- Se encarga de la interfaz gráfica y de enviar el Access Token (JWT) al backend en cada solicitud.
- Entra al directorio `frontend-smartrent` y consulta el [README del Frontend](./frontend-smartrent/README.md) para ver instrucciones de instalación y ejecución.

### 2. [Backend (Spring Boot BFF)](./ms-smartrent-bff/)
Servidor de recursos desarrollado en Java 21 y Spring Boot 3.
- Actúa como **Backend For Frontend (BFF)** y **OAuth2 Resource Server**.
- Intercepta las peticiones del frontend, valida criptográficamente el JWT emitido por Azure y aplica control de acceso con Base en Roles (RBAC).
- Entra al directorio `ms-smartrent-bff` y consulta el [README del Backend](./ms-smartrent-bff/README.md) para ver detalles de los IDs de configuración y comandos de compilación.

---

## Seguridad (Azure AD)

Toda la plataforma está securizada bajo la suite de identidad de Microsoft. Se han configurado dos App Registrations en Azure:
- Un **Client ID Público** para el frontend, que gestiona el login interactivo.
- Un **Client ID Privado** para la API del backend, que define los Roles de la aplicación (Admin, JefeBodega, Arrendatario) y los Scopes delegados.

## Dependencias Globales
- **Frontend:** Node.js, Angular CLI 17+.
- **Backend:** Java 21, Maven (incluido mediante wrapper).
