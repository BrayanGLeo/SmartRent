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
      } else {
        // Server-side error
        if (error.status === 403) {
          errorMessage = 'No tienes permisos para realizar esta acción (403 Forbidden).';
        } else if (error.status === 500) {
          errorMessage = 'Error interno del servidor (500 Server Error). Intenta más tarde.';
        } else {
          errorMessage = `Código de error: ${error.status}\nMensaje: ${error.message}`;
        }
      }

      // TODO: Usar un servicio de Toast o Snackbar, por ahora usaremos alert para que sea visible
      alert(errorMessage);
      
      return throwError(() => new Error(errorMessage));
    })
  );
};
