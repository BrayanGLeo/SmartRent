# SmartRent - Frontend Application

Esta es la aplicación cliente (SPA - Single Page Application) del proyecto SmartRent, diseñada para ofrecer a los usuarios una interfaz dinámica, rápida y segura para la gestión y acceso de recursos.

## 🛠️ Tecnologías y Herramientas

El proyecto está desarrollado con tecnología moderna para aplicaciones web escalables:

- **Angular**: Versión 22.x (Framework progresivo para la construcción de interfaces de usuario).
- **TypeScript**: Superset de JavaScript que añade tipado estático fuerte (v6.0).
- **Microsoft Authentication Library (MSAL)**: `@azure/msal-angular` y `@azure/msal-browser` para la autenticación Single Sign-On (SSO) mediante Azure Active Directory.
- **RxJS**: Programación reactiva y manejo asíncrono de flujos de datos.
- **Vitest & JSDOM**: Framework de testing ultrarrápido empleado para pruebas unitarias.
- **Prettier**: Formateador de código para mantener estilos consistentes.
- **Node.js y NPM**: Entorno de ejecución y gestor de paquetes.

## 🚀 Requisitos Previos

- Node.js (v18 o superior recomendado).
- Gestor de paquetes NPM (v11.x).

## ⚙️ Configuración y Ejecución

Primero, instala las dependencias del proyecto:

```bash
npm install
```

Para iniciar el servidor de desarrollo con recarga en vivo (hot-reload), ejecuta:

```bash
npm start
```
O de manera equivalente:
```bash
ng serve
```

La aplicación estará disponible de forma predeterminada en `http://localhost:4200/`.

## 📦 Construcción para Producción

Para empaquetar la aplicación en un formato optimizado y listo para producción, ejecuta:

```bash
npm run build
```
Los artefactos generados se guardarán en la carpeta de distribución, listos para ser desplegados en un servidor estático como AWS S3 o Azure Blob Storage.
