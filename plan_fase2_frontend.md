# Instrucciones para el Agente Antigravity: Desarrollo Frontend (SmartRent)

> [!IMPORTANT]
> **Contexto para el Agente:** Eres la instancia de Antigravity encargada de desarrollar el Frontend de la plataforma **SmartRent** (arriendo de maquinaria). Otro equipo está desarrollando el Backend (BFF en Spring Boot) en paralelo. Tu objetivo exclusivo es construir la aplicación Angular y configurarla con Microsoft Entra ID (Azure AD) usando MSAL.

## 1. Especificaciones Técnicas y de Diseño
- **Framework:** Angular (Generar usando `npx @angular/cli new frontend-smartrent --style=css --routing=true --skip-git` en el directorio de trabajo actual).
- **Librerías de Autenticación:** `@azure/msal-browser` y `@azure/msal-angular`.
- **Estilos:** Usa Vanilla CSS. Implementa un diseño **Premium y Moderno** (efectos glassmorphism, paleta de colores vibrantes, animaciones suaves, tipografía moderna). La interfaz debe deslumbrar al usuario (como dice tu directiva de Web Application Development).

## 2. Credenciales de Azure AD (¡NO MODIFICAR!)
Debes crear un archivo `src/environments/environment.ts` (y su contraparte de producción) con los siguientes valores exactos provistos por el equipo de Cloud:

```typescript
export const environment = {
  production: false,
  azure: {
    clientId: 'f5890cb5-fba6-429e-b886-323d05ea886c', // ID del Portal (SPA)
    tenantId: '753ae9df-d5ec-4c6b-8227-b334fc775087',
    authority: 'https://753ae9df-d5ec-4c6b-8227-b334fc775087.ciamlogin.com/753ae9df-d5ec-4c6b-8227-b334fc775087/v2.0',
    // La Redirect URI debe ser dinámica para que funcione tanto en localhost como al publicarse en S3
    redirectUri: window.location.origin + window.location.pathname,
    // Scopes de la API para el Interceptor (Client ID del Backend)
    apiScope: 'api://749dc676-d557-460a-b138-dac9a6744b6e/Arriendos.Leer'
  },
  apiBaseUrl: 'http://localhost:8080/api'
};
```

## 3. Configuración Core de MSAL (Angular)
Debes configurar el `MsalModule` (en `app.config.ts` si usas Angular standalone, o `app.module.ts`) con lo siguiente:

1. **Instancia `PublicClientApplication`:** Usa los datos del `environment.ts`. Asegúrate de añadir los `knownAuthorities` (ej. `['753ae9df-d5ec-4c6b-8227-b334fc775087.ciamlogin.com']`) para evitar errores de resolución de endpoints.
2. **`MsalGuard`:** Configúralo para proteger las rutas de la aplicación. Obliga al usuario a loguearse antes de acceder a componentes protegidos.
3. **`MsalInterceptor`:** ¡CRÍTICO! Debes configurar el `protectedResourceMap` para que cualquier petición HTTP que vaya hacia `http://localhost:8080/api/*` intercepte e inyecte automáticamente el token usando el scope `environment.azure.apiScope`.

## 4. Requerimientos de Componentes y Vistas
1. **Componente App (Nav):** Barra de navegación global. Debe mostrar el botón "Iniciar Sesión" (si el usuario no está autenticado) y "Cerrar Sesión" (si lo está). Usa `loginRedirect()` y `logoutRedirect()`.
2. **Componente Home/Login:** Una vista pública de bienvenida y acceso, de diseño atractivo.
3. **Componente Dashboard (Ruta Protegida `/dashboard`):** 
   - Debe usar el `MsalGuard` en el Router.
   - Extrae la información del usuario autenticado leyendo los claims del ID Token (`msalInstance.getAllAccounts()[0].idTokenClaims`).
   - Debe mostrar un saludo personalizado ("Hola, [Nombre]").
   - Muestra el correo electrónico del usuario y **los roles asignados** si los tiene.
   - Incluye un botón para hacer un `fetch` (usando `HttpClient` de Angular) hacia `http://localhost:8080/api/rentals` (simulando cargar órdenes). Aunque la API no exista aún o falle, el `MsalInterceptor` debe adjuntar la cabecera `Authorization: Bearer <token>`.

## 5. Pruebas que debes realizar y evidenciar
Una vez que construyas el proyecto, debes asegurarte de que:
- Al abrir `localhost:4200` y dar a iniciar sesión, redirija a Microsoft.
- Al volver de la autenticación, se renderice el Dashboard protegido.
- Al hacer clic en el botón de llamar a la API, la pestaña "Network" (Red) del navegador demuestre que la petición HTTP salió con la cabecera `Authorization: Bearer <token>`. 

Comienza creando el proyecto Angular y configurando MSAL. No pidas más IDs, ya tienes todos los necesarios.
