# SmartRent Frontend

Este proyecto fue generado con [Angular CLI](https://github.com/angular/angular-cli) versión 18.2.0.
Es la aplicación web Frontend de la plataforma **SmartRent** (arriendo de maquinaria), configurada con autenticación a través de Microsoft Entra ID (Azure AD) usando MSAL.

## Servidor de desarrollo

Ejecuta `npm start` o `ng serve` para iniciar el servidor de desarrollo. Navega a `http://localhost:4200/`. La aplicación se recargará automáticamente si realizas cambios en los archivos fuente.

## Construcción (Build)

Ejecuta `npm run build` o `ng build` para compilar el proyecto. Los artefactos de construcción se almacenarán en el directorio `dist/`.

## Características Principales

- **Diseño Moderno:** Interfaz estilizada con CSS puro y efectos de *glassmorphism*.
- **Autenticación (MSAL):** Integración con Microsoft Entra ID para proteger rutas y la aplicación.
- **Intercepción de peticiones (Interceptor):** Inyección automática de token de autorización (`Bearer token`) para llamadas a la API del backend.

## Ejecución de pruebas unitarias

Ejecuta `ng test` para correr las pruebas unitarias a través de [Karma](https://karma-runner.github.io).

## Más ayuda sobre Angular CLI

Para obtener más ayuda sobre el CLI de Angular, usa `ng help` o revisa el [Repositorio de Angular CLI y su documentación](https://github.com/angular/angular-cli).
