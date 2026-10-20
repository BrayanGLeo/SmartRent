import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { concatMap, map } from 'rxjs/operators';
import { getUserRoleFromAccount } from '../services/auth/auth.service';
import { AccessDeniedService } from '../services/modal/access-denied.service';

/**
 * Guard para la ruta /catalog.
 * Espera de manera asíncrona a que MSAL termine de inicializarse para evitar el error
 * "uninitialized_public_client_application".
 * - Usuarios no autenticados: permitido (catálogo público).
 * - Arrendatarios y visitantes: permitido.
 * - Administradores y otros roles internos: deniega el acceso, redirige al Home ('/')
 *   y despliega el modal de acceso denegado.
 */
export const catalogGuard: CanActivateFn = () => {
  const msalService = inject(MsalService);
  const accessDeniedService = inject(AccessDeniedService);
  const router = inject(Router);

  return msalService.initialize().pipe(
    concatMap(() => msalService.handleRedirectObservable()),
    map(() => {
      const accounts = msalService.instance.getAllAccounts();
      if (accounts.length === 0) {
        return true;
      }

      const role = getUserRoleFromAccount(accounts[0]);

      // Arrendatario o rol no reconocido: puede ver el catálogo
      if (role === 'Arrendatario' || role === 'None') {
        return true;
      }

      // Administrador y otros roles internos: denegar acceso y mostrar modal
      accessDeniedService.show('No tiene permiso para acceder a esta página.', '/');

      return router.createUrlTree(['/']);
    })
  );
};
