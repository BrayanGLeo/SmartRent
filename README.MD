<h1 align="center">
  🚜 SmartRent
</h1>

<p align="center">
  <strong>Sistema Integral de Arriendo de Maquinaria</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Angular-22.x-DD0031?style=for-the-badge&logo=angular&logoColor=white" alt="Angular">
  <img src="https://img.shields.io/badge/Spring_Boot-3.3.4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=java&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Microsoft_Entra_ID-0072C6?style=for-the-badge&logo=microsoft-azure&logoColor=white" alt="Azure AD">
</p>

---

Repositorio principal para la plataforma **SmartRent**, una solución empresarial moderna y escalable desarrollada como parte de la Evaluación de Desarrollo en Cloud Native. 

El proyecto adopta una arquitectura basada en microservicios y *Single Page Applications*, integrando los más altos estándares de seguridad corporativa mediante **Microsoft Entra ID (Azure AD)**.

## 🏗️ Estructura del Proyecto

El ecosistema de SmartRent está dividido en dos componentes principales fuertemente desacoplados:

### 1. 🌐 [Frontend (SPA - Angular)](./frontend-smartrent/)
Aplicación cliente rica e interactiva:
- ⚡ **Tecnologías**: Angular 22, TypeScript, RxJS.
- 🔐 **Autenticación**: Integración nativa con **MSAL (Microsoft Authentication Library)** para flujos interactivos de *Single Sign-On*.
- 🚀 **Propósito**: Proveer la interfaz gráfica (UI) y orquestar las llamadas a la API anexando el token JWT correspondiente.
- 📖 **Documentación**: Consulta el [README del Frontend](./frontend-smartrent/README.md) para más detalles.

### 2. ⚙️ [Backend (BFF - Spring Boot)](./ms-smartrent-bff/)
Servidor de recursos y orquestador backend:
- ☕ **Tecnologías**: Java 17/18, Spring Boot 3.3.4.
- 🛡️ **Seguridad**: Actúa como un **OAuth2 Resource Server** interceptando y validando los JWT emitidos por Azure.
- 🚦 **Propósito**: Exponer los *endpoints* de forma segura mediante políticas CORS estrictas y validación de roles (RBAC).
- 📖 **Documentación**: Consulta el [README del Backend](./ms-smartrent-bff/README.md) para conocer comandos y configuraciones.

---

## 🛡️ Ecosistema de Seguridad (Azure AD)

Toda la plataforma está securizada bajo la suite de identidad de Microsoft. Se han configurado dos *App Registrations* en Azure para establecer una relación de confianza:

* 👤 **Client ID Público (Frontend)**: Encargado de solicitar el inicio de sesión y obtener el token de acceso interactuando con el usuario.
* 🏢 **Client ID Privado (Backend API)**: Define los alcances (*scopes*) delegados y expone los roles de la aplicación (`Admin`, `JefeBodega`, `Arrendatario`), los cuales son validados mediante conversores de *claims* JWT a *authorities* de Spring Security.

## 💻 Dependencias Globales

Para ejecutar este ecosistema en un entorno de desarrollo local, asegúrate de contar con:

- **Frontend:** Node.js (v18+) y NPM (v11+).
- **Backend:** Java Development Kit (JDK 17+) y Maven.

<p align="center">
  <br>
  <i>Hecho con ❤️ para la nube</i>
</p>
