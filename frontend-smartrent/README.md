# Frontend SmartRent (Angular SPA)

Este directorio contiene la aplicación Single Page Application (SPA) desarrollada en **Angular** para el proyecto SmartRent. Esta aplicación sirve como el portal principal para los usuarios (Arrendatarios, Jefes de Bodega, y Administradores).

## Arquitectura y Autenticación

El frontend utiliza **MSAL (Microsoft Authentication Library)** para integrarse directamente con el portal de **Microsoft Entra ID (Azure AD)**. 

### Flujo de Autenticación (Auth Code Flow con PKCE)
1. Cuando un usuario no autenticado intenta acceder al portal, es redirigido a la página de inicio de sesión de Microsoft.
2. Al iniciar sesión correctamente, el usuario recibe un **Access Token** (JWT).
3. El Access Token se adjunta automáticamente en la cabecera `Authorization: Bearer <token>` para todas las peticiones enviadas al Backend (BFF).

## Comandos Útiles

Este proyecto fue generado con [Angular CLI](https://github.com/angular/angular-cli) versión 17 o superior.

### Servidor de Desarrollo
Ejecuta `npm start` o `ng serve` para iniciar el servidor de desarrollo. Navega a `http://localhost:4200/`. La aplicación se recargará automáticamente si cambias alguno de los archivos fuente.

### Construcción (Build)
Ejecuta `npm run build` o `ng build` para compilar el proyecto. Los artefactos compilados se almacenarán en el directorio `dist/`.

### Configuración MSAL requerida
Para que la aplicación funcione correctamente con Azure, se debe tener el siguiente Client ID en el `app.config.ts` o los environments:
- **Client ID (Frontend):** `f5890cb5-fba6-429e-b886-323d05ea886c`
- **Tenant ID:** `753ae9df-d5ec-4c6b-8227-b334fc775087`
