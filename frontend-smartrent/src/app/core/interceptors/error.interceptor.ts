import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'Ha ocurrido un error inesperado';
      
      if (error.error instanceof ErrorEvent) {
        // Client-side error
        errorMessage = `Error: ${error.error.message}`;
      } else if (error.status === 401) {
        // 401 se maneja silenciosamente: el componente que hizo la petición
        // decide qué hacer (ej: fallback a datos locales en catálogo).
        return throwError(() => new Error('No autorizado'));
      } else if (error.status === 403) {
        errorMessage = 'No tienes permisos para realizar esta acción (403 Forbidden).';
      } else if (error.status === 500) {
        errorMessage = 'Error interno del servidor (500 Server Error). Intenta más tarde.';
      } else {
        errorMessage = `Código de error: ${error.status}\nMensaje: ${error.message}`;
      }

      // Evitar bloquear con alert en endpoints como reportes, auditoria, usuarios o arriendos (manejados por componentes con Toast)
      if (req.url.includes('/reports') || req.url.includes('/audit') || req.url.includes('/users') || req.url.includes('/rentals')) {
        return throwError(() => error);
      }

      // Nota: Por ahora se usa alert para visibilidad en frontend hasta integrar Toast/Snackbar
      alert(errorMessage);
      
      return throwError(() => new Error(errorMessage));
    })
  );
};
